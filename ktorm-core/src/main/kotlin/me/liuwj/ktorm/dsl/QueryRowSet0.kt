package me.liuwj.ktorm.dsl

import java.math.BigDecimal
import java.sql.*
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
                    is Blob -> SerialBlob(obj)
                    is Clob -> SerialClob(obj)
                    is java.sql.Array -> if (typeMap != null) SerialArray(obj, typeMap) else SerialArray(obj)
                    else -> obj
                }
            }
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
        return getColumnValue(columnIndex)?.toString()
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

    override fun getBigDecimal(columnIndex: Int, scale: Int): BigDecimal {
        TODO()
    }
}