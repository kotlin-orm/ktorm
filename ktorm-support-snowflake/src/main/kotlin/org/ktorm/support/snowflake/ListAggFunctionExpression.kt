/*
 * Copyright 2025 the original author or authors.
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
 *
 * Original authors of the snowflake dialect were CarGurus: Don Mitchell, Ashish Shrestha, Mike Roberts, and others.
 */
package org.ktorm.support.snowflake

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.OrderByExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.SqlType
import org.ktorm.schema.VarcharSqlType

/**
 * List aggregation expression, which represents the aggregation of strings in multiple rows into
 * a single record. This implementation only covers the grouping approach, and not the window function version.
 * See [https://docs.snowflake.com/en/sql-reference/functions/listagg](listAgg) for more information.
 *
 * @property argument The string expression to concatenate,
 * @property separator The separator used to deliminate the separate row components, defaults to nothing.
 * @property isDistinct The distinct keyword used to deduplicate list elements.
 * @property withinGroupExpression The WITHIN GROUP expressions used to order the list elements.
 */
public data class ListAggFunctionExpression(
    val argument: ScalarExpression<String>,
    val separator: ScalarExpression<String>?,
    val isDistinct: Boolean = false,
    val withinGroupExpression: WithinGroupExpression = WithinGroupExpression(),
    override val sqlType: SqlType<String> = VarcharSqlType,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<String>()

public data class WithinGroupExpression(
    val orderBy: List<OrderByExpression> = emptyList(),
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * Creates a list aggregation expression.
 *
 * @param expr The string expression (usually a varchar column or a derivative of varchar columns)
 * @param separator The separator to use to separate list elements. Defaults to None, representing no separator.
 * @param isDistinct If true, include the DISTINCT keyword which dedupes list elements.
 * @return A list aggregation expression.
 */
public fun listAgg(
    expr: ColumnDeclaring<String>,
    separator: String? = null,
    isDistinct: Boolean = false
): ListAggFunctionExpression {
    val separatorExpression = separator?.let { ArgumentExpression(it, VarcharSqlType) }
    return ListAggFunctionExpression(expr.asExpression(), separatorExpression, isDistinct = isDistinct)
}

/**
 * Adds ordering to list aggregation expression. Uses the same type of ktorm expression used for other ORDER BY
 * statements.
 *
 * @param orders The list of order by expressions.
 * @return A new list aggregation expression.
 */
public fun ListAggFunctionExpression.orderBy(orders: Collection<OrderByExpression>): ListAggFunctionExpression = copy(
    withinGroupExpression = WithinGroupExpression(orders.toList())
)
