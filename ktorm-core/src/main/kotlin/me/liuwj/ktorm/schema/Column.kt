/*
 * Copyright 2018-2019 the original author or authors.
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

package me.liuwj.ktorm.schema

import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.ColumnDeclaringExpression
import me.liuwj.ktorm.expression.ColumnExpression
import me.liuwj.ktorm.expression.ScalarExpression
import kotlin.reflect.KProperty1

/**
 * Base class of column bindings. A column might be bound to a simple property, nested properties,
 * or a reference to another table.
 */
sealed class ColumnBinding

/**
 * Bind the column to nested properties, eg. `employee.manager.department.id`.
 *
 * @property properties the nested properties, cannot be empty.
 */
data class NestedBinding(val properties: List<KProperty1<*, *>>) : ColumnBinding()

/**
 * Bind the column to a reference table, equivalent to a foreign key in relational databases.
 * Entity finding functions would automatically left join all references (recursively) by default.
 *
 * @property referenceTable the reference table.
 * @property onProperty the property used to hold the referenced entity object.
 * @see me.liuwj.ktorm.entity.joinReferencesAndSelect
 * @see me.liuwj.ktorm.entity.createEntity
 */
data class ReferenceBinding(val referenceTable: Table<*>, val onProperty: KProperty1<*, *>) : ColumnBinding()

/**
 * Common interface of [Column] and [ScalarExpression].
 */
interface ColumnDeclaring<T : Any> {

    /**
     * The [SqlType] of this column or expression.
     */
    val sqlType: SqlType<T>

    /**
     * Convert this instance to a [ScalarExpression].
     *
     * If this instance is a [Column], return a [ColumnExpression], otherwise if it's already a [ScalarExpression],
     * return `this` directly.
     */
    fun asExpression(): ScalarExpression<T>

    /**
     * Wrap this instance as a [ColumnDeclaringExpression].
     *
     * This function is useful when we use columns or expressions as the selected columns in a query. If this instance
     * is a [Column], a label identifying the selected columns will be set to [ColumnDeclaringExpression.declaredName],
     * otherwise if it's a [ScalarExpression], the property will be set to null.
     */
    fun asDeclaringExpression(): ColumnDeclaringExpression

    /**
     * Wrap the given [argument] as an [ArgumentExpression] using the [sqlType].
     */
    fun wrapArgument(argument: T?): ArgumentExpression<T>
}

/**
 * Represents database columns.
 */
sealed class Column<T : Any> : ColumnDeclaring<T> {

    /**
     * The [Table] that this column belongs to.
     */
    abstract val table: Table<*>

    /**
     * The column's name.
     */
    abstract val name: String

    /**
     * The column's label, used to identify the selected columns and obtain query results.
     *
     * For example, `select a.name as label from dual`, in which `a.name as label` is a column declaring, and `label`
     * is the label.
     *
     * @see ColumnDeclaringExpression
     */
    abstract val label: String

    /**
     * The column's binding. A column might be bound to a simple property, nested properties,
     * or a reference to another table, null if the column doesn't bind to any property.
     */
    abstract val binding: ColumnBinding?

    /**
     * If the column was bound to a reference table, return the table, otherwise return null.
     *
     * Shortcut for `(binding as? ReferenceBinding)?.referenceTable`.
     */
    val referenceTable: Table<*>? get() = (binding as? ReferenceBinding)?.referenceTable

    /**
     * Convert this column to a [ColumnExpression].
     */
    override fun asExpression(): ColumnExpression<T> {
        return ColumnExpression(table.alias ?: table.tableName, name, sqlType)
    }

    /**
     * Wrap this column as a [ColumnDeclaringExpression].
     */
    override fun asDeclaringExpression(): ColumnDeclaringExpression {
        return ColumnDeclaringExpression(expression = asExpression(), declaredName = label)
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
        return "${table.alias ?: table.tableName}.$name"
    }

    /**
     * Indicates whether some other object is "equal to" this column.
     * Two columns are equal only if they are the same instance.
     */
    final override fun equals(other: Any?): Boolean {
        return this === other
    }

    /**
     * Return a hash code value for this column.
     */
    final override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}

/**
 * Simple implementation of [Column].
 */
data class SimpleColumn<T : Any>(
    override val table: Table<*>,
    override val name: String,
    override val sqlType: SqlType<T>,
    override val binding: ColumnBinding? = null
) : Column<T>() {

    override val label: String = "${table.alias ?: table.tableName}_$name"

    override fun toString(): String {
        return "${table.alias ?: table.tableName}.$name"
    }
}

/**
 * Aliased column, wrapping a [SimpleColumn] and an extra [alias] to modify the lable, designed to bind a column to
 * multiple bindings.
 *
 * Ktorm provides an `aliased` function to create a copy of an existing column with a specific alias, then we can bind
 * this new-created column to any property we want, and the origin column's binding is not influenced, that’s the way
 * Ktorm supports multiple bindings on a column.
 *
 * The generated SQL is like: `select name as label, name as label1 from dual`.
 *
 * Note that aliased bindings are only available for queries, they will be ignored when inserting or updating entities.
 *
 * @property originColumn the origin column.
 * @property alias the alias used to modify the origin column's label.
 */
data class AliasedColumn<T : Any>(
    val originColumn: SimpleColumn<T>,
    val alias: String,
    override val binding: ColumnBinding? = null
) : Column<T>() {

    override val table: Table<*> = originColumn.table

    override val name: String = originColumn.name

    override val label: String = "${table.alias ?: table.tableName}_$alias"

    override val sqlType: SqlType<T> = originColumn.sqlType

    override fun toString(): String {
        return "$originColumn as $alias"
    }
}
