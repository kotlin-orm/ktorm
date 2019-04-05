package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.expression.*

internal fun QueryExpression.toCountExpression(): SelectExpression {
    val expression = OrderByRemover.visit(this) as QueryExpression
    val countColumns = listOf(count().asDeclaringExpression())

    if (expression is SelectExpression && expression.isSimpleSelect()) {
        return expression.copy(columns = countColumns, offset = null, limit = null)
    } else {
        return SelectExpression(
            columns = countColumns,
            from = when (expression) {
                is SelectExpression -> expression.copy(offset = null, limit = null, tableAlias = "tmp_count")
                is UnionExpression -> expression.copy(offset = null, limit = null, tableAlias = "tmp_count")
            }
        )
    }
}

private fun SelectExpression.isSimpleSelect(): Boolean {
    if (groupBy.isNotEmpty()) {
        return false
    }
    if (isDistinct) {
        return false
    }
    return columns.all { it.expression is ColumnExpression }
}

private object OrderByRemover : SqlExpressionVisitor() {

    override fun visitSelect(expr: SelectExpression): SelectExpression {
        if (expr.orderBy.any { it.hasArgument() }) {
            return expr
        } else {
            return expr.copy(orderBy = emptyList())
        }
    }

    override fun visitUnion(expr: UnionExpression): UnionExpression {
        if (expr.orderBy.any { it.hasArgument() }) {
            return expr
        } else {
            return expr.copy(orderBy = emptyList())
        }
    }
}

private fun SqlExpression.hasArgument(): Boolean {
    var hasArgument = false

    val visitor = object : SqlExpressionVisitor() {
        override fun <T : Any> visitArgument(expr: ArgumentExpression<T>): ArgumentExpression<T> {
            hasArgument = true
            return expr
        }
    }

    visitor.visit(this)
    return hasArgument
}