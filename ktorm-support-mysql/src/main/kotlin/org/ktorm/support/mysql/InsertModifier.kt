package org.ktorm.support.mysql

import org.ktorm.database.Database
import org.ktorm.dsl.AliasRemover
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.expression.InsertExpression
import org.ktorm.schema.BaseTable

internal const val INSERT_MODIFIER_PROP_KEY = "insert_modifier"

/**
 * Enum class represents insert modifiers in MySQL `insert [ignore|delayed] into ...` expressions.
 * see https://dev.mysql.com/doc/refman/8.3/en/insert.html
 * @since 3.7.0
 */
public enum class InsertModifier {
    LOW_PRIORITY,

    /**
     * please note: insert delayed was deprecated in MySQL 5.6, and is scheduled for eventual removal.
     * see https://dev.mysql.com/doc/refman/8.3/en/insert-delayed.html
     */
    DELAYED,
    HIGH_PRIORITY,
    IGNORE
}

/**
 * Construct an insert expression with specified modifier in the given closure, then execute it and return the effected row count.
 *
 * Usage:
 *
 * ```kotlin
 * database.insertWithModifier(Employees, modifier = InsertModifier.IGNORE) {
 *     set(it.name, "jerry")
 *     set(it.job, "trainee")
 *     set(it.managerId, 1)
 *     set(it.hireDate, LocalDate.now())
 *     set(it.salary, 50)
 *     set(it.departmentId, 1)
 * }
 * ```
 *
 * @since 3.7
 * @param table the table to be inserted.
 * @param modifier the insert modifier
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the effected row count.
 */
public fun <T : BaseTable<*>> Database.insertWithModifier(
    table: T,
    modifier: InsertModifier,
    block: AssignmentsBuilder.(T) -> Unit
): Int {
    val builder = MySqlAssignmentsBuilder().apply { block(table) }
    if (builder.assignments.isEmpty()) {
        throw IllegalArgumentException("There are no columns to insert in the statement.")
    }

    val expression = dialect.createExpressionVisitor(AliasRemover).visit(
        InsertExpression(
            table.asExpression(),
            builder.assignments,
            extraProperties = mapOf(INSERT_MODIFIER_PROP_KEY to modifier)
        )
    )
    return executeUpdate(expression)
}

/**
 * Construct an insert ignore expression in the given closure, then execute it and return the effected row count.
 *
 * Shortcut for `this.insertWithModifier(table, IGNORE, block)`
 * Usage:
 *
 * ```kotlin
 * database.insertIgnore(Employees) {
 *     set(it.name, "jerry")
 *     set(it.job, "trainee")
 *     set(it.managerId, 1)
 *     set(it.hireDate, LocalDate.now())
 *     set(it.salary, 50)
 *     set(it.departmentId, 1)
 * }
 * ```
 *
 * @since 3.7
 * @param table the table to be inserted.
 * @param block the DSL block, an extension function of [AssignmentsBuilder], used to construct the expression.
 * @return the effected row count.
 */
public fun <T : BaseTable<*>> Database.insertIgnore(
    table: T,
    block: AssignmentsBuilder.(T) -> Unit
): Int {
    return insertWithModifier(table, InsertModifier.IGNORE, block)
}