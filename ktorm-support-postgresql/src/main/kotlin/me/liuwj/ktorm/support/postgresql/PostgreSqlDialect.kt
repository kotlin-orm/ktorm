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

package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.SqlDialect
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.IntSqlType

/**
 * [SqlDialect] implementation for PostgreSQL database.
 */
open class PostgreSqlDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return PostgreSqlFormatter(database, beautifySql, indentSize)
    }
}

/**
 * [SqlFormatter] implementation for PostgreSQL, formatting SQL expressions as strings with their execution arguments.
 */
open class PostgreSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int)
    : SqlFormatter(database, beautifySql, indentSize) {

    override fun visit(expr: SqlExpression): SqlExpression {
        val result = when (expr) {
            is InsertOrUpdateExpression -> visitInsertOrUpdate(expr)
            else -> super.visit(expr)
        }

        check(result === expr) { "SqlFormatter cannot modify the expression trees." }
        return result
    }

    override fun <T : Any> visitScalar(expr: ScalarExpression<T>): ScalarExpression<T> {
        val result = when (expr) {
            is BinaryExpression<*, *, T> -> visitBinary(expr)
            else -> super.visitScalar(expr)
        }

        return result
    }

    override fun writePagination(expr: QueryExpression) {
        newLine(Indentation.SAME)

        if (expr.limit != null) {
            write("limit ? ")
            _parameters += ArgumentExpression(expr.limit, IntSqlType)
        }
        if (expr.offset != null) {
            write("offset ? ")
            _parameters += ArgumentExpression(expr.offset, IntSqlType)
        }
    }

    protected open fun <LeftT : Any, RightT : Any, ReturnT : Any> visitBinary(expr: BinaryExpression<LeftT, RightT, ReturnT>): BinaryExpression<LeftT, RightT, ReturnT> {
        if (expr.left.removeBrackets) {
            visit(expr.left)
        } else {
            write("(")
            visit(expr.left)
            removeLastBlank()
            write(") ")
        }

        write("${expr.operator} ")

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

    protected open fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        write("insert into ${expr.table.name.quoted} (")
        for ((i, assignment) in expr.assignments.withIndex()) {
            if (i > 0) write(", ")
            write(assignment.column.name.quoted)
        }
        write(") values (")
        visitExpressionList(expr.assignments.map { it.expression as ArgumentExpression })
        removeLastBlank()
        write(") on conflict (")
        for ((i, column) in expr.conflictTarget.withIndex()) {
            if (i > 0) write(", ")
            write(column.name.quoted)
        }
        write(") do update set ")
        for ((i, assignment) in expr.updateAssignments.withIndex()) {
            if (i > 0) {
                removeLastBlank()
                write(", ")
            }
            write("${assignment.column.name.quoted} ")
            write("= ")
            visit(assignment.expression)
        }
        return expr
    }
}

/**
 * Base class designed to visit or modify PostgreSQL's expression trees using visitor pattern.
 *
 * For detailed documents, see [SqlExpressionVisitor].
 */
open class PostgreSqlExpressionVisitor : SqlExpressionVisitor() {

    override fun visit(expr: SqlExpression): SqlExpression {
        return when (expr) {
            is InsertOrUpdateExpression -> visitInsertOrUpdate(expr)
            else -> super.visit(expr)
        }
    }

    override fun <T : Any> visitScalar(expr: ScalarExpression<T>): ScalarExpression<T> {
        val result = when (expr) {
            is BinaryExpression<*, *, T> -> visitBinary(expr)
            else -> super.visitScalar(expr)
        }
        return result
    }

    protected open fun <LeftT : Any, RightT : Any, ReturnT : Any> visitBinary(expr: BinaryExpression<LeftT, RightT, ReturnT>): BinaryExpression<LeftT, RightT, ReturnT> {
        val left = visitScalar(expr.left)
        val right = visitScalar(expr.right)

        if (left === expr.left && right === expr.right) {
            return expr
        } else {
            return expr.copyWithNewOperands(left = left, right = right)
        }
    }

    protected open fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        val table = visitTable(expr.table)
        val assignments = visitColumnAssignments(expr.assignments)
        val conflictTarget = visitExpressionList(expr.conflictTarget)
        val updateAssignments = visitColumnAssignments(expr.updateAssignments)

        @Suppress("ComplexCondition")
        if (table === expr.table
            && assignments === expr.assignments
            && conflictTarget === expr.conflictTarget
            && updateAssignments === expr.updateAssignments) {
            return expr
        } else {
            return expr.copy(
                table = table,
                assignments = assignments,
                conflictTarget = conflictTarget,
                updateAssignments = updateAssignments
            )
        }
    }
}
