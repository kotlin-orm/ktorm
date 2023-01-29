/*
 * Copyright 2018-2023 the original author or authors.
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

package org.ktorm.expression

import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.SqlType

/**
 * Root class of SQL expressions or statements.
 *
 * SQL expressions are tree structures, and can be regarded as SQL abstract syntax trees (AST).
 *
 * Subclasses must satisfy the following rules:
 *
 * - Must be data class, providing common abilities such as destruction, `copy` function, `equals` function, etc.
 * - Must be immutable, any modify operation should return a new expression instance.
 *
 * To visit or modify expression trees, use [SqlExpressionVisitor].
 *
 * To format expressions as executable SQL strings, use [SqlFormatter].
 *
 * @see SqlExpressionVisitor
 * @see SqlFormatter
 */
@Suppress("UnnecessaryAbstractClass")
public abstract class SqlExpression {

    /**
     * Check if this expression is a leaf node in expression trees.
     */
    public abstract val isLeafNode: Boolean

    /**
     * Extra properties of this expression, maybe useful in [SqlFormatter] to generate some special SQLs.
     */
    public abstract val extraProperties: Map<String, Any>
}

/**
 * Base class of scalar expressions. An expression is "scalar" if it has a return value (eg. `a + 1`).
 *
 * @param T the return value's type of this scalar expression.
 */
public abstract class ScalarExpression<T : Any> : SqlExpression(), ColumnDeclaring<T> {

    abstract override val sqlType: SqlType<T>

    override fun asExpression(): ScalarExpression<T> {
        return this
    }

    override fun aliased(label: String?): ColumnDeclaringExpression<T> {
        return ColumnDeclaringExpression(this, label)
    }

    override fun wrapArgument(argument: T?): ArgumentExpression<T> {
        return ArgumentExpression(argument, sqlType)
    }
}

/**
 * Query source expression, used in the `from` clause of a [SelectExpression].
 */
@Suppress("UnnecessaryAbstractClass")
public abstract class QuerySourceExpression : SqlExpression()

/**
 * Base class of query expressions, provide common properties for [SelectExpression] and [UnionExpression].
 *
 * @property orderBy a list of order-by expressions, used in the `order by` clause of a query.
 * @property offset the offset of the first returned record.
 * @property limit max record numbers returned by the query.
 * @property tableAlias the alias when this query is nested in another query's source, eg. `select * from (...) alias`.
 */
public sealed class QueryExpression : QuerySourceExpression() {
    public abstract val orderBy: List<OrderByExpression>
    public abstract val offset: Int?
    public abstract val limit: Int?
    public abstract val tableAlias: String?
    final override val isLeafNode: Boolean = false
}

/**
 * Select expression, represents a `select` statement of SQL.
 *
 * @property columns the selected column declarations, empty means `select *`.
 * @property from the query's source, represents the `from` clause of SQL.
 * @property where the filter condition, represents the `where` clause of SQL.
 * @property groupBy the grouping conditions, represents the `group by` clause of SQL.
 * @property having the having condition, represents the `having` clause of SQL.
 * @property isDistinct mark if this query is distinct, true means the SQL is `select distinct ...`.
 */
public data class SelectExpression(
    val columns: List<ColumnDeclaringExpression<*>> = emptyList(),
    val from: QuerySourceExpression,
    val where: ScalarExpression<Boolean>? = null,
    val groupBy: List<ScalarExpression<*>> = emptyList(),
    val having: ScalarExpression<Boolean>? = null,
    val isDistinct: Boolean = false,
    override val orderBy: List<OrderByExpression> = emptyList(),
    override val offset: Int? = null,
    override val limit: Int? = null,
    override val tableAlias: String? = null,
    override val extraProperties: Map<String, Any> = emptyMap()
) : QueryExpression()

/**
 * Union expression, represents a `union` statement of SQL.
 *
 * @property left the left query of this union expression.
 * @property right the right query of this union expression.
 * @property isUnionAll mark if this union statement is `union all`.
 */
public data class UnionExpression(
    val left: QueryExpression,
    val right: QueryExpression,
    val isUnionAll: Boolean,
    override val orderBy: List<OrderByExpression> = emptyList(),
    override val offset: Int? = null,
    override val limit: Int? = null,
    override val tableAlias: String? = null,
    override val extraProperties: Map<String, Any> = emptyMap()
) : QueryExpression()

/**
 * Insert expression, represents the `insert` statement in SQL.
 *
 * @property table the table to be inserted.
 * @property assignments column assignments of the insert statement.
 */
public data class InsertExpression(
    val table: TableExpression,
    val assignments: List<ColumnAssignmentExpression<*>>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * Insert-from-query expression, eg. `insert into tmp(num) select 1 from dual`.
 *
 * @property table the table to be inserted.
 * @property columns the columns to be inserted.
 * @property query the query expression.
 */
public data class InsertFromQueryExpression(
    val table: TableExpression,
    val columns: List<ColumnExpression<*>>,
    val query: QueryExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * Update expression, represents the `update` statement in SQL.
 *
 * @property table the table to be updated.
 * @property assignments column assignments of the update statement.
 * @property where the update condition.
 */
public data class UpdateExpression(
    val table: TableExpression,
    val assignments: List<ColumnAssignmentExpression<*>>,
    val where: ScalarExpression<Boolean>? = null,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * Delete expression, represents the `delete` statement in SQL.
 *
 * @property table the table to be deleted.
 * @property where the condition.
 */
public data class DeleteExpression(
    val table: TableExpression,
    val where: ScalarExpression<Boolean>?,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * The enum of joining types in a [JoinExpression].
 */
public enum class JoinType(private val value: String) {

    /**
     * Cross join, translated to the `cross join` keyword in SQL.
     */
    CROSS_JOIN("cross join"),

    /**
     * Inner join, translated to the `inner join` keyword in SQL.
     */
    INNER_JOIN("inner join"),

    /**
     * Left join, translated to the `left join` keyword in SQL.
     */
    LEFT_JOIN("left join"),

    /**
     * Right join, translated to the `right join` keyword in SQL.
     */
    RIGHT_JOIN("right join"),

    /**
     * Full join, translated to the `full join` keyword in SQL.
     */
    FULL_JOIN("full join");

    override fun toString(): String {
        return value
    }
}

/**
 * Join expression.
 *
 * @property type the expression's type.
 * @property left the left table.
 * @property right the right table.
 * @property condition the joining condition.
 */
public data class JoinExpression(
    val type: JoinType,
    val left: QuerySourceExpression,
    val right: QuerySourceExpression,
    val condition: ScalarExpression<Boolean>? = null,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : QuerySourceExpression()

/**
 * Table expression.
 *
 * @property name the table's name.
 * @property tableAlias the table's alias.
 * @property catalog the table's catalog.
 * @property schema the table's schema.
 */
public data class TableExpression(
    val name: String,
    val tableAlias: String? = null,
    val catalog: String? = null,
    val schema: String? = null,
    override val isLeafNode: Boolean = true,
    override val extraProperties: Map<String, Any> = emptyMap()
) : QuerySourceExpression()

/**
 * Column expression.
 *
 * @property table the owner table.
 * @property name the column's name.
 */
public data class ColumnExpression<T : Any>(
    val table: TableExpression?,
    val name: String,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * Column declaring expression, represents the selected columns in a [SelectExpression].
 *
 * For example, `select a.name as label from dual`, `a.name as label` is a column declaring.
 *
 * @property expression the source expression, might be a [ColumnExpression] or other scalar expression types.
 * @property declaredName the declaring label.
 */
public data class ColumnDeclaringExpression<T : Any>(
    val expression: ScalarExpression<T>,
    val declaredName: String? = null,
    override val sqlType: SqlType<T> = expression.sqlType,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>() {

    override fun aliased(label: String?): ColumnDeclaringExpression<T> {
        return this.copy(declaredName = label)
    }
}

/**
 * Column assignment expression, represents a column assignment for insert or update statements.
 *
 * @property column the left value of the assignment.
 * @property expression the right value of the assignment, might be an [ArgumentExpression] or other scalar expressions.
 */
public data class ColumnAssignmentExpression<T : Any>(
    val column: ColumnExpression<T>,
    val expression: ScalarExpression<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * The enum of order directions in a [OrderByExpression].
 */
public enum class OrderType(private val value: String) {

    /**
     * The ascending order direction.
     */
    ASCENDING("asc"),

    /**
     * The descending order direction.
     */
    DESCENDING("desc");

    override fun toString(): String {
        return value
    }
}

/**
 * Order-by expression.
 *
 * @property expression the sorting column, might be a [ColumnExpression] or other scalar expression types.
 * @property orderType the sorting direction.
 */
public data class OrderByExpression(
    val expression: ScalarExpression<*>,
    val orderType: OrderType,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * Enum for unary expressions.
 */
public enum class UnaryExpressionType(private val value: String) {

    /**
     * Check if a column or expression is null.
     */
    IS_NULL("is null"),

    /**
     * Check if a column or expression is not null.
     */
    IS_NOT_NULL("is not null"),

    /**
     * Unary minus operator, translated to `-` in SQL.
     */
    UNARY_MINUS("-"),

    /**
     * Unary plus operator, translated to `+` in SQL.
     */
    UNARY_PLUS("+"),

    /**
     * Negate operator, translated to the `not` keyword in SQL.
     */
    NOT("not");

    override fun toString(): String {
        return value
    }
}

/**
 * Unary expression.
 *
 * @property type the expression's type.
 * @property operand the expression's operand.
 */
public data class UnaryExpression<T : Any>(
    val type: UnaryExpressionType,
    val operand: ScalarExpression<*>,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * Enum for binary expressions.
 */
public enum class BinaryExpressionType(private val value: String) {

    /**
     * Plus operator, translated to `+` in SQL.
     */
    PLUS("+"),

    /**
     * Minus operator, translated to `-` in SQL.
     */
    MINUS("-"),

    /**
     * Multiply operator, translated to `*` in SQL.
     */
    TIMES("*"),

    /**
     * Divide operator, translated to `/` in SQL.
     */
    DIV("/"),

    /**
     * Mod operator, translated to `%` in SQL.
     */
    REM("%"),

    /**
     * Like operator, translated to the `like` keyword in SQL.
     */
    LIKE("like"),

    /**
     * Not like operator, translated to the `not like` keyword in SQL.
     */
    NOT_LIKE("not like"),

    /**
     * And operator, translated to the `and` keyword in SQL.
     */
    AND("and"),

    /**
     * Or operator, translated to the `or` keyword in SQL.
     */
    OR("or"),

    /**
     * Xor operator, translated to the `xor` keyword in SQL.
     */
    XOR("xor"),

    /**
     * Less operator, translated to `<` in SQL.
     */
    LESS_THAN("<"),

    /**
     * Less-eq operator, translated to `<=` in SQL.
     */
    LESS_THAN_OR_EQUAL("<="),

    /**
     * Greater operator, translated to `>` in SQL.
     */
    GREATER_THAN(">"),

    /**
     * Greater-eq operator, translated to `>=` in SQL.
     */
    GREATER_THAN_OR_EQUAL(">="),

    /**
     * Equal operator, translated to `=` in SQL.
     */
    EQUAL("="),

    /**
     * Not-equal operator, translated to `<>` in SQL.
     */
    NOT_EQUAL("<>");

    override fun toString(): String {
        return value
    }
}

/**
 * Binary expression.
 *
 * @property type the expression's type.
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
public data class BinaryExpression<T : Any>(
    val type: BinaryExpressionType,
    val left: ScalarExpression<*>,
    val right: ScalarExpression<*>,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * Argument expression, wraps an argument passed to the executed SQL.
 *
 * @property value the argument value.
 * @property sqlType the argument's [SqlType].
 */
public data class ArgumentExpression<T : Any>(
    val value: T?,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = true,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * Wrap a SQL expression, changing its return type, translated to SQl cast(expr as type).
 *
 * @property expression the wrapped expression.
 */
public data class CastingExpression<T : Any>(
    val expression: SqlExpression,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * In-list expression, translated to the `in` keyword in SQL.
 *
 * @property left the expression's left operand.
 * @property query the expression's right operand query, cannot be used along with the [values] property.
 * @property values the expression's right operand collection, cannot be used along with the [query] property.
 * @property notInList mark if this expression is translated to `not in`.
 */
public data class InListExpression(
    val left: ScalarExpression<*>,
    val query: QueryExpression? = null,
    val values: List<ScalarExpression<*>>? = null,
    val notInList: Boolean = false,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<Boolean>()

/**
 * Exists expression, check if the specific query has at least one result.
 *
 * @property query the query expression.
 * @property notExists mark if this expression is translated to `not exists`.
 */
public data class ExistsExpression(
    val query: QueryExpression,
    val notExists: Boolean = false,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<Boolean>()

/**
 * Between expression, check if a scalar expression is in the given range.
 *
 * @property expression the left operand.
 * @property lower the lower bound of the range.
 * @property upper the upper bound of the range.
 * @property notBetween mark if this expression is translated to `not between`.
 */
public data class BetweenExpression(
    val expression: ScalarExpression<*>,
    val lower: ScalarExpression<*>,
    val upper: ScalarExpression<*>,
    val notBetween: Boolean = false,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<Boolean>()

/**
 * Case-when expression, represents a SQL case-when clause.
 *
 * There are two kind of case-when clauses in SQL, one is simple case-when clause, which has an operand following
 * the `case` keyword, for example:
 *
 * ```sql
 * case operand when a then 1 when b then 2 else 3
 * ```
 *
 * The other is searched case-when clause, which doesn't have an operand, for example:
 *
 * ```sql
 * case when a = 1 then 1 when b = 2 then 2 else 3
 * ```
 *
 * See the SQL BNF Grammar https://ronsavage.github.io/SQL/sql-2003-2.bnf.html#case%20expression
 *
 * @property operand the case operand, might be null for simple case-when clauses.
 * @property whenClauses pairs of when clauses and their results.
 * @property elseClause the result in case no when clauses are matched.
 * @since 3.6.0
 */
public data class CaseWhenExpression<T : Any>(
    val operand: ScalarExpression<*>?,
    val whenClauses: List<Pair<ScalarExpression<*>, ScalarExpression<T>>>,
    val elseClause: ScalarExpression<T>?,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap(),
) : ScalarExpression<T>()

/**
 * Function expression, represents a normal SQL function call.
 *
 * @property functionName the name of the SQL function.
 * @property arguments arguments passed to the function.
 */
public data class FunctionExpression<T : Any>(
    val functionName: String,
    val arguments: List<ScalarExpression<*>>,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * The enum of aggregate functions in a [AggregateExpression].
 */
public enum class AggregateType(private val value: String) {

    /**
     * The min function, translated to `min(column)` in SQL.
     */
    MIN("min"),

    /**
     * The max function, translated to `max(column)` in SQL.
     */
    MAX("max"),

    /**
     * The avg function, translated to `avg(column)` in SQL.
     */
    AVG("avg"),

    /**
     * The sum function, translated to `sum(column)` in SQL.
     */
    SUM("sum"),

    /**
     * The count function, translated to `count(column)` in SQL.
     */
    COUNT("count");

    override fun toString(): String {
        return value
    }
}

/**
 * Aggregate expression.
 *
 * @property type the expression's type.
 * @property argument the aggregated column, might be a [ColumnExpression] or other scalar expression types.
 * @property isDistinct mark if this aggregation is distinct.
 */
public data class AggregateExpression<T : Any>(
    val type: AggregateType,
    val argument: ScalarExpression<*>?,
    val isDistinct: Boolean = false,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * The enum of window function type.
 *
 * @since 3.6.0
 */
public enum class WindowFunctionType(private val value: String) {
    // aggregate
    /**
     * The min function, translated to `min(column)` in SQL.
     */
    MIN("min"),

    /**
     * The max function, translated to `max(column)` in SQL.
     */
    MAX("max"),

    /**
     * The avg function, translated to `avg(column)` in SQL.
     */
    AVG("avg"),

    /**
     * The sum function, translated to `sum(column)` in SQL.
     */
    SUM("sum"),

    /**
     * The count function, translated to `count(column)` in SQL.
     */
    COUNT("count"),

    // non-aggregate

    /**
     * The row_number function, translated to `row_number()` in SQL.
     */
    ROW_NUMBER("row_number"),

    /**
     * The rank function, translated to `rank()` in SQL.
     */
    RANK("rank"),

    /**
     * The dense_rank function, translated to `dense_rank()` in SQL.
     */
    DENSE_RANK("dense_rank"),

    /**
     * The percent_rank function, translated to `percent_rank()` in SQL.
     */
    PERCENT_RANK("percent_rank"),

    /**
     * The cume_dist function, translated to `cume_dist()` in SQL.
     */
    CUME_DIST("cume_dist"),

    /**
     * The lag function, translated to `lag(column, offset, default_value)` in SQL.
     */
    LAG("lag"),

    /**
     * The lead function, translated to `lead(column, offset, default_value)` in SQL.
     */
    LEAD("lead"),

    /**
     * The first_value function, translated to `first_value(column)` in SQL.
     */
    FIRST_VALUE("first_value"),

    /**
     * The last_value function, translated to `last_value(column)` in SQL.
     */
    LAST_VALUE("last_value"),

    /**
     * The nth_value function, translated to `nth_value(column, n)` in SQL.
     */
    NTH_VALUE("nth_value"),

    /**
     * The ntile function, translated to `ntile(n)` in SQL.
     */
    NTILE("ntile");

    override fun toString(): String {
        return value
    }
}

/**
 * Window function expression, represents a SQL window function call.
 *
 * @property type the type of the window function.
 * @property arguments the arguments passed to the window function.
 * @property isDistinct mark if this function is distinct.
 * @property window the window specification.
 * @since 3.6.0
 */
public data class WindowFunctionExpression<T : Any>(
    val type: WindowFunctionType,
    val arguments: List<ScalarExpression<*>>,
    val isDistinct: Boolean = false,
    val window: WindowSpecificationExpression = WindowSpecificationExpression(),
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * Window specification expression.
 *
 * @property partitionBy partition-by clause indicates how to divide the query rows into groups.
 * @property orderBy order-by clause indicates how to sort rows in each partition.
 * @property frameUnit frame unit indicates the type of relationship between the current row and frame rows.
 * @property frameStart start bound of the window frame.
 * @property frameEnd end bound of the window frame.
 * @since 3.6.0
 */
public data class WindowSpecificationExpression(
    val partitionBy: List<ScalarExpression<*>> = emptyList(),
    val orderBy: List<OrderByExpression> = emptyList(),
    val frameUnit: WindowFrameUnitType? = null,
    val frameStart: WindowFrameBoundExpression? = null,
    val frameEnd: WindowFrameBoundExpression? = null,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * The enum type of window frame unit.
 *
 * @since 3.6.0
 */
public enum class WindowFrameUnitType(private val value: String) {

    /**
     * The frame is defined by beginning and ending row positions.
     * Offsets are differences in row numbers from the current row number.
     */
    ROWS("rows"),

    /**
     * The frame is defined by rows within a value range.
     * Offsets are differences in row values from the current row value.
     */
    RANGE("range");

    override fun toString(): String {
        return value
    }
}

/**
 * The enum type of window frame bound.
 *
 * @since 3.6.0
 */
public enum class WindowFrameBoundType(private val value: String) {

    /**
     * For ROWS, the bound is the current row. For RANGE, the bound is the peers of the current row.
     */
    CURRENT_ROW("current row"),

    /**
     * The bound is the first partition row.
     */
    UNBOUNDED_PRECEDING("unbounded preceding"),

    /**
     * The bound is the last partition row.
     */
    UNBOUNDED_FOLLOWING("unbounded following"),

    /**
     * For ROWS, the bound is N rows before the current row. For RANGE, the bound is the rows with values equal to
     * the current row value minus N; if the current row value is NULL, the bound is the peers of the row.
     */
    PRECEDING("preceding"),

    /**
     * For ROWS, the bound is N rows after the current row. For RANGE, the bound is the rows with values equal to
     * the current row value plus N; if the current row value is NULL, the bound is the peers of the row.
     */
    FOLLOWING("following");

    override fun toString(): String {
        return value
    }
}

/**
 * Window frame bound expression.
 *
 * @property type frame bound type.
 * @property argument argument for the frame bound.
 * @since 3.6.0
 */
public data class WindowFrameBoundExpression(
    val type: WindowFrameBoundType,
    val argument: ScalarExpression<*>?,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()
