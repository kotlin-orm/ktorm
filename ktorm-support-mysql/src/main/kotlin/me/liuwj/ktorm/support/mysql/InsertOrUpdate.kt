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

package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.AssignmentsBuilder
import me.liuwj.ktorm.dsl.KtormDsl
import me.liuwj.ktorm.expression.ColumnAssignmentExpression
import me.liuwj.ktorm.expression.ColumnExpression
import me.liuwj.ktorm.expression.SqlExpression
import me.liuwj.ktorm.expression.TableExpression
import me.liuwj.ktorm.schema.BaseTable

/**
 * Insert or update expression, represents an insert statement with an `on duplicate key update` clause in MySQL.
 *
 * @property table the table to be inserted.
 * @property assignments the inserted column assignments.
 * @property updateAssignments the updated column assignments while any key conflict exists.
 */
data class InsertOrUpdateExpression(
    val table: TableExpression,
    val assignments: List<ColumnAssignmentExpression<*>>,
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
 * on duplicate key update salary = salary + ?
 * ```
 *
 * @param block the DSL block used to construct the expression.
 * @return the effected row count.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "This function will be removed in the future. Please use database.insertOrUpdate(table) {...} instead.",
    replaceWith = ReplaceWith("database.insertOrUpdate(this, block)")
)
fun <T : BaseTable<*>> T.insertOrUpdate(block: InsertOrUpdateStatementBuilder.(T) -> Unit): Int {
    return Database.global.insertOrUpdate(this, block)
}

/**
 * Insert a record to the table, determining if there is a key conflict while it's being inserted, and automatically
 * performs an update if any conflict exists.
 *
 * Usage:
 *
 * ```kotlin
 * database.insertOrUpdate(Employees) {
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
 * on duplicate key update salary = salary + ?
 * ```
 *
 * @since 2.7
 * @param table the table to be inserted.
 * @param block the DSL block used to construct the expression.
 * @return the effected row count.
 */
fun <T : BaseTable<*>> Database.insertOrUpdate(table: T, block: InsertOrUpdateStatementBuilder.(T) -> Unit): Int {
    val assignments = ArrayList<ColumnAssignmentExpression<*>>()
    val builder = InsertOrUpdateStatementBuilder(assignments).apply { block(table) }

    val expr = AliasRemover.visit(
        InsertOrUpdateExpression(table.asExpression(), assignments, builder.updateAssignments)
    )

    return executeUpdate(expr)
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

/**
 * [MySqlExpressionVisitor] implementation used to removed table aliases, used by Ktorm internal.
 */
internal object AliasRemover : MySqlExpressionVisitor() {

    override fun visitTable(expr: TableExpression): TableExpression {
        if (expr.tableAlias == null) {
            return expr
        } else {
            return expr.copy(tableAlias = null)
        }
    }

    override fun <T : Any> visitColumn(expr: ColumnExpression<T>): ColumnExpression<T> {
        if (expr.tableAlias == null) {
            return expr
        } else {
            return expr.copy(tableAlias = null)
        }
    }
}
