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

package org.ktorm.support.postgresql

import org.ktorm.database.CachedRowSet
import org.ktorm.database.Database
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.KtormDsl
import org.ktorm.expression.ColumnAssignmentExpression
import org.ktorm.expression.ColumnExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.expression.TableExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column

/**
 * Insert or update expression, represents an insert statement with an
 * `on conflict (key) do update set` clause in PostgreSQL.
 *
 * @property table the table to be inserted.
 * @property assignments the inserted column assignments.
 * @property conflictColumns the index columns on which the conflict may happen.
 * @property updateAssignments the updated column assignments while any key conflict exists.
 * @property returningColumns the returning columns.
 */
public data class InsertOrUpdateExpression(
    val table: TableExpression,
    val assignments: List<ColumnAssignmentExpression<*>>,
    val conflictColumns: List<ColumnExpression<*>> = emptyList(),
    val updateAssignments: List<ColumnAssignmentExpression<*>> = emptyList(),
    val returningColumns: List<ColumnExpression<*>> = emptyList(),
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
 * values (?, ?, ?, ?, ?, ?)
 * on conflict (id) do update set salary = t_employee.salary + ?
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
    val expression = buildInsertOrUpdateExpression(table, returning = emptyList(), block = block)
    return executeUpdate(expression)
}

/**
 * Insert a record to the table, determining if there is a key conflict while it's being inserted, automatically
 * performs an update if any conflict exists, and finally returns the specific column.
 *
 * Usage:
 *
 * ```kotlin
 * val id = database.insertOrUpdateReturning(Employees, Employees.id) {
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
 * insert into t_employee (id, name, job, salary, hire_date, department_id)
 * values (?, ?, ?, ?, ?, ?)
 * on conflict (id) do update set salary = t_employee.salary + ?
 * returning id
 * ```
 *
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param returning the column to return
 * @param block the DSL block used to construct the expression.
 * @return the returning column's value.
 */
public fun <T : BaseTable<*>, C : Any> Database.insertOrUpdateReturning(
    table: T, returning: Column<C>, block: InsertOrUpdateStatementBuilder.(T) -> Unit
): C? {
    val row = insertOrUpdateReturningRow(table, listOf(returning), block)
    if (row == null) {
        return null
    } else {
        return returning.sqlType.getResult(row, 1)
    }
}

/**
 * Insert a record to the table, determining if there is a key conflict while it's being inserted, automatically
 * performs an update if any conflict exists, and finally returns the specific columns.
 *
 * Usage:
 *
 * ```kotlin
 * val (id, job) = database.insertOrUpdateReturning(Employees, Pair(Employees.id, Employees.job)) {
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
 * insert into t_employee (id, name, job, salary, hire_date, department_id)
 * values (?, ?, ?, ?, ?, ?)
 * on conflict (id) do update set salary = t_employee.salary + ?
 * returning id, job
 * ```
 *
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param returning the columns to return
 * @param block the DSL block used to construct the expression.
 * @return the returning columns' values.
 */
public fun <T : BaseTable<*>, C1 : Any, C2 : Any> Database.insertOrUpdateReturning(
    table: T, returning: Pair<Column<C1>, Column<C2>>, block: InsertOrUpdateStatementBuilder.(T) -> Unit
): Pair<C1?, C2?> {
    val (c1, c2) = returning
    val row = insertOrUpdateReturningRow(table, listOf(c1, c2), block)
    if (row == null) {
        return Pair(null, null)
    } else {
        return Pair(c1.sqlType.getResult(row, 1), c2.sqlType.getResult(row, 2))
    }
}

/**
 * Insert a record to the table, determining if there is a key conflict while it's being inserted, automatically
 * performs an update if any conflict exists, and finally returns the specific columns.
 *
 * Usage:
 *
 * ```kotlin
 * val (id, job, salary) =
 * database.insertOrUpdateReturning(Employees, Triple(Employees.id, Employees.job, Employees.salary)) {
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
 * insert into t_employee (id, name, job, salary, hire_date, department_id)
 * values (?, ?, ?, ?, ?, ?)
 * on conflict (id) do update set salary = t_employee.salary + ?
 * returning id, job, salary
 * ```
 *
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param returning the columns to return
 * @param block the DSL block used to construct the expression.
 * @return the returning columns' values.
 */
public fun <T : BaseTable<*>, C1 : Any, C2 : Any, C3 : Any> Database.insertOrUpdateReturning(
    table: T, returning: Triple<Column<C1>, Column<C2>, Column<C3>>, block: InsertOrUpdateStatementBuilder.(T) -> Unit
): Triple<C1?, C2?, C3?> {
    val (c1, c2, c3) = returning
    val row = insertOrUpdateReturningRow(table, listOf(c1, c2, c3), block)
    if (row == null) {
        return Triple(null, null, null)
    } else {
        return Triple(c1.sqlType.getResult(row, 1), c2.sqlType.getResult(row, 2), c3.sqlType.getResult(row, 3))
    }
}

/**
 * Insert or update, returning one row.
 */
private fun <T : BaseTable<*>> Database.insertOrUpdateReturningRow(
    table: T, returning: List<Column<*>>, block: InsertOrUpdateStatementBuilder.(T) -> Unit
): CachedRowSet? {
    val expression = buildInsertOrUpdateExpression(table, returning, block)
    val (_, rowSet) = executeUpdateAndRetrieveKeys(expression)

    if (rowSet.size() == 0) {
        // Possible when using onConflict { doNothing() }
        return null
    }

    if (rowSet.size() == 1) {
        check(rowSet.next())
        return rowSet
    } else {
        val (sql, _) = formatExpression(expression, beautifySql = true)
        throw IllegalStateException("Expected 1 row but ${rowSet.size()} returned from sql: \n\n$sql")
    }
}

/**
 * Build an insert or update expression.
 */
private fun <T : BaseTable<*>> buildInsertOrUpdateExpression(
    table: T, returning: List<Column<*>>, block: InsertOrUpdateStatementBuilder.(T) -> Unit
): InsertOrUpdateExpression {
    val builder = InsertOrUpdateStatementBuilder().apply { block(table) }
    if (builder.assignments.isEmpty()) {
        throw IllegalArgumentException("There are no columns to insert in the statement.")
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

    return InsertOrUpdateExpression(
        table = table.asExpression(),
        assignments = builder.assignments,
        conflictColumns = conflictColumns.map { it.asExpression() },
        updateAssignments = if (builder.doNothing) emptyList() else builder.updateAssignments,
        returningColumns = returning.map { it.asExpression() }
    )
}

/**
 * Insert a record to the table and return the specific column.
 *
 * Usage:
 *
 * ```kotlin
 * val id = database.insertReturning(Employees, Employees.id) {
 *     set(it.id, 1)
 *     set(it.name, "vince")
 *     set(it.job, "engineer")
 *     set(it.salary, 1000)
 *     set(it.hireDate, LocalDate.now())
 *     set(it.departmentId, 1)
 * }
 * ```
 *
 * Generated SQL:
 *
 * ```sql
 * insert into t_employee (id, name, job, salary, hire_date, department_id)
 * values (?, ?, ?, ?, ?, ?)
 * returning id
 * ```
 *
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param returning the column to return
 * @param block the DSL block used to construct the expression.
 * @return the returning column's value.
 */
public fun <T : BaseTable<*>, C : Any> Database.insertReturning(
    table: T, returning: Column<C>, block: AssignmentsBuilder.(T) -> Unit
): C? {
    val row = insertReturningRow(table, listOf(returning), block)
    return returning.sqlType.getResult(row, 1)
}

/**
 * Insert a record to the table and return the specific columns.
 *
 * Usage:
 *
 * ```kotlin
 * val (id, job) = database.insertReturning(Employees, Pair(Employees.id, Employees.job)) {
 *     set(it.id, 1)
 *     set(it.name, "vince")
 *     set(it.job, "engineer")
 *     set(it.salary, 1000)
 *     set(it.hireDate, LocalDate.now())
 *     set(it.departmentId, 1)
 * }
 * ```
 *
 * Generated SQL:
 *
 * ```sql
 * insert into t_employee (id, name, job, salary, hire_date, department_id)
 * values (?, ?, ?, ?, ?, ?)
 * returning id, job
 * ```
 *
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param returning the columns to return
 * @param block the DSL block used to construct the expression.
 * @return the returning columns' values.
 */
public fun <T : BaseTable<*>, C1 : Any, C2 : Any> Database.insertReturning(
    table: T, returning: Pair<Column<C1>, Column<C2>>, block: AssignmentsBuilder.(T) -> Unit
): Pair<C1?, C2?> {
    val (c1, c2) = returning
    val row = insertReturningRow(table, listOf(c1, c2), block)
    return Pair(c1.sqlType.getResult(row, 1), c2.sqlType.getResult(row, 2))
}

/**
 * Insert a record to the table and return the specific columns.
 *
 * Usage:
 *
 * ```kotlin
 * val (id, job, salary) =
 * database.insertReturning(Employees, Triple(Employees.id, Employees.job, Employees.salary)) {
 *     set(it.id, 1)
 *     set(it.name, "vince")
 *     set(it.job, "engineer")
 *     set(it.salary, 1000)
 *     set(it.hireDate, LocalDate.now())
 *     set(it.departmentId, 1)
 * }
 * ```
 *
 * Generated SQL:
 *
 * ```sql
 * insert into t_employee (id, name, job, salary, hire_date, department_id)
 * values (?, ?, ?, ?, ?, ?)
 * returning id, job, salary
 * ```
 *
 * @since 3.4.0
 * @param table the table to be inserted.
 * @param returning the columns to return
 * @param block the DSL block used to construct the expression.
 * @return the returning columns' values.
 */
public fun <T : BaseTable<*>, C1 : Any, C2 : Any, C3 : Any> Database.insertReturning(
    table: T, returning: Triple<Column<C1>, Column<C2>, Column<C3>>, block: AssignmentsBuilder.(T) -> Unit
): Triple<C1?, C2?, C3?> {
    val (c1, c2, c3) = returning
    val row = insertReturningRow(table, listOf(c1, c2, c3), block)
    return Triple(c1.sqlType.getResult(row, 1), c2.sqlType.getResult(row, 2), c3.sqlType.getResult(row, 3))
}

/**
 * Insert and returning one row.
 */
private fun <T : BaseTable<*>> Database.insertReturningRow(
    table: T, returning: List<Column<*>>, block: AssignmentsBuilder.(T) -> Unit
): CachedRowSet {
    val builder = PostgreSqlAssignmentsBuilder().apply { block(table) }
    if (builder.assignments.isEmpty()) {
        throw IllegalArgumentException("There are no columns to insert in the statement.")
    }

    val expression = InsertOrUpdateExpression(
        table = table.asExpression(),
        assignments = builder.assignments,
        returningColumns = returning.map { it.asExpression() }
    )

    val (_, rowSet) = executeUpdateAndRetrieveKeys(expression)

    if (rowSet.size() == 1) {
        check(rowSet.next())
        return rowSet
    } else {
        val (sql, _) = formatExpression(expression, beautifySql = true)
        throw IllegalStateException("Expected 1 row but ${rowSet.size()} returned from sql: \n\n$sql")
    }
}

/**
 * Base class of PostgreSQL DSL builders, provide basic functions used to build assignments for insert or update DSL.
 */
@KtormDsl
public open class PostgreSqlAssignmentsBuilder : AssignmentsBuilder() {

    /**
     * A getter that returns the readonly view of the built assignments list.
     */
    internal val assignments: List<ColumnAssignmentExpression<*>> get() = _assignments
}

/**
 * DSL builder for insert or update statements.
 */
@KtormDsl
public class InsertOrUpdateStatementBuilder : PostgreSqlAssignmentsBuilder() {
    internal val conflictColumns = ArrayList<Column<*>>()
    internal val updateAssignments = ArrayList<ColumnAssignmentExpression<*>>()
    internal var doNothing = false

    /**
     * Specify the update assignments while any key conflict exists.
     */
    public fun onConflict(vararg columns: Column<*>, block: InsertOrUpdateOnConflictClauseBuilder.() -> Unit) {
        val builder = InsertOrUpdateOnConflictClauseBuilder().apply(block)
        this.conflictColumns += columns
        this.updateAssignments += builder.assignments
        this.doNothing = builder.doNothing
    }
}

/**
 * DSL builder for insert or update on conflict clause.
 */
@KtormDsl
public class InsertOrUpdateOnConflictClauseBuilder : PostgreSqlAssignmentsBuilder() {
    internal var doNothing = false

    /**
     * Explicitly tells ktorm to ignore any on-conflict errors and continue insertion.
     */
    public fun doNothing() {
        this.doNothing = true
    }

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
