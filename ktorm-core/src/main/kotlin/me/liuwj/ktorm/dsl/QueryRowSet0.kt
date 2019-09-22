package me.liuwj.ktorm.dsl

import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.sql.*
import java.sql.Date
import java.sql.ResultSet.*
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import javax.sql.rowset.serial.*

/**
 * Created by vince on Sep 02, 2019.
 */
class QueryRowSet0 internal constructor(val query: Query, rs: ResultSet) : ResultSet {
    private val _typeMap = try { rs.statement.connection.typeMap } catch (_: Throwable) { null }
    private val _metadata = QueryRowSetMetadata(rs.metaData)
    private val _values = readValues(rs)
    private var _cursor = -1
    private var _wasNull = false
    private var _fetchDirection = FETCH_FORWARD
    private var _fetchSize = 0

    private fun readValues(rs: ResultSet): List<Array<Any?>> {
        return rs.iterable().map { row ->
            Array(_metadata.columnCount) { index ->
                val obj = if (_typeMap.isNullOrEmpty()) {
                    row.getObject(index + 1)
                } else {
                    row.getObject(index + 1, _typeMap)
                }

                when (obj) {
                    is Ref -> SerialRef(obj)
                    is Struct -> SerialStruct(obj, _typeMap)
                    is SQLData -> SerialStruct(obj, _typeMap)
                    is Blob -> try { MemoryBlob(obj) } finally { obj.free() }
                    is Clob -> try { MemoryClob(obj) } finally { obj.free() }
                    is java.sql.Array -> try { MemoryArray(obj, _typeMap) } finally { obj.free() }
                    else -> obj
                }
            }
        }
    }

    private class MemoryBlob(blob: SerialBlob) : Blob by blob {

        constructor(blob: Blob) : this(SerialBlob(blob))

        constructor(bytes: ByteArray) : this(SerialBlob(bytes))

        override fun free() {
            // no-op
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    private class MemoryClob(clob: SerialClob) : Clob by clob {

        constructor(clob: Clob) : this(SerialClob(clob))

        constructor(str: String) : this(SerialClob(str.toCharArray()))

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
        if (_cursor >= -1 && _cursor < _values.size) {
            return ++_cursor < _values.size
        } else {
            throw SQLException("Invalid cursor position.")
        }
    }

    override fun close() {
        // no-op
    }

    override fun wasNull(): Boolean {
        return _wasNull
    }

    private fun getColumnValue(index: Int): Any? {
        if (index < 1 || index > _metadata.columnCount) {
            throw SQLException("Invalid column index.")
        }

        if (_values.isEmpty() || isAfterLast || isBeforeFirst) {
            throw SQLException("Invalid cursor position.")
        }

        val value = _values[_cursor][index - 1]
        _wasNull = value == null
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

    @Suppress("OverridingDeprecatedMember")
    override fun getBigDecimal(columnIndex: Int, scale: Int): BigDecimal? {
        val decimal = getBigDecimal(columnIndex)
        decimal?.setScale(scale)
        return decimal
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
            is Blob -> value.binaryStream
            is Clob -> value.asciiStream
            is String -> value.byteInputStream(Charsets.US_ASCII)
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to InputStream.")
        }
    }

    @Suppress("OverridingDeprecatedMember")
    override fun getUnicodeStream(columnIndex: Int): InputStream? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is Blob -> value.binaryStream
            is Clob -> value.characterStream.use { it.readText() }.byteInputStream(Charsets.UTF_8)
            is String -> value.byteInputStream(Charsets.UTF_8)
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to InputStream.")
        }
    }

    override fun getBinaryStream(columnIndex: Int): InputStream? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is Blob -> value.binaryStream
            is Clob -> value.asciiStream
            is ByteArray -> value.inputStream()
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

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
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

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun getUnicodeStream(columnLabel: String): InputStream? {
        return getUnicodeStream(findColumn(columnLabel))
    }

    override fun getBinaryStream(columnLabel: String): InputStream? {
        return getBinaryStream(findColumn(columnLabel))
    }

    override fun getWarnings(): SQLWarning? {
        return null
    }

    override fun clearWarnings() {
        // no-op
    }

    @Deprecated("Positioned updates and deletes are not supported.", level = DeprecationLevel.ERROR)
    override fun getCursorName(): Nothing {
        throw SQLFeatureNotSupportedException("Positioned updates and deletes are not supported.")
    }

    override fun getMetaData(): ResultSetMetaData {
        return _metadata
    }

    override fun getObject(columnIndex: Int): Any? {
        return getObject(columnIndex, _typeMap.orEmpty())
    }

    override fun getObject(columnLabel: String): Any? {
        return getObject(findColumn(columnLabel))
    }

    override fun findColumn(columnLabel: String): Int {
        for (index in 1.._metadata.columnCount) {
            if (_metadata.getColumnLabel(index).equals(columnLabel, ignoreCase = true)) {
                return index
            }
        }
        throw SQLException("Invalid column name: $columnLabel")
    }

    override fun getCharacterStream(columnIndex: Int): Reader? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is Blob -> value.binaryStream.reader()
            is Clob -> value.characterStream
            is ByteArray -> value.inputStream().reader()
            is String -> value.reader()
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to Reader.")
        }
    }

    override fun getCharacterStream(columnLabel: String): Reader? {
        return getCharacterStream(findColumn(columnLabel))
    }

    override fun getBigDecimal(columnIndex: Int): BigDecimal? {
        return when (val value = getColumnValue(columnIndex)) {
            is BigDecimal -> value
            else -> value?.toString()?.toBigDecimal()
        }
    }

    override fun getBigDecimal(columnLabel: String): BigDecimal? {
        return getBigDecimal(findColumn(columnLabel))
    }

    override fun isBeforeFirst(): Boolean {
        return _cursor <= -1 && _values.isNotEmpty()
    }

    override fun isAfterLast(): Boolean {
        return _cursor >= _values.size && _values.isNotEmpty()
    }

    override fun isFirst(): Boolean {
        return _cursor == 0 && _values.isNotEmpty()
    }

    override fun isLast(): Boolean {
        return _cursor == _values.size - 1 && _values.isNotEmpty()
    }

    override fun beforeFirst() {
        if (_values.isNotEmpty()) {
            _cursor = -1
        }
    }

    override fun afterLast() {
        if (_values.isNotEmpty()) {
            _cursor = _values.size
        }
    }

    override fun first(): Boolean {
        if (_values.isNotEmpty()) {
            _cursor = 0
            return true
        } else {
            return false
        }
    }

    override fun last(): Boolean {
        if (_values.isNotEmpty()) {
            _cursor = _values.size - 1
            return true
        } else {
            return false
        }
    }

    override fun getRow(): Int {
        if (_cursor > -1 && _cursor < _values.size && _values.isNotEmpty()) {
            return _cursor + 1
        } else {
            return 0
        }
    }

    override fun absolute(row: Int): Boolean {
        when {
            _values.isEmpty() -> {
                return false
            }
            row == 0 -> {
                beforeFirst()
                return false
            }
            row == 1 -> {
                return first()
            }
            row == -1 -> {
                return last()
            }
            row > _values.size -> {
                afterLast()
                return false
            }
            row > 0 -> {
                _cursor = row - 1
                return true
            }
            else -> {
                val adjustedRow = _values.size + row + 1
                if (adjustedRow <= 0) {
                    beforeFirst()
                    return false
                } else {
                    return absolute(adjustedRow)
                }
            }
        }
    }

    override fun relative(rows: Int): Boolean {
        if (_values.isEmpty()) {
            return false
        }

        val newCursor = _cursor + rows

        if (newCursor >= _values.size) {
            afterLast()
            return false
        }

        if (newCursor <= -1) {
            beforeFirst()
            return false
        }

        _cursor = newCursor
        return true
    }

    override fun previous(): Boolean {
        if (_cursor > -1 && _cursor <= _values.size) {
            return --_cursor > -1
        } else {
            throw SQLException("Invalid cursor position.")
        }
    }

    override fun setFetchDirection(direction: Int) {
        if (direction != FETCH_FORWARD && direction != FETCH_REVERSE && direction != FETCH_UNKNOWN) {
            throw SQLException("Invalid fetch direction: $direction")
        }

        _fetchDirection = direction
    }

    override fun getFetchDirection(): Int {
        return _fetchDirection
    }

    override fun setFetchSize(rows: Int) {
        if (rows < 0) {
            throw SQLException("Invalid fetch size: $rows")
        }

        _fetchSize = rows
    }

    override fun getFetchSize(): Int {
        return _fetchSize
    }

    override fun getType(): Int {
        return TYPE_SCROLL_INSENSITIVE
    }

    override fun getConcurrency(): Int {
        return CONCUR_READ_ONLY
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun rowUpdated(): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun rowInserted(): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun rowDeleted(): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateNull(columnIndex: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateBoolean(columnIndex: Int, x: Boolean): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateByte(columnIndex: Int, x: Byte): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateShort(columnIndex: Int, x: Short): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateInt(columnIndex: Int, x: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateLong(columnIndex: Int, x: Long): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateFloat(columnIndex: Int, x: Float): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateDouble(columnIndex: Int, x: Double): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateBigDecimal(columnIndex: Int, x: BigDecimal?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateString(columnIndex: Int, x: String?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateBytes(columnIndex: Int, x: ByteArray?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateDate(columnIndex: Int, x: Date?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateTime(columnIndex: Int, x: Time?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateTimestamp(columnIndex: Int, x: Timestamp?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateAsciiStream(columnIndex: Int, x: InputStream?, length: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateBinaryStream(columnIndex: Int, x: InputStream?, length: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateCharacterStream(columnIndex: Int, x: Reader?, length: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateObject(columnIndex: Int, x: Any?, scaleOrLength: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateObject(columnIndex: Int, x: Any?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateNull(columnLabel: String?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateBoolean(columnLabel: String?, x: Boolean): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateByte(columnLabel: String?, x: Byte): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateShort(columnLabel: String?, x: Short): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateInt(columnLabel: String?, x: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateLong(columnLabel: String?, x: Long): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateFloat(columnLabel: String?, x: Float): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateDouble(columnLabel: String?, x: Double): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateBigDecimal(columnLabel: String?, x: BigDecimal?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateString(columnLabel: String?, x: String?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateBytes(columnLabel: String?, x: ByteArray?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateDate(columnLabel: String?, x: Date?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateTime(columnLabel: String?, x: Time?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateTimestamp(columnLabel: String?, x: Timestamp?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateAsciiStream(columnLabel: String?, x: InputStream?, length: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateBinaryStream(columnLabel: String?, x: InputStream?, length: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateCharacterStream(columnLabel: String?, reader: Reader?, length: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateObject(columnLabel: String?, x: Any?, scaleOrLength: Int): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateObject(columnLabel: String?, x: Any?): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun insertRow(): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun updateRow(): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun deleteRow(): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun refreshRow(): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun cancelRowUpdates(): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun moveToInsertRow(): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    @Deprecated("The result set is not updatable.", level = DeprecationLevel.ERROR)
    override fun moveToCurrentRow(): Nothing {
        throw SQLFeatureNotSupportedException("The result set is not updatable.")
    }

    override fun getStatement(): Statement? {
        return null
    }

    override fun getObject(columnIndex: Int, map: Map<String, Class<*>>): Any? {
        val value = getColumnValue(columnIndex)

        if (value is Struct) {
            val cls = map[value.sqlTypeName]
            if (cls != null) {
                val data = cls.newInstance() as SQLData
                val input = SQLInputImpl(value.getAttributes(map), map)
                data.readSQL(input, value.sqlTypeName)
                return data
            }
        }

        return value
    }

    override fun getRef(columnIndex: Int): Ref? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is Ref -> value
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to Ref.")
        }
    }

    override fun getBlob(columnIndex: Int): Blob? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is ByteArray -> MemoryBlob(value)
            is Blob -> value
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to Blob.")
        }
    }

    override fun getClob(columnIndex: Int): Clob? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is String -> MemoryClob(value)
            is Clob -> value
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to Clob.")
        }
    }

    override fun getArray(columnIndex: Int): java.sql.Array? {
        return when (val value = getColumnValue(columnIndex)) {
            null -> null
            is java.sql.Array -> value
            else -> throw SQLException("Cannot convert ${value.javaClass.name} value to Array.")
        }
    }

    override fun getObject(columnLabel: String, map: Map<String, Class<*>>): Any? {
        return getObject(findColumn(columnLabel), map)
    }

    override fun getRef(columnLabel: String): Ref? {
        return getRef(findColumn(columnLabel))
    }

    override fun getBlob(columnLabel: String): Blob? {
        return getBlob(findColumn(columnLabel))
    }

    override fun getClob(columnLabel: String): Clob? {
        return getClob(findColumn(columnLabel))
    }

    override fun getArray(columnLabel: String): java.sql.Array? {
        return getArray(findColumn(columnLabel))
    }

    override fun getDate(columnIndex: Int, cal: Calendar): Date? {
        val defaultCalendar = Calendar.getInstance()
        defaultCalendar.time = getDate(columnIndex) ?: return null

        cal.set(Calendar.YEAR, defaultCalendar.get(Calendar.YEAR))
        cal.set(Calendar.MONTH, defaultCalendar.get(Calendar.MONTH))
        cal.set(Calendar.DAY_OF_MONTH, defaultCalendar.get(Calendar.DAY_OF_MONTH))
        return Date(cal.timeInMillis)
    }

    override fun getDate(columnLabel: String, cal: Calendar): Date? {
        return getDate(findColumn(columnLabel), cal)
    }

    override fun getTime(columnIndex: Int, cal: Calendar): Time? {
        val defaultCalendar = Calendar.getInstance()
        defaultCalendar.time = getTime(columnIndex) ?: return null

        cal.set(Calendar.HOUR_OF_DAY, defaultCalendar.get(Calendar.HOUR_OF_DAY))
        cal.set(Calendar.MINUTE, defaultCalendar.get(Calendar.MINUTE))
        cal.set(Calendar.SECOND, defaultCalendar.get(Calendar.SECOND))
        cal.set(Calendar.MILLISECOND, defaultCalendar.get(Calendar.MILLISECOND))
        return Time(cal.timeInMillis)
    }

    override fun getTime(columnLabel: String, cal: Calendar): Time? {
        return getTime(findColumn(columnLabel), cal)
    }

    override fun getTimestamp(columnIndex: Int, cal: Calendar): Timestamp? {
        val defaultCalendar = Calendar.getInstance()
        defaultCalendar.time = getTimestamp(columnIndex) ?: return null

        cal.set(Calendar.YEAR, defaultCalendar.get(Calendar.YEAR))
        cal.set(Calendar.MONTH, defaultCalendar.get(Calendar.MONTH))
        cal.set(Calendar.DAY_OF_MONTH, defaultCalendar.get(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, defaultCalendar.get(Calendar.HOUR_OF_DAY))
        cal.set(Calendar.MINUTE, defaultCalendar.get(Calendar.MINUTE))
        cal.set(Calendar.SECOND, defaultCalendar.get(Calendar.SECOND))
        cal.set(Calendar.MILLISECOND, defaultCalendar.get(Calendar.MILLISECOND))
        return Timestamp(cal.timeInMillis)
    }

    override fun getTimestamp(columnLabel: String, cal: Calendar): Timestamp? {
        return getTimestamp(findColumn(columnLabel), cal)
    }
}
