package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.SqlDialect
import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.QueryExpression
import me.liuwj.ktorm.expression.SqlFormatter
import me.liuwj.ktorm.schema.IntSqlType

/**
 * Created by vince on Sep 25, 2018.
 */
object PostgreSqlDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return PostgreSqlFormatter(database, beautifySql, indentSize)
    }

    private class PostgreSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int)
        : SqlFormatter(database, beautifySql, indentSize) {

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
    }
}