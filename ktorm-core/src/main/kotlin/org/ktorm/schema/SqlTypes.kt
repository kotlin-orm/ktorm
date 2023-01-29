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

package org.ktorm.schema

import java.math.BigDecimal
import java.sql.*
import java.sql.Date
import java.time.*
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import java.util.*
import javax.sql.rowset.serial.SerialBlob

/**
 * Define a column typed of [BooleanSqlType].
 */
public fun BaseTable<*>.boolean(name: String): Column<Boolean> {
    return registerColumn(name, BooleanSqlType)
}

/**
 * [SqlType] implementation represents `boolean` SQL type.
 */
public object BooleanSqlType : SqlType<Boolean>(Types.BOOLEAN, "boolean") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Boolean) {
        ps.setBoolean(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Boolean {
        return rs.getBoolean(index)
    }
}

/**
 * Define a column typed of [IntSqlType].
 */
public fun BaseTable<*>.int(name: String): Column<Int> {
    return registerColumn(name, IntSqlType)
}

/**
 * [SqlType] implementation represents `int` SQL type.
 */
public object IntSqlType : SqlType<Int>(Types.INTEGER, "int") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Int) {
        ps.setInt(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Int {
        return rs.getInt(index)
    }
}

/**
 * Define a column typed of [ShortSqlType].
 *
 * @since 3.1.0
 */
public fun BaseTable<*>.short(name: String): Column<Short> {
    return registerColumn(name, ShortSqlType)
}

/**
 * [SqlType] implementation represents `smallint` SQL type.
 *
 * @since 3.1.0
 */
public object ShortSqlType : SqlType<Short>(Types.SMALLINT, "smallint") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Short) {
        ps.setShort(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Short {
        return rs.getShort(index)
    }
}

/**
 * Define a column typed of [LongSqlType].
 */
public fun BaseTable<*>.long(name: String): Column<Long> {
    return registerColumn(name, LongSqlType)
}

/**
 * [SqlType] implementation represents `long` SQL type.
 */
public object LongSqlType : SqlType<Long>(Types.BIGINT, "bigint") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Long) {
        ps.setLong(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Long {
        return rs.getLong(index)
    }
}

/**
 * Define a column typed of [FloatSqlType].
 */
public fun BaseTable<*>.float(name: String): Column<Float> {
    return registerColumn(name, FloatSqlType)
}

/**
 * [SqlType] implementation represents `float` SQL type.
 */
public object FloatSqlType : SqlType<Float>(Types.FLOAT, "float") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Float) {
        ps.setFloat(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Float {
        return rs.getFloat(index)
    }
}

/**
 * Define a column typed of [DoubleSqlType].
 */
public fun BaseTable<*>.double(name: String): Column<Double> {
    return registerColumn(name, DoubleSqlType)
}

/**
 * [SqlType] implementation represents `double` SQL type.
 */
public object DoubleSqlType : SqlType<Double>(Types.DOUBLE, "double") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Double) {
        ps.setDouble(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Double {
        return rs.getDouble(index)
    }
}

/**
 * Define a column typed of [DecimalSqlType].
 */
public fun BaseTable<*>.decimal(name: String): Column<BigDecimal> {
    return registerColumn(name, DecimalSqlType)
}

/**
 * [SqlType] implementation represents `decimal` SQL type.
 */
public object DecimalSqlType : SqlType<BigDecimal>(Types.DECIMAL, "decimal") {

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
public fun BaseTable<*>.varchar(name: String): Column<String> {
    return registerColumn(name, VarcharSqlType)
}

/**
 * [SqlType] implementation represents `varchar` SQL type.
 */
public object VarcharSqlType : SqlType<String>(Types.VARCHAR, "varchar") {

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
public fun BaseTable<*>.text(name: String): Column<String> {
    return registerColumn(name, TextSqlType)
}

/**
 * [SqlType] implementation represents `text` SQL type.
 */
public object TextSqlType : SqlType<String>(Types.LONGVARCHAR, "text") {

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
public fun BaseTable<*>.blob(name: String): Column<ByteArray> {
    return registerColumn(name, BlobSqlType)
}

/**
 * [SqlType] implementation represents `blob` SQL type.
 */
public object BlobSqlType : SqlType<ByteArray>(Types.BLOB, "blob") {

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
public fun BaseTable<*>.bytes(name: String): Column<ByteArray> {
    return registerColumn(name, BytesSqlType)
}

/**
 * [SqlType] implementation represents `bytes` SQL type.
 */
public object BytesSqlType : SqlType<ByteArray>(Types.BINARY, "bytes") {

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
public fun BaseTable<*>.jdbcTimestamp(name: String): Column<Timestamp> {
    return registerColumn(name, TimestampSqlType)
}

/**
 * [SqlType] implementation represents `timestamp` SQL type.
 */
public object TimestampSqlType : SqlType<Timestamp>(Types.TIMESTAMP, "timestamp") {

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
public fun BaseTable<*>.jdbcDate(name: String): Column<Date> {
    return registerColumn(name, DateSqlType)
}

/**
 * [SqlType] implementation represents `date` SQL type.
 */
public object DateSqlType : SqlType<Date>(Types.DATE, "date") {

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
public fun BaseTable<*>.jdbcTime(name: String): Column<Time> {
    return registerColumn(name, TimeSqlType)
}

/**
 * [SqlType] implementation represents `time` SQL type.
 */
public object TimeSqlType : SqlType<Time>(Types.TIME, "time") {

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
public fun BaseTable<*>.timestamp(name: String): Column<Instant> {
    return registerColumn(name, InstantSqlType)
}

/**
 * [SqlType] implementation represents `timestamp` SQL type.
 */
public object InstantSqlType : SqlType<Instant>(Types.TIMESTAMP, "timestamp") {

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
public fun BaseTable<*>.datetime(name: String): Column<LocalDateTime> {
    return registerColumn(name, LocalDateTimeSqlType)
}

/**
 * [SqlType] implementation represents `datetime` SQL type.
 */
public object LocalDateTimeSqlType : SqlType<LocalDateTime>(Types.TIMESTAMP, "datetime") {

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
public fun BaseTable<*>.date(name: String): Column<LocalDate> {
    return registerColumn(name, LocalDateSqlType)
}

/**
 * [SqlType] implementation represents `date` SQL type.
 */
public object LocalDateSqlType : SqlType<LocalDate>(Types.DATE, "date") {

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
public fun BaseTable<*>.time(name: String): Column<LocalTime> {
    return registerColumn(name, LocalTimeSqlType)
}

/**
 * [SqlType] implementation represents `time` SQL type.
 */
public object LocalTimeSqlType : SqlType<LocalTime>(Types.TIME, "time") {

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
public fun BaseTable<*>.monthDay(name: String): Column<MonthDay> {
    return registerColumn(name, MonthDaySqlType)
}

/**
 * [SqlType] implementation used to save [MonthDay] instances, formatting them to strings with pattern `MM-dd`.
 */
public object MonthDaySqlType : SqlType<MonthDay>(Types.VARCHAR, "varchar") {
    private val formatter = DateTimeFormatterBuilder()
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
public fun BaseTable<*>.yearMonth(name: String): Column<YearMonth> {
    return registerColumn(name, YearMonthSqlType)
}

/**
 * [SqlType] implementation used to save [YearMonth] instances, formatting them to strings with pattern `yyyy-MM`.
 */
@Suppress("MagicNumber")
public object YearMonthSqlType : SqlType<YearMonth>(Types.VARCHAR, "varchar") {
    private val formatter = DateTimeFormatterBuilder()
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
public fun BaseTable<*>.year(name: String): Column<Year> {
    return registerColumn(name, YearSqlType)
}

/**
 * [SqlType] implementation used to save [Year] instances as integers.
 */
public object YearSqlType : SqlType<Year>(Types.INTEGER, "int") {

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
 * @return the registered column.
 */
public inline fun <reified C : Enum<C>> BaseTable<*>.enum(name: String): Column<C> {
    return registerColumn(name, EnumSqlType(C::class.java))
}

/**
 * [SqlType] implementation that saves enums as strings.
 *
 * @property enumClass the enum class.
 */
public class EnumSqlType<C : Enum<C>>(public val enumClass: Class<C>) : SqlType<C>(Types.OTHER, "enum") {
    private val pgStatementClass =
        try { Class.forName("org.postgresql.PGStatement") } catch (_: ClassNotFoundException) { null }

    override fun setParameter(ps: PreparedStatement, index: Int, parameter: C?) {
        if (parameter != null) {
            doSetParameter(ps, index, parameter)
        } else {
            if (pgStatementClass != null && ps.isWrapperFor(pgStatementClass)) {
                ps.setNull(index, Types.OTHER)
            } else {
                ps.setNull(index, Types.VARCHAR)
            }
        }
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: C) {
        if (pgStatementClass != null && ps.isWrapperFor(pgStatementClass)) {
            ps.setObject(index, parameter.name, Types.OTHER)
        } else {
            ps.setString(index, parameter.name)
        }
    }

    override fun doGetResult(rs: ResultSet, index: Int): C? {
        val value = rs.getString(index)
        if (value.isNullOrBlank()) {
            return null
        } else {
            return java.lang.Enum.valueOf(enumClass, value)
        }
    }
}

/**
 * Define a column typed of [UuidSqlType].
 */
public fun BaseTable<*>.uuid(name: String): Column<UUID> {
    return registerColumn(name, UuidSqlType)
}

/**
 * [SqlType] implementation represents `uuid` SQL type.
 */
public object UuidSqlType : SqlType<UUID>(Types.OTHER, "uuid") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: UUID) {
        ps.setObject(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): UUID? {
        return rs.getObject(index) as UUID?
    }
}
