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

package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.SqlDialect
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.IntSqlType

/**
 * [SqlDialect] implementation for PostgreSQL database.
 */
open class PostgreSqlDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return PostgreSqlFormatter(database, beautifySql, indentSize)
    }
}

/**
 * [SqlFormatter] implementation for PostgreSQL, formatting SQL expressions as strings with their execution arguments.
 */
open class PostgreSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int)
    : SqlFormatter(database, beautifySql, indentSize) {

    override fun <T : Any> visitScalar(expr: ScalarExpression<T>): ScalarExpression<T> {
        val result = when (expr) {
            is ILikeExpression -> visitILike(expr)
            else -> super.visitScalar(expr)
        }

        @Suppress("UNCHECKED_CAST")
        return result as ScalarExpression<T>
    }

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

    protected open fun visitILike(expr: ILikeExpression): ILikeExpression {
        if (expr.left.removeBrackets) {
            visit(expr.left)
        } else {
            write("(")
            visit(expr.left)
            removeLastBlank()
            write(") ")
        }

        write("ilike ")

        if (expr.right.removeBrackets) {
            visit(expr.right)
        } else {
            write("(")
            visit(expr.right)
            removeLastBlank()
            write(") ")
        }

        return expr
    }
}
