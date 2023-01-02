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

package org.ktorm.support.sqlite

import org.ktorm.database.*
import org.ktorm.expression.*
import org.ktorm.schema.IntSqlType

/**
 * [SqlDialect] implementation for SQLite database.
 */
public open class SQLiteDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return SQLiteFormatter(database, beautifySql, indentSize)
    }

    override fun executeUpdateAndRetrieveKeys(
        database: Database,
        sql: String,
        args: List<ArgumentExpression<*>>
    ): Pair<Int, CachedRowSet> {
        database.useConnection { conn ->
            val effects = conn.prepareStatement(sql).use { statement ->
                statement.setArguments(args)
                statement.executeUpdate()
            }

            val retrieveKeySql = "SELECT LAST_INSERT_ROWID()"
            if (database.logger.isDebugEnabled()) {
                database.logger.debug("Retrieving generated keys by SQL: $retrieveKeySql")
            }

            val rowSet = conn.prepareStatement(retrieveKeySql).use { statement ->
                statement.executeQuery().use { rs -> CachedRowSet(rs) }
            }

            return Pair(effects, rowSet)
        }
    }
}

/**
 * Base interface designed to visit or modify SQLite expression trees using visitor pattern.
 *
 * For detailed documents, see [SqlExpressionVisitor].
 */
public interface SQLiteExpressionVisitor : SqlExpressionVisitor {

    override fun visit(expr: SqlExpression): SqlExpression {
        return when (expr) {
            is InsertOrUpdateExpression -> visitInsertOrUpdate(expr)
            is BulkInsertExpression -> visitBulkInsert(expr)
            else -> super.visit(expr)
        }
    }

    public fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        val table = visitTable(expr.table)
        val assignments = visitExpressionList(expr.assignments)
        val conflictColumns = visitExpressionList(expr.conflictColumns)
        val updateAssignments = visitExpressionList(expr.updateAssignments)
        val where = expr.where?.let { visitScalar(it) }
        val returningColumns = visitExpressionList(expr.returningColumns)

        @Suppress("ComplexCondition")
        if (table === expr.table
            && assignments === expr.assignments
            && conflictColumns === expr.conflictColumns
            && updateAssignments === expr.updateAssignments
            && where === expr.where
            && returningColumns === expr.returningColumns
        ) {
            return expr
        } else {
            return expr.copy(
                table = table,
                assignments = assignments,
                conflictColumns = conflictColumns,
                updateAssignments = updateAssignments,
                where = where,
                returningColumns = returningColumns
            )
        }
    }

    public fun visitBulkInsert(expr: BulkInsertExpression): BulkInsertExpression {
        val table = visitTable(expr.table)
        val assignments = visitBulkInsertAssignments(expr.assignments)
        val conflictColumns = visitExpressionList(expr.conflictColumns)
        val updateAssignments = visitExpressionList(expr.updateAssignments)
        val where = expr.where?.let { visitScalar(it) }
        val returningColumns = visitExpressionList(expr.returningColumns)

        @Suppress("ComplexCondition")
        if (table === expr.table
            && assignments === expr.assignments
            && conflictColumns === expr.conflictColumns
            && updateAssignments === expr.updateAssignments
            && where === expr.where
            && returningColumns === expr.returningColumns
        ) {
            return expr
        } else {
            return expr.copy(
                table = table,
                assignments = assignments,
                conflictColumns = conflictColumns,
                updateAssignments = updateAssignments,
                where = where,
                returningColumns = returningColumns
            )
        }
    }

    public fun visitBulkInsertAssignments(
        assignments: List<List<ColumnAssignmentExpression<*>>>
    ): List<List<ColumnAssignmentExpression<*>>> {
        val result = ArrayList<List<ColumnAssignmentExpression<*>>>()
        var changed = false

        for (row in assignments) {
            val visited = visitExpressionList(row)
            result += visited

            if (visited !== row) {
                changed = true
            }
        }

        return if (changed) result else assignments
    }
}

/**
 * [SqlFormatter] implementation for SQLite, formatting SQL expressions as strings with their execution arguments.
 */
public open class SQLiteFormatter(
    database: Database, beautifySql: Boolean, indentSize: Int
) : SqlFormatter(database, beautifySql, indentSize), SQLiteExpressionVisitor {

    override fun visit(expr: SqlExpression): SqlExpression {
        val result = super<SQLiteExpressionVisitor>.visit(expr)
        check(result === expr) { "SqlFormatter cannot modify the expression trees." }
        return result
    }

    override fun writePagination(expr: QueryExpression) {
        newLine(Indentation.SAME)
        writeKeyword("limit ?, ? ")
        _parameters += ArgumentExpression(expr.offset ?: 0, IntSqlType)
        _parameters += ArgumentExpression(expr.limit ?: Int.MAX_VALUE, IntSqlType)
    }

    override fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        writeKeyword("insert into ")
        visitTable(expr.table)
        writeInsertColumnNames(expr.assignments.map { it.column })
        writeKeyword("values ")
        writeInsertValues(expr.assignments)

        if (expr.conflictColumns.isNotEmpty()) {
            writeKeyword("on conflict ")
            writeInsertColumnNames(expr.conflictColumns)

            if (expr.updateAssignments.isEmpty()) {
                writeKeyword("do nothing ")
            } else {
                writeKeyword("do update set ")
                writeColumnAssignments(expr.updateAssignments)

                if (expr.where != null) {
                    writeKeyword("where ")
                    visit(expr.where)
                }
            }
        }

        if (expr.returningColumns.isNotEmpty()) {
            writeKeyword("returning ")

            for ((i, column) in expr.returningColumns.withIndex()) {
                if (i > 0) write(", ")
                checkColumnName(column.name)
                write(column.name.quoted)
            }
        }

        return expr
    }

    override fun visitBulkInsert(expr: BulkInsertExpression): BulkInsertExpression {
        writeKeyword("insert into ")
        visitTable(expr.table)
        writeInsertColumnNames(expr.assignments[0].map { it.column })
        writeKeyword("values ")

        for ((i, assignments) in expr.assignments.withIndex()) {
            if (i > 0) {
                removeLastBlank()
                write(", ")
            }
            writeInsertValues(assignments)
        }

        if (expr.conflictColumns.isNotEmpty()) {
            writeKeyword("on conflict ")
            writeInsertColumnNames(expr.conflictColumns)

            if (expr.updateAssignments.isEmpty()) {
                writeKeyword("do nothing ")
            } else {
                writeKeyword("do update set ")
                writeColumnAssignments(expr.updateAssignments)

                if (expr.where != null) {
                    writeKeyword("where ")
                    visit(expr.where)
                }
            }
        }

        if (expr.returningColumns.isNotEmpty()) {
            writeKeyword("returning ")

            for ((i, column) in expr.returningColumns.withIndex()) {
                if (i > 0) write(", ")
                checkColumnName(column.name)
                write(column.name.quoted)
            }
        }

        return expr
    }
}
