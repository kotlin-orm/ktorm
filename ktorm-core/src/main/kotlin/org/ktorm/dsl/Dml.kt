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

package org.ktorm.dsl

import org.ktorm.database.CachedRowSet
import org.ktorm.database.Database
import org.ktorm.expression.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import java.sql.Statement

/**
 * Construct an update expression in the given closure, then execute it and return the effected row count.
 *
 * Usage:
 *
 * ```kotlin
 * database.update(Employees) {
 *     set(it.job, "engineer")
 *     set(it.managerId, null)
 *     set(it.salary, 100)
 *     where {
 *         it.id eq 2
 *     }
 * }
 * ```
 *
 * @since 2.7
 * @param table the table to be updated.
 * @param block the DSL block, an extension function of [UpdateStatementBuilder], used to construct the expression.
 * @return the effected row count.
 */
public fun <T : BaseTable<*>> Database.update(table: T, block: UpdateStatementBuilder.(T) -> Unit): Int {
    val builder = UpdateStatementBuilder().apply { block(table) }
    if (builder.assignments.isEmpty()) {
        throw IllegalArgumentException("There are no columns to update in the statement.")
    }

    val expression = dialect.createExpressionVisitor(AliasRemover).visit(
        UpdateExpression(table.asExpression(), builder.assignments, builder.where?.asExpression())
    )

    return executeUpdate(expression)
}

/**
 * Construct update expressions in the given closure, then batch execute them and return the effected
 * row counts for each expression.
 *
 * Note that this function is implemented based on [Statement.addBatch] and [Statement.executeBatch],
 * and any item in a batch operation must have the same structure, otherwise an exception will be thrown.
 *
 * Usage:
 *
 * ```kotlin
 * database.batchUpdate(Departments) {
 *     for (i in 1..2) {
 *         item {
 *             set(it.location, "Hong Kong")
 *             where {
 *                 it.id eq i
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @since 2.7
 * @param table the table to be updated.
 * @param block the DSL block, extension function of [BatchUpdateStatementBuilder], used to construct the expressions.
 * @return the effected row counts for each sub-operation.
 */
public fun <T : BaseTable<*>> Database.batchUpdate(
    table: T,
    block: BatchUpdateStatementBuilder<T>.() -> Unit
): IntArray {
    val builder = BatchUpdateStatementBuilder(table).apply(block)
    if (builder.expressions.isEmpty()) {
        throw IllegalArgumentException("There are no items in the batch operation.")
    }
    for (expr in builder.expressions) {
        if (expr.assignments.isEmpty()) {
            throw IllegalArgumentException("There are no columns to update in the statement.")
        }
    }

    val expressions = builder.expressions.map { dialect.createExpressionVisitor(AliasRemover).visit(it) }

    if (expressions.isEmpty()) {
        return IntArray(0)
    } else {
        return executeBatch(expressions)
    }
}

/**
 * Construct an insert expression in the given closure, then execute it and return the effected row count.
 *
 * Usage:
 *
 * ```kotlin
 * database.insert(Employees) {
 *     set(it.name, "jerry")
 *     set(it.job, "trainee")
 *     set(it.managerId, 1)
 *     set(it.hireDate, LocalDate.now())
 *     set(it.salary, 50)
 *     set(it.departmentId, 1)
 * }
 * ```
 *
 * @since 2.7
 * @param table the table to be inserted.
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the effected row count.
 */
public fun <T : BaseTable<*>> Database.insert(table: T, block: AssignmentsBuilder.(T) -> Unit): Int {
    val builder = AssignmentsBuilder().apply { block(table) }
    if (builder.assignments.isEmpty()) {
        throw IllegalArgumentException("There are no columns to insert in the statement.")
    }

    val expression = dialect.createExpressionVisitor(AliasRemover).visit(
        InsertExpression(table.asExpression(), builder.assignments)
    )

    return executeUpdate(expression)
}

/**
 * Construct an insert expression in the given closure, then execute it and return the auto-generated key.
 *
 * This function assumes that at least one auto-generated key will be returned, and that the first key in
 * the result set will be the primary key for the row.
 *
 * Usage:
 *
 * ```kotlin
 * val id = database.insertAndGenerateKey(Employees) {
 *     set(it.name, "jerry")
 *     set(it.job, "trainee")
 *     set(it.managerId, 1)
 *     set(it.hireDate, LocalDate.now())
 *     set(it.salary, 50)
 *     set(it.departmentId, 1)
 * }
 * ```
 *
 * @since 2.7
 * @param table the table to be inserted.
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the first auto-generated key.
 */
public fun <T : BaseTable<*>> Database.insertAndGenerateKey(table: T, block: AssignmentsBuilder.(T) -> Unit): Any {
    val builder = AssignmentsBuilder().apply { block(table) }
    if (builder.assignments.isEmpty()) {
        throw IllegalArgumentException("There are no columns to insert in the statement.")
    }

    val expression = dialect.createExpressionVisitor(AliasRemover).visit(
        InsertExpression(table.asExpression(), builder.assignments)
    )

    val (_, rowSet) = executeUpdateAndRetrieveKeys(expression)

    if (rowSet.next()) {
        val pk = table.singlePrimaryKey { "Key retrieval is not supported for compound primary keys." }
        val generatedKey = rowSet.getGeneratedKey(pk) ?: error("Generated key is null.")

        if (logger.isDebugEnabled()) {
            logger.debug("Generated Key: $generatedKey")
        }

        return generatedKey
    } else {
        error("No generated key returns by database.")
    }
}

/**
 * Get generated key from the row set.
 */
internal fun <T : Any> CachedRowSet.getGeneratedKey(primaryKey: Column<T>): T? {
    if (metaData.columnCount == 1) {
        return primaryKey.sqlType.getResult(this, 1)
    }

    for (index in 1..metaData.columnCount) {
        if (metaData.getColumnName(index).equals(primaryKey.name, ignoreCase = true)) {
            return primaryKey.sqlType.getResult(this, index)
        }
    }

    throw IllegalStateException("Cannot find column `${primaryKey.name}` in the returned row set.")
}

/**
 * Construct insert expressions in the given closure, then batch execute them and return the effected
 * row counts for each expression.
 *
 * Note that this function is implemented based on [Statement.addBatch] and [Statement.executeBatch],
 * and any item in a batch operation must have the same structure, otherwise an exception will be thrown.
 *
 * Usage:
 *
 * ```kotlin
 * database.batchInsert(Employees) {
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
 * @param block the DSL block, extension function of [BatchInsertStatementBuilder], used to construct the expressions.
 * @return the effected row counts for each sub-operation.
 */
public fun <T : BaseTable<*>> Database.batchInsert(
    table: T,
    block: BatchInsertStatementBuilder<T>.() -> Unit
): IntArray {
    val builder = BatchInsertStatementBuilder(table).apply(block)
    if (builder.expressions.isEmpty()) {
        throw IllegalArgumentException("There are no items in the batch operation.")
    }
    for (expr in builder.expressions) {
        if (expr.assignments.isEmpty()) {
            throw IllegalArgumentException("There are no columns to insert in the statement.")
        }
    }

    val expressions = builder.expressions.map { dialect.createExpressionVisitor(AliasRemover).visit(it) }

    if (expressions.isEmpty()) {
        return IntArray(0)
    } else {
        return executeBatch(expressions)
    }
}

/**
 * Insert the current [Query]'s results into the given table, useful when transfer data from a table to another table.
 */
public fun Query.insertTo(table: BaseTable<*>, vararg columns: Column<*>): Int {
    if (columns.isEmpty()) {
        throw IllegalArgumentException("There are no columns to insert in the statement.")
    }

    val expression = InsertFromQueryExpression(
        table = table.asExpression(),
        columns = columns.map { it.asExpression() },
        query = this.expression
    )

    return database.executeUpdate(expression)
}

/**
 * Delete the records in the [table] that matches the given [predicate].
 *
 * @since 2.7
 */
public fun <T : BaseTable<*>> Database.delete(table: T, predicate: (T) -> ColumnDeclaring<Boolean>): Int {
    val expression = dialect.createExpressionVisitor(AliasRemover).visit(
        DeleteExpression(table.asExpression(), predicate(table).asExpression())
    )

    return executeUpdate(expression)
}

/**
 * Delete all the records in the table.
 *
 * @since 2.7
 */
public fun Database.deleteAll(table: BaseTable<*>): Int {
    val expression = dialect.createExpressionVisitor(AliasRemover).visit(
        DeleteExpression(table.asExpression(), where = null)
    )

    return executeUpdate(expression)
}

/**
 * Marker annotation for Ktorm DSL builder classes.
 */
@DslMarker
public annotation class KtormDsl

/**
 * Base class of DSL builders, provide basic functions used to build assignments for insert or update DSL.
 */
@KtormDsl
public open class AssignmentsBuilder {
    @Suppress("VariableNaming")
    protected val _assignments: ArrayList<ColumnAssignmentExpression<*>> = ArrayList()

    /**
     * A getter that returns the readonly view of the built assignments list.
     */
    internal val assignments: List<ColumnAssignmentExpression<*>> get() = _assignments

    /**
     * Assign the specific column's value to another column or an expression's result.
     *
     * @since 3.1.0
     */
    public fun <C : Any> set(column: Column<C>, expr: ColumnDeclaring<C>) {
        _assignments += ColumnAssignmentExpression(column.asExpression(), expr.asExpression())
    }

    /**
     * Assign the specific column to a value.
     *
     * @since 3.1.0
     */
    public fun <C : Any> set(column: Column<C>, value: C?) {
        _assignments += ColumnAssignmentExpression(column.asExpression(), column.wrapArgument(value))
    }
}

/**
 * DSL builder for update statements.
 */
@KtormDsl
public class UpdateStatementBuilder : AssignmentsBuilder() {
    internal var where: ColumnDeclaring<Boolean>? = null

    /**
     * Specify the where clause for this update statement.
     */
    public fun where(block: () -> ColumnDeclaring<Boolean>) {
        this.where = block()
    }
}

/**
 * DSL builder for batch update statements.
 */
@KtormDsl
public class BatchUpdateStatementBuilder<T : BaseTable<*>>(internal val table: T) {
    internal val expressions = ArrayList<UpdateExpression>()

    /**
     * Add an update statement to the current batch operation.
     */
    public fun item(block: UpdateStatementBuilder.(T) -> Unit) {
        val builder = UpdateStatementBuilder()
        builder.block(table)

        expressions += UpdateExpression(table.asExpression(), builder.assignments, builder.where?.asExpression())
    }
}

/**
 * DSL builder for batch insert statements.
 */
@KtormDsl
public class BatchInsertStatementBuilder<T : BaseTable<*>>(internal val table: T) {
    internal val expressions = ArrayList<InsertExpression>()

    /**
     * Add an insert statement to the current batch operation.
     */
    public fun item(block: AssignmentsBuilder.(T) -> Unit) {
        val builder = AssignmentsBuilder()
        builder.block(table)

        expressions += InsertExpression(table.asExpression(), builder.assignments)
    }
}

/**
 * Expression visitor interceptor for removing table aliases, used by Ktorm internally.
 */
public object AliasRemover : SqlExpressionVisitorInterceptor {

    override fun intercept(expr: SqlExpression, visitor: SqlExpressionVisitor): SqlExpression? {
        if (expr is TableExpression) {
            if (expr.tableAlias == null) {
                return expr
            } else {
                return expr.copy(tableAlias = null)
            }
        }

        return null
    }
}
