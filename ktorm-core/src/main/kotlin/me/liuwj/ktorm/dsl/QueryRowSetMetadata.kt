package me.liuwj.ktorm.dsl

import java.io.Serializable
import java.sql.ResultSetMetaData

/**
 * Created by vince on Sep 03, 2019.
 */
internal class QueryRowSetMetadata : ResultSetMetaData, Serializable {


    override fun getTableName(column: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isNullable(column: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> unwrap(iface: Class<T>?): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isDefinitelyWritable(column: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isSearchable(column: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPrecision(column: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCaseSensitive(column: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getScale(column: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSchemaName(column: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getColumnClassName(column: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCatalogName(column: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getColumnType(column: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCurrency(column: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getColumnLabel(column: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isWritable(column: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isReadOnly(column: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isSigned(column: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getColumnTypeName(column: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getColumnName(column: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isAutoIncrement(column: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getColumnDisplaySize(column: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getColumnCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        val readonly: Boolean,
        val writable: Boolean
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }
}