/*
 * Copyright 2018-2019 the original author or authors.
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

package me.liuwj.ktorm.support.sqlserver

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.SqlDialect
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.IntSqlType

/**
 * [SqlDialect] implementation for Microsoft SqlServer database.
 */
open class SqlServerDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return SqlServerFormatter(database, beautifySql, indentSize)
    }
}

/**
 * [SqlFormatter] implementation for SqlServer, formatting SQL expressions as strings with their execution arguments.
 */
open class SqlServerFormatter(database: Database, beautifySql: Boolean, indentSize: Int)
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
