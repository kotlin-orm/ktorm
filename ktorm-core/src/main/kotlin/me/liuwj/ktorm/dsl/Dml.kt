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

package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.defaultValue
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.sql.PreparedStatement
import java.sql.Statement
import kotlin.collections.ArrayList

/**
 * Construct an update expression in the given closure, then execute it and return the effected row count.
 *
 * Usage:
 *
 * ```kotlin
 * database.update(Employees) {
 *     it.job to "engineer"
 *     it.managerId to null
 *     it.salary to 100
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
fun <T : BaseTable<*>> Database.update(table: T, block: UpdateStatementBuilder.(T) -> Unit): Int {
    val assignments = ArrayList<ColumnAssignmentExpression<*>>()
    val builder = UpdateStatementBuilder(assignments).apply { block(table) }

    val expression = AliasRemover.visit(
        UpdateExpression(table.asExpression(), assignments, builder.where?.asExpression())
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
 *             it.location to "Hong Kong"
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
fun <T : BaseTable<*>> Database.batchUpdate(table: T, block: BatchUpdateStatementBuilder<T>.() -> Unit): IntArray {
    val builder = BatchUpdateStatementBuilder(this, table).apply(block)
    val expressions = builder.expressions.map { AliasRemover.visit(it) }

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
 *     it.name to "jerry"
 *     it.job to "trainee"
 *     it.managerId to 1
 *     it.hireDate to LocalDate.now()
 *     it.salary to 50
 *     it.departmentId to 1
 * }
 * ```
 *
 * @since 2.7
 * @param table the table to be inserted.
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the effected row count.
 */
fun <T : BaseTable<*>> Database.insert(table: T, block: AssignmentsBuilder.(T) -> Unit): Int {
    val assignments = ArrayList<ColumnAssignmentExpression<*>>()
    AssignmentsBuilder(assignments).block(table)

    val expression = AliasRemover.visit(InsertExpression(table.asExpression(), assignments))
    return executeUpdate(expression)
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
 *         it.name to "jerry"
 *         it.job to "trainee"
 *         it.managerId to 1
 *         it.hireDate to LocalDate.now()
 *         it.salary to 50
 *         it.departmentId to 1
 *     }
 *     item {
 *         it.name to "linda"
 *         it.job to "assistant"
 *         it.managerId to 3
 *         it.hireDate to LocalDate.now()
 *         it.salary to 100
 *         it.departmentId to 2
 *     }
 * }
 * ```
 *
 * @since 2.7
 * @param table the table to be inserted.
 * @param block the DSL block, extension function of [BatchInsertStatementBuilder], used to construct the expressions.
 * @return the effected row counts for each sub-operation.
 */
fun <T : BaseTable<*>> Database.batchInsert(table: T, block: BatchInsertStatementBuilder<T>.() -> Unit): IntArray {
    val builder = BatchInsertStatementBuilder(this, table).apply(block)
    val expressions = builder.expressions.map { AliasRemover.visit(it) }

    if (expressions.isEmpty()) {
        return IntArray(0)
    } else {
        return executeBatch(expressions)
    }
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
 *     it.name to "jerry"
 *     it.job to "trainee"
 *     it.managerId to 1
 *     it.hireDate to LocalDate.now()
 *     it.salary to 50
 *     it.departmentId to 1
 * }
 * ```
 *
 * @since 2.7
 * @param table the table to be inserted.
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the first auto-generated key.
 */
fun <T : BaseTable<*>> Database.insertAndGenerateKey(table: T, block: AssignmentsBuilder.(T) -> Unit): Any {
    val assignments = ArrayList<ColumnAssignmentExpression<*>>()
    AssignmentsBuilder(assignments).block(table)

    val expression = AliasRemover.visit(InsertExpression(table.asExpression(), assignments))
    val (_, rowSet) = executeUpdateAndRetrieveKeys(expression)

    if (rowSet.next()) {
        val primaryKeys = table.primaryKeys
        if (primaryKeys.isEmpty()) {
            error("Table ${table.tableName} must have a primary key.")
        }
        if (primaryKeys.size > 1) {
            error("Key retrieval is not supported for compound primary keys.")
        }

        val generatedKey = primaryKeys[0].sqlType.getResult(rowSet, 1) ?: error("Generated key is null.")

        if (logger.isDebugEnabled()) {
            logger.debug("Generated Key: $generatedKey")
        }

        return generatedKey
    } else {
        error("No generated key returns by database.")
    }
}

/**
 * Insert the current [Query]'s results into the given table, useful when transfer data from a table to another table.
 */
fun Query.insertTo(table: BaseTable<*>, vararg columns: Column<*>): Int {
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
fun <T : BaseTable<*>> Database.delete(table: T, predicate: (T) -> ColumnDeclaring<Boolean>): Int {
    val expression = AliasRemover.visit(DeleteExpression(table.asExpression(), predicate(table).asExpression()))
    return executeUpdate(expression)
}

/**
 * Delete all the records in the table.
 *
 * @since 2.7
 */
fun Database.deleteAll(table: BaseTable<*>): Int {
    val expression = AliasRemover.visit(DeleteExpression(table.asExpression(), where = null))
    return executeUpdate(expression)
}

/**
 * Marker annotation for Ktorm DSL builder classes.
 */
@DslMarker
annotation class KtormDsl

/**
 * Base class of DSL builders, provide basic functions used to build assignments for insert or update DSL.
 */
@KtormDsl
open class AssignmentsBuilder(private val assignments: MutableList<ColumnAssignmentExpression<*>>) {

    /**
     * Assign the current column to another column or an expression's result.
     */
    infix fun <C : Any> Column<C>.to(expr: ColumnDeclaring<C>) {
        assignments += ColumnAssignmentExpression(asExpression(), expr.asExpression())
    }

    /**
     * Assign the current column to a specific value.
     */
    infix fun <C : Any> Column<C>.to(argument: C?) {
        this to wrapArgument(argument)
    }

    /**
     * Assign the current column to a specific value.
     *
     * Note that this function accepts an argument type `Any?`, that's because it is designed to avoid
     * applications call [kotlin.to] unexpectedly in the DSL closures. An exception will be thrown
     * by this function if the argument type doesn't match the column's type.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmName("toAny")
    infix fun Column<*>.to(argument: Any?) {
        this as Column<Any>
        checkAssignableFrom(argument)
        this to argument
    }

    private fun Column<Any>.checkAssignableFrom(argument: Any?) {
        if (argument == null) return

        val handler = InvocationHandler { _, method, _ ->
            // Do nothing...
            @Suppress("ForbiddenVoid")
            if (method.returnType == Void.TYPE || !method.returnType.isPrimitive) {
                null
            } else {
                method.returnType.kotlin.defaultValue
            }
        }

        val proxy = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(PreparedStatement::class.java), handler)

        try {
            sqlType.setParameter(proxy as PreparedStatement, 1, argument)
        } catch (e: ClassCastException) {
            throw IllegalArgumentException("Argument type doesn't match the column's type, column: $this", e)
        }
    }
}

/**
 * DSL builder for update statements.
 */
@KtormDsl
class UpdateStatementBuilder(
    assignments: MutableList<ColumnAssignmentExpression<*>>
) : AssignmentsBuilder(assignments) {

    internal var where: ColumnDeclaring<Boolean>? = null

    /**
     * Specify the where clause for this update statement.
     */
    fun where(block: () -> ColumnDeclaring<Boolean>) {
        this.where = block()
    }
}

/**
 * DSL builder for batch update statements.
 */
@KtormDsl
class BatchUpdateStatementBuilder<T : BaseTable<*>>(internal val database: Database, internal val table: T) {
    internal val expressions = ArrayList<SqlExpression>()

    /**
     * Add an update statement to the current batch operation.
     */
    fun item(block: UpdateStatementBuilder.(T) -> Unit) {
        val assignments = ArrayList<ColumnAssignmentExpression<*>>()
        val builder = UpdateStatementBuilder(assignments)
        builder.block(table)

        expressions += UpdateExpression(table.asExpression(), assignments, builder.where?.asExpression())
    }
}

/**
 * DSL builder for batch insert statements.
 */
@KtormDsl
class BatchInsertStatementBuilder<T : BaseTable<*>>(internal val database: Database, internal val table: T) {
    internal val expressions = ArrayList<SqlExpression>()

    /**
     * Add an insert statement to the current batch operation.
     */
    fun item(block: AssignmentsBuilder.(T) -> Unit) {
        val assignments = ArrayList<ColumnAssignmentExpression<*>>()
        val builder = AssignmentsBuilder(assignments)
        builder.block(table)

        expressions += InsertExpression(table.asExpression(), assignments)
    }
}

/**
 * [SqlExpressionVisitor] implementation used to removed table aliases, used by Ktorm internal.
 */
internal object AliasRemover : SqlExpressionVisitor() {

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
