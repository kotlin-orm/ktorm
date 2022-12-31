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

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.ColumnDeclaringExpression
import org.ktorm.expression.ColumnExpression
import org.ktorm.expression.ScalarExpression
import kotlin.reflect.KProperty1

/**
 * Base class of column bindings. A column might be bound to a simple property, nested properties,
 * or a reference to another table.
 */
public sealed class ColumnBinding

/**
 * Bind the column to nested properties, eg. `employee.manager.department.id`.
 *
 * @property properties the nested properties, cannot be empty.
 */
public data class NestedBinding(val properties: List<KProperty1<*, *>>) : ColumnBinding()

/**
 * Bind the column to a reference table, equivalent to a foreign key in relational databases.
 * Entity sequence APIs would automatically left-join all references (recursively) by default.
 *
 * @property referenceTable the reference table.
 * @property onProperty the property used to hold the referenced entity object.
 * @see org.ktorm.entity.sequenceOf
 * @see BaseTable.createEntity
 */
public data class ReferenceBinding(val referenceTable: BaseTable<*>, val onProperty: KProperty1<*, *>) : ColumnBinding()

/**
 * Common interface of [Column] and [ScalarExpression].
 */
public interface ColumnDeclaring<T : Any> {

    /**
     * The [SqlType] of this column or expression.
     */
    public val sqlType: SqlType<T>

    /**
     * Convert this instance to a [ScalarExpression].
     *
     * If this instance is a [Column], return a [ColumnExpression], otherwise if it's already a [ScalarExpression],
     * return `this` directly.
     */
    public fun asExpression(): ScalarExpression<T>

    /**
     * Wrap this instance as a [ColumnDeclaringExpression].
     */
    public fun aliased(label: String?): ColumnDeclaringExpression<T>

    /**
     * Wrap the given [argument] as an [ArgumentExpression] using the [sqlType].
     */
    public fun wrapArgument(argument: T?): ArgumentExpression<T>
}

/**
 * Represents database columns.
 */
public data class Column<T : Any>(

    /**
     * The table that this column belongs to.
     */
    val table: BaseTable<*>,

    /**
     * The column's name.
     */
    val name: String,

    /**
     * The column's primary binding. A column might be bound to a simple property, nested properties,
     * or a reference to another table, null if the column doesn't bind to any property.
     */
    val binding: ColumnBinding? = null,

    /**
     * The column's extra bindings. Useful when we need to configure two or more bindings for a column.
     *
     * @since 2.6
     */
    val extraBindings: List<ColumnBinding> = emptyList(),

    /**
     * The [SqlType] of this column or expression.
     */
    override val sqlType: SqlType<T>

) : ColumnDeclaring<T> {

    /**
     * The column's label, used to identify the selected columns and to obtain query results.
     *
     * For example, `select a.name as label from dual`, in which `a.name as label` is a column declaring, and `label`
     * is the label.
     *
     * @see ColumnDeclaringExpression
     */
    val label: String get() = toString(separator = "_")

    /**
     * Return all the bindings of this column, including the primary [binding] and [extraBindings].
     */
    val allBindings: List<ColumnBinding> get() = binding?.let { listOf(it) + extraBindings } ?: emptyList()

    /**
     * If the column is bound to a reference table, return the table, otherwise return null.
     *
     * Shortcut for `(binding as? ReferenceBinding)?.referenceTable`.
     */
    val referenceTable: BaseTable<*>? get() = (binding as? ReferenceBinding)?.referenceTable

    /**
     * Convert this column to a [ColumnExpression].
     */
    override fun asExpression(): ColumnExpression<T> {
        return ColumnExpression(table.asExpression(), name, sqlType)
    }

    /**
     * Wrap this column as a [ColumnDeclaringExpression].
     */
    override fun aliased(label: String?): ColumnDeclaringExpression<T> {
        return ColumnDeclaringExpression(asExpression(), label)
    }

    /**
     * Wrap the given [argument] as an [ArgumentExpression] using the [sqlType].
     */
    override fun wrapArgument(argument: T?): ArgumentExpression<T> {
        return ArgumentExpression(argument, sqlType)
    }

    /**
     * Return a string representation of this column.
     */
    override fun toString(): String {
        return toString(separator = ".")
    }

    private fun toString(separator: String) = buildString {
        if (table.alias != null && table.alias.isNotBlank()) {
            append("${table.alias}$separator")
        } else {
            if (table.catalog != null && table.catalog.isNotBlank()) {
                append("${table.catalog}$separator")
            }
            if (table.schema != null && table.schema.isNotBlank()) {
                append("${table.schema}$separator")
            }

            append("${table.tableName}$separator")
        }

        append(name)
    }

    /**
     * Indicates whether some other object is "equal to" this column.
     * Two columns are equal only if they are the same instance.
     */
    override fun equals(other: Any?): Boolean {
        return this === other
    }

    /**
     * Return a hash code value for this column.
     */
    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}
