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
import org.ktorm.expression.ColumnDeclaringExpression
import org.ktorm.schema.Column
import java.sql.ResultSet

/**
 * Special implementation of [ResultSet], used to hold the [Query] results for Ktorm.
 *
 * Different from normal result sets, this class provides additional features:
 *
 * - **Available offline:** It’s connection independent, it remains available after the connection closed, and it’s
 * not necessary to be closed after being used. Ktorm creates [QueryRowSet] objects with all data being retrieved from
 * the result set into memory, so we just need to wait for GC to collect them after they are not useful.
 *
 * - **Indexed access operator:** It overloads the indexed access operator, so we can use square brackets `[]` to
 * obtain the value by giving a specific [Column] instance. It’s less error-prone by the benefit of the compiler’s
 * static checking. Also, we can still use getXxx functions in the [ResultSet] to obtain our results by labels or
 * column indices.
 *
 * ```kotlin
 * val query = database.from(Employees).select()
 * for (row in query.rowSet) {
 *     println(row[Employees.name])
 * }
 * ```
 */
public class QueryRowSet internal constructor(public val query: Query, rs: ResultSet) : CachedRowSet(rs) {

    /**
     * Obtain the value of the specific [Column] instance.
     *
     * Note that if the column doesn't exist in the result set, this function will return null rather than
     * throwing an exception.
     */
    public operator fun <C : Any> get(column: Column<C>): C? {
        if (query.expression.findDeclaringColumns().isNotEmpty()) {
            // Try to find the column by label.
            for (index in 1..metaData.columnCount) {
                if (metaData.getColumnLabel(index) eq column.label) {
                    return column.sqlType.getResult(this, index)
                }
            }

            // Return null if the column doesn't exist in the result set.
            return null
        } else {
            // Try to find the column by name and its table name (happens when we are using `select *`).
            val indices = (1..metaData.columnCount).filter { index ->
                val table = column.table
                val tableName = metaData.getTableName(index)
                val tableNameMatched = tableName.isBlank() || tableName eq table.alias || tableName eq table.tableName
                val columnName = metaData.getColumnName(index)
                columnName eq column.name && tableNameMatched
            }

            return when (indices.size) {
                0 -> null // Return null if the column doesn't exist in the result set.
                1 -> column.sqlType.getResult(this, indices[0])
                else -> throw IllegalArgumentException(warningConfusedColumnName(column.name))
            }
        }
    }

    /**
     * Obtain the value of the specific [ColumnDeclaringExpression] instance.
     *
     * Note that if the column doesn't exist in the result set, this function will return null rather than
     * throwing an exception.
     */
    public operator fun <C : Any> get(column: ColumnDeclaringExpression<C>): C? {
        if (column.declaredName.isNullOrBlank()) {
            throw IllegalArgumentException("Label of the specified column cannot be null or blank.")
        }

        for (index in 1..metaData.columnCount) {
            if (metaData.getColumnLabel(index) eq column.declaredName) {
                return column.sqlType.getResult(this, index)
            }
        }

        // Return null if the column doesn't exist in the result set.
        return null
    }

    /**
     * Check if the specific [Column] exists in this result set.
     *
     * Note that if the column exists but its value is null, this function still returns `true`.
     */
    public fun hasColumn(column: Column<*>): Boolean {
        if (query.expression.findDeclaringColumns().isNotEmpty()) {
            // Try to find the column by label.
            for (index in 1..metaData.columnCount) {
                if (metaData.getColumnLabel(index) eq column.label) {
                    return true
                }
            }

            return false
        } else {
            // Try to find the column by name and its table name (happens when we are using `select *`).
            val indices = (1..metaData.columnCount).filter { index ->
                val table = column.table
                val tableName = metaData.getTableName(index)
                val tableNameMatched = tableName.isBlank() || tableName eq table.alias || tableName eq table.tableName
                val columnName = metaData.getColumnName(index)
                columnName eq column.name && tableNameMatched
            }

            if (indices.size > 1) {
                val logger = query.database.logger
                if (logger.isWarnEnabled()) {
                    logger.warn(warningConfusedColumnName(column.name))
                }
            }

            return indices.isNotEmpty()
        }
    }

    /**
     * Check if the specific [Column] exists in this result set.
     *
     * Note that if the column exists but its value is null, this function still returns `true`.
     */
    public fun hasColumn(column: ColumnDeclaringExpression<*>): Boolean {
        if (column.declaredName.isNullOrBlank()) {
            throw IllegalArgumentException("Label of the specified column cannot be null or blank.")
        }

        for (index in 1..metaData.columnCount) {
            if (metaData.getColumnLabel(index) eq column.declaredName) {
                return true
            }
        }

        return false
    }

    private infix fun String?.eq(other: String?) = this.equals(other, ignoreCase = true)

    private fun warningConfusedColumnName(name: String): String {
        return "Confused column name, there are more than one column named '$name' in query: \n\n${query.sql}\n"
    }
}
