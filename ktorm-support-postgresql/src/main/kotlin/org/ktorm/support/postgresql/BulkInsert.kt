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

package org.ktorm.support.postgresql

import org.ktorm.database.Database
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.KtormDsl
import org.ktorm.dsl.batchInsert
import org.ktorm.expression.ColumnAssignmentExpression
import org.ktorm.expression.ColumnExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.expression.TableExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import java.util.*
import kotlin.collections.ArrayList

// We leave some prepared statement parameters reserved for the query dialect building process
private const val RESERVED_SQL_EXPR_BATCH_SIZE = 100

// Max number of assignments we allow per batch in Postgresql (Max size as defined by Postgresql - reserved)
private const val MAX_SQL_EXPR_BATCH_SIZE = Short.MAX_VALUE - RESERVED_SQL_EXPR_BATCH_SIZE

/**
 * Bulk insert expression, represents a bulk insert statement in PostgreSQL.
 *
 * For example:
 *
 * ```sql
 * insert into table (column1, column2) values (?, ?), (?, ?), (?, ?)...
 * on conflict (...) do update set ...`
 * ```
 *
 * @property table the table to be inserted.
 * @property assignments column assignments of the bulk insert statement.
 * @property conflictColumns the index columns on which the conflict may happens.
 * @property updateAssignments the updated column assignments while key conflict exists.
 */
public data class BulkInsertExpression(
    val table: TableExpression,
    val assignments: List<List<ColumnAssignmentExpression<*>>>,
    val conflictColumns: List<ColumnExpression<*>>? = null,
    val updateAssignments: List<ColumnAssignmentExpression<*>> = emptyList(),
    val returningColumns: List<ColumnExpression<*>> = emptyList(),
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

/**
 * Construct a bulk insert expression in the given closure, then execute it and return the effected row count.
 *
 * The usage is almost the same as [batchInsert], but this function is implemented by generating a special SQL
 * using PostgreSQL's bulk insert syntax, instead of based on JDBC batch operations. For this reason, its performance
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
 * @since 3.3.0
 * @param table the table to be inserted.
 * @param block the DSL block, extension function of [BulkInsertStatementBuilder], used to construct the expression.
 * @return the effected row count.
 * @see batchInsert
 */
public fun <T : BaseTable<*>> Database.bulkInsert(
    table: T, block: BulkInsertStatementBuilder<T>.(T) -> Unit
): Int {
    var affectedTotal = 0

    val builder = BulkInsertStatementBuilder(table).apply { block(table) }

    if (builder.assignments.isEmpty()) return 0

    executeQueryInBatches(builder) { assignments ->
        val expression = BulkInsertExpression(
            table = table.asExpression(),
            assignments = assignments
        )

        val total = executeUpdate(expression)

        affectedTotal += total
    }

    return affectedTotal
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
 * @since 3.3.0
 * @param table the table to be inserted.
 * @param block the DSL block used to construct the expression.
 * @return the effected row count.
 * @see bulkInsert
 */
public fun <T : BaseTable<*>> Database.bulkInsertOrUpdate(
    table: T, block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit
): Int {
    var affectedTotal = 0

    val builder = BulkInsertOrUpdateStatementBuilder(table).apply { block(table) }

    if (builder.assignments.isEmpty()) return 0

    val conflictColumns = builder.conflictColumns.ifEmpty { table.primaryKeys }
    if (conflictColumns.isEmpty()) {
        val msg =
            "Table '$table' doesn't have a primary key, " +
            "you must specify the conflict columns when calling onConflict(col) { .. }"
        throw IllegalStateException(msg)
    }

    if (!builder.explicitlyDoNothing && builder.updateAssignments.isEmpty()) {
        val msg = "You cannot leave a on-conflict clause empty! If you desire no update action at all" +
            " you must explicitly invoke `doNothing()`"
        throw IllegalStateException(msg)
    }

    executeQueryInBatches(builder) { assignments ->
        val expression = BulkInsertExpression(
            table = table.asExpression(),
            assignments = assignments,
            conflictColumns = conflictColumns.map { it.asExpression() },
            updateAssignments = if (builder.explicitlyDoNothing) emptyList() else builder.updateAssignments,
        )

        val total = executeUpdate(expression)

        affectedTotal += total
    }

    return affectedTotal
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
        val builder = PostgreSqlAssignmentsBuilder().apply(block)

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
    internal val updateAssignments = ArrayList<ColumnAssignmentExpression<*>>()
    internal val conflictColumns = ArrayList<Column<*>>()

    internal var explicitlyDoNothing: Boolean = false

    /**
     * Specify the update assignments while any key conflict exists.
     */
    public fun onConflict(vararg columns: Column<*>, block: InsertOrUpdateOnConflictClauseBuilder.() -> Unit) {
        val builder = InsertOrUpdateOnConflictClauseBuilder().apply(block)

        explicitlyDoNothing = builder.explicitlyDoNothing

        updateAssignments += builder.assignments

        conflictColumns += columns
    }
}

/**
 * Construct a bulk insert expression in the given closure, then execute it and return the effected row count.
 *
 * The usage is almost the same as [batchInsert], but this function is implemented by generating a special SQL
 * using PostgreSQL's bulk insert syntax, instead of based on JDBC batch operations. For this reason, its performance
 * is much better than [batchInsert].
 *
 * The generated SQL is like: `insert into table (column1, column2) values (?, ?), (?, ?), (?, ?)...
 * returning id`.
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
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param block the DSL block, extension function of [BulkInsertStatementBuilder], used to construct the expression.
 * @param returningColumn the column to return
 * @return the returning column value.
 * @see batchInsert
 */
public fun <T : BaseTable<*>, R : Any> Database.bulkInsertReturning(
    table: T,
    returningColumn: Column<R>,
    block: BulkInsertStatementBuilder<T>.(T) -> Unit
): List<R?> {
    val (_, rowSet) = this.bulkInsertReturningAux(
        table,
        listOf(returningColumn),
        block
    )

    return rowSet.asIterable().map { row ->
        returningColumn.sqlType.getResult(row, 1)
    }
}

/**
 * Construct a bulk insert expression in the given closure, then execute it and return the effected row count.
 *
 * The usage is almost the same as [batchInsert], but this function is implemented by generating a special SQL
 * using PostgreSQL's bulk insert syntax, instead of based on JDBC batch operations. For this reason, its performance
 * is much better than [batchInsert].
 *
 * The generated SQL is like: `insert into table (column1, column2) values (?, ?), (?, ?), (?, ?)...
 * returning id, job`.
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
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param block the DSL block, extension function of [BulkInsertStatementBuilder], used to construct the expression.
 * @param returningColumns the columns to return
 * @return the returning columns' values.
 * @see batchInsert
 */
public fun <T : BaseTable<*>, R1 : Any, R2 : Any> Database.bulkInsertReturning(
    table: T,
    returningColumns: Pair<Column<R1>, Column<R2>>,
    block: BulkInsertStatementBuilder<T>.(T) -> Unit
): List<Pair<R1?, R2?>> {
    val (_, rowSet) = this.bulkInsertReturningAux(
        table,
        returningColumns.toList(),
        block
    )

    return rowSet.asIterable().map { row ->
        Pair(
            returningColumns.first.sqlType.getResult(row, 1),
            returningColumns.second.sqlType.getResult(row, 2)
        )
    }
}

/**
 * Construct a bulk insert expression in the given closure, then execute it and return the effected row count.
 *
 * The usage is almost the same as [batchInsert], but this function is implemented by generating a special SQL
 * using PostgreSQL's bulk insert syntax, instead of based on JDBC batch operations. For this reason, its performance
 * is much better than [batchInsert].
 *
 * The generated SQL is like: `insert into table (column1, column2) values (?, ?), (?, ?), (?, ?)...
 * returning id, job, salary`.
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
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param block the DSL block, extension function of [BulkInsertStatementBuilder], used to construct the expression.
 * @param returningColumns the columns to return
 * @return the returning columns' values.
 * @see batchInsert
 */
public fun <T : BaseTable<*>, R1 : Any, R2 : Any, R3 : Any> Database.bulkInsertReturning(
    table: T,
    returningColumns: Triple<Column<R1>, Column<R2>, Column<R3>>,
    block: BulkInsertStatementBuilder<T>.(T) -> Unit
): List<Triple<R1?, R2?, R3?>> {
    val (_, rowSet) = this.bulkInsertReturningAux(
        table,
        returningColumns.toList(),
        block
    )

    return rowSet.asIterable().map { row ->
        var i = 0
        Triple(
            returningColumns.first.sqlType.getResult(row, ++i),
            returningColumns.second.sqlType.getResult(row, ++i),
            returningColumns.third.sqlType.getResult(row, ++i)
        )
    }
}

private fun <T : BaseTable<*>> Database.bulkInsertReturningAux(
    table: T,
    returningColumns: List<Column<*>>,
    block: BulkInsertStatementBuilder<T>.(T) -> Unit
): Pair<Int, CompositeCachedRowSet> {
    var affectedTotal = 0
    val cachedRowSets = CompositeCachedRowSet()

    val builder = BulkInsertStatementBuilder(table).apply { block(table) }

    if (builder.assignments.isEmpty()) return Pair(0, CompositeCachedRowSet())

    executeQueryInBatches(builder) { assignments ->
        val expression = BulkInsertExpression(
            table.asExpression(),
            assignments,
            returningColumns = returningColumns.map { it.asExpression() }
        )

        val (total, rows) = executeUpdateAndRetrieveKeys(expression)

        affectedTotal += total
        cachedRowSets.add(rows)
    }

    return Pair(affectedTotal, cachedRowSets)
}

/**
 * Bulk insert records to the table, determining if there is a key conflict while inserting each of them,
 * and automatically performs updates if any conflict exists.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsertOrUpdateReturning(Employees, Pair(Employees.id, Employees.name)) {
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
 * returning id, job, ...
 * ```
 *
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param block the DSL block used to construct the expression.
 * @param returningColumn the column to return
 * @return the returning column value.
 * @see bulkInsert
 */
public fun <T : BaseTable<*>, R : Any> Database.bulkInsertOrUpdateReturning(
    table: T,
    returningColumn: Column<R>,
    block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit
): List<R?> {
    val (_, rowSet) = this.bulkInsertOrUpdateReturningAux(
        table,
        listOf(returningColumn),
        block
    )

    return rowSet.asIterable().map { row ->
        returningColumn.sqlType.getResult(row, 1)
    }
}

/**
 * Bulk insert records to the table, determining if there is a key conflict while inserting each of them,
 * and automatically performs updates if any conflict exists.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsertOrUpdateReturning(Employees, Pair(Employees.id, Employees.name)) {
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
 * returning id, job, ...
 * ```
 *
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param block the DSL block used to construct the expression.
 * @param returningColumns the column to return
 * @return the returning columns' values.
 * @see bulkInsert
 */
public fun <T : BaseTable<*>, R1 : Any, R2 : Any> Database.bulkInsertOrUpdateReturning(
    table: T,
    returningColumns: Pair<Column<R1>, Column<R2>>,
    block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit
): List<Pair<R1?, R2?>> {
    val (_, rowSet) = this.bulkInsertOrUpdateReturningAux(
        table,
        returningColumns.toList(),
        block
    )

    return rowSet.asIterable().map { row ->
        Pair(
            returningColumns.first.sqlType.getResult(row, 1),
            returningColumns.second.sqlType.getResult(row, 2)
        )
    }
}

/**
 * Bulk insert records to the table, determining if there is a key conflict while inserting each of them,
 * and automatically performs updates if any conflict exists.
 *
 * Usage:
 *
 * ```kotlin
 * database.bulkInsertOrUpdateReturning(Employees, Pair(Employees.id, Employees.name, Employees.salary)) {
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
 * returning id, job, ...
 * ```
 *
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param block the DSL block used to construct the expression.
 * @param returningColumns the column to return
 * @return the returning columns' values.
 * @see bulkInsert
 */
public fun <T : BaseTable<*>, R1 : Any, R2 : Any, R3 : Any> Database.bulkInsertOrUpdateReturning(
    table: T,
    returningColumns: Triple<Column<R1>, Column<R2>, Column<R3>>,
    block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit
): List<Triple<R1?, R2?, R3?>> {
    val (_, rowSet) = this.bulkInsertOrUpdateReturningAux(
        table,
        returningColumns.toList(),
        block
    )

    return rowSet.asIterable().map { row ->
        var i = 0
        Triple(
            returningColumns.first.sqlType.getResult(row, ++i),
            returningColumns.second.sqlType.getResult(row, ++i),
            returningColumns.third.sqlType.getResult(row, ++i)
        )
    }
}

private fun <T : BaseTable<*>> Database.bulkInsertOrUpdateReturningAux(
    table: T,
    returningColumns: List<Column<*>>,
    block: BulkInsertOrUpdateStatementBuilder<T>.(T) -> Unit
): Pair<Int, CompositeCachedRowSet> {
    var affectedTotal = 0
    val cachedRowSets = CompositeCachedRowSet()

    val builder = BulkInsertOrUpdateStatementBuilder(table).apply { block(table) }

    if (builder.assignments.isEmpty()) return Pair(0, CompositeCachedRowSet())

    val conflictColumns = builder.conflictColumns.ifEmpty { table.primaryKeys }
    if (conflictColumns.isEmpty()) {
        val msg =
            "Table '$table' doesn't have a primary key, " +
                "you must specify the conflict columns when calling onConflict(col) { .. }"
        throw IllegalStateException(msg)
    }

    if (!builder.explicitlyDoNothing && builder.updateAssignments.isEmpty()) {
        val msg = "You cannot leave a on-conflict clause empty! If you desire no update action at all" +
            " you must explicitly invoke `doNothing()`"
        throw IllegalStateException(msg)
    }

    executeQueryInBatches(builder) { assignments ->
        val expression = BulkInsertExpression(
            table = table.asExpression(),
            assignments = assignments,
            conflictColumns = conflictColumns.map { it.asExpression() },
            updateAssignments = if (builder.explicitlyDoNothing) emptyList() else builder.updateAssignments,
            returningColumns = returningColumns.map { it.asExpression() }
        )

        val (total, rows) = executeUpdateAndRetrieveKeys(expression)

        affectedTotal += total
        cachedRowSets.add(rows)
    }

    return Pair(affectedTotal, cachedRowSets)
}

private fun <T : BaseTable<*>> executeQueryInBatches(
    builder: BulkInsertStatementBuilder<T>,
    execute: (List<List<ColumnAssignmentExpression<*>>>) -> Unit
) {
    var batchAssignmentCount = 0
    val currentBatch = LinkedList<List<ColumnAssignmentExpression<*>>>()
    builder.assignments.forEach { assignments ->
        assignments.size.let { size ->
            if (size > MAX_SQL_EXPR_BATCH_SIZE) {
                throw IllegalArgumentException(
                    "The maximum number of assignments per item is $MAX_SQL_EXPR_BATCH_SIZE, but $size detected!"
                )
            }

            if (batchAssignmentCount + size >= MAX_SQL_EXPR_BATCH_SIZE) {
                execute(currentBatch)
                currentBatch.clear()
                batchAssignmentCount = 0
            }

            currentBatch.add(assignments)
            batchAssignmentCount += size
        }
    }
    // Flush the remaining
    if (currentBatch.isNotEmpty()) {
        execute(currentBatch)
    }
}
