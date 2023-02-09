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

package org.ktorm.support.postgresql

import org.ktorm.dsl.cast
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.SqlType
import org.ktorm.schema.TextSqlType

/**
 * PostgreSQL array_position function for enums, translated to `array_position(value, cast(column as text))`.
 * Uses the `name` attribute of the enums as actual value for the query.
 */
public fun <T : Enum<T>> arrayPosition(value: Array<T?>, column: ColumnDeclaring<T>): FunctionExpression<Int> =
    arrayPosition(value.map { it?.name }.toTypedArray(), column.cast(TextSqlType))

/**
 * PostgreSQL array_position function for enums, translated to `array_position(value, cast(column as text))`.
 * Uses the `name` attribute of the enums as actual value for the query.
 */
public inline fun <reified T : Enum<T>> arrayPosition(
    value: Collection<T?>,
    column: ColumnDeclaring<T>
): FunctionExpression<Int> =
    arrayPosition(value.map { it?.name }.toTypedArray(), column.cast(TextSqlType))

/**
 * PostgreSQL array_position function, translated to `array_position(value, column)`.
 */
public fun arrayPosition(value: TextArray, column: ColumnDeclaring<String>): FunctionExpression<Int> =
    arrayPosition(value, column, TextArraySqlType)

/**
 * PostgreSQL array_position function, translated to `array_position(value, column)`.
 */
public fun <T : Any> arrayPosition(
    value: Array<T?>,
    column: ColumnDeclaring<T>,
    arraySqlType: SqlType<Array<T?>>
): FunctionExpression<Int> =
    FunctionExpression(
        functionName = "array_position",
        arguments = listOf(ArgumentExpression(value, arraySqlType), column.asExpression()),
        sqlType = IntSqlType
    )
