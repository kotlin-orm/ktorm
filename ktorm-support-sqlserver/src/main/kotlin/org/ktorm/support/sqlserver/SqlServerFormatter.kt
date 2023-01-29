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

package org.ktorm.support.sqlserver

import org.ktorm.database.Database
import org.ktorm.expression.*

/**
 * [SqlFormatter] implementation for SqlServer, formatting SQL expressions as strings with their execution arguments.
 */
public open class SqlServerFormatter(
    database: Database, beautifySql: Boolean, indentSize: Int
) : SqlFormatter(database, beautifySql, indentSize) {

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

        writeKeyword("select * ")
        newLine(Indentation.SAME)
        writeKeyword("from (")
        newLine(Indentation.INNER)
        writeKeyword("select top $maxRowNum *, row_number() over(order by ")
        visitExpressionList(expr.upliftOrderByColumns())
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

    private fun QueryExpression.upliftOrderByColumns(): List<OrderByExpression> {
        val query = this

        val interceptor = object : SqlExpressionVisitorInterceptor {
            override fun intercept(expr: SqlExpression, visitor: SqlExpressionVisitor): SqlExpression? {
                if (expr is ColumnExpression<*>) {
                    val alias = (query as? SelectExpression)?.columns?.find { it.expression == expr }?.declaredName
                    if (alias == null && query is SelectExpression && query.columns.isNotEmpty()) {
                        throw IllegalStateException("Order-by column '${expr.name}' must exist in the select clause.")
                    }

                    return expr.copy(table = TableExpression(name = "", tableAlias = "_t1"), name = alias ?: expr.name)
                }

                return null
            }
        }

        return orderBy.map { database.dialect.createExpressionVisitor(interceptor).visitOrderBy(it) }
    }

    override fun writePagination(expr: QueryExpression) {
        throw AssertionError("Never happen.")
    }

    override fun visitWindowFrameBound(expr: WindowFrameBoundExpression): WindowFrameBoundExpression {
        if (expr.type == WindowFrameBoundType.PRECEDING || expr.type == WindowFrameBoundType.FOLLOWING) {
            // SQL Server doesn't support arguments for `N preceding` & `N following`,
            // we have to write literal number to the generated SQL.
            val argument = expr.argument as ArgumentExpression
            write("${argument.value} ")
        }

        writeKeyword("${expr.type} ")
        return expr
    }
}
