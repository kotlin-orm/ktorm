package me.liuwj.ktorm.support.oracle

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.SqlDialect
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.IntSqlType

/**
 * Created by vince on Sep 25, 2018.
 */
object OracleDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return OracleFormatter(database, beautifySql, indentSize)
    }

    private class OracleFormatter(database: Database, beautifySql: Boolean, indentSize: Int)
        : SqlFormatter(database, beautifySql, indentSize) {

        override fun visitQuery(expr: QueryExpression): QueryExpression {
            if (expr.offset == null && expr.limit == null) {
                return super.visitQuery(expr)
            }

            val offset = expr.offset ?: 0
            val minRowNum = offset + 1
            val maxRowNum = expr.limit?.let { offset + it } ?: Int.MAX_VALUE

            write("select * from (")
            newLine(Indentation.INNER)
            write("select rownum rn, a.* ")
            newLine(Indentation.SAME)
            write("from ")

            visitQuerySource(
                when (expr) {
                    is SelectExpression -> expr.copy(tableAlias = "a", offset = null, limit = null)
                    is UnionExpression -> expr.copy(tableAlias = "a", offset = null, limit = null)
                }
            )

            newLine(Indentation.SAME)
            write("where rownum <= ?")
            newLine(Indentation.OUTER)
            write(") ")
            newLine(Indentation.SAME)
            write("where rn >= ?")

            _parameters += ArgumentExpression(maxRowNum, IntSqlType)
            _parameters += ArgumentExpression(minRowNum, IntSqlType)

            return expr
        }
    }
}
