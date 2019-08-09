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

package me.liuwj.ktorm.entity

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.expression.BinaryExpression
import me.liuwj.ktorm.expression.BinaryExpressionType
import me.liuwj.ktorm.expression.QuerySourceExpression
import me.liuwj.ktorm.schema.*
import kotlin.reflect.jvm.jvmErasure

/**
 * Obtain a map of entity objects by IDs, auto left joining all the reference tables.
 */
@Suppress("UNCHECKED_CAST")
fun <E : Entity<E>, K : Any> Table<E>.findMapByIds(ids: Collection<K>): Map<K, E> {
    return findListByIds(ids).associateBy { it.implementation.getPrimaryKeyValue(this) as K }
}

/**
 * Obtain a list of entity objects by IDs, auto left joining all the reference tables.
 */
@Suppress("UNCHECKED_CAST")
fun <E : Entity<E>> Table<E>.findListByIds(ids: Collection<Any>): List<E> {
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
@Suppress("UNCHECKED_CAST")
fun <E : Entity<E>> Table<E>.findById(id: Any): E? {
    val primaryKey = this.primaryKey as? Column<Any> ?: error("Table $tableName doesn't have a primary key.")
    return findOne { primaryKey eq id }
}

/**
 * Obtain a entity object matching the given [predicate], auto left joining all the reference tables.
 *
 * This function will return `null` if no records found, and throw an exception if there are more than one record.
 */
inline fun <E : Entity<E>, T : Table<E>> T.findOne(predicate: (T) -> ColumnDeclaring<Boolean>): E? {
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
fun <E : Entity<E>> Table<E>.findAll(): List<E> {
    // return this.asSequence().toList()
    return this
        .joinReferencesAndSelect()
        .map { row -> this.createEntity(row) }
}

/**
 * Obtain a list of entity objects matching the given [predicate], auto left joining all the reference tables.
 */
inline fun <E : Entity<E>, T : Table<E>> T.findList(predicate: (T) -> ColumnDeclaring<Boolean>): List<E> {
    // return this.asSequence().filter(predicate).toList()
    return this
        .joinReferencesAndSelect()
        .where { predicate(this) }
        .map { row -> this.createEntity(row) }
}

/**
 * Return a new-created [Query] object, left joining all the reference tables, and selecting all columns of them.
 */
fun Table<*>.joinReferencesAndSelect(): Query {
    val joinedTables = ArrayList<Table<*>>()

    return this
        .joinReferences(this.asExpression(), joinedTables)
        .select(joinedTables.flatMap { it.columns })
}

private fun Table<*>.joinReferences(
    expr: QuerySourceExpression,
    joinedTables: MutableList<Table<*>>
): QuerySourceExpression {

    var curr = expr

    joinedTables += this

    for (column in columns) {
        val binding = column.binding
        if (binding is ReferenceBinding) {
            val refTable = binding.referenceTable
            val primaryKey = refTable.primaryKey ?: error("Table ${refTable.tableName} doesn't have a primary key.")

            curr = curr.leftJoin(refTable, on = column eq primaryKey)
            curr = refTable.joinReferences(curr, joinedTables)
        }
    }

    return curr
}

private infix fun ColumnDeclaring<*>.eq(column: ColumnDeclaring<*>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.EQUAL, asExpression(), column.asExpression(), BooleanSqlType)
}
