package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.prepareStatement
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.*
import java.sql.ResultSet
import javax.sql.rowset.RowSetProvider

/**
 * [Query] 对象表示一个查询操作，此类实现了 [Iterable] 接口，因此支持使用 for 循环迭代查询结果集中的数据，
 * 也天然支持了针对 [Iterable] 的一系列 Kotlin 扩展函数，如 map, filter, associateBy 等
 */
class Query(private var _expr: QueryExpression) : Iterable<QueryRowSet> {

    /**
     * 获取该查询的 SQL 表达式
     */
    val expression: QueryExpression get() = _expr

    /**
     * 返回该查询的 SQL 字符串，提供换行、缩进支持，可在 debug 时确认所生成的 SQL 是否符合预期
     */
    val sql: String get() = Database.global.formatExpression(expression, beautifySql = true).first

    /**
     * 执行查询，获取结果集
     */
    private val _rowSetLazy = lazy(LazyThreadSafetyMode.NONE) {
        expression.prepareStatement { statement, logger ->
            statement.executeQuery().use { rs ->
                val rowSet = rowSetFactory.createCachedRowSet()
                rowSet.populate(rs)
                QueryRowSet(this, rowSet).apply { logger.debug("Results: {}", size()) }
            }
        }
    }

    /**
     * 该查询的结果集对象，懒初始化，在通过 [Iterable] 对查询进行迭代的时候执行 SQL 获取结果集
     */
    val rowSet: QueryRowSet by _rowSetLazy

    /**
     * 获取符合该查询条件（去除 offset, limit）的总记录数，用于支持分页
     */
    val totalRecords: Int by lazy(LazyThreadSafetyMode.NONE) {
        // Execute the query first to stop the modification on it...
        val rowSet = this.rowSet
        val expression = this.expression

        if (expression.offset == null && expression.limit == null) {
            rowSet.size()
        } else {
            val countExpr = SelectExpression(
                columns = listOf(
                    ColumnDeclaringExpression(
                        expression = AggregateExpression(
                            type = AggregateType.COUNT,
                            argument = null,
                            isDistinct = false,
                            sqlType = IntSqlType
                        )
                    )
                ),
                from = when (expression) {
                    is SelectExpression -> expression.copy(offset = null, limit = null, tableAlias = "tmp_count")
                    is UnionExpression -> expression.copy(offset = null, limit = null, tableAlias = "tmp_count")
                }
            )

            countExpr.prepareStatement { statement, logger ->
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        rs.getInt(1).also { logger.debug("Total Records: {}", it) }
                    } else {
                        val (sql, _) = Database.global.formatExpression(countExpr, beautifySql = true)
                        throw IllegalStateException("No result return for sql: $sql")
                    }
                }
            }
        }
    }

    /**
     * 提供给框架内部使用，用于修改该查询的 SQL 表达式
     */
    internal fun withExpression(func: (QueryExpression) -> QueryExpression): Query {
        if (_rowSetLazy.isInitialized()) {
            throw UnsupportedOperationException("Cannot modify a query after it's executed.")
        }

        _expr = func(_expr)
        return this
    }

    override fun iterator(): Iterator<QueryRowSet> {
        return rowSet.iterator()
    }

    companion object {
        private val rowSetFactory = RowSetProvider.newFactory()
    }
}

/**
 * 返回 [ResultSet] 的迭代器
 */
operator fun <T : ResultSet> T.iterator() = object : Iterator<T> {
    private val rs = this@iterator
    private var hasNext: Boolean? = null

    override fun hasNext(): Boolean {
        return hasNext ?: rs.next().also { hasNext = it }
    }

    override fun next(): T {
        return if (hasNext()) rs.also { hasNext = null } else throw NoSuchElementException()
    }
}

/**
 * 将 [ResultSet] 转换为 [Iterable] 对象，以支持 Kotlin 提供的 map, filter 等扩展函数的使用
 */
fun <T : ResultSet> T.iterable(): Iterable<T> {
    return Iterable { iterator() }
}

fun SqlExpression.select(columns: Collection<ColumnDeclaring<*>>): Query {
    val declarations = columns.map { it.asDeclaringExpression() }
    return Query(SelectExpression(columns = declarations, from = this))
}

fun SqlExpression.select(vararg columns: ColumnDeclaring<*>): Query {
    return select(columns.asList())
}

fun Table<*>.select(columns: Collection<ColumnDeclaring<*>>): Query {
    return asExpression().select(columns)
}

fun Table<*>.select(vararg columns: ColumnDeclaring<*>): Query {
    return asExpression().select(columns.asList())
}

fun SqlExpression.selectDistinct(columns: Collection<ColumnDeclaring<*>>): Query {
    val declarations = columns.map { it.asDeclaringExpression() }
    return Query(SelectExpression(columns = declarations, from = this, isDistinct = true))
}

fun SqlExpression.selectDistinct(vararg columns: ColumnDeclaring<*>): Query {
    return selectDistinct(columns.asList())
}

fun Table<*>.selectDistinct(columns: Collection<ColumnDeclaring<*>>): Query {
    return asExpression().selectDistinct(columns)
}

fun Table<*>.selectDistinct(vararg columns: ColumnDeclaring<*>): Query {
    return asExpression().selectDistinct(columns.asList())
}

fun Query.where(block: () -> ScalarExpression<Boolean>): Query {
    return this.withExpression { expr ->
        when (expr) {
            is SelectExpression -> expr.copy(where = block())
            is UnionExpression -> throw IllegalStateException("Where clause is not supported in a union expression.")
        }
    }
}

inline fun Query.whereWithConditions(block: (MutableList<ScalarExpression<Boolean>>) -> Unit): Query {
    val conditions = ArrayList<ScalarExpression<Boolean>>().apply(block)

    if (conditions.isEmpty()) {
        return this
    } else {
        return this.where { conditions.reduce { a, b -> a and b } }
    }
}

inline fun Query.whereWithOrConditions(block: (MutableList<ScalarExpression<Boolean>>) -> Unit): Query {
    val conditions = ArrayList<ScalarExpression<Boolean>>().apply(block)

    if (conditions.isEmpty()) {
        return this
    } else {
        return this.where { conditions.reduce { a, b -> a or b } }
    }
}

fun Iterable<ScalarExpression<Boolean>>.combineConditions(): ScalarExpression<Boolean> {
    if (this.any()) {
        return this.reduce { a, b -> a and b }
    } else {
        return ArgumentExpression(true, BooleanSqlType)
    }
}

fun Query.groupBy(vararg columns: ColumnDeclaring<*>): Query {
    return this.withExpression { expr ->
        when (expr) {
            is SelectExpression -> expr.copy(groupBy = columns.map { it.asExpression() })
            is UnionExpression -> throw IllegalStateException("Group by clause is not supported in a union expression.")
        }
    }
}

fun Query.having(block: () -> ScalarExpression<Boolean>): Query {
    return this.withExpression { expr ->
        when (expr) {
            is SelectExpression -> expr.copy(having = block())
            is UnionExpression -> throw IllegalStateException("Having clause is not supported in a union expression.")
        }
    }
}

fun Query.orderBy(vararg orders: OrderByExpression): Query {
    return this.withExpression { expr ->
        when (expr) {
            is SelectExpression -> expr.copy(orderBy = orders.asList())
            is UnionExpression -> {
                val replacer = OrderByReplacer(expr)
                expr.copy(orderBy = orders.map { replacer.visit(it) as OrderByExpression })
            }
        }
    }
}

private class OrderByReplacer(query: UnionExpression) : SqlExpressionVisitor() {
    val declaringColumns = query.findDeclaringColumns()

    override fun visitOrderBy(expr: OrderByExpression): OrderByExpression {
        val declaring = declaringColumns.find { it.declaredName != null && it.expression == expr.expression }

        if (declaring == null) {
            throw IllegalArgumentException("Could not find the ordering column in the union expression, column: $expr")
        } else {
            return OrderByExpression(
                expression = ColumnExpression(
                    tableAlias = null,
                    name = declaring.declaredName!!,
                    sqlType = declaring.expression.sqlType
                ),
                orderType = expr.orderType
            )
        }
    }
}

fun ColumnDeclaring<*>.asc(): OrderByExpression {
    return OrderByExpression(asExpression(), OrderType.ASCENDING)
}

fun ColumnDeclaring<*>.desc(): OrderByExpression {
    return OrderByExpression(asExpression(), OrderType.DESCENDING)
}

fun <C : Number> min(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MIN, column.asExpression(), false, column.sqlType)
}

fun <C : Number> minDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MIN, column.asExpression(), true, column.sqlType)
}

fun <C : Number> max(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MAX, column.asExpression(), false, column.sqlType)
}

fun <C : Number> maxDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MAX, column.asExpression(), true, column.sqlType)
}

fun <C : Number> avg(column: ColumnDeclaring<C>): AggregateExpression<Double> {
    return AggregateExpression(AggregateType.AVG, column.asExpression(), false, DoubleSqlType)
}

fun <C : Number> avgDistinct(column: ColumnDeclaring<C>): AggregateExpression<Double> {
    return AggregateExpression(AggregateType.AVG, column.asExpression(), true, DoubleSqlType)
}

fun <C : Number> sum(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.SUM, column.asExpression(), false, column.sqlType)
}

fun <C : Number> sumDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.SUM, column.asExpression(), true, column.sqlType)
}

fun <C : Any> count(column: ColumnDeclaring<C>): AggregateExpression<Int> {
    return AggregateExpression(AggregateType.COUNT, column.asExpression(), false, IntSqlType)
}

fun <C : Any> countDistinct(column: ColumnDeclaring<C>): AggregateExpression<Int> {
    return AggregateExpression(AggregateType.COUNT, column.asExpression(), true, IntSqlType)
}

fun Query.limit(offset: Int, limit: Int): Query {
    if (offset == 0 && limit == 0) {
        return this
    }

    return this.withExpression { expr ->
        when (expr) {
            is SelectExpression -> expr.copy(offset = offset, limit = limit)
            is UnionExpression -> expr.copy(offset = offset, limit = limit)
        }
    }
}

fun Query.union(right: Query): Query {
    return this.withExpression { UnionExpression(left = expression, right = right.expression, isUnionAll = false) }
}

fun Query.unionAll(right: Query): Query {
    return this.withExpression { UnionExpression(left = expression, right = right.expression, isUnionAll = true) }
}