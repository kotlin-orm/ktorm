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

package org.ktorm.support.oracle

import org.ktorm.database.Database
import org.ktorm.expression.*
import org.ktorm.schema.IntSqlType

/**
 * [SqlFormatter] implementation for Oracle, formatting SQL expressions as strings with their execution arguments.
 */
public open class OracleFormatter(
    database: Database, beautifySql: Boolean, indentSize: Int
) : SqlFormatter(database, beautifySql, indentSize) {

    override fun shouldQuote(identifier: String): Boolean {
        // Oracle doesn't support underscores as the first character for unquoted identifiers.
        return identifier.startsWith('_') || super.shouldQuote(identifier)
    }

    override fun visitQuery(expr: QueryExpression): QueryExpression {
        if (expr.offset == null && expr.limit == null) {
            return super.visitQuery(expr)
        }

        // forUpdate() function in the core lib was removed, uncomment the following lines
        // when we add this feature back in the Oracle dialect.
        // if (expr is SelectExpression && expr.forUpdate) {
        //     throw DialectFeatureNotSupportedException("Locking is not supported when using offset/limit params.")
        // }

        val offset = expr.offset ?: 0
        val minRowNum = offset + 1
        val maxRowNum = expr.limit?.let { offset + it } ?: Int.MAX_VALUE

        val tempTableName = "_t"

        writeKeyword("select * ")
        newLine(Indentation.SAME)
        writeKeyword("from (")
        newLine(Indentation.INNER)
        writeKeyword("select ")
        write("${tempTableName.quoted}.*, ")
        writeKeyword("rownum ")
        write("${"_rn".quoted} ")
        newLine(Indentation.SAME)
        writeKeyword("from ")

        visitQuerySource(
            when (expr) {
                is SelectExpression -> expr.copy(tableAlias = tempTableName, offset = null, limit = null)
                is UnionExpression -> expr.copy(tableAlias = tempTableName, offset = null, limit = null)
            }
        )

        newLine(Indentation.SAME)
        writeKeyword("where rownum <= ?")
        newLine(Indentation.OUTER)
        write(") ")
        newLine(Indentation.SAME)
        writeKeyword("where ")
        write("${"_rn".quoted} >= ? ")

        _parameters += ArgumentExpression(maxRowNum, IntSqlType)
        _parameters += ArgumentExpression(minRowNum, IntSqlType)

        return expr
    }

    override fun writePagination(expr: QueryExpression) {
        throw AssertionError("Never happen.")
    }

    override fun visitUnion(expr: UnionExpression): UnionExpression {
        if (expr.orderBy.isEmpty()) {
            return super.visitUnion(expr)
        }

        // Use sub-query when there are order-by columns.
        writeKeyword("select * ")
        newLine(Indentation.SAME)
        writeKeyword("from ")
        visitQuerySource(expr.copy(orderBy = emptyList(), tableAlias = null))
        newLine(Indentation.SAME)
        writeKeyword("order by ")
        visitExpressionList(expr.orderBy)
        return expr
    }
}
