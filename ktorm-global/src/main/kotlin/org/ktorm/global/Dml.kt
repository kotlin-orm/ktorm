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
import org.ktorm.dsl.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.ColumnDeclaring
import java.sql.Statement

/**
 * Construct an update expression in the given closure, then execute it and return the effected row count.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.update {
 *     set(it.job, "engineer")
 *     set(it.managerId, null)
 *     set(it.salary, 100)
 *     where {
 *         it.id eq 2
 *     }
 * }
 * ```
 *
 * @param block the DSL block, an extension function of [UpdateStatementBuilder], used to construct the expression.
 * @return the effected row count.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <T : BaseTable<*>> T.update(block: UpdateStatementBuilder.(T) -> Unit): Int {
    return Database.global.update(this, block)
}

/**
 * Construct update expressions in the given closure, then batch execute them and return the effected
 * row counts for each expression.
 *
 * Note that this function is implemented based on [Statement.addBatch] and [Statement.executeBatch],
 * and any item in a batch operation must have the same structure, otherwise an exception will be thrown.
 *
 * Usage:
 *
 * ```kotlin
 * Departments.batchUpdate {
 *     for (i in 1..2) {
 *         item {
 *             set(it.location, "Hong Kong")
 *             where {
 *                 it.id eq i
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param block the DSL block, extension function of [BatchUpdateStatementBuilder], used to construct the expressions.
 * @return the effected row counts for each sub-operation.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <T : BaseTable<*>> T.batchUpdate(block: BatchUpdateStatementBuilder<T>.() -> Unit): IntArray {
    return Database.global.batchUpdate(this, block)
}

/**
 * Construct an insert expression in the given closure, then execute it and return the effected row count.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.insert {
 *     set(it.name, "jerry")
 *     set(it.job, "trainee")
 *     set(it.managerId, 1)
 *     set(it.hireDate, LocalDate.now())
 *     set(it.salary, 50)
 *     set(it.departmentId, 1)
 * }
 * ```
 *
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the effected row count.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <T : BaseTable<*>> T.insert(block: AssignmentsBuilder.(T) -> Unit): Int {
    return Database.global.insert(this, block)
}

/**
 * Construct insert expressions in the given closure, then batch execute them and return the effected
 * row counts for each expression.
 *
 * Note that this function is implemented based on [Statement.addBatch] and [Statement.executeBatch],
 * and any item in a batch operation must have the same structure, otherwise an exception will be thrown.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.batchInsert {
 *     item {
 *         set(it.name, "jerry")
 *         set(it.job, "trainee")
 *         set(it.managerId, 1)
 *         set(it.hireDate, LocalDate.now())
 *         set(it.salary, 50)
 *         set(it.departmentId, 1)
 *     }
 *     item {
 *         set(it.name, "linda")
 *         set(it.job, "assistant")
 *         set(it.managerId, 3)
 *         set(it.hireDate, LocalDate.now())
 *         set(it.salary, 100)
 *         set(it.departmentId, 2)
 *     }
 * }
 * ```
 *
 * @param block the DSL block, extension function of [BatchInsertStatementBuilder], used to construct the expressions.
 * @return the effected row counts for each sub-operation.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <T : BaseTable<*>> T.batchInsert(block: BatchInsertStatementBuilder<T>.() -> Unit): IntArray {
    return Database.global.batchInsert(this, block)
}

/**
 * Construct an insert expression in the given closure, then execute it and return the auto-generated key.
 *
 * This function assumes that at least one auto-generated key will be returned, and that the first key in
 * the result set will be the primary key for the row.
 *
 * Usage:
 *
 * ```kotlin
 * val id = Employees.insertAndGenerateKey {
 *     set(it.name, "jerry")
 *     set(it.job, "trainee")
 *     set(it.managerId, 1)
 *     set(it.hireDate, LocalDate.now())
 *     set(it.salary, 50)
 *     set(it.departmentId, 1)
 * }
 * ```
 *
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the first auto-generated key.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <T : BaseTable<*>> T.insertAndGenerateKey(block: AssignmentsBuilder.(T) -> Unit): Any {
    return Database.global.insertAndGenerateKey(this, block)
}

/**
 * Delete the records in the table that matches the given [predicate].
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <T : BaseTable<*>> T.delete(predicate: (T) -> ColumnDeclaring<Boolean>): Int {
    return Database.global.delete(this, predicate)
}

/**
 * Delete all the records in the table.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun BaseTable<*>.deleteAll(): Int {
    return Database.global.deleteAll(this)
}
