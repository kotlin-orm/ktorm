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

@file:Suppress("MatchingDeclarationName")

package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.ScalarExpression
import me.liuwj.ktorm.schema.BooleanSqlType
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.VarcharSqlType

/**
 * ILike expression, represents PostgreSQL's `ilike` keyword.
 *
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
data class ILikeExpression<LeftT : Any>(
    override val left: ScalarExpression<LeftT>,
    override val right: ScalarExpression<String>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : BinaryExpression<LeftT, String, Boolean>() {
    override val sqlType: SqlType<Boolean> = BooleanSqlType
    override val operator: String = "ilike"
    override fun copyWithNewOperands(left: ScalarExpression<LeftT>, right: ScalarExpression<String>) = copy(left = left, right = right)
}

/**
 * ILike operator, translated to the `ilike` keyword in PostgreSQL.
 */
infix fun <T : Any> ColumnDeclaring<T>.ilike(expr: ColumnDeclaring<String>): ILikeExpression<T> {
    return ILikeExpression(asExpression(), expr.asExpression())
}

/**
 * ILike operator, translated to the `ilike` keyword in PostgreSQL.
 */
infix fun <T : Any> ColumnDeclaring<T>.ilike(argument: String): ILikeExpression<T> {
    return this ilike ArgumentExpression(argument, VarcharSqlType)
}
