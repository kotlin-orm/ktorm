/*
 * Copyright 2018-2020 the original author or authors.
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

package org.ktorm.dsl

import org.ktorm.expression.*

internal fun QueryExpression.toCountExpression(): SelectExpression {
    val expression = OrderByRemover.visit(this) as QueryExpression
    val count = count().aliased(null)

    if (expression is SelectExpression && expression.isSimpleSelect()) {
        return expression.copy(columns = listOf(count), offset = null, limit = null)
    } else {
        return SelectExpression(
            columns = listOf(count),
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
