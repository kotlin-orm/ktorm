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

import org.ktorm.database.CachedRowSet
import org.ktorm.database.Database
import org.ktorm.database.asIterable
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.KtormDsl
import org.ktorm.dsl.batchInsert
import org.ktorm.expression.ColumnAssignmentExpression
import org.ktorm.expression.ColumnExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.expression.TableExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column

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
    val conflictColumns: List<ColumnExpression<*>> = emptyList(),
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
    val builder = BulkInsertStatementBuilder(table).apply { block(table) }
    val expression = BulkInsertExpression(table.asExpression(), builder.assignments)
    return executeUpdate(expression)
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
    val builder = BulkInsertOrUpdateStatementBuilder(table).apply { block(table) }

    val conflictColumns = builder.conflictColumns.ifEmpty { table.primaryKeys }
    if (conflictColumns.isEmpty()) {
        val msg =
            "Table '$table' doesn't have a primary key, " +
                    "you must specify the conflict columns when calling onConflict(col) { .. }"
        throw IllegalStateException(msg)
    }

    val expression = BulkInsertExpression(
        table = table.asExpression(),
        assignments = builder.assignments,
        conflictColumns = conflictColumns.map { it.asExpression() },
        updateAssignments = builder.updateAssignments
    )

    return executeUpdate(expression)
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

    /**
     * Specify the update assignments while any key conflict exists.
     */
    public fun onConflict(vararg columns: Column<*>, block: BulkInsertOrUpdateOnConflictClauseBuilder.() -> Unit) {
        val builder = BulkInsertOrUpdateOnConflictClauseBuilder().apply(block)
        updateAssignments += builder.assignments
        conflictColumns += columns
    }
}

/**
 * DSL builder for bulk insert or update on conflict clause.
 */
@KtormDsl
public class BulkInsertOrUpdateOnConflictClauseBuilder : PostgreSqlAssignmentsBuilder() {

    /**
     * Reference the 'EXCLUDED' table in a ON CONFLICT clause.
     */
    public fun <T : Any> excluded(column: Column<T>): ColumnExpression<T> {
        // excluded.name
        return ColumnExpression(
            table = TableExpression(name = "excluded"),
            name = column.name,
            sqlType = column.sqlType
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
 * returning column1, column2, ...`.
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
 * @return the effected row count.
 * @see batchInsert
 */
public fun <T : BaseTable<*>, R : Any> Database.bulkInsertReturning(
    table: T,
    returningColumn: Column<R>,
    block: BulkInsertReturningStatementBuilder<T>.(T) -> Unit
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

public fun <T : BaseTable<*>, R1 : Any, R2 : Any> Database.bulkInsertReturning(
    table: T,
    returningColumns: Pair<Column<R1>, Column<R2>>,
    block: BulkInsertReturningStatementBuilder<T>.(T) -> Unit
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

public fun <T : BaseTable<*>, R1 : Any, R2 : Any, R3 : Any> Database.bulkInsertReturning(
    table: T,
    returningColumns: Triple<Column<R1>, Column<R2>, Column<R3>>,
    block: BulkInsertReturningStatementBuilder<T>.(T) -> Unit
): List<Triple<R1?, R2?, R3?>> {
    val (_, rowSet) = this.bulkInsertReturningAux(
        table,
        returningColumns.toList(),
        block
    )

    return rowSet.asIterable().map { row ->
        Triple(
            returningColumns.first.sqlType.getResult(row, 1),
            returningColumns.second.sqlType.getResult(row, 2),
            returningColumns.third.sqlType.getResult(row, 3)
        )
    }
}

private fun <T : BaseTable<*>> Database.bulkInsertReturningAux(
    table: T,
    returningColumns: List<Column<*>>,
    block: BulkInsertReturningStatementBuilder<T>.(T) -> Unit
): Pair<Int, CachedRowSet> {
    val builder = BulkInsertReturningStatementBuilder(table).apply { block(table) }

    val expression = BulkInsertExpression(
        table.asExpression(),
        builder.assignments,
        returningColumns = returningColumns.map { it.asExpression() }
    )

    return executeUpdateAndRetrieveKeys(expression)
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
 * @return the effected row count.
 * @see bulkInsert
 */
public fun <T : BaseTable<*>> Database.bulkInsertOrUpdateReturning(
    table: T, block: BulkInsertOrUpdateReturningStatementBuilder<T>.(T) -> Unit
): Pair<Int, CachedRowSet> {
    val builder = BulkInsertOrUpdateReturningStatementBuilder(table).apply { block(table) }

    val conflictColumns = builder.conflictColumns.ifEmpty { table.primaryKeys }
    if (conflictColumns.isEmpty()) {
        val msg =
            "Table '$table' doesn't have a primary key, " +
                    "you must specify the conflict columns when calling onConflict(col) { .. }"
        throw IllegalStateException(msg)
    }

    val expression = BulkInsertExpression(
        table = table.asExpression(),
        assignments = builder.assignments,
        conflictColumns = conflictColumns.map { it.asExpression() },
        updateAssignments = builder.updateAssignments,
        returningColumns = builder.returningColumns.ifEmpty { table.primaryKeys }.map { it.asExpression() }
    )

    return executeUpdateAndRetrieveKeys(expression)
}

/**
 * DSL builder for bulk insert statements.
 */
@KtormDsl
public open class BulkInsertReturningStatementBuilder<T : BaseTable<*>>(internal val table: T) {
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
public class BulkInsertOrUpdateReturningStatementBuilder<T : BaseTable<*>>(table: T) :
    BulkInsertStatementBuilder<T>(table) {
    internal val updateAssignments = ArrayList<ColumnAssignmentExpression<*>>()
    internal val conflictColumns = ArrayList<Column<*>>()
    internal val returningColumns = ArrayList<Column<*>>()

    /**
     * Specify the update assignments while any key conflict exists.
     */
    public fun onConflict(vararg columns: Column<*>, block: BulkInsertOrUpdateOnConflictClauseBuilder.() -> Unit) {
        val builder = BulkInsertOrUpdateOnConflictClauseBuilder().apply(block)
        updateAssignments += builder.assignments
        conflictColumns += columns
    }

    /**
     * Specify the columns to return.
     */
    public fun returning(vararg returningColumns: Column<*>) {
        this.returningColumns += returningColumns
    }
}
