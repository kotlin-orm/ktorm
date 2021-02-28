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

package org.ktorm.support.postgresql

import org.ktorm.database.Database
import org.ktorm.database.DialectFeatureNotSupportedException
import org.ktorm.database.SqlDialect
import org.ktorm.expression.*
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.Table

/**
 * [SqlDialect] implementation for PostgreSQL database.
 */
public open class PostgreSqlDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return PostgreSqlFormatter(database, beautifySql, indentSize)
    }
}

/**
 * Postgres Specific ForUpdateOption. See docs: https://www.postgresql.org/docs/13/sql-select.html#SQL-FOR-UPDATE-SHARE
 */
public class PostgresForUpdateOption(
    private val lockStrength: LockStrength,
    private val onLock: OnLock,
    private vararg val tables: Table<*>
) : ForUpdateOption {

    /**
     * Generates SQL locking clause.
     */
    public fun toLockingClause(): String {
        val lockingClause = StringBuilder(lockStrength.keywords)
        if (tables.isNotEmpty()) {
            tables.joinTo(lockingClause, prefix = "of ", postfix = " ") { it.tableName }
        }
        onLock.keywords?.let { lockingClause.append(it) }
        return lockingClause.toString()
    }

    /**
     * Lock strength.
     */
    public enum class LockStrength(public val keywords: String) {
        Update("for update "),
        NoKeyUpdate("for no key update "),
        Share("for share "),
        KeyShare("for key share ")
    }

    /**
     * Behavior when a lock is detected.
     */
    public enum class OnLock(public val keywords: String?) {
        Wait(null),
        NoWait("no wait "),
        SkipLocked("skip locked ")
    }
}

/**
 * [SqlFormatter] implementation for PostgreSQL, formatting SQL expressions as strings with their execution arguments.
 */
public open class PostgreSqlFormatter(
    database: Database, beautifySql: Boolean, indentSize: Int
) : SqlFormatter(database, beautifySql, indentSize) {
    override fun writeForUpdate(forUpdate: ForUpdateOption) {
        when (forUpdate) {
            is PostgresForUpdateOption -> writeKeyword(forUpdate.toLockingClause())
            else -> throw DialectFeatureNotSupportedException(
                "Unsupported ForUpdateOption ${forUpdate::class.java.name}."
            )
        }
    }

    override fun checkColumnName(name: String) {
        val maxLength = database.maxColumnNameLength
        if (maxLength > 0 && name.length > maxLength) {
            throw IllegalStateException("The identifier '$name' is too long. Maximum length is $maxLength")
        }
    }

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
            is ILikeExpression -> visitILike(expr)
            is HStoreExpression -> visitHStore(expr)
            else -> super.visitScalar(expr)
        }

        @Suppress("UNCHECKED_CAST")
        return result as ScalarExpression<T>
    }

    override fun visitTable(expr: TableExpression): TableExpression {
        if (expr.catalog != null && expr.catalog!!.isNotBlank()) {
            write("${expr.catalog!!.quoted}.")
        }
        if (expr.schema != null && expr.schema!!.isNotBlank()) {
            write("${expr.schema!!.quoted}.")
        }

        write("${expr.name.quoted} ")

        if (expr.tableAlias != null && expr.tableAlias!!.isNotBlank()) {
            writeKeyword("as ") // The 'as' keyword is required for update statements in PostgreSQL.
            write("${expr.tableAlias!!.quoted} ")
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

    protected open fun visitILike(expr: ILikeExpression): ILikeExpression {
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

    protected open fun <T : Any> visitHStore(expr: HStoreExpression<T>): HStoreExpression<T> {
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

    protected open fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        writeKeyword("insert into ")
        visitTable(expr.table)
        writeInsertColumnNames(expr.assignments.map { it.column })
        writeKeyword("values ")
        writeInsertValues(expr.assignments)

        if (expr.updateAssignments.isNotEmpty()) {
            writeKeyword("on conflict ")
            writeInsertColumnNames(expr.conflictColumns)
            writeKeyword("do update set ")
            visitColumnAssignments(expr.updateAssignments)
        }

        if (expr.returningColumns.isNotEmpty()) {
            writeKeyword(" returning ")
            expr.returningColumns.forEachIndexed { i, column ->
                if (i > 0) write(", ")
                checkColumnName(column.name)
                write(column.name.quoted)
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

        if (expr.updateAssignments.isNotEmpty()) {
            writeKeyword("on conflict ")
            writeInsertColumnNames(expr.conflictColumns)
            writeKeyword("do update set ")
            visitColumnAssignments(expr.updateAssignments)
        }

        if (expr.returningColumns.isNotEmpty()) {
            writeKeyword(" returning ")
            expr.returningColumns.forEachIndexed { i, column ->
                if (i > 0) write(", ")
                checkColumnName(column.name)
                write(column.name.quoted)
            }
        }

        return expr
    }
}

/**
 * Base class designed to visit or modify PostgreSQL's expression trees using visitor pattern.
 *
 * For detailed documents, see [SqlExpressionVisitor].
 */
public open class PostgreSqlExpressionVisitor : SqlExpressionVisitor() {

    override fun visit(expr: SqlExpression): SqlExpression {
        return when (expr) {
            is InsertOrUpdateExpression -> visitInsertOrUpdate(expr)
            is BulkInsertExpression -> visitBulkInsert(expr)
            else -> super.visit(expr)
        }
    }

    override fun <T : Any> visitScalar(expr: ScalarExpression<T>): ScalarExpression<T> {
        val result = when (expr) {
            is ILikeExpression -> visitILike(expr)
            is HStoreExpression -> visitHStore(expr)
            else -> super.visitScalar(expr)
        }

        @Suppress("UNCHECKED_CAST")
        return result as ScalarExpression<T>
    }

    protected open fun visitILike(expr: ILikeExpression): ILikeExpression {
        val left = visitScalar(expr.left)
        val right = visitScalar(expr.right)

        if (left === expr.left && right === expr.right) {
            return expr
        } else {
            return expr.copy(left = left, right = right)
        }
    }

    protected open fun <T : Any> visitHStore(expr: HStoreExpression<T>): HStoreExpression<T> {
        val left = visitScalar(expr.left)
        val right = visitScalar(expr.right)

        if (left === expr.left && right === expr.right) {
            return expr
        } else {
            return expr.copy(left = left, right = right)
        }
    }

    protected open fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        val table = visitTable(expr.table)
        val assignments = visitColumnAssignments(expr.assignments)
        val conflictColumns = visitExpressionList(expr.conflictColumns)
        val updateAssignments = visitColumnAssignments(expr.updateAssignments)

        @Suppress("ComplexCondition")
        if (table === expr.table
            && assignments === expr.assignments
            && conflictColumns === expr.conflictColumns
            && updateAssignments === expr.updateAssignments
        ) {
            return expr
        } else {
            return expr.copy(
                table = table,
                assignments = assignments,
                conflictColumns = conflictColumns,
                updateAssignments = updateAssignments
            )
        }
    }

    protected open fun visitBulkInsert(expr: BulkInsertExpression): BulkInsertExpression {
        val table = expr.table
        val assignments = visitBulkInsertAssignments(expr.assignments)
        val conflictColumns = visitExpressionList(expr.conflictColumns)
        val updateAssignments = visitColumnAssignments(expr.updateAssignments)

        @Suppress("ComplexCondition")
        if (table === expr.table
            && assignments === expr.assignments
            && conflictColumns === expr.conflictColumns
            && updateAssignments === expr.updateAssignments
        ) {
            return expr
        } else {
            return expr.copy(
                table = table,
                assignments = assignments,
                conflictColumns = conflictColumns,
                updateAssignments = updateAssignments
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
