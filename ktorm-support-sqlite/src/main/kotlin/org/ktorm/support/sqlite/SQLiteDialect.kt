/*
 * Copyright 2018-2022 the original author or authors.
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
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.QueryExpression
import org.ktorm.expression.SqlFormatter
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

    override fun writePagination(expr: QueryExpression) {
        newLine(Indentation.SAME)
        writeKeyword("limit ?, ? ")
        _parameters += ArgumentExpression(expr.offset ?: 0, IntSqlType)
        _parameters += ArgumentExpression(expr.limit ?: Int.MAX_VALUE, IntSqlType)
    }
}
