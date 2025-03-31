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

import org.ktorm.dsl.*
import org.ktorm.expression.*
import org.ktorm.schema.*
import java.util.Collections.emptyMap

public data class Subquery(val query: Query, val alias: String)

/**
 * contextualize a [ColumnDeclaring] to the [subquery].
 */
public fun <T : Any> ColumnDeclaring<T>.apropos(subquery: Subquery): SubqueryColumnDeclaringExpression<T> {
    val expr = when (this) {
        is ColumnDeclaringExpression -> this
        is Column -> this.aliased(label)
        else -> this.aliased(null)
    }

    return SubqueryColumnDeclaringExpression(
        expression = expr,
        subqueryName = subquery.alias
    )
}

/**
 * contextualize an `aliased column name` to the [subquery].
 */
public fun <T : Any> String.apropos(sqlType: SqlType<T>, subquery: Subquery): SubqueryColumnDeclaringExpression<T> =
    SubqueryColumnDeclaringExpression(
        expression = ColumnDeclaringExpression(
            ColumnExpression(null, this, sqlType),
            this),
        subqueryName = subquery.alias
    )

/**
 * Equal operator, translated to = in SQL.
 */
public infix fun SubqueryColumnDeclaringExpression<*>.eq(expr: ColumnDeclaring<*>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.EQUAL, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * subquery column declaring expression, representing a column in that has been contextualized to a subquery.
 */
public data class SubqueryColumnDeclaringExpression<T : Any>(
    val expression: ColumnDeclaringExpression<T>,
    val subqueryName: String,
    override val sqlType: SqlType<T> = expression.sqlType,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * Obtain the value of the column contextualized to a subquery. Returns null if the column is null or not present.
 */
public operator fun <C : Any> QueryRowSet.get(column: SubqueryColumnDeclaringExpression<C>): C? {
    for (index in 1..metaData.columnCount) {
        if (metaData.getColumnLabel(index).equals(column.expression.declaredName, ignoreCase = true)) {
            return column.sqlType.getResult(this, index)
        }
    }

    return null
}
