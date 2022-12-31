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

package org.ktorm.dsl

import org.ktorm.expression.AggregateExpression
import org.ktorm.expression.AggregateType
import org.ktorm.schema.*

/**
 * The min function, translated to `min(column)` in SQL.
 */
public fun <C : Comparable<C>> min(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MIN, column.asExpression(), false, column.sqlType)
}

/**
 * The min function with distinct, translated to `min(distinct column)` in SQL.
 */
public fun <C : Comparable<C>> minDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MIN, column.asExpression(), true, column.sqlType)
}

/**
 * The max function, translated to `max(column)` in SQL.
 */
public fun <C : Comparable<C>> max(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MAX, column.asExpression(), false, column.sqlType)
}

/**
 * The max function with distinct, translated to `max(distinct column)` in SQL.
 */
public fun <C : Comparable<C>> maxDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MAX, column.asExpression(), true, column.sqlType)
}

/**
 * The avg function, translated to `avg(column)` in SQL.
 */
public fun <C : Number> avg(column: ColumnDeclaring<C>): AggregateExpression<Double> {
    return AggregateExpression(AggregateType.AVG, column.asExpression(), false, DoubleSqlType)
}

/**
 * The avg function with distinct, translated to `avg(distinct column)` in SQL.
 */
public fun <C : Number> avgDistinct(column: ColumnDeclaring<C>): AggregateExpression<Double> {
    return AggregateExpression(AggregateType.AVG, column.asExpression(), true, DoubleSqlType)
}

/**
 * The sum function, translated to `sum(column)` in SQL.
 */
public fun <C : Number> sum(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.SUM, column.asExpression(), false, column.sqlType)
}

/**
 * The sum function with distinct, translated to `sum(distinct column)` in SQL.
 */
public fun <C : Number> sumDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.SUM, column.asExpression(), true, column.sqlType)
}

/**
 * The count function, translated to `count(column)` in SQL.
 */
public fun count(column: ColumnDeclaring<*>? = null): AggregateExpression<Int> {
    return AggregateExpression(AggregateType.COUNT, column?.asExpression(), false, IntSqlType)
}

/**
 * The count function with distinct, translated to `count(distinct column)` in SQL.
 */
public fun countDistinct(column: ColumnDeclaring<*>? = null): AggregateExpression<Int> {
    return AggregateExpression(AggregateType.COUNT, column?.asExpression(), true, IntSqlType)
}
