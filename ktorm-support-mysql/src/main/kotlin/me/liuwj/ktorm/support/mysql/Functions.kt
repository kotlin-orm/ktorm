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

package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.FunctionExpression
import me.liuwj.ktorm.schema.*
import java.time.LocalDate

/**
 * MySQL json_contains function, translated to `json_contains(column, json_array(item))`.
 */
fun <T : Any> Column<List<T>>.jsonContains(item: T, itemSqlType: SqlType<T>): FunctionExpression<Boolean> {
    val listSqlType = this.sqlType

    // json_contains(column, json_array(item))
    return FunctionExpression(
        functionName = "json_contains",
        arguments = listOf(
            asExpression(),
            FunctionExpression(
                functionName = "json_array",
                arguments = listOf(ArgumentExpression(item, itemSqlType)),
                sqlType = listSqlType
            )
        ),
        sqlType = BooleanSqlType
    )
}

/**
 * MySQL json_contains function, translated to `json_contains(column, json_array(item))`.
 */
infix fun Column<List<Int>>.jsonContains(item: Int): FunctionExpression<Boolean> {
    return this.jsonContains(item, IntSqlType)
}

/**
 * MySQL json_contains function, translated to `json_contains(column, json_array(item))`.
 */
infix fun Column<List<Long>>.jsonContains(item: Long): FunctionExpression<Boolean> {
    return this.jsonContains(item, LongSqlType)
}

/**
 * MySQL json_contains function, translated to `json_contains(column, json_array(item))`.
 */
infix fun Column<List<Double>>.jsonContains(item: Double): FunctionExpression<Boolean> {
    return this.jsonContains(item, DoubleSqlType)
}

/**
 * MySQL json_contains function, translated to `json_contains(column, json_array(item))`.
 */
infix fun Column<List<Float>>.jsonContains(item: Float): FunctionExpression<Boolean> {
    return this.jsonContains(item, FloatSqlType)
}

/**
 * MySQL json_contains function, translated to `json_contains(column, json_array(item))`.
 */
infix fun Column<List<String>>.jsonContains(item: String): FunctionExpression<Boolean> {
    return this.jsonContains(item, VarcharSqlType)
}

/**
 * MySQL json_extract function, translated to `json_extract(column, path)`.
 */
fun <T : Any> Column<*>.jsonExtract(path: String, sqlType: SqlType<T>): FunctionExpression<T> {
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
fun rand(): FunctionExpression<Double> {
    return FunctionExpression(functionName = "rand", arguments = emptyList(), sqlType = DoubleSqlType)
}

/**
 * MySQL greatest function, translated to `greatest(column1, column2, ...)`.
 */
fun <T : Comparable<T>> greatest(vararg columns: ColumnDeclaring<T>): FunctionExpression<T> {
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
fun <T : Comparable<T>> greatest(left: ColumnDeclaring<T>, right: T): FunctionExpression<T> {
    return greatest(left, left.wrapArgument(right))
}

/**
 * MySQL greatest function, translated to `greatest(left, right)`.
 */
fun <T : Comparable<T>> greatest(left: T, right: ColumnDeclaring<T>): FunctionExpression<T> {
    return greatest(right.wrapArgument(left), right)
}

/**
 * MySQL least function, translated to `least(column1, column2, ...)`.
 */
fun <T : Comparable<T>> least(vararg columns: ColumnDeclaring<T>): FunctionExpression<T> {
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
fun <T : Comparable<T>> least(left: ColumnDeclaring<T>, right: T): FunctionExpression<T> {
    return least(left, left.wrapArgument(right))
}

/**
 * MySQL least function, translated to `least(left, right)`.
 */
fun <T : Comparable<T>> least(left: T, right: ColumnDeclaring<T>): FunctionExpression<T> {
    return least(right.wrapArgument(left), right)
}

/**
 * MySQL ifnull function, translated to `ifnull(left, right)`.
 */
fun <T : Any> ColumnDeclaring<T>.ifNull(right: ColumnDeclaring<T>): FunctionExpression<T> {
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
fun <T : Any> ColumnDeclaring<T>.ifNull(right: T?): FunctionExpression<T> {
    return this.ifNull(wrapArgument(right))
}

/**
 * MySQL datediff function, translated to `datediff(left, right)`.
 */
fun dateDiff(left: ColumnDeclaring<LocalDate>, right: ColumnDeclaring<LocalDate>): FunctionExpression<Int> {
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
fun dateDiff(left: ColumnDeclaring<LocalDate>, right: LocalDate): FunctionExpression<Int> {
    return dateDiff(left, left.wrapArgument(right))
}

/**
 * MySQL datediff function, translated to `datediff(left, right)`.
 */
fun dateDiff(left: LocalDate, right: ColumnDeclaring<LocalDate>): FunctionExpression<Int> {
    return dateDiff(right.wrapArgument(left), right)
}

/**
 * MySQL replace function, translated to `replace(str, oldValue, newValue)`.
 */
fun ColumnDeclaring<String>.replace(oldValue: String, newValue: String): FunctionExpression<String> {
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
