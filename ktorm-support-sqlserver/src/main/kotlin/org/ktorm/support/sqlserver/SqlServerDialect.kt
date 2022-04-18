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

package org.ktorm.support.sqlserver

import org.ktorm.database.Database
import org.ktorm.database.SqlDialect
import org.ktorm.expression.*

/**
 * [SqlDialect] implementation for Microsoft SqlServer database.
 */
public open class SqlServerDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return SqlServerFormatter(database, beautifySql, indentSize)
    }
}

/**
 * [SqlFormatter] implementation for SqlServer, formatting SQL expressions as strings with their execution arguments.
 */
public open class SqlServerFormatter(
    database: Database, beautifySql: Boolean, indentSize: Int
) : SqlFormatter(database, beautifySql, indentSize) {

    override fun checkColumnName(name: String) {
        val maxLength = database.maxColumnNameLength
        if (maxLength > 0 && name.length > maxLength) {
            throw IllegalStateException("The identifier '$name' is too long. Maximum length is $maxLength")
        }
    }

    override fun visitQuery(expr: QueryExpression): QueryExpression {
        if (expr.offset == null && expr.limit == null) {
            return super.visitQuery(expr)
        }

        // forUpdate() function in the core lib was removed, uncomment the following lines
        // when we add this feature back in the SqlServer dialect.
        // if (expr is SelectExpression && expr.forUpdate) {
        //     throw DialectFeatureNotSupportedException("Locking is not supported when using offset/limit params.")
        // }

        if (expr.orderBy.isEmpty()) {
            writePagingQuery(expr)
        } else {
            writePagingQueryWithOrderBy(expr)
        }

        return expr
    }

    private fun writePagingQuery(expr: QueryExpression) {
        val offset = expr.offset ?: 0
        val minRowNum = offset + 1
        val maxRowNum = expr.limit?.let { offset + it } ?: Int.MAX_VALUE

        writeKeyword("select * ")
        newLine(Indentation.SAME)
        writeKeyword("from (")
        newLine(Indentation.INNER)
        writeKeyword("select top $maxRowNum *, row_number() over(order by _order_by) _rownum ")
        newLine(Indentation.SAME)
        writeKeyword("from (")
        newLine(Indentation.INNER)
        writeKeyword("select *, _order_by = 0 ")
        newLine(Indentation.SAME)
        writeKeyword("from ")

        visitQuerySource(
            when (expr) {
                is SelectExpression -> expr.copy(tableAlias = "_t1", offset = null, limit = null)
                is UnionExpression -> expr.copy(tableAlias = "_t1", offset = null, limit = null)
            }
        )

        newLine(Indentation.OUTER)
        write(") ${"_t2".quoted} ")
        newLine(Indentation.OUTER)
        write(") ${"_t3".quoted} ")
        newLine(Indentation.SAME)
        writeKeyword("where _rownum >= $minRowNum ")
    }

    private fun writePagingQueryWithOrderBy(expr: QueryExpression) {
        val offset = expr.offset ?: 0
        val minRowNum = offset + 1
        val maxRowNum = expr.limit?.let { offset + it } ?: Int.MAX_VALUE

        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        val visitor = object : SqlExpressionVisitor() {
            override fun <T : Any> visitColumn(column: ColumnExpression<T>): ColumnExpression<T> {
                val alias = (expr as? SelectExpression)?.columns?.find { it.expression == column }?.declaredName
                if (alias == null && expr is SelectExpression && expr.columns.isNotEmpty()) {
                    throw IllegalStateException("Order-by column '${column.name}' must exist in the select clause.")
                }

                return column.copy(table = TableExpression(name = "", tableAlias = "_t1"), name = alias ?: column.name)
            }
        }

        writeKeyword("select * ")
        newLine(Indentation.SAME)
        writeKeyword("from (")
        newLine(Indentation.INNER)
        writeKeyword("select top $maxRowNum *, row_number() over(order by ")
        visitOrderByList(expr.orderBy.map { visitor.visit(it) as OrderByExpression })
        removeLastBlank()
        writeKeyword(") _rownum ")
        newLine(Indentation.SAME)
        writeKeyword("from ")

        visitQuerySource(
            when (expr) {
                is SelectExpression -> {
                    expr.copy(orderBy = emptyList(), tableAlias = "_t1", offset = null, limit = null)
                }
                is UnionExpression -> {
                    expr.copy(orderBy = emptyList(), tableAlias = "_t1", offset = null, limit = null)
                }
            }
        )

        newLine(Indentation.OUTER)
        write(") ${"_t2".quoted} ")
        newLine(Indentation.SAME)
        writeKeyword("where _rownum >= $minRowNum ")
    }

    override fun writePagination(expr: QueryExpression) {
        throw AssertionError("Never happen.")
    }
}
