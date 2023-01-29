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

@file:Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")

package org.ktorm.support.postgresql

import org.ktorm.database.Database
import org.ktorm.dsl.batchInsert
import org.ktorm.schema.BaseTable
import java.lang.reflect.InvocationTargetException

/**
 * Obtain the global database instance via reflection, throwing an exception if ktorm-global is not
 * available in the classpath.
 */
@Suppress("SwallowedException")
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
internal val Database.Companion.global: Database get() {
    try {
        val cls = Class.forName("org.ktorm.global.GlobalKt")
        val method = cls.getMethod("getGlobal", Database.Companion::class.java)
        return method.invoke(null, Database.Companion) as Database
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("Cannot detect the global database object, please add ktorm-global to classpath", e)
    } catch (e: InvocationTargetException) {
        throw e.targetException
    }
}

/**
 * Insert a record to the table, determining if there is a key conflict while it's being inserted, and automatically
 * performs an update if any conflict exists.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.insertOrUpdate {
 *     set(it.id, 1)
 *     set(it.name, "vince")
 *     set(it.job, "engineer")
 *     set(it.salary, 1000)
 *     set(it.hireDate, LocalDate.now())
 *     set(it.departmentId, 1)
 *     onConflict {
 *         set(it.salary, it.salary + 900)
 *     }
 * }
 * ```
 *
 * Generated SQL:
 *
 * ```sql
 * insert into t_employee (id, name, job, salary, hire_date, department_id) values (?, ?, ?, ?, ?, ?)
 * on conflict (id) do update set salary = salary + ?
 * ```
 *
 * @param block the DSL block used to construct the expression.
 * @return the effected row count.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <T : BaseTable<*>> T.insertOrUpdate(block: InsertOrUpdateStatementBuilder.(T) -> Unit): Int {
    return Database.global.insertOrUpdate(this, block)
}

/**
 * Construct a bulk insert expression in the given closure, then execute it and return the
 * effected row count.
 *
 * The usage is almost the same as [batchInsert], but this function is implemented by generating a
 * special SQL using PostgreSQL bulk insert syntax, instead of based on JDBC batch operations.
 * For this reason, its performance is much better than [batchInsert].
 *
 * The generated SQL is like: `insert into table (column1, column2) values (?, ?), (?, ?), (?, ?)...`.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.bulkInsert {
 *     item {
 *         set(it.id, 1)
 *         set(it.name, "vince")
 *         set(it.job, "engineer")
 *         set(it.salary, 1000)
 *         set(it.hireDate, LocalDate.now())
 *         set(it.departmentId, 1)
 *     }
 *     item {
 *         set(it.id, 5)
 *         set(it.name, "vince")
 *         set(it.job, "engineer")
 *         set(it.salary, 1000)
 *         set(it.hireDate, LocalDate.now())
 *         set(it.departmentId, 1)
 *     }
 * }
 * ```
 *
 * @since 3.3.0
 * @param block the DSL block, extension function of [BulkInsertStatementBuilder], used to construct the expression.
 * @return the effected row count.
 * @see batchInsert
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <T : BaseTable<*>> T.bulkInsert(block: BulkInsertStatementBuilder<T>.(T) -> Unit): Int {
    return Database.global.bulkInsert(this, block)
}

/**
 * Bulk insert records to the table, determining if there is a key conflict while inserting each of them,
 * and automatically performs updates if any conflict exists.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.bulkInsertOrUpdate {
 *     item {
 *         set(it.id, 1)
 *         set(it.name, "vince")
 *         set(it.job, "engineer")
 *         set(it.salary, 1000)
 *         set(it.hireDate, LocalDate.now())
 *         set(it.departmentId, 1)
 *     }
 *     item {
 *         set(it.id, 5)
 *         set(it.name, "vince")
 *         set(it.job, "engineer")
 *         set(it.salary, 1000)
 *         set(it.hireDate, LocalDate.now())
 *         set(it.departmentId, 1)
 *     }
 *     onConflict {
 *         set(it.salary, it.salary + 900)
 *     }
 * }
 * ```
 *
 * Generated SQL:
 *
 * ```sql
 * insert into t_employee (id, name, job, salary, hire_date, department_id)
 * values (?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?)
 * on conflict (id) do update set salary = salary + ?
 * ```
 *
 * @since 3.3.0
 * @param block the DSL block used to construct the expression.
 * @return the effected row count.
 * @see bulkInsert
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun <T : BaseTable<*>> T.bulkInsertOrUpdate(block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit): Int {
    return Database.global.bulkInsertOrUpdate(this, block)
}
