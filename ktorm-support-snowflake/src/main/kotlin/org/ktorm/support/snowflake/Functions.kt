/*
 * Copyright 2025 the original author or authors.
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
 *
 * Original authors of the snowflake dialect were CarGurus: Don Mitchell, Ashish Shrestha, Mike Roberts, and others.
 */
package org.ktorm.support.snowflake

import org.ktorm.expression.AggregateExpression
import org.ktorm.expression.AggregateType
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.ColumnExpression
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.DoubleSqlType
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.LocalDateSqlType
import org.ktorm.schema.LocalDateTimeSqlType
import org.ktorm.schema.LongSqlType
import org.ktorm.schema.SqlType
import org.ktorm.schema.VarcharSqlType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

public enum class DatePart(public val sql: String) {
    SECOND("SECOND"),
    DAY("DAY"),
    MONTH("MONTH"),
    YEAR("YEAR"),
    WEEK("WEEK"),
    QUARTER("QUARTER")
}

public enum class TimePart(public val sql: String) {
    HOUR("HOUR"),
    MINUTE("MINUTE"),
    SECOND("SECOND"),
    MILLISECOND("MS"),
    MICROSECOND("US"),
    NANOSECOND("NS")
}

// FIXME make it more generic than LocalDateTime
/**
 * Sql function to convert a timestamp to a date.
 *
 * @return The function expression representing the to_date sql function.
 */
public fun ColumnDeclaring<LocalDateTime>.toDate(): FunctionExpression<LocalDate> {
    return FunctionExpression(
        functionName = "to_date",
        arguments = listOf(this.asExpression()),
        sqlType = LocalDateSqlType
    )
}

@JvmName("stringToDate")
public fun ColumnDeclaring<String>.toDate(): FunctionExpression<LocalDate> {
    return FunctionExpression(
        functionName = "to_date",
        arguments = listOf(this.asExpression()),
        sqlType = LocalDateSqlType
    )
}

private const val DATE_TRUNC = "date_trunc"

// TODO make it more generic than LocalDateTime
/**
 * Sql function to convert a truncate a date or timestamp to a particular day unit.
 *
 * @param part The date part to truncate to.
 *
 * @return The function expression representing the date_trunc sql function.
 */
public fun ColumnDeclaring<LocalDateTime>.dateTrunc(part: DatePart): FunctionExpression<LocalDate> {
    return FunctionExpression(
        functionName = DATE_TRUNC,
        arguments = listOf(
            ArgumentExpression(part.sql, VarcharSqlType),
            this.asExpression()
        ),
        sqlType = LocalDateSqlType
    )
}

/**
 * Sql function to convert a truncate a date or timestamp to a particular day unit.
 *
 * @param part The date part to truncate to.
 *
 * @return The function expression representing the date_trunc sql function.
 */
@JvmName("dateTruncZonedDateTime")
public fun ColumnDeclaring<ZonedDateTime>.dateTrunc(part: DatePart): FunctionExpression<LocalDate> {
    return FunctionExpression(
        functionName = DATE_TRUNC,
        arguments = listOf(
            ArgumentExpression(part.sql, VarcharSqlType),
            this.asExpression()
        ),
        sqlType = LocalDateSqlType
    )
}

/**
 * Sql function to convert a truncate a date or timestamp to a particular time unit.
 *
 * @param part The time part to truncate to.
 *
 * @return The function expression representing the date_trunc sql function.
 */
public fun ColumnDeclaring<LocalDateTime>.dateTrunc(part: TimePart): FunctionExpression<LocalDateTime> {
    return FunctionExpression(
        functionName = DATE_TRUNC,
        arguments = listOf(
            ArgumentExpression(part.sql, VarcharSqlType),
            this.asExpression()
        ),
        sqlType = LocalDateTimeSqlType
    )
}

@JvmName("hourFromZonedDateTime")
public fun ColumnDeclaring<ZonedDateTime>.hour(): FunctionExpression<Int> {
    return FunctionExpression(
        functionName = "HOUR",
        arguments = listOf(
            this.asExpression()
        ),
        sqlType = IntSqlType
    )
}

@JvmName("hourFromLocalDateTime")
public fun ColumnDeclaring<LocalDateTime>.hour(): FunctionExpression<Int> {
    return FunctionExpression(
        functionName = "HOUR",
        arguments = listOf(
            this.asExpression()
        ),
        sqlType = IntSqlType
    )
}

@JvmName("hourFromString")
public fun ColumnDeclaring<String>.hour(): FunctionExpression<Int> {
    return FunctionExpression(
        functionName = "HOUR",
        arguments = listOf(
            this.asExpression()
        ),
        sqlType = IntSqlType
    )
}

@JvmName("zonedDateTimeDiff")
public fun ColumnDeclaring<ZonedDateTime>.dateDiff(part: TimePart, end: ColumnDeclaring<ZonedDateTime>):
    FunctionExpression<Int> {
    return FunctionExpression(
        functionName = "DATEDIFF",
        arguments = listOf(
            ArgumentExpression(part.sql, VarcharSqlType),
            this.asExpression(),
            end.asExpression()
        ),
        sqlType = IntSqlType
    )
}

@JvmName("localDateTimeDiff")
public fun ColumnDeclaring<LocalDateTime>.dateDiff(part: TimePart, end: ColumnDeclaring<LocalDateTime>):
    FunctionExpression<Int> {
    return FunctionExpression(
        functionName = "DATEDIFF",
        arguments = listOf(
            ArgumentExpression(part.sql, VarcharSqlType),
            this.asExpression(),
            end.asExpression()
        ),
        sqlType = IntSqlType
    )
}

/**
 * Sql function to add a unit of time to a LocalDate.
 *
 * @param part The time part to add.
 * @param value The amount of timeparts to add to the date
 *
 * @return The function expression representing the dateadd sql function.
 */
public fun ColumnDeclaring<LocalDate>.dateAdd(part: DatePart, value: Int): FunctionExpression<LocalDate> {
    return FunctionExpression(
        functionName = "dateadd",
        arguments = listOf(
            ArgumentExpression(part.sql, VarcharSqlType),
            ArgumentExpression(value, IntSqlType),
            this.asExpression()
        ),
        sqlType = LocalDateSqlType
    )
}

/**
 * Sql function to add a unit of time to a [LocalDateTime].
 *
 * @param part The time part to add.
 * @param value The amount of timeparts to add to the date
 *
 * @return The function expression representing the dateadd sql function.
 */
public fun ColumnDeclaring<ZonedDateTime>.dateTimeAdd(part: DatePart, value: Int): FunctionExpression<LocalDateTime> {
    return FunctionExpression(
        functionName = "dateadd",
        arguments = listOf(
            ArgumentExpression(part.sql, VarcharSqlType),
            ArgumentExpression(value, IntSqlType),
            this.asExpression()
        ),
        sqlType = LocalDateTimeSqlType
    )
}

/**
 * Sql function to decode a value into other values.
 *
 * @param T The class of the input values.
 * @param U The class of the output values.
 * @param decodings The mapping of input values to output values.
 * @param default The default output value to use if the column value does not match any of the input value mappings.
 * @param outputSqlType The sql type of the output.
 *
 * @return The function expression representing the decode sql function.
 */
public fun <T : Any, U : Any> ColumnDeclaring<T>.decode(
    decodings: Map<T, U>,
    default: U?,
    outputSqlType: SqlType<U>
): FunctionExpression<U> {
    val column = this
    var arguments = decodings.map { (input, output) ->
        listOf<ArgumentExpression<*>>(
            ArgumentExpression(input, column.sqlType),
            ArgumentExpression(output, outputSqlType)
        )
    }.flatten()

    default?.let { arguments += ArgumentExpression(default, outputSqlType) }

    return FunctionExpression(
        "decode",
        listOf(this.asExpression()) + arguments,
        outputSqlType
    )
}

/**
 * Sql function to decode a value into other values.
 *
 * @param T The class of the input values.
 * @param U The class of the output values.
 * @param decodings The mapping of input values to output values.
 * @param outputSqlType The sql type of the output.
 *
 * @return The function expression representing the decode sql function.
 */
public fun <T : Any, U : Any> ColumnDeclaring<T>.decode(
    decodings: Map<T, U>,
    outputSqlType: SqlType<U>
): FunctionExpression<U> {
    return this.decode(decodings, null, outputSqlType)
}

/**
 * Sql function to provide a default for a column value if it is null.
 *
 * @param T The class of the column values.
 * @param default The default output value.
 *
 * @return The function expression representing the decode sql function.
 */
public fun <T : Any> ColumnDeclaring<T>.nvl(default: T): FunctionExpression<T> {
    val column = this
    val argument = ArgumentExpression(default, column.sqlType)

    return FunctionExpression("nvl", listOf(column.asExpression(), argument), column.sqlType)
}

/**
 * Strips the table from a column expression, for situations in which the table context can interfere with the
 * SQL generation logic.
 * @return The column expression without a table.
 **/
public fun <T : Any> ColumnExpression<T>.stripTable(): ColumnExpression<T> = this.copy(table = null)

/**
 * Sql function to provide a default for a column value if it is null. TO DO: be able to allow any scalar expression
 * as a default.
 *
 * @param T The class of the column values.
 * @param default The other column to use as a default value
 *
 * @return The function expression representing the decode sql function.
 */
public fun <T : Any> ColumnDeclaring<T>.nvl(default: ColumnDeclaring<T>): FunctionExpression<T> {
    val column = this

    return FunctionExpression("nvl", listOf(column.asExpression(), default.asExpression()), column.sqlType)
}

/**
 * Sql function to return a 64-bit hash (which is not the same as the xxhash algorithm used for ad extra hashing in
 * SiteVisitorClicks).
 *
 * @return The function expression representing the hash sql function.
 */
public fun ColumnDeclaring<String>.hash(): FunctionExpression<Long> {
    return FunctionExpression("hash", listOf(this.asExpression()), LongSqlType)
}

/**
 * Sql function to return SHA2 hash.
 *
 * Default is a 256-bit hash, but Snowflake supports 224-, 256-, 384-, and 512-bit digest sizes.
 *
 * @return The function expression representing the sha2 sql function.
 */
public fun ColumnDeclaring<String>.sha2(digestSize: Int = 256): FunctionExpression<String> {
    require(digestSize in supportedSha2DigestSizes) {
        "$digestSize is not one of these Snowflake supported SHA2 digest sizes: " +
                supportedSha2DigestSizes.joinToString()
    }
    return FunctionExpression(
        "sha2",
        listOf(this.asExpression(), ArgumentExpression(digestSize, IntSqlType)),
        VarcharSqlType
    )
}

private val supportedSha2DigestSizes = setOf(224, 256, 384, 512)

/**
 * Sql function to return the absolute value of a number.
 *
 * @return The function expression representing abs hash sql function.
 */
public fun <T : Number> ColumnDeclaring<T>.abs(): FunctionExpression<T> {
    return FunctionExpression("abs", listOf(this.asExpression()), this.sqlType)
}

/**
 * Sql function to return the absolute value of a number.
 *
 * @return The function expression representing abs hash sql function.
 */
public fun <T : Number> ColumnDeclaring<T>.floor(): FunctionExpression<T> {
    return FunctionExpression("floor", listOf(this.asExpression()), this.sqlType)
}

/**
 * Sql function to access the key value of an object or variant.
 *
 * @param T The class of the source column, which should represent an object or variant in snowflake.
 * @param V The class of the attribute value.
 * @param key The key to access.
 * @param outputSqlType The sql type of the result, which should be a sql type of V
 *
 * @return The function expression representing the sql function that is the access of an object at a particular key
 * value.
 */
public fun <T : Any, V : Any> ColumnDeclaring<T>.get(
    key: String,
    outputSqlType: SqlType<V>
): FunctionExpression<V> = FunctionExpression(
    "get", listOf(this.asExpression(), ArgumentExpression(key, VarcharSqlType)), outputSqlType
)

/**
 * Sql function to convert the timezone of a date to a different region.
 *
 * @param region The IANA strings used for timezones, see https://nodatime.org/TimeZones
 *
 * @return The function expression representing abs hash sql function.
 */
public fun ColumnDeclaring<ZonedDateTime>.convertTimezone(region: ColumnDeclaring<String>): FunctionExpression<String> {
    return FunctionExpression(
        "convert_timezone",
        listOf(region.asExpression(), this.asExpression()),
        VarcharSqlType
    )
}

public fun ColumnDeclaring<ZonedDateTime>.convertTimezone(targetTz: String): FunctionExpression<String> {
    return FunctionExpression(
        functionName = "convert_timezone",
        arguments = listOf(
            ArgumentExpression(targetTz, VarcharSqlType),
            this.asExpression()
        ),
        VarcharSqlType
    )
}

public fun iff(
    condition: ColumnDeclaring<Boolean>,
    then: String,
    otherwise: String
): FunctionExpression<String> {
    return FunctionExpression(
        functionName = "iff",
        arguments = listOf(
            condition.asExpression(),
            ArgumentExpression(then, VarcharSqlType),
            ArgumentExpression(otherwise, VarcharSqlType)
        ),
        sqlType = VarcharSqlType
    )
}

/**
 * Sql function `concat(s1, s2, ...)`.
 *
 * @param values List of columns or string values
 * @return the function representing `concat` of the arguments
 */
public fun concat(vararg values: Any): FunctionExpression<String> {
    val arguments = values.map {
        when (it) {
            is String -> ArgumentExpression(it, VarcharSqlType)
            is Char -> ArgumentExpression(it.toString(), VarcharSqlType)
            is ColumnDeclaring<*> -> it.asExpression()
            else -> error("Unsupported argument for concat $it")
        }
    }
    return FunctionExpression("concat", arguments, VarcharSqlType)
}

/**
 * Sql function to_char(val).
 *
 * @param value Column or integer/long/double
 * @return the function representing the conversion of numerical field to string.
 */
public fun toChar(value: Any): FunctionExpression<String> {
    val argument = when (value) {
        is Long -> ArgumentExpression(value, LongSqlType)
        is Double -> ArgumentExpression(value, DoubleSqlType)
        is Int -> ArgumentExpression(value, IntSqlType)
        is ColumnDeclaring<*> -> value.asExpression()
        else -> error("Unsupported argument for toChar $value")
    }
    return FunctionExpression("to_char", listOf(argument), VarcharSqlType)
}

/**
 * Sql function length(val).
 *
 * @param value String column
 * @return the function representing the of characters in the string.git
 */
public fun length(value: ColumnDeclaring<String>): FunctionExpression<Int> =
    FunctionExpression("length", listOf(value.asExpression()), IntSqlType)

/**
 * Sql function count(val)
 * The default ktorm implementation is int only
 */
public fun countLong(column: ColumnDeclaring<*>? = null): AggregateExpression<Long> {
    return AggregateExpression(AggregateType.COUNT, column?.asExpression(), false, LongSqlType)
}
