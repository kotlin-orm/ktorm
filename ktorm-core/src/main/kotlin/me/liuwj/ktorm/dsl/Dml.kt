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
import me.liuwj.ktorm.database.prepareStatement
import me.liuwj.ktorm.database.use
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.sql.PreparedStatement
import java.sql.Statement
import java.util.*
import kotlin.collections.ArrayList

/**
 * Construct an update expression in the given closure, then execute it and return the effected row count.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.update {
 *     it.job to "engineer"
 *     it.managerId to null
 *     it.salary to 100
 *     where {
 *         it.id eq 2
 *     }
 * }
 * ```
 *
 * @param block the DSL block, an extension function of [UpdateStatementBuilder], used to construct the expression.
 * @return the effected row count.
 */
fun <T : BaseTable<*>> T.update(block: UpdateStatementBuilder.(T) -> Unit): Int {
    val assignments = ArrayList<ColumnAssignmentExpression<*>>()
    val builder = UpdateStatementBuilder(assignments).apply { block(this@update) }

    val expression = AliasRemover.visit(UpdateExpression(asExpression(), assignments, builder.where?.asExpression()))
    return expression.executeUpdate()
}

/**
 * Execute the current SQL expression, and return the effected row count. Used by Ktorm internal.
 */
internal fun SqlExpression.executeUpdate(): Int {
    this.prepareStatement { statement ->
        val effects = statement.executeUpdate()

        val logger = Database.global.logger
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug("Effects: $effects")
        }

        return effects
    }
}

/**
 * Construct update expressions in the given closure, then batch execute them and return the effected
 * row counts for each expression.
 *
 * Note that this function is implemented based on [Statement.addBatch] and [Statement.executeBatch],
 * and any item in a batch operation must generate the same SQL, otherwise an exception will be thrown.
 *
 * Usage:
 *
 * ```kotlin
 * Departments.batchUpdate {
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
 * @param block the DSL block, extension function of [BatchUpdateStatementBuilder], used to construct the expressions.
 * @return the effected row counts for each sub-operation.
 */
fun <T : BaseTable<*>> T.batchUpdate(block: BatchUpdateStatementBuilder<T>.() -> Unit): IntArray {
    val builder = BatchUpdateStatementBuilder(this).apply(block)
    val expressions = builder.expressions.map { AliasRemover.visit(it) }

    if (expressions.isEmpty()) {
        return IntArray(0)
    } else {
        return expressions.executeBatch()
    }
}

private fun List<SqlExpression>.executeBatch(): IntArray {
    val database = Database.global
    val logger = database.logger
    val (sql, _) = database.formatExpression(this[0])

    if (logger != null && logger.isDebugEnabled()) {
        logger.debug("SQL: $sql")
    }

    database.useConnection { conn ->
        conn.prepareStatement(sql).use { statement ->
            for (expr in this) {
                val (_, args) = database.formatExpression(expr)

                if (logger != null && logger.isDebugEnabled()) {
                    logger.debug("Parameters: " + args.map { "${it.value}(${it.sqlType.typeName})" })
                }

                for ((i, arg) in args.withIndex()) {
                    @Suppress("UNCHECKED_CAST")
                    val sqlType = arg.sqlType as SqlType<Any>
                    sqlType.setParameter(statement, i + 1, arg.value)
                }

                statement.addBatch()
            }

            val effects = statement.executeBatch()

            if (logger != null && logger.isDebugEnabled()) {
                logger.debug("Effects: ${effects?.contentToString()}")
            }

            return effects
        }
    }
}

/**
 * Construct an insert expression in the given closure, then execute it and return the effected row count.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.insert {
 *     it.name to "jerry"
 *     it.job to "trainee"
 *     it.managerId to 1
 *     it.hireDate to LocalDate.now()
 *     it.salary to 50
 *     it.departmentId to 1
 * }
 * ```
 *
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the effected row count.
 */
fun <T : BaseTable<*>> T.insert(block: AssignmentsBuilder.(T) -> Unit): Int {
    val assignments = ArrayList<ColumnAssignmentExpression<*>>()
    AssignmentsBuilder(assignments).block(this)

    val expression = AliasRemover.visit(InsertExpression(asExpression(), assignments))
    return expression.executeUpdate()
}

/**
 * Construct insert expressions in the given closure, then batch execute them and return the effected
 * row counts for each expression.
 *
 * Note that this function is implemented based on [Statement.addBatch] and [Statement.executeBatch],
 * and any item in a batch operation must generate the same SQL, otherwise an exception will be thrown.
 *
 * Usage:
 *
 * ```kotlin
 * Employees.batchInsert {
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
 * @param block the DSL block, extension function of [BatchInsertStatementBuilder], used to construct the expressions.
 * @return the effected row counts for each sub-operation.
 */
fun <T : BaseTable<*>> T.batchInsert(block: BatchInsertStatementBuilder<T>.() -> Unit): IntArray {
    val builder = BatchInsertStatementBuilder(this).apply(block)
    val expressions = builder.expressions.map { AliasRemover.visit(it) }

    if (expressions.isEmpty()) {
        return IntArray(0)
    } else {
        return expressions.executeBatch()
    }
}

/**
 * Construct an insert expression in the given closure, then execute it and return the auto-generated key.
 *
 * Usage:
 *
 * ```kotlin
 * val id = Employees.insertAndGenerateKey {
 *     it.name to "jerry"
 *     it.job to "trainee"
 *     it.managerId to 1
 *     it.hireDate to LocalDate.now()
 *     it.salary to 50
 *     it.departmentId to 1
 * }
 * ```
 *
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the auto-generated key.
 */
fun <T : BaseTable<*>> T.insertAndGenerateKey(block: AssignmentsBuilder.(T) -> Unit): Any {
    val assignments = ArrayList<ColumnAssignmentExpression<*>>()
    AssignmentsBuilder(assignments).block(this)

    val expression = AliasRemover.visit(InsertExpression(asExpression(), assignments))

    return with(Database.global) {
        dialect.prepareStatement(expression, autoGeneratedKeys = true) { statement ->
            val effects = statement.executeUpdate()

            if (logger?.isDebugEnabled() == true) {
                logger.debug("Effects: $effects")
            }

            dialect.generatedKey(statement, primaryKey)
        }
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

    return expression.executeUpdate()
}

/**
 * Delete the records in the table that matches the given [predicate].
 */
fun <T : BaseTable<*>> T.delete(predicate: (T) -> ColumnDeclaring<Boolean>): Int {
    val expression = AliasRemover.visit(DeleteExpression(asExpression(), predicate(this).asExpression()))
    return expression.executeUpdate()
}

/**
 * Delete all the records in the table.
 */
fun BaseTable<*>.deleteAll(): Int {
    val expression = AliasRemover.visit(DeleteExpression(asExpression(), where = null))
    return expression.executeUpdate()
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
class BatchUpdateStatementBuilder<T : BaseTable<*>>(internal val table: T) {
    internal val expressions = ArrayList<SqlExpression>()
    internal val sqls = HashSet<String>()

    /**
     * Add an update statement to the current batch operation.
     */
    fun item(block: UpdateStatementBuilder.(T) -> Unit) {
        val assignments = ArrayList<ColumnAssignmentExpression<*>>()
        val builder = UpdateStatementBuilder(assignments)
        builder.block(table)

        val expr = UpdateExpression(table.asExpression(), assignments, builder.where?.asExpression())

        val (sql, _) = Database.global.formatExpression(expr, beautifySql = true)

        if (sqls.isEmpty() || sql in sqls) {
            sqls += sql
            expressions += expr
        } else {
            throw IllegalArgumentException("Every item in a batch operation must be the same. SQL: \n\n$sql")
        }
    }
}

/**
 * DSL builder for batch insert statements.
 */
@KtormDsl
class BatchInsertStatementBuilder<T : BaseTable<*>>(internal val table: T) {
    internal val expressions = ArrayList<SqlExpression>()
    internal val sqls = HashSet<String>()

    /**
     * Add an insert statement to the current batch operation.
     */
    fun item(block: AssignmentsBuilder.(T) -> Unit) {
        val assignments = ArrayList<ColumnAssignmentExpression<*>>()
        val builder = AssignmentsBuilder(assignments)
        builder.block(table)

        val expr = InsertExpression(table.asExpression(), assignments)

        val (sql, _) = Database.global.formatExpression(expr, beautifySql = true)

        if (sqls.isEmpty() || sql in sqls) {
            sqls += sql
            expressions += expr
        } else {
            throw IllegalArgumentException("Every item in a batch operation must be the same. SQL: \n\n$sql")
        }
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
