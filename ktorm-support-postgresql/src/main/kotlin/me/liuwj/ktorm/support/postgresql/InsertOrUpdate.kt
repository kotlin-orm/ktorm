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

package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.prepareStatement
import me.liuwj.ktorm.dsl.AssignmentsBuilder
import me.liuwj.ktorm.dsl.KtormDsl
import me.liuwj.ktorm.expression.ColumnAssignmentExpression
import me.liuwj.ktorm.expression.ColumnExpression
import me.liuwj.ktorm.expression.SqlExpression
import me.liuwj.ktorm.expression.TableExpression
import me.liuwj.ktorm.schema.Table

/**
 * Insert or update expression, represents an insert statement with an
 * `on conflict (key) do update set` clause in PostgreSQL.
 *
 * @property table the table to be inserted.
 * @property assignments the inserted column assignments.
 * @property conflictTarget the index columns on which the conflict may happens.
 * @property updateAssignments the updated column assignments while any key conflict exists.
 */
data class InsertOrUpdateExpression(
    val table: TableExpression,
    val assignments: List<ColumnAssignmentExpression<*>>,
    val conflictTarget: List<ColumnExpression<*>>,
    val updateAssignments: List<ColumnAssignmentExpression<*>> = emptyList(),
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * Insert a record to the table, determining if there is a key conflict while it's being inserted, and automatically
 * performs an update if any conflict exists.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.insertOrUpdate {
 *     it.id to 1
 *     it.name to "vince"
 *     it.job to "engineer"
 *     it.salary to 1000
 *     it.hireDate to LocalDate.now()
 *     it.departmentId to 1
 *     onDuplicateKey {
 *         it.salary to it.salary + 900
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
fun <T : Table<*>> T.insertOrUpdate(block: InsertOrUpdateStatementBuilder.(T) -> Unit): Int {
    val assignments = ArrayList<ColumnAssignmentExpression<*>>()
    val builder = InsertOrUpdateStatementBuilder(assignments).apply { block(this@insertOrUpdate) }

    val primaryKey = this.primaryKey ?: error("Table $tableName doesn't have a primary key.")

    val expression = InsertOrUpdateExpression(
        table = asExpression(),
        assignments = assignments,
        conflictTarget = listOf(primaryKey.asExpression()),
        updateAssignments = builder.updateAssignments
    )

    expression.prepareStatement { statement ->
        val effects = statement.executeUpdate()

        val logger = Database.global.logger
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug("Effects: $effects")
        }

        return effects
    }
}

/**
 * DSL builder for insert or update statements.
 */
@KtormDsl
class InsertOrUpdateStatementBuilder(
    assignments: MutableList<ColumnAssignmentExpression<*>>
) : AssignmentsBuilder(assignments) {

    internal val updateAssignments = ArrayList<ColumnAssignmentExpression<*>>()

    /**
     * Specify the update assignments while any key conflict exists.
     */
    fun onDuplicateKey(block: AssignmentsBuilder.() -> Unit) {
        val assignments = ArrayList<ColumnAssignmentExpression<*>>()
        AssignmentsBuilder(assignments).apply(block)
        updateAssignments += assignments
    }
}
