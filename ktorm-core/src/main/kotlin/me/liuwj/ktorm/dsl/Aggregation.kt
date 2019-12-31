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

package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.entity.*
import me.liuwj.ktorm.expression.AggregateExpression
import me.liuwj.ktorm.expression.AggregateType
import me.liuwj.ktorm.schema.*

/**
 * The min function, translated to `min(column)` in SQL.
 */
fun <C : Comparable<C>> min(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MIN, column.asExpression(), false, column.sqlType)
}

/**
 * The min function with distinct, translated to `min(distinct column)` in SQL.
 */
fun <C : Comparable<C>> minDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MIN, column.asExpression(), true, column.sqlType)
}

/**
 * The max function, translated to `max(column)` in SQL.
 */
fun <C : Comparable<C>> max(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MAX, column.asExpression(), false, column.sqlType)
}

/**
 * The max function with distinct, translated to `max(distinct column)` in SQL.
 */
fun <C : Comparable<C>> maxDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MAX, column.asExpression(), true, column.sqlType)
}

/**
 * The avg function, translated to `avg(column)` in SQL.
 */
fun <C : Number> avg(column: ColumnDeclaring<C>): AggregateExpression<Double> {
    return AggregateExpression(AggregateType.AVG, column.asExpression(), false, DoubleSqlType)
}

/**
 * The avg function with distinct, translated to `avg(distinct column)` in SQL.
 */
fun <C : Number> avgDistinct(column: ColumnDeclaring<C>): AggregateExpression<Double> {
    return AggregateExpression(AggregateType.AVG, column.asExpression(), true, DoubleSqlType)
}

/**
 * The sum function, translated to `sum(column)` in SQL.
 */
fun <C : Number> sum(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.SUM, column.asExpression(), false, column.sqlType)
}

/**
 * The sum function with distinct, translated to `sum(distinct column)` in SQL.
 */
fun <C : Number> sumDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.SUM, column.asExpression(), true, column.sqlType)
}

/**
 * The count function, translated to `count(column)` in SQL.
 */
fun count(column: ColumnDeclaring<*>? = null): AggregateExpression<Int> {
    return AggregateExpression(AggregateType.COUNT, column?.asExpression(), false, IntSqlType)
}

/**
 * The count function with distinct, translated to `count(distinct column)` in SQL.
 */
fun countDistinct(column: ColumnDeclaring<*>? = null): AggregateExpression<Int> {
    return AggregateExpression(AggregateType.COUNT, column?.asExpression(), true, IntSqlType)
}

/**
 * Check if all the records in the table matches the given [predicate].
 */
inline fun <E : Any, T : BaseTable<E>> T.all(predicate: (T) -> ColumnDeclaring<Boolean>): Boolean {
    return asSequence().all(predicate)
}

/**
 * Check if there is any record in the table.
 */
fun <E : Any, T : BaseTable<E>> T.any(): Boolean {
    return asSequence().any()
}

/**
 * Check if there is any record in the table matches the given [predicate].
 */
inline fun <E : Any, T : BaseTable<E>> T.any(predicate: (T) -> ColumnDeclaring<Boolean>): Boolean {
    return asSequence().any(predicate)
}

/**
 * Return `true` if there is no records in the table.
 */
fun <E : Any, T : BaseTable<E>> T.none(): Boolean {
    return asSequence().none()
}

/**
 * Return `true` if there is no records in the table that matches the given [predicate].
 */
inline fun <E : Any, T : BaseTable<E>> T.none(predicate: (T) -> ColumnDeclaring<Boolean>): Boolean {
    return asSequence().none(predicate)
}

/**
 * Return the records count in the table.
 */
fun <E : Any, T : BaseTable<E>> T.count(): Int {
    return asSequence().count()
}

/**
 * Return the records count in the table that matches the given [predicate].
 */
inline fun <E : Any, T : BaseTable<E>> T.count(predicate: (T) -> ColumnDeclaring<Boolean>): Int {
    return asSequence().count(predicate)
}

/**
 * Return the sum of the given column or expression of all the records, null if there are no records in the table.
 */
inline fun <E : Any, T : BaseTable<E>, C : Number> T.sumBy(selector: (T) -> ColumnDeclaring<C>): C? {
    return asSequence().sumBy(selector)
}

/**
 * Return the max value of the given column or expression of all the records, null if there are no records in the table.
 */
inline fun <E : Any, T : BaseTable<E>, C : Comparable<C>> T.maxBy(selector: (T) -> ColumnDeclaring<C>): C? {
    return asSequence().maxBy(selector)
}

/**
 * Return the min value of the given column or expression of all the records, null if there are no records in the table.
 */
inline fun <E : Any, T : BaseTable<E>, C : Comparable<C>> T.minBy(selector: (T) -> ColumnDeclaring<C>): C? {
    return asSequence().minBy(selector)
}

/**
 * Return the average value of the given column or expression of all the records, null if there are no records.
 */
inline fun <E : Any, T : BaseTable<E>> T.averageBy(selector: (T) -> ColumnDeclaring<out Number>): Double? {
    return asSequence().averageBy(selector)
}
