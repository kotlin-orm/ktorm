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

            val retrieveKeySql = "select last_insert_rowid()"
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
 * [SqlFormatter] implementation for SQLite, formatting SQL expressions as strings with their execution arguments.
 */
public open class SQLiteFormatter(
    database: Database, beautifySql: Boolean, indentSize: Int
) : SqlFormatter(database, beautifySql, indentSize) {

    override fun visit(expr: SqlExpression): SqlExpression {
        val result = when (expr) {
            is InsertOrUpdateExpression -> visitInsertOrUpdate(expr)
            is BulkInsertExpression -> visitBulkInsert(expr)
            else -> super.visit(expr)
        }

        check(result === expr) { "SqlFormatter cannot modify the expression trees." }
        return result
    }

    override fun writePagination(expr: QueryExpression) {
        newLine(Indentation.SAME)
        writeKeyword("limit ?, ? ")
        _parameters += ArgumentExpression(expr.offset ?: 0, IntSqlType)
        _parameters += ArgumentExpression(expr.limit ?: Int.MAX_VALUE, IntSqlType)
    }

    override fun visitUnion(expr: UnionExpression): UnionExpression {
        when (expr.left) {
            is SelectExpression -> visitQuery(expr.left)
            is UnionExpression -> visitUnion(expr.left as UnionExpression)
        }

        if (expr.isUnionAll) {
            writeKeyword("union all ")
        } else {
            writeKeyword("union ")
        }

        when (expr.right) {
            is SelectExpression -> visitQuery(expr.right)
            is UnionExpression -> visitUnion(expr.right as UnionExpression)
        }

        if (expr.orderBy.isNotEmpty()) {
            newLine(Indentation.SAME)
            writeKeyword("order by ")
            visitOrderByList(expr.orderBy)
        }
        if (expr.offset != null || expr.limit != null) {
            writePagination(expr)
        }
        return expr
    }

    protected open fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        writeKeyword("insert into ")
        visitTable(expr.table)
        writeInsertColumnNames(expr.assignments.map { it.column })
        writeKeyword("values ")
        writeInsertValues(expr.assignments)

        if (expr.conflictColumns.isNotEmpty()) {
            writeKeyword("on conflict ")
            writeInsertColumnNames(expr.conflictColumns)

            if (expr.updateAssignments.isNotEmpty()) {
                writeKeyword("do update set ")
                visitColumnAssignments(expr.updateAssignments)

                if (expr.where != null) {
                    writeKeyword("where ")
                    visit(expr.where)
                }
            } else {
                writeKeyword("do nothing ")
            }
        }

        return expr
    }

    protected open fun visitBulkInsert(expr: BulkInsertExpression): BulkInsertExpression {
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

            if (expr.updateAssignments.isNotEmpty()) {
                writeKeyword("do update set ")
                visitColumnAssignments(expr.updateAssignments)

                if (expr.where != null) {
                    writeKeyword("where ")
                    visit(expr.where)
                }
            } else {
                writeKeyword("do nothing ")
            }
        }

        return expr
    }
}

/**
 * Base class designed to visit or modify SQLite's expression trees using visitor pattern.
 *
 * For detailed documents, see [SqlExpressionVisitor].
 */
public open class SQLiteExpressionVisitor : SqlExpressionVisitor() {

    override fun visit(expr: SqlExpression): SqlExpression {
        return when (expr) {
            is InsertOrUpdateExpression -> visitInsertOrUpdate(expr)
            is BulkInsertExpression -> visitBulkInsert(expr)
            else -> super.visit(expr)
        }
    }

    protected open fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        val table = visitTable(expr.table)
        val assignments = visitColumnAssignments(expr.assignments)
        val conflictColumns = visitExpressionList(expr.conflictColumns)
        val updateAssignments = visitColumnAssignments(expr.updateAssignments)
        val where = expr.where?.let { visitScalar(it) }

        @Suppress("ComplexCondition")
        if (table === expr.table
            && assignments === expr.assignments
            && conflictColumns === expr.conflictColumns
            && updateAssignments === expr.updateAssignments
            && where === expr.where
        ) {
            return expr
        } else {
            return expr.copy(
                table = table,
                assignments = assignments,
                conflictColumns = conflictColumns,
                updateAssignments = updateAssignments,
                where = where
            )
        }
    }

    protected open fun visitBulkInsert(expr: BulkInsertExpression): BulkInsertExpression {
        val table = visitTable(expr.table)
        val assignments = visitBulkInsertAssignments(expr.assignments)
        val conflictColumns = visitExpressionList(expr.conflictColumns)
        val updateAssignments = visitColumnAssignments(expr.updateAssignments)
        val where = expr.where?.let { visitScalar(it) }

        @Suppress("ComplexCondition")
        if (table === expr.table
            && assignments === expr.assignments
            && conflictColumns === expr.conflictColumns
            && updateAssignments === expr.updateAssignments
            && where === expr.where
        ) {
            return expr
        } else {
            return expr.copy(
                table = table,
                assignments = assignments,
                conflictColumns = conflictColumns,
                updateAssignments = updateAssignments,
                where = where
            )
        }
    }

    protected open fun visitBulkInsertAssignments(
        assignments: List<List<ColumnAssignmentExpression<*>>>
    ): List<List<ColumnAssignmentExpression<*>>> {
        val result = ArrayList<List<ColumnAssignmentExpression<*>>>()
        var changed = false

        for (row in assignments) {
            val visited = visitColumnAssignments(row)
            result += visited

            if (visited !== row) {
                changed = true
            }
        }

        return if (changed) result else assignments
    }
}
