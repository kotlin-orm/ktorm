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

import org.ktorm.database.Database
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.QueryExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.expression.SqlFormatter
import org.ktorm.schema.IntSqlType

/**
 * [SqlFormatter] implementation for SQLite, formatting SQL expressions as strings with their execution arguments.
 */
public open class SQLiteFormatter(
    database: Database, beautifySql: Boolean, indentSize: Int
) : SqlFormatter(database, beautifySql, indentSize), SQLiteExpressionVisitor {

    override fun visit(expr: SqlExpression): SqlExpression {
        val result = super<SQLiteExpressionVisitor>.visit(expr)
        check(result === expr) { "SqlFormatter cannot modify the expression tree." }
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
                write(column.name.quoted)
            }
        }

        return expr
    }
}
