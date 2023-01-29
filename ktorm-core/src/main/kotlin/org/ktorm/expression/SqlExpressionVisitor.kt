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

/**
 * Base interface designed to visit or modify SQL expression trees using visitor pattern.
 *
 * This interface provides a general [visit] function to dispatch different type of expression nodes to their specific
 * `visit*` functions. Custom expression types that are unknown to Ktorm will be dispatched to [visitUnknown].
 *
 * For each expression type, there is a corresponding `visit*` function in this interface; for [SelectExpression], it's
 * [visitSelect]; for [TableExpression], it's [visitTable]; and so on. Those functions generally accept an expression
 * instance of the specific type and dispatch the children nodes to their own `visit*` functions. Finally, after all
 * children nodes are visited, the parent expression instance will be directly returned if no children are modified.
 *
 * As SQL expressions are immutable, to modify an expression, we need to override a child `visit*` function, and return
 * a new-created expression in it. Then its parent's `visit*` function will notice the change and create a new parent
 * expression using the modified child node returned by us. As the process is recursive, the ancestor nodes also returns
 * new-created instances. Finally, as a result of calling [visit], a new expression tree will be returned with our
 * modifications applied.
 *
 * [SqlFormatter] is a typical example used to format expressions as executable SQL strings.
 */
@Suppress("ComplexInterface")
public interface SqlExpressionVisitor {

    /**
     * Dispatch different type of expression nodes to their specific `visit*` functions. Custom expression types that
     * are unknown to Ktorm will be dispatched to [visitUnknown].
     */
    public fun visit(expr: SqlExpression): SqlExpression {
        return when (expr) {
            is ScalarExpression<*> -> visitScalar(expr)
            is QueryExpression -> visitQuery(expr)
            is QuerySourceExpression -> visitQuerySource(expr)
            is InsertExpression -> visitInsert(expr)
            is InsertFromQueryExpression -> visitInsertFromQuery(expr)
            is UpdateExpression -> visitUpdate(expr)
            is DeleteExpression -> visitDelete(expr)
            is ColumnAssignmentExpression<*> -> visitColumnAssignment(expr)
            is OrderByExpression -> visitOrderBy(expr)
            is WindowSpecificationExpression -> visitWindowSpecification(expr)
            is WindowFrameBoundExpression -> visitWindowFrameBound(expr)
            else -> visitUnknown(expr)
        }
    }

    /**
     * Function that visits a general [ScalarExpression], this function dispatches different type of scalar expressions
     * to their specific `visit*` functions. Custom expression types that are unknown to Ktorm will be dispatched to
     * [visitUnknown]
     */
    public fun <T : Any> visitScalar(expr: ScalarExpression<T>): ScalarExpression<T> {
        val result = when (expr) {
            is ColumnExpression -> visitColumn(expr)
            is ColumnDeclaringExpression -> visitColumnDeclaring(expr)
            is UnaryExpression -> visitUnary(expr)
            is BinaryExpression -> visitBinary(expr)
            is ArgumentExpression -> visitArgument(expr)
            is CastingExpression -> visitCasting(expr)
            is InListExpression -> visitInList(expr)
            is ExistsExpression -> visitExists(expr)
            is BetweenExpression -> visitBetween(expr)
            is CaseWhenExpression -> visitCaseWhen(expr)
            is FunctionExpression -> visitFunction(expr)
            is AggregateExpression -> visitAggregate(expr)
            is WindowFunctionExpression -> visitWindowFunction(expr)
            else -> visitUnknown(expr)
        }

        @Suppress("UNCHECKED_CAST")
        return result as ScalarExpression<T>
    }

    /**
     * Function that visits a [QuerySourceExpression].
     */
    public fun visitQuerySource(expr: QuerySourceExpression): QuerySourceExpression {
        return when (expr) {
            is QueryExpression -> visitQuery(expr)
            is JoinExpression -> visitJoin(expr)
            is TableExpression -> visitTable(expr)
            else -> visitUnknown(expr) as QuerySourceExpression
        }
    }

    /**
     * Function that visits a [QueryExpression].
     */
    public fun visitQuery(expr: QueryExpression): QueryExpression {
        return when (expr) {
            is SelectExpression -> visitSelect(expr)
            is UnionExpression -> visitUnion(expr)
        }
    }

    /**
     * Function that visits a [SelectExpression].
     */
    public fun visitSelect(expr: SelectExpression): SelectExpression {
        val columns = visitExpressionList(expr.columns)
        val from = visitQuerySource(expr.from)
        val where = expr.where?.let { visitScalar(it) }
        val groupBy = visitExpressionList(expr.groupBy)
        val having = expr.having?.let { visitScalar(it) }
        val orderBy = visitExpressionList(expr.orderBy)

        @Suppress("ComplexCondition")
        if (columns === expr.columns
            && from === expr.from
            && where === expr.where
            && orderBy === expr.orderBy
            && groupBy === expr.groupBy
            && having === expr.having
        ) {
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

    /**
     * Function that visits an [UnionExpression].
     */
    public fun visitUnion(expr: UnionExpression): UnionExpression {
        val left = visitQuery(expr.left)
        val right = visitQuery(expr.right)
        val orderBy = visitExpressionList(expr.orderBy)

        if (left === expr.left && right === expr.right && orderBy === expr.orderBy) {
            return expr
        } else {
            return expr.copy(left = left, right = right, orderBy = orderBy)
        }
    }

    /**
     * Function that visits an [InsertExpression].
     */
    public fun visitInsert(expr: InsertExpression): InsertExpression {
        val table = visitTable(expr.table)
        val assignments = visitExpressionList(expr.assignments)

        if (table === expr.table && assignments === expr.assignments) {
            return expr
        } else {
            return expr.copy(table = table, assignments = assignments)
        }
    }

    /**
     * Function that visits an [InsertFromQueryExpression].
     */
    public fun visitInsertFromQuery(expr: InsertFromQueryExpression): InsertFromQueryExpression {
        val table = visitTable(expr.table)
        val columns = visitExpressionList(expr.columns)
        val query = visitQuery(expr.query)

        if (table === expr.table && columns === expr.columns && query === expr.query) {
            return expr
        } else {
            return expr.copy(table = table, columns = columns, query = query)
        }
    }

    /**
     * Function that visits an [UpdateExpression].
     */
    public fun visitUpdate(expr: UpdateExpression): UpdateExpression {
        val table = visitTable(expr.table)
        val assignments = visitExpressionList(expr.assignments)
        val where = expr.where?.let { visitScalar(it) }

        if (table === expr.table && assignments === expr.assignments && where === expr.where) {
            return expr
        } else {
            return expr.copy(table = table, assignments = assignments, where = where)
        }
    }

    /**
     * Function that visits a [DeleteExpression].
     */
    public fun visitDelete(expr: DeleteExpression): DeleteExpression {
        val table = visitTable(expr.table)
        val where = expr.where?.let { visitScalar(it) }

        if (table === expr.table && where === expr.where) {
            return expr
        } else {
            return expr.copy(table = table, where = where)
        }
    }

    /**
     * Helper function for visiting a list of expressions.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : SqlExpression> visitExpressionList(
        original: List<T>,
        subVisitor: (T) -> T = { visit(it) as T }
    ): List<T> {
        val result = ArrayList<T>()
        var changed = false

        for (expr in original) {
            val visited = subVisitor(expr)
            result += visited

            if (visited !== expr) {
                changed = true
            }
        }

        return if (changed) result else original
    }

    /**
     * Function that visits a [JoinExpression].
     */
    public fun visitJoin(expr: JoinExpression): JoinExpression {
        val left = visitQuerySource(expr.left)
        val right = visitQuerySource(expr.right)
        val condition = expr.condition?.let { visitScalar(it) }

        if (left === expr.left && right === expr.right && condition === expr.condition) {
            return expr
        } else {
            return expr.copy(left = left, right = right, condition = condition)
        }
    }

    /**
     * Function that visits a [TableExpression].
     */
    public fun visitTable(expr: TableExpression): TableExpression {
        return expr
    }

    /**
     * Function that visits a [ColumnExpression].
     */
    public fun <T : Any> visitColumn(expr: ColumnExpression<T>): ColumnExpression<T> {
        val table = expr.table?.let { visitTable(it) }

        if (table === expr.table) {
            return expr
        } else {
            return expr.copy(table = table)
        }
    }

    /**
     * Function that visits a [ColumnDeclaringExpression].
     */
    public fun <T : Any> visitColumnDeclaring(
        expr: ColumnDeclaringExpression<T>
    ): ColumnDeclaringExpression<T> {
        val expression = visitScalar(expr.expression)

        if (expression === expr.expression) {
            return expr
        } else {
            return expr.copy(expression = expression)
        }
    }

    /**
     * Function that visits a [ColumnAssignmentExpression].
     */
    public fun <T : Any> visitColumnAssignment(
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

    /**
     * Function that visits an [OrderByExpression].
     */
    public fun visitOrderBy(expr: OrderByExpression): OrderByExpression {
        val expression = visitScalar(expr.expression)

        if (expression === expr.expression) {
            return expr
        } else {
            return expr.copy(expression = expression)
        }
    }

    /**
     * Function that visits an [UnaryExpression].
     */
    public fun <T : Any> visitUnary(expr: UnaryExpression<T>): UnaryExpression<T> {
        val operand = visitScalar(expr.operand)

        if (operand === expr.operand) {
            return expr
        } else {
            return expr.copy(operand = operand)
        }
    }

    /**
     * Function that visits a [BinaryExpression].
     */
    public fun <T : Any> visitBinary(expr: BinaryExpression<T>): BinaryExpression<T> {
        val left = visitScalar(expr.left)
        val right = visitScalar(expr.right)

        if (left === expr.left && right === expr.right) {
            return expr
        } else {
            return expr.copy(left = left, right = right)
        }
    }

    /**
     * Function that visits an [ArgumentExpression].
     */
    public fun <T : Any> visitArgument(expr: ArgumentExpression<T>): ArgumentExpression<T> {
        return expr
    }

    /**
     * Function that visits a [CastingExpression].
     */
    public fun <T : Any> visitCasting(expr: CastingExpression<T>): CastingExpression<T> {
        val expression = visit(expr.expression)

        if (expression === expr.expression) {
            return expr
        } else {
            return expr.copy(expression = expression)
        }
    }

    /**
     * Function that visits an [InListExpression].
     */
    public fun visitInList(expr: InListExpression): InListExpression {
        val left = visitScalar(expr.left)
        val query = expr.query?.let { visitQuery(it) }
        val values = expr.values?.let { visitExpressionList(it) }

        if (left === expr.left && query === expr.query && values === expr.values) {
            return expr
        } else {
            return expr.copy(left = left, query = query, values = values)
        }
    }

    /**
     * Function that visits an [ExistsExpression].
     */
    public fun visitExists(expr: ExistsExpression): ExistsExpression {
        val query = visitQuery(expr.query)

        if (query === expr.query) {
            return expr
        } else {
            return expr.copy(query = query)
        }
    }

    /**
     * Function that visits a [BetweenExpression].
     */
    public fun visitBetween(expr: BetweenExpression): BetweenExpression {
        val expression = visitScalar(expr.expression)
        val lower = visitScalar(expr.lower)
        val upper = visitScalar(expr.upper)

        if (expression === expr.expression && lower === expr.lower && upper === expr.upper) {
            return expr
        } else {
            return expr.copy(expression = expression, lower = lower, upper = upper)
        }
    }

    /**
     * Function that visits a [CaseWhenExpression].
     */
    public fun <T : Any> visitCaseWhen(expr: CaseWhenExpression<T>): CaseWhenExpression<T> {
        val operand = expr.operand?.let { visitScalar(it) }
        val whenClauses = visitWhenClauses(expr.whenClauses)
        val elseClause = expr.elseClause?.let { visitScalar(it) }

        if (operand === expr.operand && whenClauses === expr.whenClauses && elseClause === expr.elseClause) {
            return expr
        } else {
            return expr.copy(operand = operand, whenClauses = whenClauses, elseClause = elseClause)
        }
    }

    /**
     * Helper function for visiting when clauses of [CaseWhenExpression].
     */
    public fun <T : Any> visitWhenClauses(
        originalClauses: List<Pair<ScalarExpression<*>, ScalarExpression<T>>>
    ): List<Pair<ScalarExpression<*>, ScalarExpression<T>>> {
        val resultClauses = ArrayList<Pair<ScalarExpression<*>, ScalarExpression<T>>>()
        var changed = false

        for ((condition, result) in originalClauses) {
            val visitedCondition = visitScalar(condition)
            val visitedResult = visitScalar(result)
            resultClauses += Pair(visitedCondition, visitedResult)

            if (visitedCondition !== condition || visitedResult !== result) {
                changed = true
            }
        }

        return if (changed) resultClauses else originalClauses
    }

    /**
     * Function that visits a [FunctionExpression].
     */
    public fun <T : Any> visitFunction(expr: FunctionExpression<T>): FunctionExpression<T> {
        val arguments = visitExpressionList(expr.arguments)

        if (arguments === expr.arguments) {
            return expr
        } else {
            return expr.copy(arguments = arguments)
        }
    }

    /**
     * Function that visits an [AggregateExpression].
     */
    public fun <T : Any> visitAggregate(expr: AggregateExpression<T>): AggregateExpression<T> {
        val argument = expr.argument?.let { visitScalar(it) }

        if (argument === expr.argument) {
            return expr
        } else {
            return expr.copy(argument = argument)
        }
    }

    /**
     * Function that visits a [WindowFunctionExpression].
     */
    public fun <T : Any> visitWindowFunction(expr: WindowFunctionExpression<T>): WindowFunctionExpression<T> {
        val arguments = visitExpressionList(expr.arguments)
        val window = visitWindowSpecification(expr.window)

        if (arguments === expr.arguments && window === expr.window) {
            return expr
        } else {
            return expr.copy(arguments = arguments, window = window)
        }
    }

    /**
     * Function that visits a [WindowSpecificationExpression].
     */
    public fun visitWindowSpecification(expr: WindowSpecificationExpression): WindowSpecificationExpression {
        val partitionBy = visitExpressionList(expr.partitionBy)
        val orderBy = visitExpressionList(expr.orderBy)
        val frameStart = expr.frameStart?.let { visitWindowFrameBound(it) }
        val frameEnd = expr.frameEnd?.let { visitWindowFrameBound(it) }

        @Suppress("ComplexCondition")
        if (partitionBy === expr.partitionBy
            && orderBy === expr.orderBy
            && frameStart === expr.frameStart
            && frameEnd === expr.frameEnd
        ) {
            return expr
        } else {
            return expr.copy(partitionBy = partitionBy, orderBy = orderBy, frameStart = frameStart, frameEnd = frameEnd)
        }
    }

    /**
     * Function that visits a [WindowFrameBoundExpression].
     */
    public fun visitWindowFrameBound(expr: WindowFrameBoundExpression): WindowFrameBoundExpression {
        val argument = expr.argument?.let { visitScalar(it) }

        if (argument == expr.argument) {
            return expr
        } else {
            return expr.copy(argument = argument)
        }
    }

    /**
     * Function that visits an unknown expression.
     */
    public fun visitUnknown(expr: SqlExpression): SqlExpression {
        return expr
    }
}
