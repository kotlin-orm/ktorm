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

import org.ktorm.database.*
import org.ktorm.expression.*

/**
 * [SqlDialect] implementation for SQLite database.
 */
public open class SQLiteDialect : SqlDialect {

    override fun createExpressionVisitor(interceptor: SqlExpressionVisitorInterceptor): SqlExpressionVisitor {
        return SQLiteExpressionVisitor::class.newVisitorInstance(interceptor)
    }

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

            val retrieveKeySql = "SELECT LAST_INSERT_ROWID()"
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
