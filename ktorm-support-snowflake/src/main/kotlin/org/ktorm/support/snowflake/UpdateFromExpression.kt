/*
 * Copyright 2025 the original author or authors.
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
 *
 * Original authors of the snowflake dialect were CarGurus: Don Mitchell, Ashish Shrestha, Mike Roberts, and others.
 */
package org.ktorm.support.snowflake

import org.ktorm.database.Database
import org.ktorm.dsl.QuerySource
import org.ktorm.dsl.and
import org.ktorm.dsl.from
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.expression.ColumnAssignmentExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.expression.TableExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.ColumnDeclaring

/**
 * Update from expression, represents the `update` statement in SQL with from statement to join to extra tables.
 * Uses a syntax akin to Query expressions, with [execute] as the method that forces execution instead of map collection
 * method.
 *
 * @property database the database
 * @property table the table to be updated.
 * @property fromTable the table to be joined to.
 * @property assignments column assignments of the update statement.
 * @property where the update condition.
 */
public data class UpdateFromExpression(
    val database: Database,
    val table: TableExpression,
    val fromTable: TableExpression,
    val assignments: List<ColumnAssignmentExpression<*>>,
    val where: ScalarExpression<Boolean>? = null,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression() {
    /**
     * The method used to add a where clause to [UpdateFromExpression].
     *
     * @param block The block with the boolean condition for the update from expression. Note that only one block is
     * ever applied (so a [where] call will override a previous [where] call, you cannot chain them)
     * and the join conditionals MUST be present.
     *
     * @return An update from expression.
     */
    public fun where(block: () -> ColumnDeclaring<Boolean>): UpdateFromExpression {
        return this.copy(where = block().asExpression())
    }

    /**
     * Create a mutable list, then add filter conditions to the list in the given callback function, finally combine
     * them with the [and] operator and set the combined condition as the `where` clause of this query.
     *
     * Note that if we don't add any conditions to the list, the `where` clause would not be set.
     *
     * @param block The block that will add boolean conditions to a list. This will replace the `where` clause of the
     * update statement.
     *
     * @return An [UpdateFromExpression] with the conditions added within a compound [and] construction.
     */
    public fun whereWithConditions(block: (MutableList<ColumnDeclaring<Boolean>>) -> Unit): UpdateFromExpression {
        var conditions: List<ColumnDeclaring<Boolean>> = ArrayList<ColumnDeclaring<Boolean>>().apply(block)
        if (conditions.isEmpty()) {
            return this
        } else {
            val whereClause = conditions.reduce { acc, condition -> acc.and(condition) }
            return this.copy(where = whereClause.asExpression())
        }
    }

    /**
     * The method used to execute the query, currently only method that can execute the query in [UpdateFromExpression].
     *
     * @return The number of records updated.
     */
    public fun execute(): Int = this.database.executeUpdate(this)
}

/**
 * The standard method to create an [UpdateFromExpression], in a manner similar to [Database.from] (for creating
 * [QuerySource] expressions. Note that this uses the [UpdateStatmentBuilder] to specify the columns to set, but does
 * not handle the [where] clause as is done in [Database.update]. Also unlike [Database.update],
 * does not perform the update immediately.
 *
 * @param T The table class to update.
 * @param U The table class to join to for the update.
 * @param table The table to update.
 * @param fromTable The table to join to for the update.
 * @param block The block that sets the values for the columns to update.
 *
 * @return An update from expression. Note that unlike the base Database.update method, does not execute immediately.
 */
public fun <T : BaseTable<*>, U : BaseTable<*>> Database.updateFrom(
    table: T,
    fromTable: U,
    block: UpdateFromStatementBuilder.(T) -> Unit
): UpdateFromExpression {
    return buildUpdateFromExpression(
        this,
        table,
        fromTable,
        block
    )
}

private fun <T : BaseTable<*>, U : BaseTable<*>> buildUpdateFromExpression(
    database: Database,
    table: T,
    fromTable: U,
    block: UpdateFromStatementBuilder.(T) -> Unit
): UpdateFromExpression {
    val builder = UpdateFromStatementBuilder()
    builder.block(table)
    return UpdateFromExpression(
        database,
        table.asExpression(),
        fromTable.asExpression(),
        builder.assignments(),
    )
}
