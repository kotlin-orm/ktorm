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

package org.ktorm.support.mysql

import org.ktorm.database.Database
import org.ktorm.database.SqlDialect
import org.ktorm.expression.*
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.VarcharSqlType

/**
 * [SqlDialect] implementation for MySQL database.
 */
public open class MySqlDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return MySqlFormatter(database, beautifySql, indentSize)
    }
}

/**
 * [SqlFormatter] implementation for MySQL, formatting SQL expressions as strings with their execution arguments.
 */
public open class MySqlFormatter(
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

    override fun <T : Any> visitScalar(expr: ScalarExpression<T>): ScalarExpression<T> {
        val result = when (expr) {
            is MatchAgainstExpression -> visitMatchAgainst(expr)
            is DefaultValueExpression -> visitDefaultValue(expr)
            else -> super.visitScalar(expr)
        }

        @Suppress("UNCHECKED_CAST")
        return result as ScalarExpression<T>
    }

    override fun visitQuerySource(expr: QuerySourceExpression): QuerySourceExpression {
        return when (expr) {
            is NaturalJoinExpression -> visitNaturalJoin(expr)
            else -> super.visitQuerySource(expr)
        }
    }

    override fun visitSelect(expr: SelectExpression): SelectExpression {
        super.visitSelect(expr)

        val locking = expr.extraProperties["locking"] as LockingClause?
        if (locking != null) {
            when (locking.mode) {
                LockingMode.FOR_UPDATE -> writeKeyword("for update ")
                LockingMode.FOR_SHARE -> writeKeyword("for share ")
                LockingMode.LOCK_IN_SHARE_MODE -> writeKeyword("lock in share mode ")
            }

            if (locking.tables.isNotEmpty()) {
                writeKeyword("of ")

                for ((i, table) in locking.tables.withIndex()) {
                    if (i > 0) {
                        removeLastBlank()
                        write(", ")
                    }

                    if (table.tableAlias != null && table.tableAlias!!.isNotBlank()) {
                        write("${table.tableAlias!!.quoted} ")
                    } else {
                        write("${table.name.quoted} ")
                    }
                }
            }

            when (locking.wait) {
                LockingWait.WAIT -> { /* do nothing */ }
                LockingWait.NOWAIT -> writeKeyword("nowait ")
                LockingWait.SKIP_LOCKED -> writeKeyword("skip locked ")
            }
        }

        return expr
    }

    override fun writePagination(expr: QueryExpression) {
        newLine(Indentation.SAME)
        writeKeyword("limit ?, ? ")
        _parameters += ArgumentExpression(expr.offset ?: 0, IntSqlType)
        _parameters += ArgumentExpression(expr.limit ?: Int.MAX_VALUE, IntSqlType)
    }

    protected open fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        writeKeyword("insert into ")
        visitTable(expr.table)
        writeInsertColumnNames(expr.assignments.map { it.column })
        writeKeyword("values ")
        writeInsertValues(expr.assignments)

        if (expr.updateAssignments.isNotEmpty()) {
            writeKeyword("on duplicate key update ")
            visitColumnAssignments(expr.updateAssignments)
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

        if (expr.updateAssignments.isNotEmpty()) {
            writeKeyword("on duplicate key update ")
            visitColumnAssignments(expr.updateAssignments)
        }

        return expr
    }

    protected open fun visitNaturalJoin(expr: NaturalJoinExpression): NaturalJoinExpression {
        visitQuerySource(expr.left)
        newLine(Indentation.SAME)
        writeKeyword("natural join ")
        visitQuerySource(expr.right)
        return expr
    }

    protected open fun visitMatchAgainst(expr: MatchAgainstExpression): MatchAgainstExpression {
        writeKeyword("match (")
        visitExpressionList(expr.matchColumns)
        removeLastBlank()
        writeKeyword(") against (?")
        _parameters += ArgumentExpression(expr.searchString, VarcharSqlType)
        expr.searchModifier?.let { writeKeyword(" $it") }
        write(") ")
        return expr
    }

    protected open fun <T : Any> visitDefaultValue(expr: DefaultValueExpression<T>): DefaultValueExpression<T> {
        writeKeyword("default ")
        return expr
    }
}

/**
 * Base class designed to visit or modify MySQL expression trees using visitor pattern.
 *
 * For detailed documents, see [SqlExpressionVisitor].
 */
public open class MySqlExpressionVisitor : SqlExpressionVisitor() {

    override fun visit(expr: SqlExpression): SqlExpression {
        return when (expr) {
            is InsertOrUpdateExpression -> visitInsertOrUpdate(expr)
            is BulkInsertExpression -> visitBulkInsert(expr)
            else -> super.visit(expr)
        }
    }

    override fun <T : Any> visitScalar(expr: ScalarExpression<T>): ScalarExpression<T> {
        val result = when (expr) {
            is MatchAgainstExpression -> visitMatchAgainst(expr)
            is DefaultValueExpression -> visitDefaultValue(expr)
            else -> super.visitScalar(expr)
        }

        @Suppress("UNCHECKED_CAST")
        return result as ScalarExpression<T>
    }

    override fun visitQuerySource(expr: QuerySourceExpression): QuerySourceExpression {
        return when (expr) {
            is NaturalJoinExpression -> visitNaturalJoin(expr)
            else -> super.visitQuerySource(expr)
        }
    }

    protected open fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        val table = visitTable(expr.table)
        val assignments = visitColumnAssignments(expr.assignments)
        val updateAssignments = visitColumnAssignments(expr.updateAssignments)

        if (table === expr.table && assignments === expr.assignments && updateAssignments === expr.updateAssignments) {
            return expr
        } else {
            return expr.copy(table = table, assignments = assignments, updateAssignments = updateAssignments)
        }
    }

    protected open fun visitBulkInsert(expr: BulkInsertExpression): BulkInsertExpression {
        val table = visitTable(expr.table)
        val assignments = visitBulkInsertAssignments(expr.assignments)
        val updateAssignments = visitColumnAssignments(expr.updateAssignments)

        if (table === expr.table && assignments === expr.assignments && updateAssignments === expr.updateAssignments) {
            return expr
        } else {
            return expr.copy(table = table, assignments = assignments, updateAssignments = updateAssignments)
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

    protected open fun visitNaturalJoin(expr: NaturalJoinExpression): NaturalJoinExpression {
        val left = visitQuerySource(expr.left)
        val right = visitQuerySource(expr.right)

        if (left === expr.left && right === expr.right) {
            return expr
        } else {
            return expr.copy(left = left, right = right)
        }
    }

    protected open fun visitMatchAgainst(expr: MatchAgainstExpression): MatchAgainstExpression {
        val matchColumns = visitExpressionList(expr.matchColumns)

        if (matchColumns === expr.matchColumns) {
            return expr
        } else {
            return expr.copy(matchColumns = matchColumns)
        }
    }

    protected open fun <T : Any> visitDefaultValue(expr: DefaultValueExpression<T>): DefaultValueExpression<T> {
        return expr
    }
}
