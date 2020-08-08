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

package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.schema.BaseTable
import java.lang.reflect.InvocationTargetException

/**
 * Obtain the global database instance via reflection, throwing an exception if ktorm-global is not
 * available in the classpath.
 */
@Suppress("SwallowedException")
internal val Database.Companion.global: Database get() {
    try {
        val cls = Class.forName("me.liuwj.ktorm.global.GlobalKt")
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
 *     onDuplicateKey {
 *         set(it.salary, it.salary + 900)
 *     }
 * }
 * ```
 *
 * Generated SQL:
 *
 * ```sql
 * insert into t_employee (id, name, job, salary, hire_date, department_id) values (?, ?, ?, ?, ?, ?)
 * on conflict (id) do update set salary = t_employee.salary + ?
 * ```
 *
 * @param block the DSL block used to construct the expression.
 * @return the effected row count.
 */
fun <T : BaseTable<*>> T.insertOrUpdate(block: InsertOrUpdateStatementBuilder.(T) -> Unit): Int {
    return Database.global.insertOrUpdate(this, block)
}
