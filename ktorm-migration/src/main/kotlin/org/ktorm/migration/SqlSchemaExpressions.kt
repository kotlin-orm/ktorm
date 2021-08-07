/*
 * Copyright 2018-2021 the original author or authors.
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

package org.ktorm.migration

import org.ktorm.expression.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.SqlType
import kotlin.reflect.KClass

// Indexes

public data class CreateIndexExpression(
    val name: String,
    val on: TableReferenceExpression,
    val columns: List<ColumnExpression<*>>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class DropIndexExpression(
    val name: String,
    val on: TableReferenceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()


// Views

public data class CreateViewExpression(
    val name: TableReferenceExpression,
    val query: SelectExpression,
    val orReplace: Boolean = false,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class DropViewExpression(
    val name: TableReferenceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

// Tables

public data class CreateTableExpression(
    val name: TableReferenceExpression,
    val columns: List<ColumnDeclarationExpression<*>>,
    val constraints: Map<String, TableConstraintExpression> = emptyMap(),
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class DropTableExpression(
    val table: TableReferenceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class TruncateTableExpression(
    val table: TableReferenceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableAddExpression(
    val table: TableReferenceExpression,
    val column: ColumnDeclaringExpression<*>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableDropColumnExpression(
    val table: TableReferenceExpression,
    val column: ColumnExpression<*>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableModifyColumnExpression(
    val table: TableReferenceExpression,
    val column: ColumnExpression<*>,
    val newType: SqlType<*>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableSetColumnConstraintExpression<T : Any>(
    val table: TableReferenceExpression,
    val column: ColumnExpression<T>,
    val tableConstraint: ColumnConstraintExpression<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableDropColumnConstraintExpression(
    val table: TableReferenceExpression,
    val column: ColumnExpression<*>,
    val type: KClass<ColumnConstraintExpression<*>>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableAddConstraintExpression(
    val table: TableReferenceExpression,
    val constraintName: String,
    val tableConstraint: TableConstraintExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableDropConstraintExpression(
    val table: TableReferenceExpression,
    val constraintName: String,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

// Components

public data class ColumnDeclarationExpression<T : Any>(
    val name: String,
    val sqlType: SqlType<T>,
    val constraints: List<ColumnConstraintExpression<T>> = emptyList(),
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public abstract class TableConstraintExpression() : SqlExpression()
public data class ForeignKeyTableConstraintExpression(
    val otherTable: TableReferenceExpression,
    val correspondence: Map<ColumnExpression<*>, ColumnExpression<*>>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : TableConstraintExpression()

public data class CheckTableConstraintExpression(
    val condition: ScalarExpression<Boolean>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : TableConstraintExpression()

public data class UniqueTableConstraintExpression(
    val across: List<ColumnExpression<*>>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : TableConstraintExpression()

public data class PrimaryKeyTableConstraintExpression(
    val across: List<ColumnExpression<*>>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : TableConstraintExpression()

public abstract class ColumnConstraintExpression<in T : Any>() : SqlExpression()
public object PrimaryKeyColumnConstraintExpression : ColumnConstraintExpression<Any>() {
    override val isLeafNode: Boolean
        get() = true
    override val extraProperties: Map<String, Any>
        get() = mapOf()
}

public object UniqueColumnConstraintExpression : ColumnConstraintExpression<Any>() {
    override val isLeafNode: Boolean
        get() = true
    override val extraProperties: Map<String, Any>
        get() = mapOf()
}

public object NotNullColumnConstraintExpression : ColumnConstraintExpression<Any>() {
    override val isLeafNode: Boolean
        get() = true
    override val extraProperties: Map<String, Any>
        get() = mapOf()
}

public data class DefaultColumnConstraintExpression<T : Any>(
    val value: ScalarExpression<T>
) : ColumnConstraintExpression<T>() {
    override val isLeafNode: Boolean
        get() = true
    override val extraProperties: Map<String, Any>
        get() = mapOf()
}

public data class AutoIncrementColumnConstraintExpression<T : Any>(
    val startAt: Int = 0,
    val increaseBy: Int = 1
) : ColumnConstraintExpression<T>() {
    override val isLeafNode: Boolean
        get() = true
    override val extraProperties: Map<String, Any>
        get() = mapOf()
}

public data class TableReferenceExpression(
    val name: String,
    val catalog: String? = null,
    val schema: String? = null,
    override val isLeafNode: Boolean = true,
    override val extraProperties: Map<String, Any> = mapOf(),
): SqlExpression()
public fun BaseTable<*>.asReferenceExpression(): TableReferenceExpression = TableReferenceExpression(name = tableName, catalog = catalog, schema = schema)