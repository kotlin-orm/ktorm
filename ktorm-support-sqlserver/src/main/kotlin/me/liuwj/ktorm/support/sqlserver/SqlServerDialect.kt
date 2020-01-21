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

package me.liuwj.ktorm.support.sqlserver

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.SqlDialect
import me.liuwj.ktorm.expression.*

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

        write("select * ")
        newLine(Indentation.SAME)
        write("from (")
        newLine(Indentation.INNER)
        write("select top $maxRowNum *, row_number() over(order by _order_by) _rownum ")
        newLine(Indentation.SAME)
        write("from (")
        newLine(Indentation.INNER)
        write("select *, _order_by = 0 ")
        newLine(Indentation.SAME)
        write("from ")

        visitQuerySource(
            when (expr) {
                is SelectExpression -> expr.copy(tableAlias = "_t1", offset = null, limit = null)
                is UnionExpression -> expr.copy(tableAlias = "_t1", offset = null, limit = null)
            }
        )

        newLine(Indentation.OUTER)
        write(") _t2 ")
        newLine(Indentation.OUTER)
        write(") _t3 ")
        newLine(Indentation.SAME)
        write("where _rownum >= $minRowNum ")
    }

    private fun writePagingQueryWithOrderBy(expr: QueryExpression) {
        val offset = expr.offset ?: 0
        val minRowNum = offset + 1
        val maxRowNum = expr.limit?.let { offset + it } ?: Int.MAX_VALUE

        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        val visitor = object : SqlExpressionVisitor() {
            override fun <T : Any> visitColumn(column: ColumnExpression<T>): ColumnExpression<T> {
                val alias = (expr as? SelectExpression)?.columns?.find { it.expression == column }?.declaredName
                return column.copy(tableAlias = "_t1", name = alias ?: column.name)
            }
        }

        write("select * ")
        newLine(Indentation.SAME)
        write("from (")
        newLine(Indentation.INNER)
        write("select top $maxRowNum *, row_number() over(order by ")
        visitOrderByList(expr.orderBy.map { visitor.visit(it) as OrderByExpression })
        removeLastBlank()
        write(") _rownum ")
        newLine(Indentation.SAME)
        write("from ")

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
        write(") _t2 ")
        newLine(Indentation.SAME)
        write("where _rownum >= $minRowNum ")
    }
}
