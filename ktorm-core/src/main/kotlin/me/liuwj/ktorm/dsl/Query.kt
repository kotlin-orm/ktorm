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
 *
 * @property expression 该查询的 SQL 表达式
 */
data class Query(val expression: QueryExpression) : Iterable<QueryRowSet> {

    /**
     * 返回该查询的 SQL 字符串，提供换行、缩进支持，可在 debug 时确认所生成的 SQL 是否符合预期
     */
    val sql: String by lazy {
        Database.global.formatExpression(expression, beautifySql = true).first
    }

    /**
     * 该查询的结果集对象，懒初始化，在通过 [Iterable] 对查询进行迭代的时候执行 SQL 获取结果集
     */
    val rowSet: QueryRowSet by lazy {
        expression.prepareStatement { statement, logger ->
            statement.executeQuery().use { rs ->
                val rowSet = rowSetFactory.createCachedRowSet()
                rowSet.populate(rs)
                QueryRowSet(this, rowSet).apply { logger.debug("Results: {}", size()) }
            }
        }
    }

    /**
     * 获取符合该查询条件（去除 offset, limit）的总记录数，用于支持分页
     */
    val totalRecords: Int by lazy {
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

fun QuerySourceExpression.select(columns: Collection<ColumnDeclaring<*>>): Query {
    val declarations = columns.map { it.asDeclaringExpression() }
    return Query(SelectExpression(columns = declarations, from = this))
}

fun QuerySourceExpression.select(vararg columns: ColumnDeclaring<*>): Query {
    return select(columns.asList())
}

fun Table<*>.select(columns: Collection<ColumnDeclaring<*>>): Query {
    return asExpression().select(columns)
}

fun Table<*>.select(vararg columns: ColumnDeclaring<*>): Query {
    return asExpression().select(columns.asList())
}

fun QuerySourceExpression.selectDistinct(columns: Collection<ColumnDeclaring<*>>): Query {
    val declarations = columns.map { it.asDeclaringExpression() }
    return Query(SelectExpression(columns = declarations, from = this, isDistinct = true))
}

fun QuerySourceExpression.selectDistinct(vararg columns: ColumnDeclaring<*>): Query {
    return selectDistinct(columns.asList())
}

fun Table<*>.selectDistinct(columns: Collection<ColumnDeclaring<*>>): Query {
    return asExpression().selectDistinct(columns)
}

fun Table<*>.selectDistinct(vararg columns: ColumnDeclaring<*>): Query {
    return asExpression().selectDistinct(columns.asList())
}

inline fun Query.where(block: () -> ScalarExpression<Boolean>): Query {
    return this.copy(
        expression = when (expression) {
            is SelectExpression -> expression.copy(where = block())
            is UnionExpression -> throw IllegalStateException("Where clause is not supported in a union expression.")
        }
    )
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
    return this.copy(
        expression = when (expression) {
            is SelectExpression -> expression.copy(groupBy = columns.map { it.asExpression() })
            is UnionExpression -> throw IllegalStateException("Group by clause is not supported in a union expression.")
        }
    )
}

inline fun Query.having(block: () -> ScalarExpression<Boolean>): Query {
    return this.copy(
        expression = when (expression) {
            is SelectExpression -> expression.copy(having = block())
            is UnionExpression -> throw IllegalStateException("Having clause is not supported in a union expression.")
        }
    )
}

fun Query.orderBy(vararg orders: OrderByExpression): Query {
    return this.copy(
        expression = when (expression) {
            is SelectExpression -> expression.copy(orderBy = orders.asList())
            is UnionExpression -> {
                val replacer = OrderByReplacer(expression)
                expression.copy(orderBy = orders.map { replacer.visit(it) as OrderByExpression })
            }
        }
    )
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

fun Query.limit(offset: Int, limit: Int): Query {
    if (offset == 0 && limit == 0) {
        return this
    }

    return this.copy(
        expression = when (expression) {
            is SelectExpression -> expression.copy(offset = offset, limit = limit)
            is UnionExpression -> expression.copy(offset = offset, limit = limit)
        }
    )
}

fun Query.union(right: Query): Query {
    return this.copy(expression = UnionExpression(left = expression, right = right.expression, isUnionAll = false))
}

fun Query.unionAll(right: Query): Query {
    return this.copy(expression = UnionExpression(left = expression, right = right.expression, isUnionAll = true))
}