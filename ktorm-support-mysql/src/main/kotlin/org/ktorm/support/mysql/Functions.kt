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

package org.ktorm.support.mysql

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.*
import java.time.LocalDate

/**
 * MySQL json_contains function, translated to `json_contains(column, json_array(item))`.
 */
public inline fun <reified T : Any> Column<List<T>>.jsonContains(
    item: T,
    sqlType: SqlType<T> = SqlType.of() ?: error("Cannot detect the item's SqlType, please specify manually.")
): FunctionExpression<Boolean> {
    val listSqlType = this.sqlType

    // json_contains(column, json_array(item))
    return FunctionExpression(
        functionName = "json_contains",
        arguments = listOf(
            asExpression(),
            FunctionExpression(
                functionName = "json_array",
                arguments = listOf(ArgumentExpression(item, sqlType)),
                sqlType = listSqlType
            )
        ),
        sqlType = BooleanSqlType
    )
}

/**
 * MySQL json_extract function, translated to `json_extract(column, path)`.
 */
public inline fun <reified T : Any> Column<*>.jsonExtract(
    path: String,
    sqlType: SqlType<T> = SqlType.of() ?: error("Cannot detect the result's SqlType, please specify manually.")
): FunctionExpression<T> {
    // json_extract(column, path)
    return FunctionExpression(
        functionName = "json_extract",
        arguments = listOf(asExpression(), ArgumentExpression(path, VarcharSqlType)),
        sqlType = sqlType
    )
}

/**
 * MySQL rand function, translated to `rand()`.
 */
public fun rand(): FunctionExpression<Double> {
    return FunctionExpression(functionName = "rand", arguments = emptyList(), sqlType = DoubleSqlType)
}

/**
 * MySQL greatest function, translated to `greatest(column1, column2, ...)`.
 */
public fun <T : Comparable<T>> greatest(vararg columns: ColumnDeclaring<T>): FunctionExpression<T> {
    // greatest(left, right)
    return FunctionExpression(
        functionName = "greatest",
        arguments = columns.map { it.asExpression() },
        sqlType = columns[0].sqlType
    )
}

/**
 * MySQL greatest function, translated to `greatest(left, right)`.
 */
public fun <T : Comparable<T>> greatest(left: ColumnDeclaring<T>, right: T): FunctionExpression<T> {
    return greatest(left, left.wrapArgument(right))
}

/**
 * MySQL greatest function, translated to `greatest(left, right)`.
 */
public fun <T : Comparable<T>> greatest(left: T, right: ColumnDeclaring<T>): FunctionExpression<T> {
    return greatest(right.wrapArgument(left), right)
}

/**
 * MySQL least function, translated to `least(column1, column2, ...)`.
 */
public fun <T : Comparable<T>> least(vararg columns: ColumnDeclaring<T>): FunctionExpression<T> {
    // least(left, right)
    return FunctionExpression(
        functionName = "least",
        arguments = columns.map { it.asExpression() },
        sqlType = columns[0].sqlType
    )
}

/**
 * MySQL least function, translated to `least(left, right)`.
 */
public fun <T : Comparable<T>> least(left: ColumnDeclaring<T>, right: T): FunctionExpression<T> {
    return least(left, left.wrapArgument(right))
}

/**
 * MySQL least function, translated to `least(left, right)`.
 */
public fun <T : Comparable<T>> least(left: T, right: ColumnDeclaring<T>): FunctionExpression<T> {
    return least(right.wrapArgument(left), right)
}

/**
 * MySQL ifnull function, translated to `ifnull(left, right)`.
 */
public fun <T : Any> ColumnDeclaring<T>.ifNull(right: ColumnDeclaring<T>): FunctionExpression<T> {
    // ifnull(left, right)
    return FunctionExpression(
        functionName = "ifnull",
        arguments = listOf(this, right).map { it.asExpression() },
        sqlType = sqlType
    )
}

/**
 * MySQL ifnull function, translated to `ifnull(left, right)`.
 */
public fun <T : Any> ColumnDeclaring<T>.ifNull(right: T?): FunctionExpression<T> {
    return this.ifNull(wrapArgument(right))
}

/**
 * MySQL `if` function, translated to `if(condition, then, otherwise)`.
 *
 * @since 3.1.0
 */
@Suppress("FunctionNaming", "FunctionName")
public fun <T : Any> IF(
    condition: ColumnDeclaring<Boolean>,
    then: ColumnDeclaring<T>,
    otherwise: ColumnDeclaring<T>
): FunctionExpression<T> {
    // if(condition, then, otherwise)
    return FunctionExpression(
        functionName = "if",
        arguments = listOf(condition, then, otherwise).map { it.asExpression() },
        sqlType = then.sqlType
    )
}

/**
 * MySQL `if` function, translated to `if(condition, then, otherwise)`.
 *
 * @since 3.1.0
 */
@Suppress("FunctionNaming", "FunctionName")
public inline fun <reified T : Any> IF(
    condition: ColumnDeclaring<Boolean>,
    then: T,
    otherwise: T,
    sqlType: SqlType<T> = SqlType.of() ?: error("Cannot detect the param's SqlType, please specify manually.")
): FunctionExpression<T> {
    // if(condition, then, otherwise)
    return FunctionExpression(
        functionName = "if",
        arguments = listOf(
            condition.asExpression(),
            ArgumentExpression(then, sqlType),
            ArgumentExpression(otherwise, sqlType)
        ),
        sqlType = sqlType
    )
}

/**
 * MySQL datediff function, translated to `datediff(left, right)`.
 */
public fun dateDiff(left: ColumnDeclaring<LocalDate>, right: ColumnDeclaring<LocalDate>): FunctionExpression<Int> {
    // datediff(left, right)
    return FunctionExpression(
        functionName = "datediff",
        arguments = listOf(left.asExpression(), right.asExpression()),
        sqlType = IntSqlType
    )
}

/**
 * MySQL datediff function, translated to `datediff(left, right)`.
 */
public fun dateDiff(left: ColumnDeclaring<LocalDate>, right: LocalDate): FunctionExpression<Int> {
    return dateDiff(left, left.wrapArgument(right))
}

/**
 * MySQL datediff function, translated to `datediff(left, right)`.
 */
public fun dateDiff(left: LocalDate, right: ColumnDeclaring<LocalDate>): FunctionExpression<Int> {
    return dateDiff(right.wrapArgument(left), right)
}

/**
 * MySQL replace function, translated to `replace(str, oldValue, newValue)`.
 */
public fun ColumnDeclaring<String>.replace(oldValue: String, newValue: String): FunctionExpression<String> {
    // replace(str, oldValue, newValue)
    return FunctionExpression(
        functionName = "replace",
        arguments = listOf(
            this.asExpression(),
            ArgumentExpression(oldValue, VarcharSqlType),
            ArgumentExpression(newValue, VarcharSqlType)
        ),
        sqlType = VarcharSqlType
    )
}

/**
 * MySQL lower function, translated to `lower(str). `
 */
public fun ColumnDeclaring<String>.toLowerCase(): FunctionExpression<String> {
    // lower(str)
    return FunctionExpression(
        functionName = "lower",
        arguments = listOf(this.asExpression()),
        sqlType = VarcharSqlType
    )
}

/**
 * MySQL upper function, translated to `upper(str). `
 */
public fun ColumnDeclaring<String>.toUpperCase(): FunctionExpression<String> {
    // upper(str)
    return FunctionExpression(
        functionName = "upper",
        arguments = listOf(this.asExpression()),
        sqlType = VarcharSqlType
    )
}
