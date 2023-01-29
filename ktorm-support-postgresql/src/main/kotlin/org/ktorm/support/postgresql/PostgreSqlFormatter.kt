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

package org.ktorm.support.postgresql

import org.ktorm.database.Database
import org.ktorm.expression.*
import org.ktorm.schema.IntSqlType

/**
 * [SqlFormatter] implementation for PostgreSQL, formatting SQL expressions as strings with their execution arguments.
 */
public open class PostgreSqlFormatter(
    database: Database, beautifySql: Boolean, indentSize: Int
) : SqlFormatter(database, beautifySql, indentSize), PostgreSqlExpressionVisitor {

    override fun visit(expr: SqlExpression): SqlExpression {
        val result = super<PostgreSqlExpressionVisitor>.visit(expr)
        check(result === expr) { "SqlFormatter cannot modify the expression tree." }
        return result
    }

    override fun visitSelect(expr: SelectExpression): SelectExpression {
        super<SqlFormatter>.visitSelect(expr)

        val locking = expr.extraProperties["locking"] as LockingClause?
        if (locking != null) {
            when (locking.mode) {
                LockingMode.FOR_UPDATE -> writeKeyword("for update ")
                LockingMode.FOR_NO_KEY_UPDATE -> writeKeyword("for no key update ")
                LockingMode.FOR_SHARE -> writeKeyword("for share ")
                LockingMode.FOR_KEY_SHARE -> writeKeyword("for key share ")
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

        if (expr.limit != null) {
            writeKeyword("limit ? ")
            _parameters += ArgumentExpression(expr.limit, IntSqlType)
        }
        if (expr.offset != null) {
            writeKeyword("offset ? ")
            _parameters += ArgumentExpression(expr.offset, IntSqlType)
        }
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

            if (expr.updateAssignments.isNotEmpty()) {
                writeKeyword("do update set ")
                writeColumnAssignments(expr.updateAssignments)
            } else {
                writeKeyword("do nothing ")
            }
        }

        if (expr.returningColumns.isNotEmpty()) {
            writeKeyword("returning ")

            for ((i, column) in expr.returningColumns.withIndex()) {
                if (i > 0) write(", ")
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

            if (expr.updateAssignments.isNotEmpty()) {
                writeKeyword("do update set ")
                writeColumnAssignments(expr.updateAssignments)
            } else {
                writeKeyword("do nothing ")
            }
        }

        if (expr.returningColumns.isNotEmpty()) {
            writeKeyword("returning ")

            for ((i, column) in expr.returningColumns.withIndex()) {
                if (i > 0) write(", ")
                write(column.name.quoted)
            }
        }

        return expr
    }

    override fun visitTable(expr: TableExpression): TableExpression {
        if (!expr.catalog.isNullOrBlank()) {
            write("${expr.catalog!!.quoted}.")
        }
        if (!expr.schema.isNullOrBlank()) {
            write("${expr.schema!!.quoted}.")
        }

        write("${expr.name.quoted} ")

        if (!expr.tableAlias.isNullOrBlank()) {
            writeKeyword("as ") // The 'as' keyword is required for update statements in PostgreSQL.
            write("${expr.tableAlias!!.quoted} ")
        }

        return expr
    }

    override fun visitILike(expr: ILikeExpression): ILikeExpression {
        if (expr.left.removeBrackets) {
            visit(expr.left)
        } else {
            write("(")
            visit(expr.left)
            removeLastBlank()
            write(") ")
        }

        writeKeyword("ilike ")

        if (expr.right.removeBrackets) {
            visit(expr.right)
        } else {
            write("(")
            visit(expr.right)
            removeLastBlank()
            write(") ")
        }

        return expr
    }

    override fun <T : Any> visitHStore(expr: HStoreExpression<T>): HStoreExpression<T> {
        if (expr.left.removeBrackets) {
            visit(expr.left)
        } else {
            write("(")
            visit(expr.left)
            removeLastBlank()
            write(") ")
        }

        writeKeyword("${expr.type} ")

        if (expr.right.removeBrackets) {
            visit(expr.right)
        } else {
            write("(")
            visit(expr.right)
            removeLastBlank()
            write(") ")
        }

        return expr
    }

    override fun <T : Any> visitCube(expr: CubeExpression<T>): CubeExpression<T> {
        if (expr.left.removeBrackets) {
            visit(expr.left)
        } else {
            write("(")
            visit(expr.left)
            removeLastBlank()
            write(") ")
        }

        writeKeyword("${expr.type} ")

        if (expr.right.removeBrackets) {
            visit(expr.right)
        } else {
            write("(")
            visit(expr.right)
            removeLastBlank()
            write(") ")
        }

        return expr
    }

    override fun <T : Any> visitDefaultValue(expr: DefaultValueExpression<T>): DefaultValueExpression<T> {
        writeKeyword("default ")
        return expr
    }
}
