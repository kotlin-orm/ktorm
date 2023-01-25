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

package org.ktorm.support.sqlite

import org.ktorm.database.Database
import org.ktorm.database.asIterable
import org.ktorm.dsl.AliasRemover
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.KtormDsl
import org.ktorm.dsl.batchInsert
import org.ktorm.expression.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring

/**
 * Bulk insert expression, represents a bulk insert statement in SQLite.
 *
 * For example:
 *
 * ```sql
 * insert into table (column1, column2)
 * values (?, ?), (?, ?), (?, ?)...
 * on conflict (...) do update set ...`
 * ```
 *
 * @property table the table to be inserted.
 * @property assignments column assignments of the bulk insert statement.
 * @property conflictColumns the index columns on which the conflict may happen.
 * @property updateAssignments the updated column assignments while key conflict exists.
 * @property where the condition whether the update assignments should be executed.
 * @property returningColumns the returning columns.
 */
public data class BulkInsertExpression(
    val table: TableExpression,
    val assignments: List<List<ColumnAssignmentExpression<*>>>,
    val conflictColumns: List<ColumnExpression<*>> = emptyList(),
    val updateAssignments: List<ColumnAssignmentExpression<*>> = emptyList(),
    val where: ScalarExpression<Boolean>? = null,
    val returningColumns: List<ColumnExpression<*>> = emptyList(),
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * Bulk insert records to the table and return the effected row count.
 *
 * The usage is almost the same as [batchInsert], but this function is implemented by generating a special SQL
 * using SQLite bulk insert syntax, instead of based on JDBC batch operations. For this reason, its performance
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
 * @param table the table to be inserted.
 * @param block the DSL block, extension function of [BulkInsertStatementBuilder], used to construct the expression.
 * @return the effected row count.
 * @see batchInsert
 */
public fun <T : BaseTable<*>> Database.bulkInsert(
    table: T, block: BulkInsertStatementBuilder<T>.(T) -> Unit
): Int {
    val expression = buildBulkInsertExpression(table, returning = emptyList(), block = block)
    return executeUpdate(expression)
}

/**
 * Bulk insert records to the table and return the specific column's values.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsertReturning(Employees, Employees.id) {
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
 * Generated SQL:
 *
 * ```sql
 * insert into table (name, job, manager_id, hire_date, salary, department_id)
 * values (?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?)...
 * returning id
 * ```
 *
 * @since 3.6.0
 * @param table the table to be inserted.
 * @param returning the column to return
 * @param block the DSL block, extension function of [BulkInsertStatementBuilder], used to construct the expression.
 * @return the returning column's values.
 */
public fun <T : BaseTable<*>, C : Any> Database.bulkInsertReturning(
    table: T, returning: Column<C>, block: BulkInsertStatementBuilder<T>.(T) -> Unit
): List<C?> {
    val expression = buildBulkInsertExpression(table, listOf(returning), block)
    val rowSet = executeQuery(expression)
    return rowSet.asIterable().map { row -> returning.sqlType.getResult(row, 1) }
}

/**
 * Bulk insert records to the table and return the specific columns' values.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsertReturning(Employees, Pair(Employees.id, Employees.job)) {
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
 * Generated SQL:
 *
 * ```sql
 * insert into table (name, job, manager_id, hire_date, salary, department_id)
 * values (?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?)...
 * returning id, job
 * ```
 *
 * @since 3.6.0
 * @param table the table to be inserted.
 * @param returning the columns to return
 * @param block the DSL block, extension function of [BulkInsertStatementBuilder], used to construct the expression.
 * @return the returning columns' values.
 */
public fun <T : BaseTable<*>, C1 : Any, C2 : Any> Database.bulkInsertReturning(
    table: T, returning: Pair<Column<C1>, Column<C2>>, block: BulkInsertStatementBuilder<T>.(T) -> Unit
): List<Pair<C1?, C2?>> {
    val (c1, c2) = returning
    val expression = buildBulkInsertExpression(table, listOf(c1, c2), block)
    val rowSet = executeQuery(expression)
    return rowSet.asIterable().map { row -> Pair(c1.sqlType.getResult(row, 1), c2.sqlType.getResult(row, 2)) }
}

/**
 * Bulk insert records to the table and return the specific columns' values.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsertReturning(Employees, Triple(Employees.id, Employees.job, Employees.salary)) {
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
 * Generated SQL:
 *
 * ```sql
 * insert into table (name, job, manager_id, hire_date, salary, department_id)
 * values (?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?)...
 * returning id, job, salary
 * ```
 *
 * @since 3.6.0
 * @param table the table to be inserted.
 * @param returning the columns to return
 * @param block the DSL block, extension function of [BulkInsertStatementBuilder], used to construct the expression.
 * @return the returning columns' values.
 */
public fun <T : BaseTable<*>, C1 : Any, C2 : Any, C3 : Any> Database.bulkInsertReturning(
    table: T, returning: Triple<Column<C1>, Column<C2>, Column<C3>>, block: BulkInsertStatementBuilder<T>.(T) -> Unit
): List<Triple<C1?, C2?, C3?>> {
    val (c1, c2, c3) = returning
    val expression = buildBulkInsertExpression(table, listOf(c1, c2, c3), block)
    val rowSet = executeQuery(expression)
    return rowSet.asIterable().map { row ->
        Triple(c1.sqlType.getResult(row, 1), c2.sqlType.getResult(row, 2), c3.sqlType.getResult(row, 3))
    }
}

/**
 * Bulk insert records to the table, returning row set.
 */
private fun <T : BaseTable<*>> Database.buildBulkInsertExpression(
    table: T, returning: List<Column<*>>, block: BulkInsertStatementBuilder<T>.(T) -> Unit
): SqlExpression {
    val builder = BulkInsertStatementBuilder(table).apply { block(table) }
    if (builder.assignments.isEmpty()) {
        throw IllegalArgumentException("There are no items in the bulk operation.")
    }
    for (assignments in builder.assignments) {
        if (assignments.isEmpty()) {
            throw IllegalArgumentException("There are no columns to insert in the statement.")
        }
    }

    return dialect.createExpressionVisitor(AliasRemover).visit(
        BulkInsertExpression(
            table = table.asExpression(),
            assignments = builder.assignments,
            returningColumns = returning.map { it.asExpression() }
        )
    )
}

/**
 * Bulk insert records to the table, determining if there is a key conflict while inserting each of them,
 * and automatically performs updates if any conflict exists.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsertOrUpdate(Employees) {
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
 * on conflict (id) do update set salary = t_employee.salary + ?
 * ```
 *
 * @param table the table to be inserted.
 * @param block the DSL block used to construct the expression.
 * @return the effected row count.
 */
public fun <T : BaseTable<*>> Database.bulkInsertOrUpdate(
    table: T, block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit
): Int {
    val expression = buildBulkInsertOrUpdateExpression(table, returning = emptyList(), block = block)
    return executeUpdate(expression)
}

/**
 * Bulk insert records to the table, determining if there is a key conflict while inserting each of them,
 * automatically performs updates if any conflict exists, and finally returns the specific column.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsertOrUpdateReturning(Employees, Employees.id) {
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
 * on conflict (id) do update set salary = t_employee.salary + ?
 * returning id
 * ```
 *
 * @since 3.6.0
 * @param table the table to be inserted.
 * @param returning the column to return
 * @param block the DSL block used to construct the expression.
 * @return the returning column's values.
 */
public fun <T : BaseTable<*>, C : Any> Database.bulkInsertOrUpdateReturning(
    table: T, returning: Column<C>, block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit
): List<C?> {
    val expression = buildBulkInsertOrUpdateExpression(table, listOf(returning), block)
    val rowSet = executeQuery(expression)
    return rowSet.asIterable().map { row -> returning.sqlType.getResult(row, 1) }
}

/**
 * Bulk insert records to the table, determining if there is a key conflict while inserting each of them,
 * automatically performs updates if any conflict exists, and finally returns the specific columns.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsertOrUpdateReturning(Employees, Pair(Employees.id, Employees.job)) {
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
 * on conflict (id) do update set salary = t_employee.salary + ?
 * returning id, job
 * ```
 *
 * @since 3.6.0
 * @param table the table to be inserted.
 * @param returning the columns to return
 * @param block the DSL block used to construct the expression.
 * @return the returning columns' values.
 */
public fun <T : BaseTable<*>, C1 : Any, C2 : Any> Database.bulkInsertOrUpdateReturning(
    table: T, returning: Pair<Column<C1>, Column<C2>>, block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit
): List<Pair<C1?, C2?>> {
    val (c1, c2) = returning
    val expression = buildBulkInsertOrUpdateExpression(table, listOf(c1, c2), block)
    val rowSet = executeQuery(expression)
    return rowSet.asIterable().map { row -> Pair(c1.sqlType.getResult(row, 1), c2.sqlType.getResult(row, 2)) }
}

/**
 * Bulk insert records to the table, determining if there is a key conflict while inserting each of them,
 * automatically performs updates if any conflict exists, and finally returns the specific columns.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsertOrUpdateReturning(Employees, Triple(Employees.id, Employees.job, Employees.salary)) {
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
 * on conflict (id) do update set salary = t_employee.salary + ?
 * returning id, job, salary
 * ```
 *
 * @since 3.6.0
 * @param table the table to be inserted.
 * @param returning the columns to return
 * @param block the DSL block used to construct the expression.
 * @return the returning columns' values.
 */
public fun <T : BaseTable<*>, C1 : Any, C2 : Any, C3 : Any> Database.bulkInsertOrUpdateReturning(
    table: T,
    returning: Triple<Column<C1>, Column<C2>, Column<C3>>,
    block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit
): List<Triple<C1?, C2?, C3?>> {
    val (c1, c2, c3) = returning
    val expression = buildBulkInsertOrUpdateExpression(table, listOf(c1, c2, c3), block)
    val rowSet = executeQuery(expression)
    return rowSet.asIterable().map { row ->
        Triple(c1.sqlType.getResult(row, 1), c2.sqlType.getResult(row, 2), c3.sqlType.getResult(row, 3))
    }
}

/**
 * Build a bulk insert or update expression.
 */
private fun <T : BaseTable<*>> Database.buildBulkInsertOrUpdateExpression(
    table: T, returning: List<Column<*>>, block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit
): SqlExpression {
    val builder = BulkInsertOrUpdateStatementBuilder(table).apply { block(table) }
    if (builder.assignments.isEmpty()) {
        throw IllegalArgumentException("There are no items in the bulk operation.")
    }
    for (assignments in builder.assignments) {
        if (assignments.isEmpty()) {
            throw IllegalArgumentException("There are no columns to insert in the statement.")
        }
    }

    val conflictColumns = builder.conflictColumns.ifEmpty { table.primaryKeys }
    if (conflictColumns.isEmpty()) {
        val msg = "" +
            "Table '$table' doesn't have a primary key, " +
            "you must specify the conflict columns when calling onConflict(col) { .. }"
        throw IllegalStateException(msg)
    }

    if (!builder.doNothing && builder.updateAssignments.isEmpty()) {
        val msg = "" +
            "Cannot leave the onConflict clause empty! " +
            "If you desire no update action at all please explicitly call `doNothing()`"
        throw IllegalStateException(msg)
    }

    return dialect.createExpressionVisitor(AliasRemover).visit(
        BulkInsertExpression(
            table = table.asExpression(),
            assignments = builder.assignments,
            conflictColumns = conflictColumns.map { it.asExpression() },
            updateAssignments = if (builder.doNothing) emptyList() else builder.updateAssignments,
            where = builder.where?.asExpression(),
            returningColumns = returning.map { it.asExpression() }
        )
    )
}

/**
 * DSL builder for bulk insert statements.
 */
@KtormDsl
public open class BulkInsertStatementBuilder<T : BaseTable<*>>(internal val table: T) {
    internal val assignments = ArrayList<List<ColumnAssignmentExpression<*>>>()

    /**
     * Add the assignments of a new row to the bulk insert.
     */
    public fun item(block: AssignmentsBuilder.() -> Unit) {
        val builder = SQLiteAssignmentsBuilder().apply(block)

        if (assignments.isEmpty()
            || assignments[0].map { it.column.name } == builder.assignments.map { it.column.name }
        ) {
            assignments += builder.assignments
        } else {
            throw IllegalArgumentException("Every item in a batch operation must be the same.")
        }
    }
}

/**
 * DSL builder for bulk insert or update statements.
 */
@KtormDsl
public class BulkInsertOrUpdateStatementBuilder<T : BaseTable<*>>(table: T) : BulkInsertStatementBuilder<T>(table) {
    internal val conflictColumns = ArrayList<Column<*>>()
    internal val updateAssignments = ArrayList<ColumnAssignmentExpression<*>>()
    internal var doNothing: Boolean = false
    internal var where: ColumnDeclaring<Boolean>? = null

    /**
     * Specify the update assignments while any key conflict exists.
     */
    public fun onConflict(vararg columns: Column<*>, block: InsertOrUpdateOnConflictClauseBuilder.() -> Unit) {
        val builder = InsertOrUpdateOnConflictClauseBuilder().apply(block)
        this.conflictColumns += columns
        this.updateAssignments += builder.assignments
        this.doNothing = builder.doNothing
        this.where = builder.where
    }
}
