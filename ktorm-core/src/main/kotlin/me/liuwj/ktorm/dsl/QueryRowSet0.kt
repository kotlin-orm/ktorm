package me.liuwj.ktorm.dsl

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.math.BigDecimal
import java.sql.*
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.sql.rowset.serial.*

/**
 * Created by vince on Sep 02, 2019.
 */
class QueryRowSet0 internal constructor(val query: Query, rs: ResultSet) : ResultSet {
    private val metadata = QueryRowSetMetadata(rs.metaData)
    private val values = readValues(rs)
    private var cursor = -1
    private var wasNull = false

    private fun readValues(rs: ResultSet): List<Array<Any?>> {
        val typeMap = try { rs.statement.connection.typeMap } catch (_: Throwable) { null }

        return rs.iterable().map { row ->
            Array(metadata.columnCount) { index ->
                val obj = if (typeMap.isNullOrEmpty()) {
                    row.getObject(index + 1)
                } else {
                    row.getObject(index + 1, typeMap)
                }

                when (obj) {
                    is Ref -> SerialRef(obj)
                    is Struct -> SerialStruct(obj, typeMap)
                    is SQLData -> SerialStruct(obj, typeMap)
                    is Blob -> try { MemoryBlob(obj) } finally { obj.free() }
                    is Clob -> try { MemoryClob(obj) } finally { obj.free() }
                    is java.sql.Array -> try { MemoryArray(obj, typeMap) } finally { obj.free() }
                    else -> obj
                }
            }
        }
    }

    private class MemoryBlob(blob: Blob) : Blob by SerialBlob(blob) {

        override fun free() {
            // no-op
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    private class MemoryClob(clob: Clob) : Clob by SerialClob(clob) {

        override fun free() {
            // no-op
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    private class MemoryArray(
        array: java.sql.Array,
        typeMap: Map<String, Class<*>>?
    ) : java.sql.Array by if (typeMap != null) SerialArray(array, typeMap) else SerialArray(array) {

        override fun free() {
            // no-op
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    override fun next(): Boolean {
        if (cursor >= -1 && cursor < values.size) {
            return ++cursor != values.size
        } else {
            throw SQLException("Invalid cursor position.")
        }
    }

    override fun close() {
        // no-op
    }

    override fun wasNull(): Boolean {
        return wasNull
    }

    private fun getColumnValue(index: Int): Any? {
        if (index < 1 || index > metadata.columnCount) {
            throw SQLException("Invalid column index.")
        }

        // todo: check cursor

        val value = values[cursor][index - 1]
        wasNull = value == null
        return value
    }

    override fun getString(columnIndex: Int): String? {
        return when (val value = getColumnValue(columnIndex)) {
            is Clob -> value.characterStream.use { it.readText() }
            else -> value?.toString()
        }
    }

    override fun getBoolean(columnIndex: Int): Boolean {
        return when (val value = getColumnValue(columnIndex)) {
            null -> false
            is Boolean -> value
            is Number -> value.toDouble().toBits() != 0.0.toBits()
            else -> value.toString().toDouble().toBits() != 0.0.toBits()
        }
    }

    override fun getByte(columnIndex: Int): Byte {
        return when (val value = getColumnValue(columnIndex)) {
            is Number -> value.toByte()
            else -> value?.toString()?.toByte() ?: 0
        }
    }

    override fun getShort(columnIndex: Int): Short {
        return when (val value = getColumnValue(columnIndex)) {
            is Number -> value.toShort()
            else -> value?.toString()?.toShort() ?: 0
        }
    }

    override fun getInt(columnIndex: Int): Int {
        return when (val value = getColumnValue(columnIndex)) {
            is Number -> value.toInt()
            else -> value?.toString()?.toInt() ?: 0
        }
    }

    override fun getLong(columnIndex: Int): Long {
        return when (val value = getColumnValue(columnIndex)) {
            is Number -> value.toLong()
            else -> value?.toString()?.toLong() ?: 0
        }
    }

    override fun getFloat(columnIndex: Int): Float {
        return when (val value = getColumnValue(columnIndex)) {
            is Number -> value.toFloat()
            else -> value?.toString()?.toFloat() ?: 0.0F
        }
    }

    override fun getDouble(columnIndex: Int): Double {
        return when (val value = getColumnValue(columnIndex)) {
            is Number -> value.toDouble()
            else -> value?.toString()?.toDouble() ?: 0.0
        }
    }

    override fun getBigDecimal(columnIndex: Int, scale: Int): BigDecimal? {
        TODO()
    }

    override fun getBytes(columnIndex: Int): ByteArray? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is ByteArray -> value
            is Blob -> value.binaryStream.use { it.readBytes() }
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to byte[].")
        }
    }

    override fun getDate(columnIndex: Int): Date? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is java.util.Date -> Date(value.time)
            is String -> {
                val date = DateFormat.getDateInstance().parse(value)
                Date(date.time)
            }
            else -> {
                throw SQLException("Cannot convert ${value.javaClass.name} value to Date.")
            }
        }
    }

    override fun getTime(columnIndex: Int): Time? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is java.util.Date -> Time(value.time)
            is String -> {
                val date = DateFormat.getTimeInstance().parse(value)
                Time(date.time)
            }
            else -> {
                throw SQLException("Cannot convert ${value.javaClass.name} value to Time.")
            }
        }
    }

    override fun getTimestamp(columnIndex: Int): Timestamp? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is java.util.Date -> Timestamp(value.time)
            is String -> {
                val date = DateFormat.getDateTimeInstance().parse(value)
                Timestamp(date.time)
            }
            else -> {
                throw SQLException("Cannot convert ${value.javaClass.name} value to Timestamp.")
            }
        }
    }

    fun getLocalDate(columnIndex: Int): LocalDate? {
        return getDate(columnIndex)?.toLocalDate()
    }

    fun getLocalTime(columnIndex: Int): LocalTime? {
        return getTime(columnIndex)?.toLocalTime()
    }

    fun getLocalDateTime(columnIndex: Int): LocalDateTime? {
        return getTimestamp(columnIndex)?.toLocalDateTime()
    }

    fun getInstant(columnIndex: Int): Instant? {
        return getTimestamp(columnIndex)?.toInstant()
    }

    override fun getAsciiStream(columnIndex: Int): InputStream? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is Clob -> value.asciiStream
            is String -> ByteArrayInputStream(value.toByteArray(Charsets.US_ASCII))
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to InputStream.")
        }
    }

    override fun getUnicodeStream(columnIndex: Int): InputStream? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is String -> ByteArrayInputStream(value.toByteArray(Charsets.UTF_8))
            is Clob -> ByteArrayInputStream(value.characterStream.use { it.readText().toByteArray(Charsets.UTF_8) })
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to InputStream.")
        }
    }

    override fun getBinaryStream(columnIndex: Int): InputStream? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is ByteArray -> ByteArrayInputStream(value)
            is Blob -> value.binaryStream
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to InputStream.")
        }
    }

    override fun getString(columnLabel: String): String? {
        return getString(findColumn(columnLabel))
    }

    override fun getBoolean(columnLabel: String): Boolean {
        return getBoolean(findColumn(columnLabel))
    }

    override fun getByte(columnLabel: String): Byte {
        return getByte(findColumn(columnLabel))
    }

    override fun getShort(columnLabel: String): Short {
        return getShort(findColumn(columnLabel))
    }

    override fun getInt(columnLabel: String): Int {
        return getInt(findColumn(columnLabel))
    }

    override fun getLong(columnLabel: String): Long {
        return getLong(findColumn(columnLabel))
    }

    override fun getFloat(columnLabel: String): Float {
        return getFloat(findColumn(columnLabel))
    }

    override fun getDouble(columnLabel: String): Double {
        return getDouble(findColumn(columnLabel))
    }

    override fun getBigDecimal(columnLabel: String, scale: Int): BigDecimal? {
        return getBigDecimal(findColumn(columnLabel), scale)
    }

    override fun getBytes(columnLabel: String): ByteArray? {
        return getBytes(findColumn(columnLabel))
    }

    override fun getDate(columnLabel: String): Date? {
        return getDate(findColumn(columnLabel))
    }

    override fun getTime(columnLabel: String): Time? {
        return getTime(findColumn(columnLabel))
    }

    override fun getTimestamp(columnLabel: String): Timestamp? {
        return getTimestamp(findColumn(columnLabel))
    }

    fun getLocalDate(columnLabel: String): LocalDate? {
        return getLocalDate(findColumn(columnLabel))
    }

    fun getLocalTime(columnLabel: String): LocalTime? {
        return getLocalTime(findColumn(columnLabel))
    }

    fun getLocalDateTime(columnLabel: String): LocalDateTime? {
        return getLocalDateTime(findColumn(columnLabel))
    }

    fun getInstant(columnLabel: String): Instant? {
        return getInstant(findColumn(columnLabel))
    }

    override fun getAsciiStream(columnLabel: String): InputStream? {
        return getAsciiStream(findColumn(columnLabel))
    }

    override fun getUnicodeStream(columnLabel: String): InputStream? {
        return getUnicodeStream(findColumn(columnLabel))
    }

    override fun getBinaryStream(columnLabel: String): InputStream? {
        return getBinaryStream(findColumn(columnLabel))
    }

    override fun findColumn(columnLabel: String): Int {
        for (index in 1..metadata.columnCount) {
            if (metadata.getColumnLabel(index).equals(columnLabel, ignoreCase = true)) {
                return index
            }
        }
        throw SQLException("Invalid column name: $columnLabel")
    }
}