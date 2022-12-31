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

package org.ktorm.database

import java.sql.ResultSetMetaData
import java.sql.ResultSetMetaData.columnNullableUnknown
import java.sql.SQLException
import java.sql.Types

/**
 * Created by vince on Sep 03, 2019.
 */
internal class CachedRowSetMetadata(metadata: ResultSetMetaData) : ResultSetMetaData {
    private val columns = Array(metadata.columnCount) { index ->
        val i = index + 1

        ColumnInfo(
            autoIncrement = metadata.runCatching { isAutoIncrement(i) }.getOrDefault(false),
            caseSensitive = metadata.runCatching { isCaseSensitive(i) }.getOrDefault(false),
            searchable = metadata.runCatching { isSearchable(i) }.getOrDefault(false),
            currency = metadata.runCatching { isCurrency(i) }.getOrDefault(false),
            nullable = metadata.runCatching { isNullable(i) }.getOrDefault(columnNullableUnknown),
            signed = metadata.runCatching { isSigned(i) }.getOrDefault(true),
            columnDisplaySize = metadata.runCatching { getColumnDisplaySize(i) }.getOrDefault(0),
            columnLabel = metadata.runCatching { getColumnLabel(i) }.getOrNull().orEmpty(),
            columnName = metadata.runCatching { getColumnName(i) }.getOrNull().orEmpty(),
            schemaName = metadata.runCatching { getSchemaName(i) }.getOrNull().orEmpty(),
            precision = metadata.runCatching { getPrecision(i) }.getOrDefault(0),
            scale = metadata.runCatching { getScale(i) }.getOrDefault(0),
            tableName = metadata.runCatching { getTableName(i) }.getOrNull().orEmpty(),
            catalogName = metadata.runCatching { getCatalogName(i) }.getOrNull().orEmpty(),
            columnType = metadata.runCatching { getColumnType(i) }.getOrDefault(Types.VARCHAR),
            columnTypeName = metadata.runCatching { getColumnTypeName(i) }.getOrNull().orEmpty(),
            columnClassName = metadata.runCatching { getColumnClassName(i) }.getOrNull().orEmpty(),
            readonly = true,
            writable = false,
            definitelyWritable = false
        )
    }

    @Suppress("UnusedPrivateMember")
    private operator fun get(col: Int): ColumnInfo {
        if (col <= 0 || col > columns.size) {
            throw SQLException("Invalid column index: $col")
        }

        return columns[col - 1]
    }

    private data class ColumnInfo(
        val autoIncrement: Boolean,
        val caseSensitive: Boolean,
        val searchable: Boolean,
        val currency: Boolean,
        val nullable: Int,
        val signed: Boolean,
        val columnDisplaySize: Int,
        val columnLabel: String,
        val columnName: String,
        val schemaName: String,
        val precision: Int,
        val scale: Int,
        val tableName: String,
        val catalogName: String,
        val columnType: Int,
        val columnTypeName: String,
        val columnClassName: String,
        val readonly: Boolean,
        val writable: Boolean,
        val definitelyWritable: Boolean
    )

    override fun getColumnCount(): Int {
        return columns.size
    }

    override fun isAutoIncrement(columnIndex: Int): Boolean {
        return this[columnIndex].autoIncrement
    }

    override fun isCaseSensitive(columnIndex: Int): Boolean {
        return this[columnIndex].caseSensitive
    }

    override fun isSearchable(columnIndex: Int): Boolean {
        return this[columnIndex].searchable
    }

    override fun isCurrency(columnIndex: Int): Boolean {
        return this[columnIndex].currency
    }

    override fun isNullable(columnIndex: Int): Int {
        return this[columnIndex].nullable
    }

    override fun isSigned(columnIndex: Int): Boolean {
        return this[columnIndex].signed
    }

    override fun getColumnDisplaySize(columnIndex: Int): Int {
        return this[columnIndex].columnDisplaySize
    }

    override fun getColumnLabel(columnIndex: Int): String {
        return this[columnIndex].columnLabel
    }

    override fun getColumnName(columnIndex: Int): String {
        return this[columnIndex].columnName
    }

    override fun getSchemaName(columnIndex: Int): String {
        return this[columnIndex].schemaName
    }

    override fun getPrecision(columnIndex: Int): Int {
        return this[columnIndex].precision
    }

    override fun getScale(columnIndex: Int): Int {
        return this[columnIndex].scale
    }

    override fun getTableName(columnIndex: Int): String {
        return this[columnIndex].tableName
    }

    override fun getCatalogName(columnIndex: Int): String {
        return this[columnIndex].catalogName
    }

    override fun getColumnType(columnIndex: Int): Int {
        return this[columnIndex].columnType
    }

    override fun getColumnTypeName(columnIndex: Int): String {
        return this[columnIndex].columnTypeName
    }

    override fun isReadOnly(columnIndex: Int): Boolean {
        return this[columnIndex].readonly
    }

    override fun isWritable(columnIndex: Int): Boolean {
        return this[columnIndex].writable
    }

    override fun isDefinitelyWritable(columnIndex: Int): Boolean {
        return this[columnIndex].definitelyWritable
    }

    override fun getColumnClassName(columnIndex: Int): String {
        return this[columnIndex].columnClassName
    }

    override fun <T : Any> unwrap(iface: Class<T>): T {
        return iface.cast(this)
    }

    override fun isWrapperFor(iface: Class<*>): Boolean {
        return iface.isInstance(this)
    }
}
