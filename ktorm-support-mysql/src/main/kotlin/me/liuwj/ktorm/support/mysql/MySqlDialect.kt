package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.SqlDialect
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.IntSqlType

/**
 * Created by vince on Sep 24, 2018.
 */
object MySqlDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return MySqlFormatter(database, beautifySql, indentSize)
    }

    private class MySqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int)
        : SqlFormatter(database, beautifySql, indentSize) {

        override fun writePagination(expr: QueryExpression) {
            newLine(Indentation.SAME)
            write("limit ?, ? ")
            _parameters += ArgumentExpression(expr.offset ?: 0, IntSqlType)
            _parameters += ArgumentExpression(expr.limit ?: Int.MAX_VALUE, IntSqlType)
        }

        override fun visitUnknown(expr: SqlExpression): SqlExpression {
            return when (expr) {
                is InsertOrUpdateExpression -> visitInsertOrUpdate(expr)
                is BulkInsertExpression -> visitBulkInsert(expr)
                is NaturalJoinExpression -> visitNaturalJoin(expr)
                else -> super.visitUnknown(expr)
            }
        }

        private fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
            write("insert into ${expr.table.name.quoted} (")
            for ((i, assignment) in expr.assignments.withIndex()) {
                if (i > 0) write(", ")
                write(assignment.column.name.quoted)
            }
            write(") values ")
            writeValues(expr.assignments)

            if (expr.updateAssignments.isNotEmpty()) {
                write("on duplicate key update ")
                visitColumnAssignments(expr.updateAssignments)
            }

            return expr
        }

        private fun visitBulkInsert(expr: BulkInsertExpression): BulkInsertExpression {
            write("insert into ${expr.table.name.quoted} (")
            for ((i, assignment) in expr.assignments[0].withIndex()) {
                if (i > 0) write(", ")
                write(assignment.column.name.quoted)
            }
            write(") values ")

            for ((i, assignments) in expr.assignments.withIndex()) {
                if (i > 0) {
                    removeLastBlank()
                    write(", ")
                }
                writeValues(assignments)
            }

            return expr
        }

        private fun writeValues(assignments: List<ColumnAssignmentExpression<*>>) {
            write("(")
            visitExpressionList(assignments.map { it.expression as ArgumentExpression })
            removeLastBlank()
            write(") ")
        }

        private fun visitNaturalJoin(expr: NaturalJoinExpression): NaturalJoinExpression {
            visitQuerySource(expr.left)
            newLine(Indentation.SAME)
            write("natural join ")
            visitQuerySource(expr.right)
            return expr
        }
    }
}