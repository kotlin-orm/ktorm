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

package me.liuwj.ktorm.entity

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.Table

/**
 * Obtain a map of entity objects by IDs, auto left joining all the reference tables.
 */
@Suppress("DEPRECATION", "UNCHECKED_CAST")
@Deprecated(
    message = "This function will be removed in the future. Use database.sequenceOf().filter{}.associateBy{} instead.",
    replaceWith = ReplaceWith("database.sequenceOf(this).filter(predicate).associateBy(keySelector)")
)
fun <E : Entity<E>, K : Any> Table<E>.findMapByIds(ids: Collection<K>): Map<K, E> {
    return findListByIds(ids).associateBy { it.implementation.getPrimaryKeyValue(this) as K }
}

/**
 * Obtain a list of entity objects by IDs, auto left joining all the reference tables.
 */
@Suppress("DEPRECATION", "UNCHECKED_CAST")
@Deprecated(
    message = "This function will be removed in the future. Use database.sequenceOf().filter{}.toList() instead.",
    replaceWith = ReplaceWith("database.sequenceOf(this).filter(predicate).toList()")
)
fun <E : Any> BaseTable<E>.findListByIds(ids: Collection<Any>): List<E> {
    if (ids.isEmpty()) {
        return emptyList()
    } else {
        val primaryKey = this.primaryKey as? Column<Any> ?: error("Table $tableName doesn't have a primary key.")
        return findList { primaryKey inList ids }
    }
}

/**
 * Obtain a entity object by its ID, auto left joining all the reference tables.
 *
 * This function will return `null` if no records found, and throw an exception if there are more than one record.
 */
@Suppress("DEPRECATION", "UNCHECKED_CAST")
@Deprecated(
    message = "This function will be removed in the future. Please use database.sequenceOf(..).find {..} instead.",
    replaceWith = ReplaceWith("database.sequenceOf(this).find(predicate)")
)
fun <E : Any> BaseTable<E>.findById(id: Any): E? {
    val primaryKey = this.primaryKey as? Column<Any> ?: error("Table $tableName doesn't have a primary key.")
    return findOne { primaryKey eq id }
}

/**
 * Obtain a entity object matching the given [predicate], auto left joining all the reference tables.
 *
 * This function will return `null` if no records found, and throw an exception if there are more than one record.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "This function will be removed in the future. Please use database.sequenceOf(..).find {..} instead.",
    replaceWith = ReplaceWith("database.sequenceOf(this).find(predicate)")
)
inline fun <E : Any, T : BaseTable<E>> T.findOne(predicate: (T) -> ColumnDeclaring<Boolean>): E? {
    val list = findList(predicate)
    when (list.size) {
        0 -> return null
        1 -> return list[0]
        else -> error("Expected one result(or null) to be returned by findOne(), but found: ${list.size}")
    }
}

/**
 * Obtain all the entity objects from this table, auto left joining all the reference tables.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "This function will be removed in the future. Please use database.sequenceOf(..).toList() instead.",
    replaceWith = ReplaceWith("database.sequenceOf(this).toList()")
)
fun <E : Any> BaseTable<E>.findAll(): List<E> {
    // return this.asSequence().toList()
    return this
        .joinReferencesAndSelect()
        .map { row -> this.createEntity(row) }
}

/**
 * Obtain a list of entity objects matching the given [predicate], auto left joining all the reference tables.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "This function will be removed in the future. Use database.sequenceOf().filter{}.toList() instead.",
    replaceWith = ReplaceWith("database.sequenceOf(this).filter(predicate).toList()")
)
inline fun <E : Any, T : BaseTable<E>> T.findList(predicate: (T) -> ColumnDeclaring<Boolean>): List<E> {
    // return this.asSequence().filter(predicate).toList()
    return this
        .joinReferencesAndSelect()
        .where { predicate(this) }
        .map { row -> this.createEntity(row) }
}

/**
 * Return a new-created [Query] object, left joining all the reference tables, and selecting all columns of them.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "This function will be removed in the future. Use database.from(..).joinReferencesAndSelect() instead.",
    replaceWith = ReplaceWith("database.from(this).joinReferencesAndSelect()")
)
fun BaseTable<*>.joinReferencesAndSelect(): Query {
    return Database.global.from(this).joinReferencesAndSelect()
}
