package me.liuwj.ktorm.dsl

import java.io.Serializable
import java.sql.ResultSetMetaData
import java.sql.SQLException

/**
 * Created by vince on Sep 03, 2019.
 */
internal class QueryRowSetMetadata : ResultSetMetaData, Serializable {
    private val colCount: Int = 0
    private val colInfo: Array<ColInfo> = emptyArray()

    private fun checkColRange(col: Int) {
        if (col <= 0 || col > colCount) {
            throw SQLException("Invalid column index: $col")
        }
    }

    override fun getColumnCount(): Int {
        return colCount
    }

    override fun isAutoIncrement(columnIndex: Int): Boolean {
        checkColRange(columnIndex)
        return colInfo[columnIndex].autoIncrement
    }

    override fun isCaseSensitive(columnIndex: Int): Boolean {
        checkColRange(columnIndex)
        return colInfo[columnIndex].caseSensitive
    }

    override fun isSearchable(columnIndex: Int): Boolean {
        checkColRange(columnIndex)
        return colInfo[columnIndex].searchable
    }

    override fun isCurrency(columnIndex: Int): Boolean {
        checkColRange(columnIndex)
        return colInfo[columnIndex].currency
    }

    override fun isNullable(columnIndex: Int): Int {
        checkColRange(columnIndex)
        return colInfo[columnIndex].nullable
    }

    override fun isSigned(columnIndex: Int): Boolean {
        checkColRange(columnIndex)
        return colInfo[columnIndex].signed
    }

    override fun getColumnDisplaySize(columnIndex: Int): Int {
        checkColRange(columnIndex)
        return colInfo[columnIndex].columnDisplaySize
    }

    override fun getColumnLabel(columnIndex: Int): String {
        checkColRange(columnIndex)
        return colInfo[columnIndex].columnLabel
    }

    override fun getColumnName(columnIndex: Int): String {
        checkColRange(columnIndex)
        return colInfo[columnIndex].columnName
    }

    override fun getSchemaName(columnIndex: Int): String {
        checkColRange(columnIndex)
        return colInfo[columnIndex].schemaName
    }

    override fun getPrecision(columnIndex: Int): Int {
        checkColRange(columnIndex)
        return colInfo[columnIndex].colPrecision
    }

    override fun getScale(columnIndex: Int): Int {
        checkColRange(columnIndex)
        return colInfo[columnIndex].colScale
    }

    override fun getTableName(columnIndex: Int): String {
        checkColRange(columnIndex)
        return colInfo[columnIndex].tableName
    }

    override fun getCatalogName(columnIndex: Int): String {
        checkColRange(columnIndex)
        return colInfo[columnIndex].catName
    }

    override fun getColumnType(columnIndex: Int): Int {
        checkColRange(columnIndex)
        return colInfo[columnIndex].colType
    }

    override fun getColumnTypeName(columnIndex: Int): String {
        checkColRange(columnIndex)
        return colInfo[columnIndex].colTypeName
    }

    override fun isReadOnly(columnIndex: Int): Boolean {
        checkColRange(columnIndex)
        return colInfo[columnIndex].readonly
    }

    override fun isWritable(columnIndex: Int): Boolean {
        checkColRange(columnIndex)
        return colInfo[columnIndex].writable
    }

    override fun isDefinitelyWritable(columnIndex: Int): Boolean {
        return isWritable(columnIndex)
    }

    override fun getColumnClassName(columnIndex: Int): String {
        checkColRange(columnIndex)
        return colInfo[columnIndex].colClassName
    }

    companion object {
        private const val serialVersionUID = 1L
    }

    private data class ColInfo(
        val autoIncrement: Boolean,
        val caseSensitive: Boolean,
        val currency: Boolean,
        val nullable: Int,
        val signed: Boolean,
        val searchable: Boolean,
        val columnDisplaySize: Int,
        val columnLabel: String,
        val columnName: String,
        val schemaName: String,
        val colPrecision: Int,
        val colScale: Int,
        val tableName: String,
        val catName: String,
        val colType: Int,
        val colTypeName: String,
        val colClassName: String,
        val readonly: Boolean,
        val writable: Boolean
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }
}