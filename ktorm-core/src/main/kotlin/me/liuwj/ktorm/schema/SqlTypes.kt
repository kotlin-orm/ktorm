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

import java.math.BigDecimal
import java.sql.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import javax.sql.rowset.serial.SerialBlob

/**
 * Define a column typed of [BooleanSqlType].
 */
fun <E : Any> BaseTable<E>.boolean(name: String): BaseTable<E>.ColumnRegistration<Boolean> {
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
fun <E : Any> BaseTable<E>.int(name: String): BaseTable<E>.ColumnRegistration<Int> {
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
fun <E : Any> BaseTable<E>.long(name: String): BaseTable<E>.ColumnRegistration<Long> {
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
fun <E : Any> BaseTable<E>.float(name: String): BaseTable<E>.ColumnRegistration<Float> {
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
fun <E : Any> BaseTable<E>.double(name: String): BaseTable<E>.ColumnRegistration<Double> {
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
fun <E : Any> BaseTable<E>.decimal(name: String): BaseTable<E>.ColumnRegistration<BigDecimal> {
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
fun <E : Any> BaseTable<E>.varchar(name: String): BaseTable<E>.ColumnRegistration<String> {
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
fun <E : Any> BaseTable<E>.text(name: String): BaseTable<E>.ColumnRegistration<String> {
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
fun <E : Any> BaseTable<E>.blob(name: String): BaseTable<E>.ColumnRegistration<ByteArray> {
    return registerColumn(name, BlobSqlType)
}

/**
 * [SqlType] implementation represents `blob` SQL type.
 */
object BlobSqlType : SqlType<ByteArray>(Types.BLOB, "blob") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: ByteArray) {
        ps.setBlob(index, SerialBlob(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): ByteArray? {
        val blob = rs.getBlob(index) ?: return null

        try {
            return blob.binaryStream.use { it.readBytes() }
        } finally {
            blob.free()
        }
    }
}

/**
 * Define a column typed of [BytesSqlType].
 */
fun <E : Any> BaseTable<E>.bytes(name: String): BaseTable<E>.ColumnRegistration<ByteArray> {
    return registerColumn(name, BytesSqlType)
}

/**
 * [SqlType] implementation represents `bytes` SQL type.
 */
object BytesSqlType : SqlType<ByteArray>(Types.BINARY, "bytes") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: ByteArray) {
        ps.setBytes(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): ByteArray? {
        return rs.getBytes(index)
    }
}

/**
 * Define a column typed of [TimestampSqlType].
 */
fun <E : Any> BaseTable<E>.jdbcTimestamp(name: String): BaseTable<E>.ColumnRegistration<Timestamp> {
    return registerColumn(name, TimestampSqlType)
}

/**
 * [SqlType] implementation represents `timestamp` SQL type.
 */
object TimestampSqlType : SqlType<Timestamp>(Types.TIMESTAMP, "timestamp") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Timestamp) {
        ps.setTimestamp(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Timestamp? {
        return rs.getTimestamp(index)
    }
}

/**
 * Define a column typed of [DateSqlType].
 */
fun <E : Any> BaseTable<E>.jdbcDate(name: String): BaseTable<E>.ColumnRegistration<Date> {
    return registerColumn(name, DateSqlType)
}

/**
 * [SqlType] implementation represents `date` SQL type.
 */
object DateSqlType : SqlType<Date>(Types.DATE, "date") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Date) {
        ps.setDate(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Date? {
        return rs.getDate(index)
    }
}

/**
 * Define a column typed of [TimeSqlType].
 */
fun <E : Any> BaseTable<E>.jdbcTime(name: String): BaseTable<E>.ColumnRegistration<Time> {
    return registerColumn(name, TimeSqlType)
}

/**
 * [SqlType] implementation represents `time` SQL type.
 */
object TimeSqlType : SqlType<Time>(Types.TIME, "time") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Time) {
        ps.setTime(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Time? {
        return rs.getTime(index)
    }
}

/**
 * Define a column typed of [InstantSqlType].
 */
fun <E : Any> BaseTable<E>.timestamp(name: String): BaseTable<E>.ColumnRegistration<Instant> {
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

/**
 * Define a column typed of [LocalDateTimeSqlType].
 */
fun <E : Any> BaseTable<E>.datetime(name: String): BaseTable<E>.ColumnRegistration<LocalDateTime> {
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
fun <E : Any> BaseTable<E>.date(name: String): BaseTable<E>.ColumnRegistration<LocalDate> {
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
fun <E : Any> BaseTable<E>.time(name: String): BaseTable<E>.ColumnRegistration<LocalTime> {
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
fun <E : Any> BaseTable<E>.monthDay(name: String): BaseTable<E>.ColumnRegistration<MonthDay> {
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
fun <E : Any> BaseTable<E>.yearMonth(name: String): BaseTable<E>.ColumnRegistration<YearMonth> {
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
fun <E : Any> BaseTable<E>.year(name: String): BaseTable<E>.ColumnRegistration<Year> {
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
 * Define a column typed of [EnumSqlType].
 *
 * @param name the column's name.
 * @param typeRef the generic type information of this column, generally created by [me.liuwj.ktorm.schema.typeRef].
 * @return the column registration that wraps the registered column.
 */
fun <E : Any, C : Enum<C>> BaseTable<E>.enum(
    name: String,
    typeRef: TypeReference<C>
): BaseTable<E>.ColumnRegistration<C> {
    @Suppress("UNCHECKED_CAST")
    return registerColumn(name, EnumSqlType(typeRef.referencedType as Class<C>))
}

/**
 * [SqlType] implementation that saves enums as strings.
 *
 * @property enumClass the enum class.
 */
class EnumSqlType<C : Enum<C>>(val enumClass: Class<C>) : SqlType<C>(Types.VARCHAR, "varchar") {
    private val valueOf = enumClass.getDeclaredMethod("valueOf", String::class.java)

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: C) {
        ps.setString(index, parameter.name)
    }

    override fun doGetResult(rs: ResultSet, index: Int): C? {
        return rs.getString(index)?.takeIf { it.isNotBlank() }?.let { enumClass.cast(valueOf(null, it)) }
    }
}
