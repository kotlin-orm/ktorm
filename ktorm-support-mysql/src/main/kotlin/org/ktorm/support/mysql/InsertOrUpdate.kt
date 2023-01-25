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

package org.ktorm.support.mysql

import org.ktorm.database.Database
import org.ktorm.dsl.AliasRemover
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.KtormDsl
import org.ktorm.expression.ColumnAssignmentExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.expression.TableExpression
import org.ktorm.schema.BaseTable

/**
 * Insert or update expression, represents an insert statement with an
 * `on duplicate key update` clause in MySQL.
 *
 * @property table the table to be inserted.
 * @property assignments the inserted column assignments.
 * @property updateAssignments the updated column assignments while any key conflict exists.
 */
public data class InsertOrUpdateExpression(
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
 * database.insertOrUpdate(Employees) {
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
 * on duplicate key update salary = salary + ?
 * ```
 *
 * @since 2.7
 * @param table the table to be inserted.
 * @param block the DSL block used to construct the expression.
 * @return the effected row count.
 */
public fun <T : BaseTable<*>> Database.insertOrUpdate(
    table: T, block: InsertOrUpdateStatementBuilder.(T) -> Unit
): Int {
    val builder = InsertOrUpdateStatementBuilder().apply { block(table) }
    if (builder.assignments.isEmpty()) {
        throw IllegalArgumentException("There are no columns to insert in the statement.")
    }

    val expression = dialect.createExpressionVisitor(AliasRemover).visit(
        InsertOrUpdateExpression(table.asExpression(), builder.assignments, builder.updateAssignments)
    )

    return executeUpdate(expression)
}

/**
 * Base class of MySQL DSL builders, provide basic functions used to build assignments for insert or update DSL.
 */
@KtormDsl
public open class MySqlAssignmentsBuilder : AssignmentsBuilder() {

    /**
     * A getter that returns the readonly view of the built assignments list.
     */
    internal val assignments: List<ColumnAssignmentExpression<*>> get() = _assignments
}

/**
 * DSL builder for insert or update statements.
 */
@KtormDsl
public class InsertOrUpdateStatementBuilder : MySqlAssignmentsBuilder() {
    internal val updateAssignments = ArrayList<ColumnAssignmentExpression<*>>()

    /**
     * Specify the update assignments while any key conflict exists.
     */
    public fun onDuplicateKey(block: AssignmentsBuilder.() -> Unit) {
        val builder = MySqlAssignmentsBuilder().apply(block)
        updateAssignments += builder.assignments
    }
}
