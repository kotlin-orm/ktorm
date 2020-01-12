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

package me.liuwj.ktorm.support.sqlite

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.SqlDialect
import me.liuwj.ktorm.database.use
import me.liuwj.ktorm.dsl.setArguments
import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.QueryExpression
import me.liuwj.ktorm.expression.SqlFormatter
import me.liuwj.ktorm.schema.IntSqlType
import java.sql.Connection
import java.sql.ResultSet

/**
 * [SqlDialect] implementation for SQLite database.
 */
open class SQLiteDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return SQLiteFormatter(database, beautifySql, indentSize)
    }

    /**
     * Excecute a statment from the provided sql string, pass the result set to a provided function
     * and return its result.
     * The typical use case for this will be an insert operation that returns an auto-generated key.
     * @param conn The database connection to use
     * @param sql The formatted SQL statement
     * @param args The arguments to be provided to the statement
     * @param resultSetHandler A function to convert the result set into a value of type T
     * @return The result of [resultSetHandler]
     */

    override fun <T : Any> executeAndGenerate(
        conn: Connection,
        sql: String,
        args: List<ArgumentExpression<*>>,
        resultSetHandler: (ResultSet) -> T
    ): T {
        conn.prepareStatement(sql).use { statement ->
            statement.setArguments(args)
            statement.executeUpdate()
        }
        return conn.prepareStatement("select last_insert_rowid()").use { statement ->
            statement.executeQuery().use { resultSet ->
                resultSetHandler(resultSet)
            }
        }
    }
}

/**
 * [SqlFormatter] implementation for SQLite, formatting SQL expressions as strings with their execution arguments.
 */
open class SQLiteFormatter(database: Database, beautifySql: Boolean, indentSize: Int)
    : SqlFormatter(database, beautifySql, indentSize) {

    override fun writePagination(expr: QueryExpression) {
        newLine(Indentation.SAME)
        write("limit ?, ? ")
        _parameters += ArgumentExpression(expr.offset ?: 0, IntSqlType)
        _parameters += ArgumentExpression(expr.limit ?: Int.MAX_VALUE, IntSqlType)
    }
}
