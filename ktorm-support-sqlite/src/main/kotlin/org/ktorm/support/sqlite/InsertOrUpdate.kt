/*
 * Copyright 2018-2022 the original author or authors.
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
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.KtormDsl
import org.ktorm.expression.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring

/**
 * Insert or update expression, represents an insert statement with an
 * `on conflict (key) do update set` clause in SQLite.
 *
 * @property table the table to be inserted.
 * @property assignments the inserted column assignments.
 * @property conflictColumns the index columns on which the conflict may happen.
 * @property updateAssignments the updated column assignments while any key conflict exists.
 * @property where the condition whether the update assignments should be executed.
 */
public data class InsertOrUpdateExpression(
    val table: TableExpression,
    val assignments: List<ColumnAssignmentExpression<*>>,
    val conflictColumns: List<ColumnExpression<*>> = emptyList(),
    val updateAssignments: List<ColumnAssignmentExpression<*>> = emptyList(),
    val where: ScalarExpression<Boolean>? = null,
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
 *     onConflict(it.id) {
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
 * @param table the table to be inserted.
 * @param block the DSL block used to construct the expression.
 * @return the effected row count.
 */
public fun <T : BaseTable<*>> Database.insertOrUpdate(
    table: T, block: InsertOrUpdateStatementBuilder.(T) -> Unit
): Int {
    val expression = AliasRemover.visit(buildInsertOrUpdateExpression(table, block = block))
    return executeUpdate(expression)
}

/**
 * Build an insert or update expression.
 */
private fun <T : BaseTable<*>> buildInsertOrUpdateExpression(
    table: T, block: InsertOrUpdateStatementBuilder.(T) -> Unit
): InsertOrUpdateExpression {
    val builder = InsertOrUpdateStatementBuilder().apply { block(table) }

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
        where = builder.where?.asExpression()
    )
}

/**
 * Base class of SQLite DSL builders, provide basic functions used to build assignments for insert or update DSL.
 */
@KtormDsl
public open class SQLiteAssignmentsBuilder : AssignmentsBuilder() {

    /**
     * A getter that returns the readonly view of the built assignments list.
     */
    internal val assignments: List<ColumnAssignmentExpression<*>> get() = _assignments
}

/**
 * DSL builder for insert or update statements.
 */
@KtormDsl
public class InsertOrUpdateStatementBuilder : SQLiteAssignmentsBuilder() {
    internal val conflictColumns = ArrayList<Column<*>>()
    internal val updateAssignments = ArrayList<ColumnAssignmentExpression<*>>()
    internal var doNothing = false
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

/**
 * DSL builder for insert or update on conflict clause.
 */
@KtormDsl
public class InsertOrUpdateOnConflictClauseBuilder : SQLiteAssignmentsBuilder() {
    internal var doNothing = false
    internal var where: ColumnDeclaring<Boolean>? = null

    /**
     * Explicitly tells ktorm to ignore any on-conflict errors and continue insertion.
     */
    public fun doNothing() {
        this.doNothing = true
    }

    /**
     * Specify the where condition for the update clause.
     */
    public fun where(block: () -> ColumnDeclaring<Boolean>) {
        this.where = block()
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

/**
 * [SQLiteExpressionVisitor] implementation used to removed table aliases, used by Ktorm internal.
 */
internal object AliasRemover : SQLiteExpressionVisitor() {

    override fun visitTable(expr: TableExpression): TableExpression {
        if (expr.tableAlias == null) {
            return expr
        } else {
            return expr.copy(tableAlias = null)
        }
    }
}
