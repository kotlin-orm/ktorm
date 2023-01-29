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

@file:Suppress("MatchingDeclarationName")

package org.ktorm.support.postgresql

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.SqlType
import org.ktorm.schema.VarcharSqlType

/**
 * ILike expression, represents PostgreSQL `ilike` keyword.
 *
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
public data class ILikeExpression(
    val left: ScalarExpression<*>,
    val right: ScalarExpression<*>,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<Boolean>()

/**
 * ILike operator, translated to the `ilike` keyword in PostgreSQL.
 */
public infix fun ColumnDeclaring<*>.ilike(expr: ColumnDeclaring<String>): ILikeExpression {
    return ILikeExpression(asExpression(), expr.asExpression())
}

/**
 * ILike operator, translated to the `ilike` keyword in PostgreSQL.
 */
public infix fun ColumnDeclaring<*>.ilike(argument: String): ILikeExpression {
    return this ilike ArgumentExpression(argument, VarcharSqlType)
}
