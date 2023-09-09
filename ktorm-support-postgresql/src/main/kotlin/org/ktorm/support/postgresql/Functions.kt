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

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.VarcharSqlType

/**
 * Returns the subscript of the first occurrence of the second argument in the array, or NULL if it's not present.
 * If the third argument is given, the search begins at that subscript. The array must be one-dimensional.
 *
 * array_position(ARRAY['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'], 'mon') → 2
 */
public fun arrayPosition(
    array: ColumnDeclaring<TextArray>, value: ColumnDeclaring<String>, offset: Int? = null
): FunctionExpression<Int> {
    // array_position(array, value[, offset])
    return FunctionExpression(
        functionName = "array_position",
        arguments = listOfNotNull(
            array.asExpression(),
            value.asExpression(),
            offset?.let { ArgumentExpression(it, IntSqlType) }
        ),
        sqlType = IntSqlType
    )
}

/**
 * Returns the subscript of the first occurrence of the second argument in the array, or NULL if it's not present.
 * If the third argument is given, the search begins at that subscript. The array must be one-dimensional.
 *
 * array_position(ARRAY['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'], 'mon') → 2
 */
public fun arrayPosition(
    array: ColumnDeclaring<TextArray>, value: String, offset: Int? = null
): FunctionExpression<Int> {
    return arrayPosition(array, ArgumentExpression(value, VarcharSqlType), offset)
}

/**
 * Returns the subscript of the first occurrence of the second argument in the array, or NULL if it's not present.
 * If the third argument is given, the search begins at that subscript. The array must be one-dimensional.
 *
 * array_position(ARRAY['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'], 'mon') → 2
 */
public fun arrayPosition(
    array: TextArray, value: ColumnDeclaring<String>, offset: Int? = null
): FunctionExpression<Int> {
    return arrayPosition(ArgumentExpression(array, TextArraySqlType), value, offset)
}