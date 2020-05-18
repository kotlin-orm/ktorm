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

package me.liuwj.ktorm.global

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.ColumnDeclaring
import java.sql.Statement

/**
 * Construct an update expression in the given closure, then execute it and return the effected row count.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.update {
 *     it.job to "engineer"
 *     it.managerId to null
 *     it.salary to 100
 *     where {
 *         it.id eq 2
 *     }
 * }
 * ```
 *
 * @param block the DSL block, an extension function of [UpdateStatementBuilder], used to construct the expression.
 * @return the effected row count.
 */
fun <T : BaseTable<*>> T.update(block: UpdateStatementBuilder.(T) -> Unit): Int {
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
 *             it.location to "Hong Kong"
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
fun <T : BaseTable<*>> T.batchUpdate(block: BatchUpdateStatementBuilder<T>.() -> Unit): IntArray {
    return Database.global.batchUpdate(this, block)
}

/**
 * Construct an insert expression in the given closure, then execute it and return the effected row count.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.insert {
 *     it.name to "jerry"
 *     it.job to "trainee"
 *     it.managerId to 1
 *     it.hireDate to LocalDate.now()
 *     it.salary to 50
 *     it.departmentId to 1
 * }
 * ```
 *
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the effected row count.
 */
fun <T : BaseTable<*>> T.insert(block: AssignmentsBuilder.(T) -> Unit): Int {
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
 *         it.name to "jerry"
 *         it.job to "trainee"
 *         it.managerId to 1
 *         it.hireDate to LocalDate.now()
 *         it.salary to 50
 *         it.departmentId to 1
 *     }
 *     item {
 *         it.name to "linda"
 *         it.job to "assistant"
 *         it.managerId to 3
 *         it.hireDate to LocalDate.now()
 *         it.salary to 100
 *         it.departmentId to 2
 *     }
 * }
 * ```
 *
 * @param block the DSL block, extension function of [BatchInsertStatementBuilder], used to construct the expressions.
 * @return the effected row counts for each sub-operation.
 */
fun <T : BaseTable<*>> T.batchInsert(block: BatchInsertStatementBuilder<T>.() -> Unit): IntArray {
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
 *     it.name to "jerry"
 *     it.job to "trainee"
 *     it.managerId to 1
 *     it.hireDate to LocalDate.now()
 *     it.salary to 50
 *     it.departmentId to 1
 * }
 * ```
 *
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the first auto-generated key.
 */
fun <T : BaseTable<*>> T.insertAndGenerateKey(block: AssignmentsBuilder.(T) -> Unit): Any {
    return Database.global.insertAndGenerateKey(this, block)
}

/**
 * Delete the records in the table that matches the given [predicate].
 */
fun <T : BaseTable<*>> T.delete(predicate: (T) -> ColumnDeclaring<Boolean>): Int {
    return Database.global.delete(this, predicate)
}

/**
 * Delete all the records in the table.
 */
fun BaseTable<*>.deleteAll(): Int {
    return Database.global.deleteAll(this)
}
