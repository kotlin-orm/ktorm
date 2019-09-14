package me.liuwj.ktorm.dsl

import java.sql.*
import javax.sql.rowset.serial.*

/**
 * Created by vince on Sep 02, 2019.
 */
class QueryRowSet0 internal constructor(val query: Query, rs: ResultSet) : ResultSet {
    private val metadata = QueryRowSetMetadata(rs.metaData)
    private val values = readValues(rs)
    private var cursor = -1

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
            throw SQLException("Invalid cursor positionn.")
        }
    }
}