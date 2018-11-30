/**
 * 该文件提供 Table 类的扩展函数，以支持往表中注册各种不同类型的字段，该文件中的所有函数均直接调用 Table.registerColumn 方法
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

fun <E : Entity<E>> Table<E>.boolean(name: String): Table<E>.ColumnRegistration<Boolean> {
    return registerColumn(name, BooleanSqlType)
}

object BooleanSqlType : AbstractSqlType<Boolean>(Types.BOOLEAN, "tinyint") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: Boolean) {
        ps.setBoolean(index, parameter)
    }

    override fun getNullableResult(rs: ResultSet, index: Int): Boolean? {
        return rs.getBoolean(index)
    }
}

fun <E : Entity<E>> Table<E>.int(name: String): Table<E>.ColumnRegistration<Int> {
    return registerColumn(name, IntSqlType)
}

object IntSqlType : AbstractSqlType<Int>(Types.INTEGER, "int") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: Int) {
        ps.setInt(index, parameter)
    }

    override fun getNullableResult(rs: ResultSet, index: Int): Int? {
        return rs.getInt(index)
    }
}

fun <E : Entity<E>> Table<E>.long(name: String): Table<E>.ColumnRegistration<Long> {
    return registerColumn(name, LongSqlType)
}

object LongSqlType : AbstractSqlType<Long>(Types.BIGINT, "bigint") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: Long) {
        ps.setLong(index, parameter)
    }

    override fun getNullableResult(rs: ResultSet, index: Int): Long? {
        return rs.getLong(index)
    }
}

fun <E : Entity<E>> Table<E>.float(name: String): Table<E>.ColumnRegistration<Float> {
    return registerColumn(name, FloatSqlType)
}

object FloatSqlType : AbstractSqlType<Float>(Types.FLOAT, "float") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: Float) {
        ps.setFloat(index, parameter)
    }

    override fun getNullableResult(rs: ResultSet, index: Int): Float? {
        return rs.getFloat(index)
    }
}

fun <E : Entity<E>> Table<E>.double(name: String): Table<E>.ColumnRegistration<Double> {
    return registerColumn(name, DoubleSqlType)
}

object DoubleSqlType : AbstractSqlType<Double>(Types.DOUBLE, "double") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: Double) {
        ps.setDouble(index, parameter)
    }

    override fun getNullableResult(rs: ResultSet, index: Int): Double? {
        return rs.getDouble(index)
    }
}

fun <E : Entity<E>> Table<E>.decimal(name: String): Table<E>.ColumnRegistration<BigDecimal> {
    return registerColumn(name, DecimalSqlType)
}

object DecimalSqlType : AbstractSqlType<BigDecimal>(Types.DECIMAL, "decimal") {

    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: BigDecimal) {
        ps.setBigDecimal(index, parameter)
    }

    override fun getNullableResult(rs: ResultSet, index: Int): BigDecimal? {
        return rs.getBigDecimal(index)
    }
}

fun <E : Entity<E>> Table<E>.varchar(name: String): Table<E>.ColumnRegistration<String> {
    return registerColumn(name, VarcharSqlType)
}

object VarcharSqlType : AbstractSqlType<String>(Types.VARCHAR, "varchar") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: String) {
        ps.setString(index, parameter)
    }

    override fun getNullableResult(rs: ResultSet, index: Int): String? {
        return rs.getString(index)
    }
}

fun <E : Entity<E>> Table<E>.text(name: String): Table<E>.ColumnRegistration<String> {
    return registerColumn(name, TextSqlType)
}

object TextSqlType : AbstractSqlType<String>(Types.LONGVARCHAR, "text") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: String) {
        ps.setString(index, parameter)
    }

    override fun getNullableResult(rs: ResultSet, index: Int): String? {
        return rs.getString(index)
    }
}

fun <E : Entity<E>> Table<E>.blob(name: String): Table<E>.ColumnRegistration<ByteArray> {
    return registerColumn(name, BlobSqlType)
}

object BlobSqlType : AbstractSqlType<ByteArray>(Types.BLOB, "blob") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: ByteArray) {
        ByteArrayInputStream(parameter).use { ps.setBlob(index, it) }
    }

    override fun getNullableResult(rs: ResultSet, index: Int): ByteArray? {
        return rs.getBlob(index)?.binaryStream?.use { it.readBytes() }
    }
}

fun <E : Entity<E>> Table<E>.datetime(name: String): Table<E>.ColumnRegistration<LocalDateTime> {
    return registerColumn(name, LocalDateTimeSqlType)
}

object LocalDateTimeSqlType : AbstractSqlType<LocalDateTime>(Types.TIMESTAMP, "datetime") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: LocalDateTime) {
        ps.setTimestamp(index, Timestamp.valueOf(parameter))
    }

    override fun getNullableResult(rs: ResultSet, index: Int): LocalDateTime? {
        return rs.getTimestamp(index)?.toLocalDateTime()
    }
}

fun <E : Entity<E>> Table<E>.date(name: String): Table<E>.ColumnRegistration<LocalDate> {
    return registerColumn(name, LocalDateSqlType)
}

object LocalDateSqlType : AbstractSqlType<LocalDate>(Types.DATE, "date") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: LocalDate) {
        ps.setDate(index, Date.valueOf(parameter))
    }

    override fun getNullableResult(rs: ResultSet, index: Int): LocalDate? {
        return rs.getDate(index)?.toLocalDate()
    }
}

fun <E : Entity<E>> Table<E>.time(name: String): Table<E>.ColumnRegistration<LocalTime> {
    return registerColumn(name, LocalTimeSqlType)
}

object LocalTimeSqlType : AbstractSqlType<LocalTime>(Types.TIME, "time") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: LocalTime) {
        ps.setTime(index, Time.valueOf(parameter))
    }

    override fun getNullableResult(rs: ResultSet, index: Int): LocalTime? {
        return rs.getTime(index)?.toLocalTime()
    }
}

fun <E : Entity<E>> Table<E>.monthDay(name: String): Table<E>.ColumnRegistration<MonthDay> {
    return registerColumn(name, MonthDaySqlType)
}

object MonthDaySqlType : AbstractSqlType<MonthDay>(Types.VARCHAR, "varchar(10)") {
    val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .toFormatter()

    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: MonthDay) {
        ps.setString(index, parameter.format(formatter))
    }

    override fun getNullableResult(rs: ResultSet, index: Int): MonthDay? {
        return rs.getString(index)?.let { MonthDay.parse(it, formatter) }
    }
}

fun <E : Entity<E>> Table<E>.yearMonth(name: String): Table<E>.ColumnRegistration<YearMonth> {
    return registerColumn(name, YearMonthSqlType)
}

object YearMonthSqlType : AbstractSqlType<YearMonth>(Types.VARCHAR, "varchar(10)") {
    val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .toFormatter()

    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: YearMonth) {
        ps.setString(index, parameter.format(formatter))
    }

    override fun getNullableResult(rs: ResultSet, index: Int): YearMonth? {
        return rs.getString(index)?.let { YearMonth.parse(it, formatter) }
    }
}

fun <E : Entity<E>> Table<E>.year(name: String): Table<E>.ColumnRegistration<Year> {
    return registerColumn(name, YearSqlType)
}

object YearSqlType : AbstractSqlType<Year>(Types.INTEGER, "int") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: Year) {
        ps.setInt(index, parameter.value)
    }

    override fun getNullableResult(rs: ResultSet, index: Int): Year? {
        return Year.of(rs.getInt(index))
    }
}

fun <E : Entity<E>> Table<E>.timestamp(name: String): Table<E>.ColumnRegistration<Instant> {
    return registerColumn(name, InstantSqlType)
}

object InstantSqlType : AbstractSqlType<Instant>(Types.TIMESTAMP, "timestamp") {
    override fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: Instant) {
        ps.setTimestamp(index, Timestamp.from(parameter))
    }

    override fun getNullableResult(rs: ResultSet, index: Int): Instant? {
        return rs.getTimestamp(index)?.toInstant()
    }
}