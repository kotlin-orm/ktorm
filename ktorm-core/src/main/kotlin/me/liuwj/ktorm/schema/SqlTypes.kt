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

package me.liuwj.ktorm.schema

import me.liuwj.ktorm.entity.Entity
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.sql.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField

/**
 * Define a column typed of [BooleanSqlType].
 */
fun <E : Entity<E>> Table<E>.boolean(name: String): Table<E>.ColumnRegistration<Boolean> {
    return registerColumn(name, BooleanSqlType)
}

/**
 * [SqlType] implementation represents `boolean` SQL type.
 */
object BooleanSqlType : SqlType<Boolean>(Types.BOOLEAN, "boolean") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Boolean) {
        ps.setBoolean(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Boolean? {
        return rs.getBoolean(index)
    }
}

/**
 * Define a column typed of [IntSqlType].
 */
fun <E : Entity<E>> Table<E>.int(name: String): Table<E>.ColumnRegistration<Int> {
    return registerColumn(name, IntSqlType)
}

/**
 * [SqlType] implementation represents `int` SQL type.
 */
object IntSqlType : SqlType<Int>(Types.INTEGER, "int") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Int) {
        ps.setInt(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Int? {
        return rs.getInt(index)
    }
}

/**
 * Define a column typed of [LongSqlType].
 */
fun <E : Entity<E>> Table<E>.long(name: String): Table<E>.ColumnRegistration<Long> {
    return registerColumn(name, LongSqlType)
}

/**
 * [SqlType] implementation represents `long` SQL type.
 */
object LongSqlType : SqlType<Long>(Types.BIGINT, "bigint") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Long) {
        ps.setLong(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Long? {
        return rs.getLong(index)
    }
}

/**
 * Define a column typed of [FloatSqlType].
 */
fun <E : Entity<E>> Table<E>.float(name: String): Table<E>.ColumnRegistration<Float> {
    return registerColumn(name, FloatSqlType)
}

/**
 * [SqlType] implementation represents `float` SQL type.
 */
object FloatSqlType : SqlType<Float>(Types.FLOAT, "float") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Float) {
        ps.setFloat(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Float? {
        return rs.getFloat(index)
    }
}

/**
 * Define a column typed of [DoubleSqlType].
 */
fun <E : Entity<E>> Table<E>.double(name: String): Table<E>.ColumnRegistration<Double> {
    return registerColumn(name, DoubleSqlType)
}

/**
 * [SqlType] implementation represents `double` SQL type.
 */
object DoubleSqlType : SqlType<Double>(Types.DOUBLE, "double") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Double) {
        ps.setDouble(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Double? {
        return rs.getDouble(index)
    }
}

/**
 * Define a column typed of [DecimalSqlType].
 */
fun <E : Entity<E>> Table<E>.decimal(name: String): Table<E>.ColumnRegistration<BigDecimal> {
    return registerColumn(name, DecimalSqlType)
}

/**
 * [SqlType] implementation represents `decimal` SQL type.
 */
object DecimalSqlType : SqlType<BigDecimal>(Types.DECIMAL, "decimal") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: BigDecimal) {
        ps.setBigDecimal(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): BigDecimal? {
        return rs.getBigDecimal(index)
    }
}

/**
 * Define a column typed of [VarcharSqlType].
 */
fun <E : Entity<E>> Table<E>.varchar(name: String): Table<E>.ColumnRegistration<String> {
    return registerColumn(name, VarcharSqlType)
}

/**
 * [SqlType] implementation represents `varchar` SQL type.
 */
object VarcharSqlType : SqlType<String>(Types.VARCHAR, "varchar") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: String) {
        ps.setString(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): String? {
        return rs.getString(index)
    }
}

/**
 * Define a column typed of [TextSqlType].
 */
fun <E : Entity<E>> Table<E>.text(name: String): Table<E>.ColumnRegistration<String> {
    return registerColumn(name, TextSqlType)
}

/**
 * [SqlType] implementation represents `text` SQL type.
 */
object TextSqlType : SqlType<String>(Types.LONGVARCHAR, "text") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: String) {
        ps.setString(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): String? {
        return rs.getString(index)
    }
}

/**
 * Define a column typed of [BlobSqlType].
 */
fun <E : Entity<E>> Table<E>.blob(name: String): Table<E>.ColumnRegistration<ByteArray> {
    return registerColumn(name, BlobSqlType)
}

/**
 * [SqlType] implementation represents `blob` SQL type.
 */
object BlobSqlType : SqlType<ByteArray>(Types.BLOB, "blob") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: ByteArray) {
        ByteArrayInputStream(parameter).use { ps.setBlob(index, it) }
    }

    override fun doGetResult(rs: ResultSet, index: Int): ByteArray? {
        return rs.getBlob(index)?.binaryStream?.use { it.readBytes() }
    }
}

/**
 * Define a column typed of [LocalDateTimeSqlType].
 */
fun <E : Entity<E>> Table<E>.datetime(name: String): Table<E>.ColumnRegistration<LocalDateTime> {
    return registerColumn(name, LocalDateTimeSqlType)
}

/**
 * [SqlType] implementation represents `datetime` SQL type.
 */
object LocalDateTimeSqlType : SqlType<LocalDateTime>(Types.TIMESTAMP, "datetime") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: LocalDateTime) {
        ps.setTimestamp(index, Timestamp.valueOf(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): LocalDateTime? {
        return rs.getTimestamp(index)?.toLocalDateTime()
    }
}

/**
 * Define a column typed of [LocalDateSqlType].
 */
fun <E : Entity<E>> Table<E>.date(name: String): Table<E>.ColumnRegistration<LocalDate> {
    return registerColumn(name, LocalDateSqlType)
}

/**
 * [SqlType] implementation represents `date` SQL type.
 */
object LocalDateSqlType : SqlType<LocalDate>(Types.DATE, "date") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: LocalDate) {
        ps.setDate(index, Date.valueOf(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): LocalDate? {
        return rs.getDate(index)?.toLocalDate()
    }
}

/**
 * Define a column typed of [LocalTimeSqlType].
 */
fun <E : Entity<E>> Table<E>.time(name: String): Table<E>.ColumnRegistration<LocalTime> {
    return registerColumn(name, LocalTimeSqlType)
}

/**
 * [SqlType] implementation represents `time` SQL type.
 */
object LocalTimeSqlType : SqlType<LocalTime>(Types.TIME, "time") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: LocalTime) {
        ps.setTime(index, Time.valueOf(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): LocalTime? {
        return rs.getTime(index)?.toLocalTime()
    }
}

/**
 * Define a column typed of [MonthDaySqlType], instances of [MonthDay] are saved as strings in format `MM-dd`.
 */
fun <E : Entity<E>> Table<E>.monthDay(name: String): Table<E>.ColumnRegistration<MonthDay> {
    return registerColumn(name, MonthDaySqlType)
}

/**
 * [SqlType] implementation used to save [MonthDay] instances, formating them to strings with pattern `MM-dd`.
 */
object MonthDaySqlType : SqlType<MonthDay>(Types.VARCHAR, "varchar") {
    val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .toFormatter()

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: MonthDay) {
        ps.setString(index, parameter.format(formatter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): MonthDay? {
        return rs.getString(index)?.let { MonthDay.parse(it, formatter) }
    }
}

/**
 * Define a column typed of [YearMonthSqlType], instances of [YearMonth] are saved as strings in format `yyyy-MM`.
 */
fun <E : Entity<E>> Table<E>.yearMonth(name: String): Table<E>.ColumnRegistration<YearMonth> {
    return registerColumn(name, YearMonthSqlType)
}

/**
 * [SqlType] implementation used to save [YearMonth] instances, formating them to strings with pattern `yyyy-MM`.
 */
@Suppress("MagicNumber")
object YearMonthSqlType : SqlType<YearMonth>(Types.VARCHAR, "varchar") {
    val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .toFormatter()

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: YearMonth) {
        ps.setString(index, parameter.format(formatter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): YearMonth? {
        return rs.getString(index)?.let { YearMonth.parse(it, formatter) }
    }
}

/**
 * Define a column typed of [YearSqlType], instances of [Year] are saved as integers.
 */
fun <E : Entity<E>> Table<E>.year(name: String): Table<E>.ColumnRegistration<Year> {
    return registerColumn(name, YearSqlType)
}

/**
 * [SqlType] implementation used to save [Year] instances as integers.
 */
object YearSqlType : SqlType<Year>(Types.INTEGER, "int") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Year) {
        ps.setInt(index, parameter.value)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Year? {
        return Year.of(rs.getInt(index))
    }
}

/**
 * Define a column typed of [InstantSqlType].
 */
fun <E : Entity<E>> Table<E>.timestamp(name: String): Table<E>.ColumnRegistration<Instant> {
    return registerColumn(name, InstantSqlType)
}

/**
 * [SqlType] implementation represents `timestamp` SQL type.
 */
object InstantSqlType : SqlType<Instant>(Types.TIMESTAMP, "timestamp") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Instant) {
        ps.setTimestamp(index, Timestamp.from(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): Instant? {
        return rs.getTimestamp(index)?.toInstant()
    }
}
