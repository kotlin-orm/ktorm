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

@file:Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")

package org.ktorm.global

import org.ktorm.database.Database
import org.ktorm.entity.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.ColumnDeclaring

/**
 * Check if all the records in the table matches the given [predicate].
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline fun <E : Any, T : BaseTable<E>> T.all(predicate: (T) -> ColumnDeclaring<Boolean>): Boolean {
    return Database.global.sequenceOf(this).all(predicate)
}

/**
 * Check if there is any record in the table.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <E : Any, T : BaseTable<E>> T.any(): Boolean {
    return Database.global.sequenceOf(this).any()
}

/**
 * Check if there is any record in the table matches the given [predicate].
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline fun <E : Any, T : BaseTable<E>> T.any(predicate: (T) -> ColumnDeclaring<Boolean>): Boolean {
    return Database.global.sequenceOf(this).any(predicate)
}

/**
 * Return `true` if there is no records in the table.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <E : Any, T : BaseTable<E>> T.none(): Boolean {
    return Database.global.sequenceOf(this).none()
}

/**
 * Return `true` if there is no records in the table that matches the given [predicate].
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline fun <E : Any, T : BaseTable<E>> T.none(predicate: (T) -> ColumnDeclaring<Boolean>): Boolean {
    return Database.global.sequenceOf(this).none(predicate)
}

/**
 * Return `true` if the table has no records.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <E : Any, T : BaseTable<E>> T.isEmpty(): Boolean {
    return Database.global.sequenceOf(this).isEmpty()
}

/**
 * Return `true` if the table has at lease one record.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <E : Any, T : BaseTable<E>> T.isNotEmpty(): Boolean {
    return Database.global.sequenceOf(this).isNotEmpty()
}

/**
 * Return the records count in the table.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <E : Any, T : BaseTable<E>> T.count(): Int {
    return Database.global.sequenceOf(this).count()
}

/**
 * Return the records count in the table that matches the given [predicate].
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline fun <E : Any, T : BaseTable<E>> T.count(predicate: (T) -> ColumnDeclaring<Boolean>): Int {
    return Database.global.sequenceOf(this).count(predicate)
}

/**
 * Return the sum of the given column or expression of all the records, null if there are no records in the table.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline fun <E : Any, T : BaseTable<E>, C : Number> T.sumBy(selector: (T) -> ColumnDeclaring<C>): C? {
    return Database.global.sequenceOf(this).sumBy(selector)
}

/**
 * Return the max value of the given column or expression of all the records, null if there are no records in the table.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline fun <E : Any, T : BaseTable<E>, C : Comparable<C>> T.maxBy(selector: (T) -> ColumnDeclaring<C>): C? {
    return Database.global.sequenceOf(this).maxBy(selector)
}

/**
 * Return the min value of the given column or expression of all the records, null if there are no records in the table.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline fun <E : Any, T : BaseTable<E>, C : Comparable<C>> T.minBy(selector: (T) -> ColumnDeclaring<C>): C? {
    return Database.global.sequenceOf(this).minBy(selector)
}

/**
 * Return the average value of the given column or expression of all the records, null if there are no records.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline fun <E : Any, T : BaseTable<E>> T.averageBy(selector: (T) -> ColumnDeclaring<out Number>): Double? {
    return Database.global.sequenceOf(this).averageBy(selector)
}
