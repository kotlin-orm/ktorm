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

package org.ktorm.support.mysql

import org.ktorm.database.Database
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.KtormDsl
import org.ktorm.dsl.batchInsert
import org.ktorm.expression.ColumnAssignmentExpression
import org.ktorm.expression.FunctionExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.expression.TableExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.ColumnDeclaring

/**
 * Bulk insert expression, represents a bulk insert statement in MySQL.
 *
 * For example: `insert into table (column1, column2) values (?, ?), (?, ?), (?, ?)...`.
 *
 * @property table the table to be inserted.
 * @property assignments column assignments of the bulk insert statement.
 * @property updateAssignments the updated column assignments while key conflict exists.
 */
public data class BulkInsertExpression(
    val table: TableExpression,
    val assignments: List<List<ColumnAssignmentExpression<*>>>,
    val updateAssignments: List<ColumnAssignmentExpression<*>> = emptyList(),
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * Construct a bulk insert expression in the given closure, then execute it and return the effected row count.
 *
 * The usage is almost the same as [batchInsert], but this function is implemented by generating a special SQL
 * using MySQL's bulk insert syntax, instead of based on JDBC batch operations. For this reason, its performance
 * is much better than [batchInsert].
 *
 * The generated SQL is like: `insert into table (column1, column2) values (?, ?), (?, ?), (?, ?)...`.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsert(Employees) {
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
 * @since 2.7
 * @param table the table to be inserted.
 * @param block the DSL block, extension function of [BulkInsertStatementBuilder], used to construct the expression.
 * @return the effected row count.
 * @see batchInsert
 */
public fun <T : BaseTable<*>> Database.bulkInsert(table: T, block: BulkInsertStatementBuilder<T>.() -> Unit): Int {
    val builder = BulkInsertStatementBuilder(table).apply(block)

    val expr = AliasRemover.visit(
        BulkInsertExpression(table.asExpression(), builder.assignments, builder.updateAssignments)
    )

    return executeUpdate(expr)
}

/**
 * DSL builder for bulk insert statements.
 */
@KtormDsl
public class BulkInsertStatementBuilder<T : BaseTable<*>>(internal val table: T) {
    internal val assignments = ArrayList<List<ColumnAssignmentExpression<*>>>()
    internal val updateAssignments = ArrayList<ColumnAssignmentExpression<*>>()

    /**
     * Add the assignments of a new row to the bulk insert.
     */
    public fun item(block: AssignmentsBuilder.(T) -> Unit) {
        val builder = MySqlAssignmentsBuilder()
        builder.block(table)

        if (assignments.isEmpty()
            || assignments[0].map { it.column.name } == builder.assignments.map { it.column.name }
        ) {
            assignments += builder.assignments
        } else {
            throw IllegalArgumentException("Every item in a batch operation must be the same.")
        }
    }

    /**
     * Specify the update assignments while any key conflict exists.
     */
    public fun onDuplicateKey(block: BulkInsertOnDuplicateKeyClauseBuilder.(T) -> Unit) {
        val builder = BulkInsertOnDuplicateKeyClauseBuilder()
        builder.block(table)
        updateAssignments += builder.assignments
    }
}

/**
 * DSL builder for bulk insert assignments.
 */
@KtormDsl
public class BulkInsertOnDuplicateKeyClauseBuilder : MySqlAssignmentsBuilder() {

    /**
     * Use VALUES() function in a ON DUPLICATE KEY UPDATE clause.
     */
    public fun <T : Any> values(expr: ColumnDeclaring<T>): FunctionExpression<T> {
        // values(expr)
        return FunctionExpression(
            functionName = "values",
            arguments = listOf(expr.asExpression()),
            sqlType = expr.sqlType
        )
    }
}
