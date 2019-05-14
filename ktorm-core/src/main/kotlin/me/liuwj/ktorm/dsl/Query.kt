/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.prepareStatement
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.*
import java.sql.ResultSet
import javax.sql.rowset.RowSetProvider

/**
 * [Query] is an abstraction of query operations and the core class of Ktorm's query DSL.
 *
 * The constructor of this class accepts a parameter of type [QueryExpression], which is the abstract
 * representation of the executing SQL statements. Usually, we don't use the constructor to create query
 * objects but use the [Table.select] extension function instead.
 *
 * [Query] implements the [Iterable] interface, so we can iterate the results by a for-each loop:
 *
 * ```kotlin
 * for (row in Employees.select()) {
 *     println(row[Employees.name])
 * }
 * ```
 *
 * Moreover, there are many extension functions for [Iterable] in Kotlin standard lib, so we can also
 * process the results via functions such as [Iterable.map], [Iterable.filter], [Iterable.reduce], etc.
 *
 * Query objects are immutable. Query DSL functions are provided as its extension functions normally. We can
 * chaining call these functions to modify them and create new query objects. Here is a simple example:
 *
 * ```kotlin
 * val query = Employees
 *     .select(Employees.salary)
 *     .where { (Employees.departmentId eq 1) and (Employees.name like "%vince%") }
 * ```
 *
 * Easy to know that the query obtains the salary of an employee named vince in department 1. The generated
 * SQL is easy too:
 *
 * ```sql
 * select t_employee.salary as t_employee_salary
 * from t_employee
 * where (t_employee.department_id = ?) and (t_employee.name like ?)
 * ```
 *
 * More usages can be found in the documentations of those DSL functions.
 *
 * @property expression the underlying SQL expression of this query object
 */
data class Query(val expression: QueryExpression) : Iterable<QueryRowSet> {

    /**
     * The executable SQL string of this query.
     *
     * Useful when we want to ensure if the generated SQL is expected while debugging.
     */
    val sql: String by lazy(LazyThreadSafetyMode.NONE) {
        Database.global.formatExpression(expression, beautifySql = true).first
    }

    /**
     * The [ResultSet] object of this query, lazy initialized after first access, obtained from the database by
     * executing the generated SQL.
     *
     * Note that the return type of this property is not a normal [ResultSet], but a [QueryRowSet] instead. That's
     * a special implementation provided by Ktorm, different from normal result sets, it is available offline and
     * overrides the indexed access operator. More details can be found in the documentation of [QueryRowSet].
     */
    val rowSet: QueryRowSet by lazy(LazyThreadSafetyMode.NONE) {
        expression.prepareStatement { statement ->
            statement.executeQuery().use { rs ->
                val rowSet = rowSetFactory.createCachedRowSet()
                rowSet.populate(rs)

                val logger = Database.global.logger
                if (logger != null && logger.isDebugEnabled()) {
                    logger.debug("Results: ${rowSet.size()}")
                }

                QueryRowSet(this, rowSet)
            }
        }
    }

    /**
     * The total records count of this query ignoring the pagination params.
     *
     * If the query doesn't limits the results via [Query.limit] function, return the size of the result set. Or if
     * it does, return the total records count of the query ignoring the offset and limit parameters. This property
     * is provided to support pagination, we can calculate the page count through dividing it by out page size.
     */
    val totalRecords: Int by lazy(LazyThreadSafetyMode.NONE) {
        if (expression.offset == null && expression.limit == null) {
            rowSet.size()
        } else {
            val countExpr = expression.toCountExpression()

            countExpr.prepareStatement { statement ->
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        rs.getInt(1).also { total ->
                            val logger = Database.global.logger
                            if (logger != null && logger.isDebugEnabled()) {
                                logger.debug("Total Records: $total")
                            }
                        }
                    } else {
                        val (sql, _) = Database.global.formatExpression(countExpr, beautifySql = true)
                        throw IllegalStateException("No result return for sql: $sql")
                    }
                }
            }
        }
    }

    /**
     * Return an iterator over the rows of this query.
     *
     * Note that this function is simply implemented as `rowSet.iterator()`, so every element returned by the iterator
     * exactly shares the same instance as the [rowSet] property.
     *
     * @see rowSet
     * @see ResultSet.iterator
     */
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
@Suppress("IteratorHasNextCallsNextMethod")
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

inline fun Query.where(block: () -> ColumnDeclaring<Boolean>): Query {
    return this.copy(
        expression = when (expression) {
            is SelectExpression -> expression.copy(where = block().asExpression())
            is UnionExpression -> throw IllegalStateException("Where clause is not supported in a union expression.")
        }
    )
}

inline fun Query.whereWithConditions(block: (MutableList<ColumnDeclaring<Boolean>>) -> Unit): Query {
    val conditions = ArrayList<ColumnDeclaring<Boolean>>().apply(block)

    if (conditions.isEmpty()) {
        return this
    } else {
        return this.where { conditions.reduce { a, b -> a and b } }
    }
}

inline fun Query.whereWithOrConditions(block: (MutableList<ColumnDeclaring<Boolean>>) -> Unit): Query {
    val conditions = ArrayList<ColumnDeclaring<Boolean>>().apply(block)

    if (conditions.isEmpty()) {
        return this
    } else {
        return this.where { conditions.reduce { a, b -> a or b } }
    }
}

fun Iterable<ColumnDeclaring<Boolean>>.combineConditions(): ColumnDeclaring<Boolean> {
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

inline fun Query.having(block: () -> ColumnDeclaring<Boolean>): Query {
    return this.copy(
        expression = when (expression) {
            is SelectExpression -> expression.copy(having = block().asExpression())
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
