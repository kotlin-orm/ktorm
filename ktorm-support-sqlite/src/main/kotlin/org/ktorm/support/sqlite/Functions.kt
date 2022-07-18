/*
 * Copyright 2018-2022 the original author or authors.
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

package org.ktorm.support.sqlite

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.*
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.*
import java.util.*

@PublishedApi
internal inline fun <reified T : Any> sqlTypeOf(): SqlType<T>? {
    val sqlType = when (T::class) {
        Boolean::class -> BooleanSqlType
        Int::class -> IntSqlType
        Short::class -> ShortSqlType
        Long::class -> LongSqlType
        Float::class -> FloatSqlType
        Double::class -> DoubleSqlType
        BigDecimal::class -> DecimalSqlType
        String::class -> VarcharSqlType
        ByteArray::class -> BytesSqlType
        Timestamp::class -> TimestampSqlType
        Date::class -> DateSqlType
        Time::class -> TimeSqlType
        Instant::class -> InstantSqlType
        LocalDateTime::class -> LocalDateTimeSqlType
        LocalDate::class -> LocalDateSqlType
        LocalTime::class -> LocalTimeSqlType
        MonthDay::class -> MonthDaySqlType
        YearMonth::class -> YearMonthSqlType
        Year::class -> YearSqlType
        UUID::class -> UuidSqlType
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    return sqlType as SqlType<T>?
}

// region SQLite: The JSON1 Extension

/**
 * SQLite json_extract function, translated to `json_extract(column, path)`.
 */
public inline fun <reified T : Any> ColumnDeclaring<*>.jsonExtract(
    path: String,
    sqlType: SqlType<T> = sqlTypeOf() ?: error("Cannot detect the result's SqlType, please specify manually.")
): FunctionExpression<T> {
    // json_extract(column, path)
    return FunctionExpression(
        functionName = "json_extract",
        arguments = listOf(asExpression(), ArgumentExpression(path, VarcharSqlType)),
        sqlType = sqlType
    )
}

/**
 * SQLite json_patch function, translated to `json_patch(left, right)`.
 */
public fun ColumnDeclaring<*>.jsonPatch(right: ColumnDeclaring<*>): FunctionExpression<String> {
    // json_patch(left, right)
    return FunctionExpression(
        functionName = "json_patch",
        arguments = listOf(this, right).map { it.asExpression() },
        sqlType = VarcharSqlType
    )
}

/**
 * SQLite json_remove function, translated to `json_remove(column, path)`.
 */
public fun ColumnDeclaring<*>.jsonRemove(path: String): FunctionExpression<String> {
    // json_remove(column, path)
    return FunctionExpression(
        functionName = "json_remove",
        arguments = listOf(asExpression(), ArgumentExpression(path, VarcharSqlType)),
        sqlType = VarcharSqlType
    )
}

/**
 * SQLite json_valid function, translated to `json_valid(column)`.
 */
public fun ColumnDeclaring<*>.jsonValid(): FunctionExpression<Boolean> {
    // json_valid(column)
    return FunctionExpression(
        functionName = "json_valid",
        arguments = listOf(asExpression()),
        sqlType = BooleanSqlType
    )
}

// endregion

// region SQLite: Built-in Aggregate Functions

/**
 * SQLite avg function, translated to `avg(column)`.
 */
public fun <T : Any> Column<T>.avg(): FunctionExpression<Double> {
    // avg(column)
    return FunctionExpression(functionName = "avg", arguments = listOf(asExpression()), sqlType = DoubleSqlType)
}

/**
 * SQLite count function, translated to `count(column)`.
 */
public fun <T : Any> Column<T>.count(): FunctionExpression<Long> {
    // count(column)
    return FunctionExpression(functionName = "count", arguments = listOf(asExpression()), sqlType = LongSqlType)
}

/**
 * SQLite count function, translated to `count()`.
 */
public fun count(): FunctionExpression<Long> {
    // count()
    return FunctionExpression(functionName = "count", arguments = emptyList(), sqlType = LongSqlType)
}

/**
 * SQLite group_concat function, translated to `group_concat(column)`.
 */
public fun <T : Any> Column<T>.groupConcat(): FunctionExpression<String> {
    // group_concat(column)
    return FunctionExpression(
        functionName = "group_concat", arguments = listOf(asExpression()), sqlType = VarcharSqlType
    )
}

/**
 * SQLite group_concat function, translated to `group_concat(column, separator)`.
 */
public fun <T : Any> Column<T>.groupConcat(separator: String): FunctionExpression<String> {
    // group_concat(column, separator)
    return FunctionExpression(
        functionName = "group_concat",
        arguments = listOf(asExpression(), ArgumentExpression(separator, VarcharSqlType)),
        sqlType = VarcharSqlType
    )
}

/**
 * SQLite max function, translated to `max(column)`.
 */
public fun <T : Any> Column<T>.max(): FunctionExpression<Long> {
    // max(column)
    return FunctionExpression(functionName = "max", arguments = listOf(asExpression()), sqlType = LongSqlType)
}

/**
 * SQLite min function, translated to `min(column)`.
 */
public fun <T : Any> Column<T>.min(): FunctionExpression<Long> {
    // min(column)
    return FunctionExpression(functionName = "min", arguments = listOf(asExpression()), sqlType = LongSqlType)
}

/**
 * SQLite sum function, translated to `sum(column)`.
 */
public fun <T : Any> Column<T>.sum(): FunctionExpression<Long> {
    // sum(column)
    return FunctionExpression(functionName = "sum", arguments = listOf(asExpression()), sqlType = LongSqlType)
}

/**
 * SQLite total function, translated to `total(column)`.
 */
public fun <T : Any> Column<T>.total(): FunctionExpression<Long> {
    // total(column)
    return FunctionExpression(functionName = "total", arguments = listOf(asExpression()), sqlType = LongSqlType)
}

// endregion

// region SQLite: Built-In Scalar SQL Functions

/**
 * SQLite random function, translated to `random()`.
 */
public fun random(): FunctionExpression<Long> {
    return FunctionExpression(functionName = "random", arguments = emptyList(), sqlType = LongSqlType)
}

/**
 * SQLite ifnull function, translated to `ifnull(left, right)`.
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
 * SQLite ifnull function, translated to `ifnull(left, right)`.
 */
public fun <T : Any> ColumnDeclaring<T>.ifNull(right: T?): FunctionExpression<T> {
    return this.ifNull(wrapArgument(right))
}

/**
 * SQLite iif function, translated to `iif(condition, then, otherwise)`.
 */
public fun <T : Any> iif(
    condition: ColumnDeclaring<Boolean>,
    then: ColumnDeclaring<T>,
    otherwise: ColumnDeclaring<T>
): FunctionExpression<T> {
    // iif(condition, then, otherwise)
    return FunctionExpression(
        functionName = "iif",
        arguments = listOf(condition, then, otherwise).map { it.asExpression() },
        sqlType = then.sqlType
    )
}

/**
 * SQLite iif function, translated to `iif(condition, then, otherwise)`.
 */
public inline fun <reified T : Any> iif(
    condition: ColumnDeclaring<Boolean>,
    then: T,
    otherwise: T,
    sqlType: SqlType<T> = sqlTypeOf() ?: error("Cannot detect the param's SqlType, please specify manually.")
): FunctionExpression<T> {
    // iif(condition, then, otherwise)
    return FunctionExpression(
        functionName = "iif",
        arguments = listOf(
            condition.asExpression(),
            ArgumentExpression(then, sqlType),
            ArgumentExpression(otherwise, sqlType)
        ),
        sqlType = sqlType
    )
}

/**
 * SQLite instr function, translated to `instr(left, right)`.
 */
public fun ColumnDeclaring<String>.instr(right: ColumnDeclaring<String>): FunctionExpression<Int> {
    // instr(left, right)
    return FunctionExpression(
        functionName = "instr",
        arguments = listOf(this, right).map { it.asExpression() },
        sqlType = IntSqlType
    )
}

/**
 * SQLite instr function, translated to `instr(left, right)`.
 */
public fun ColumnDeclaring<String>.instr(right: String): FunctionExpression<Int> {
    return this.instr(wrapArgument(right))
}

/**
 * SQLite replace function, translated to `replace(str, oldValue, newValue)`.
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
 * SQLite lower function, translated to `lower(str). `
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
 * SQLite upper function, translated to `upper(str). `
 */
public fun ColumnDeclaring<String>.toUpperCase(): FunctionExpression<String> {
    // upper(str)
    return FunctionExpression(
        functionName = "upper",
        arguments = listOf(this.asExpression()),
        sqlType = VarcharSqlType
    )
}

// endregion
