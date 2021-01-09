/*
 * Copyright 2018-2021 the original author or authors.
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

package org.ktorm.global

import org.ktorm.database.Database
import org.ktorm.entity.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table

/**
 * Create an [EntitySequence] from this table.
 */
public fun <E : Any, T : BaseTable<E>> T.asSequence(withReferences: Boolean = true): EntitySequence<E, T> {
    return Database.global.sequenceOf(this, withReferences)
}

/**
 * Insert the given entity into this table and return the affected record number. Only non-null properties
 * are inserted.
 *
 * If we use an auto-increment key in our table, we need to tell Ktorm which is the primary key by calling
 * [Table.primaryKey] function while registering columns, then this function will obtain the generated key
 * from the database and fill it into the corresponding property after the insertion completes. But this
 * requires us not to set the primary key’s value beforehand, otherwise, if you do that, the given value
 * will be inserted into the database, and no keys generated.
 *
 * Note that after calling this function, the [entity] will **be associated with the current table**.
 *
 * @see Entity.flushChanges
 * @see Entity.delete
 */
@Deprecated(
    message = "This function will be removed in the future. Please use addEntity(entity) instead.",
    replaceWith = ReplaceWith("addEntity(entity)")
)
public fun <E : Entity<E>> Table<E>.add(entity: E): Int {
    return Database.global.sequenceOf(this).add(entity)
}

/**
 * Insert the given entity into this table and return the affected record number. Only non-null properties
 * are inserted.
 *
 * If we use an auto-increment key in our table, we need to tell Ktorm which is the primary key by calling
 * [Table.primaryKey] function while registering columns, then this function will obtain the generated key
 * from the database and fill it into the corresponding property after the insertion completes. But this
 * requires us not to set the primary key’s value beforehand, otherwise, if you do that, the given value
 * will be inserted into the database, and no keys generated.
 *
 * Note that after calling this function, the [entity] will **be associated with the current table**.
 *
 * @see Entity.flushChanges
 * @see Entity.delete
 */
public fun <E : Entity<E>> Table<E>.addEntity(entity: E): Int {
    return Database.global.sequenceOf(this).add(entity)
}

/**
 * Update the non-null properties of the given entity to the database and return the affected record number.
 *
 * Note that after calling this function, the [entity] will **be associated with the current table**.
 *
 * @see Entity.flushChanges
 * @see Entity.delete
 * @since 3.1.0
 */
public fun <E : Entity<E>> Table<E>.updateEntity(entity: E): Int {
    return Database.global.sequenceOf(this).update(entity)
}

/**
 * Obtain a entity object matching the given [predicate], auto left joining all the reference tables.
 */
public inline fun <E : Any, T : BaseTable<E>> T.findOne(predicate: (T) -> ColumnDeclaring<Boolean>): E? {
    return Database.global.sequenceOf(this).find(predicate)
}

/**
 * Obtain all the entity objects from this table, auto left joining all the reference tables.
 */
public fun <E : Any> BaseTable<E>.findAll(): List<E> {
    return Database.global.sequenceOf(this).toList()
}

/**
 * Obtain a list of entity objects matching the given [predicate], auto left joining all the reference tables.
 */
public inline fun <E : Any, T : BaseTable<E>> T.findList(predicate: (T) -> ColumnDeclaring<Boolean>): List<E> {
    return Database.global.sequenceOf(this).filter(predicate).toList()
}
