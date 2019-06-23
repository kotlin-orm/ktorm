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

package me.liuwj.ktorm.expression

/**
 * Base class designed to visit or modify SQL expression trees using visitor pattern.
 *
 * This class provides a general [visit] function to dispatch different type of expression nodes to the specific
 * `visit*` functions. Custom expression types that are unknown to Ktorm will be dispatched to [visitUnknown].
 *
 * For each expression type, there is a corresponding `visit*` function in this class; for [SelectExpression], it's
 * [visitSelect]; for [TableExpression], it's [visitTable]; and so on. Those functions generally accept an expression
 * instance of the specific type and dispatch the children nodes to their own `visit*` functions. Finally, after all
 * children nodes are visited, the parent expression instance will be directly returned if no children are modified.
 *
 * To modify an expression tree, we need to override a `visit*` function, and return a new-created expression in it.
 * Then the parent's `visit*` function will detect it and create a new parent expression using the modified child node
 * returned by us. That's recursive, so the ancestor nodes also returns new-created instances. Finally, when we call
 * [visit], a new expression tree will be returned with our modifications applied.
 *
 * [SqlFormatter] is a typical example used to format expressions as executable SQL strings.
 */
open class SqlExpressionVisitor {

    /**
     * Dispatch different type of expression nodes to the specific `visit*` functions. Custom expression types that
     * are unknown to Ktorm will be dispatched to [visitUnknown].
     */
    open fun visit(expr: SqlExpression): SqlExpression {
        return when (expr) {
            is ScalarExpression<*> -> visitScalar(expr)
            is QueryExpression -> visitQuery(expr)
            is QuerySourceExpression -> visitQuerySource(expr)
            is ColumnDeclaringExpression -> visitColumnDeclaring(expr)
            is OrderByExpression -> visitOrderBy(expr)
            is ColumnAssignmentExpression<*> -> visitColumnAssignment(expr)
            is InsertExpression -> visitInsert(expr)
            is InsertFromQueryExpression -> visitInsertFromQuery(expr)
            is UpdateExpression -> visitUpdate(expr)
            is DeleteExpression -> visitDelete(expr)
            else -> visitUnknown(expr)
        }
    }

    protected open fun <T : Any> visitScalar(expr: ScalarExpression<T>): ScalarExpression<T> {
        val result = when (expr) {
            is CastingExpression -> visitCasting(expr)
            is UnaryExpression -> visitUnary(expr)
            is BinaryExpression -> visitBinary(expr)
            is ColumnExpression -> visitColumn(expr)
            is InListExpression<*> -> visitInList(expr)
            is ExistsExpression -> visitExists(expr)
            is AggregateExpression -> visitAggregate(expr)
            is BetweenExpression<*> -> visitBetween(expr)
            is ArgumentExpression -> visitArgument(expr)
            is FunctionExpression -> visitFunction(expr)
            else -> visitUnknown(expr)
        }

        @Suppress("UNCHECKED_CAST")
        return result as ScalarExpression<T>
    }

    protected open fun visitQuerySource(expr: QuerySourceExpression): QuerySourceExpression {
        return when (expr) {
            is TableExpression -> visitTable(expr)
            is JoinExpression -> visitJoin(expr)
            is QueryExpression -> visitQuery(expr)
            else -> visitUnknown(expr) as QuerySourceExpression
        }
    }

    protected open fun visitQuery(expr: QueryExpression): QueryExpression {
        return when (expr) {
            is SelectExpression -> visitSelect(expr)
            is UnionExpression -> visitUnion(expr)
        }
    }

    protected open fun <T : Any> visitCasting(expr: CastingExpression<T>): CastingExpression<T> {
        val expression = visit(expr.expression)

        if (expression === expr.expression) {
            return expr
        } else {
            return expr.copy(expression = expression)
        }
    }

    protected open fun <T : SqlExpression> visitExpressionList(original: List<T>): List<T> {
        val result = ArrayList<T>()
        var changed = false

        for (expr in original) {
            @Suppress("UNCHECKED_CAST")
            val visited = visit(expr) as T
            result += visited

            if (visited !== expr) {
                changed = true
            }
        }

        return if (changed) result else original
    }

    protected open fun <T : Any> visitUnary(expr: UnaryExpression<T>): UnaryExpression<T> {
        val operand = visitScalar(expr.operand)

        if (operand === expr.operand) {
            return expr
        } else {
            return expr.copy(operand = operand)
        }
    }

    protected open fun <T : Any> visitBinary(expr: BinaryExpression<T>): BinaryExpression<T> {
        val left = visitScalar(expr.left)
        val right = visitScalar(expr.right)

        if (left === expr.left && right === expr.right) {
            return expr
        } else {
            return expr.copy(left = left, right = right)
        }
    }

    protected open fun visitTable(expr: TableExpression): TableExpression {
        return expr
    }

    protected open fun <T : Any> visitColumn(expr: ColumnExpression<T>): ColumnExpression<T> {
        return expr
    }

    protected open fun visitColumnDeclaring(expr: ColumnDeclaringExpression): ColumnDeclaringExpression {
        val expression = visitScalar(expr.expression)

        if (expression === expr.expression) {
            return expr
        } else {
            return expr.copy(expression = expression)
        }
    }

    protected open fun visitOrderBy(expr: OrderByExpression): OrderByExpression {
        val expression = visitScalar(expr.expression)

        if (expression === expr.expression) {
            return expr
        } else {
            return expr.copy(expression = expression)
        }
    }

    protected open fun visitColumnDeclaringList(
        original: List<ColumnDeclaringExpression>
    ): List<ColumnDeclaringExpression> {
        return visitExpressionList(original)
    }

    protected open fun visitOrderByList(original: List<OrderByExpression>): List<OrderByExpression> {
        return visitExpressionList(original)
    }

    protected open fun visitGroupByList(original: List<ScalarExpression<*>>): List<ScalarExpression<*>> {
        return visitExpressionList(original)
    }

    protected open fun visitSelect(expr: SelectExpression): SelectExpression {
        val columns = visitColumnDeclaringList(expr.columns)
        val from = visitQuerySource(expr.from)
        val where = expr.where?.let { visitScalar(it) }
        val groupBy = visitGroupByList(expr.groupBy)
        val having = expr.having?.let { visitScalar(it) }
        val orderBy = visitOrderByList(expr.orderBy)

        @Suppress("ComplexCondition")
        if (columns === expr.columns
            && from === expr.from
            && where === expr.where
            && orderBy === expr.orderBy
            && groupBy === expr.groupBy
            && having === expr.having) {
            return expr
        } else {
            return expr.copy(
                columns = columns,
                from = from,
                where = where,
                groupBy = groupBy,
                having = having,
                orderBy = orderBy
            )
        }
    }

    protected open fun visitUnion(expr: UnionExpression): UnionExpression {
        val left = visitQuery(expr.left)
        val right = visitQuery(expr.right)
        val orderBy = visitOrderByList(expr.orderBy)

        if (left === expr.left && right === expr.right && orderBy === expr.orderBy) {
            return expr
        } else {
            return expr.copy(left = left, right = right, orderBy = orderBy)
        }
    }

    protected open fun visitJoin(expr: JoinExpression): JoinExpression {
        val left = visitQuerySource(expr.left)
        val right = visitQuerySource(expr.right)
        val condition = expr.condition?.let { visitScalar(it) }

        if (left === expr.left && right === expr.right && condition === expr.condition) {
            return expr
        } else {
            return expr.copy(left = left, right = right, condition = condition)
        }
    }

    protected open fun <T : Any> visitInList(expr: InListExpression<T>): InListExpression<T> {
        val left = visitScalar(expr.left)
        val query = expr.query?.let { visitQuery(it) }
        val values = expr.values?.let { visitExpressionList(it) }

        if (left === expr.left && query === expr.query && values === expr.values) {
            return expr
        } else {
            return expr.copy(left = left, query = query, values = values)
        }
    }

    protected open fun visitExists(expr: ExistsExpression): ExistsExpression {
        val query = visitQuery(expr.query)

        if (query === expr.query) {
            return expr
        } else {
            return expr.copy(query = query)
        }
    }

    protected open fun <T : Any> visitAggregate(expr: AggregateExpression<T>): AggregateExpression<T> {
        val argument = expr.argument?.let { visitScalar(it) }

        if (argument === expr.argument) {
            return expr
        } else {
            return expr.copy(argument = argument)
        }
    }

    protected open fun <T : Any> visitBetween(expr: BetweenExpression<T>): BetweenExpression<T> {
        val expression = visitScalar(expr.expression)
        val lower = visitScalar(expr.lower)
        val upper = visitScalar(expr.upper)

        if (expression === expr.expression && lower === expr.lower && upper === expr.upper) {
            return expr
        } else {
            return expr.copy(expression = expression, lower = lower, upper = upper)
        }
    }

    protected open fun <T : Any> visitArgument(expr: ArgumentExpression<T>): ArgumentExpression<T> {
        return expr
    }

    protected open fun <T : Any> visitFunction(expr: FunctionExpression<T>): FunctionExpression<T> {
        val arguments = visitExpressionList(expr.arguments)

        if (arguments === expr.arguments) {
            return expr
        } else {
            return expr.copy(arguments = arguments)
        }
    }

    protected open fun <T : Any> visitColumnAssignment(
        expr: ColumnAssignmentExpression<T>
    ): ColumnAssignmentExpression<T> {
        val column = visitColumn(expr.column)
        val expression = visitScalar(expr.expression)

        if (column === expr.column && expression === expr.expression) {
            return expr
        } else {
            return expr.copy(column, expression)
        }
    }

    protected open fun visitColumnAssignments(
        original: List<ColumnAssignmentExpression<*>>
    ): List<ColumnAssignmentExpression<*>> {
        return visitExpressionList(original)
    }

    protected open fun visitInsert(expr: InsertExpression): InsertExpression {
        val table = visitTable(expr.table)
        val assignments = visitColumnAssignments(expr.assignments)

        if (table === expr.table && assignments === expr.assignments) {
            return expr
        } else {
            return expr.copy(table = table, assignments = assignments)
        }
    }

    protected open fun visitInsertFromQuery(expr: InsertFromQueryExpression): InsertFromQueryExpression {
        val table = visitTable(expr.table)
        val columns = visitExpressionList(expr.columns)
        val query = visitQuery(expr.query)

        if (table === expr.table && columns === expr.columns && query === expr.query) {
            return expr
        } else {
            return expr.copy(table = table, columns = columns, query = query)
        }
    }

    protected open fun visitUpdate(expr: UpdateExpression): UpdateExpression {
        val table = visitTable(expr.table)
        val assignments = visitColumnAssignments(expr.assignments)
        val where = expr.where?.let { visitScalar(it) }

        if (table === expr.table && assignments === expr.assignments && where === expr.where) {
            return expr
        } else {
            return expr.copy(table = table, assignments = assignments, where = where)
        }
    }

    protected open fun visitDelete(expr: DeleteExpression): DeleteExpression {
        val table = visitTable(expr.table)
        val where = expr.where?.let { visitScalar(it) }

        if (table === expr.table && where === expr.where) {
            return expr
        } else {
            return expr.copy(table = table, where = where)
        }
    }

    protected open fun visitUnknown(expr: SqlExpression): SqlExpression {
        return expr
    }
}
