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

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.expression.TableExpression
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmErasure

/**
 * Base class of Ktorm's table objects, represents relational tables in the database.
 *
 * @property tableName the table's name.
 * @property alias the table's alias.
 */
@Suppress("UNCHECKED_CAST")
open class Table<E : Entity<E>>(
    val tableName: String,
    val alias: String? = null,
    entityClass: KClass<E>? = null
) : TypeReference<E>() {

    private val _refCounter = AtomicInteger()
    private val _columns = LinkedHashMap<String, Column<*>>()
    private var _primaryKeyName: String? = null

    /**
     * The entity class this table is bound to.
     */
    val entityClass: KClass<E>? =
        (entityClass ?: referencedKotlinType.jvmErasure as KClass<E>).takeIf { it != Nothing::class }

    /**
     * Return all columns of the table.
     */
    val columns: List<Column<*>> get() = _columns.values.toList()

    /**
     * The primary key column of this table.
     */
    val primaryKey: Column<*>? get() = _primaryKeyName?.let { this[it] }

    /**
     * Obtain a column from this table by the name.
     */
    operator fun get(columnName: String): Column<*> {
        return _columns[columnName] ?: throw NoSuchElementException(columnName)
    }

    /**
     * Return a new-created table object with all properties (including the table name and columns and so on) being
     * copied from this table, but applying a new alias given by the parameter.
     *
     * Usually, table objects are defined as Kotlin singleton objects or subclasses extending from [Table]. But limited
     * to the Kotlin language, although this function can create a copied table object with a specific alias, it's
     * return type cannot be the same as the caller's type but only [Table].
     *
     * So we recommend that if we need to use table aliases, please don't define tables as Kotlin's singleton objects,
     * please use classes instead, and override this [aliased] function to return the same type as the concrete table
     * classes.
     *
     * More details can be found in our website: https://ktorm.liuwj.me/en/joining.html#Self-Joining-amp-Table-Aliases
     */
    open fun aliased(alias: String): Table<E> {
        val result = Table(tableName, alias, entityClass)
        result.rewriteDefinitions(columns, _primaryKeyName, copyReferences = true)
        return result
    }

    private fun rewriteDefinitions(columns: List<Column<*>>, primaryKeyName: String?, copyReferences: Boolean) {
        _primaryKeyName = primaryKeyName
        _columns.clear()

        if (copyReferences) {
            _refCounter.set(0)
        }

        for (column in columns) {
            val binding = column.binding

            val newBinding = if (copyReferences && binding is ReferenceBinding) {
                binding.copy(referenceTable = copyReference(binding.referenceTable))
            } else {
                binding
            }

            when (column) {
                is SimpleColumn -> {
                    _columns[column.name] = column.copy(table = this, binding = newBinding)
                }
                is AliasedColumn -> {
                    val col = column as AliasedColumn<Any>
                    val originColumn = col.originColumn.copy(table = this)
                    _columns[col.alias] = col.copy(originColumn = originColumn, binding = newBinding)
                }
            }
        }
    }

    private fun copyReference(table: Table<*>): Table<*> {
        val copy = table.aliased("_ref${_refCounter.getAndIncrement()}")

        val columns = copy.columns.map { column ->
            val binding = column.binding
            if (binding !is ReferenceBinding) {
                column
            } else {
                val newBinding = binding.copy(referenceTable = copyReference(binding.referenceTable))
                when (column) {
                    is SimpleColumn -> column.copy(binding = newBinding)
                    is AliasedColumn -> column.copy(binding = newBinding)
                }
            }
        }

        copy.rewriteDefinitions(columns, copy._primaryKeyName, copyReferences = false)
        return copy
    }

    /**
     * Register a column to this table with the given [name] and [sqlType].
     *
     * This function returns a [ColumnRegistration], we can perform more modificiation to the registered column by
     * calling the returned object, such as configure a binding, mark it as the primary key, and so on.
     */
    fun <C : Any> registerColumn(name: String, sqlType: SqlType<C>): ColumnRegistration<C> {
        if (name in _columns) {
            throw IllegalArgumentException("Duplicate column name: $name")
        }

        _columns[name] = SimpleColumn(this, name, sqlType)
        return ColumnRegistration(name)
    }

    /**
     * Create a copied [AliasedColumn] of the current column with a specific [alias] and register it to the table.
     * This function is designed to bind a column to multiple bindings.
     *
     * This function returns a [ColumnRegistration], then we can bind the new-created column to any other property,
     * and the origin column's binding is not influenced, that’s the way Ktorm supports multiple bindings on a column.
     *
     * The generated SQL is like: `select name as label, name as label1 from dual`.
     *
     * Note that aliased bindings are only available for query operations, they will be ignored when inserting or
     * updating entities.
     *
     * @see AliasedColumn
     */
    fun <C : Any> Column<C>.aliased(alias: String): ColumnRegistration<C> {
        if (alias in _columns) {
            throw IllegalArgumentException("Duplicate column name: $alias")
        }
        if (this !is SimpleColumn) {
            throw IllegalArgumentException("The aliased function is only available on a SimpleColumn.")
        }

        _columns[alias] = AliasedColumn(this.copy(binding = null), alias)
        return ColumnRegistration(alias)
    }

    /**
     * Wrap a new registered column, providing more operations, such as configure a binding, mark it as
     * the primary key, and so on.
     *
     * This class implements the [ReadOnlyProperty] interface, so it can be used as a property delegate,
     * and this delegate returns the registered column. For example:
     *
     * ```kotlin
     * val foo by registerColumn("foo", IntSqlType)
     * ```
     *
     * Here, the `registerColumn` function returns a `ColumnRegistration<Int>` and the `foo` property's
     * type is `Column<Int>`.
     */
    inner class ColumnRegistration<C : Any>(private val key: String) : ReadOnlyProperty<Table<E>, Column<C>> {

        /**
         * Current table object.
         */
        @PublishedApi
        internal val table = this@Table

        /**
         * Return the registered column.
         */
        override operator fun getValue(thisRef: Table<E>, property: KProperty<*>): Column<C> {
            check(thisRef === this@Table)
            return getColumn()
        }

        /**
         * Return the registered column.
         */
        fun getColumn(): Column<C> {
            val column = _columns[key] ?: throw NoSuchElementException(key)
            return column as Column<C>
        }

        /**
         * Mark the registered column as the primary key.
         */
        fun primaryKey(): ColumnRegistration<C> {
            if (getColumn() is AliasedColumn) {
                throw UnsupportedOperationException("Cannot set aliased column $key as a primary key.")
            }

            _primaryKeyName = key
            return this
        }

        /**
         * Bind the column to a reference table, equivalent to a foreign key in relational databases.
         * Entity finding functions would automatically left join all references (recursively) by default.
         *
         * @param referenceTable the reference table, will be copied by calling its [aliased] function with
         * an alias like `_refN`.
         *
         * @param selector a lambda in which we should return the property used to hold the referenced entities.
         * For exmaple: `val departmentId by int("department_id").references(Departments) { it.department }`.
         *
         * @return this column registration.
         *
         * @see me.liuwj.ktorm.entity.joinReferencesAndSelect
         * @see me.liuwj.ktorm.entity.createEntity
         */
        inline fun <R : Entity<R>> references(referenceTable: Table<R>, selector: (E) -> R?): ColumnRegistration<C> {
            val properties = detectBindingProperties(selector)

            if (properties.size > 1) {
                throw IllegalArgumentException("Reference binding doesn't support nested properties.")
            } else {
                return doBind(ReferenceBinding(referenceTable, properties[0]))
            }
        }

        /**
         * Bind the column to nested properties, eg. `employee.manager.department.id`.
         *
         * @param selector a lambda in which we should return the property we want to bind.
         * For example: `val name by varchar("name").bindTo { it.name }`.
         *
         * @return this column registration.
         */
        inline fun bindTo(selector: (E) -> C?): ColumnRegistration<C> {
            val properties = detectBindingProperties(selector)
            return doBind(NestedBinding(properties))
        }

        @PublishedApi
        internal inline fun detectBindingProperties(selector: (E) -> Any?): List<KProperty1<*, *>> {
            val entityClass = table.entityClass ?: error("No entity class configured for table: ${table.tableName}")
            val properties = ArrayList<KProperty1<*, *>>()

            val proxy = ColumnBindingHandler.createProxy(entityClass, properties)
            selector(proxy as E)

            if (properties.isEmpty()) {
                throw IllegalArgumentException("No binding properties found.")
            } else {
                return properties
            }
        }

        @PublishedApi
        internal fun doBind(binding: ColumnBinding): ColumnRegistration<C> {
            val checkedBinding = when (binding) {
                is NestedBinding -> binding
                is ReferenceBinding -> {
                    checkCircularReference(binding.referenceTable)
                    ReferenceBinding(copyReference(binding.referenceTable), binding.onProperty)
                }
            }

            val column = _columns[key] ?: throw NoSuchElementException(key)

            _columns[key] = when (column) {
                is SimpleColumn -> column.copy(binding = checkedBinding)
                is AliasedColumn -> column.copy(binding = checkedBinding)
            }

            return this
        }

        private fun checkCircularReference(root: Table<*>, stack: LinkedList<String> = LinkedList()) {
            stack.push(root.tableName)

            if (tableName == root.tableName) {
                val msg = "Circular reference detected, current table: %s, reference route: %s"
                throw IllegalArgumentException(msg.format(tableName, stack.asReversed()))
            }

            for (column in root.columns) {
                val ref = column.referenceTable
                if (ref != null) {
                    checkCircularReference(ref, stack)
                }
            }

            stack.pop()
        }
    }

    /**
     * Convert this table to a [TableExpression].
     */
    fun asExpression(): TableExpression {
        return TableExpression(tableName, alias)
    }

    /**
     * Return a string representation of this table.
     */
    override fun toString(): String {
        if (alias == null) {
            return "table $tableName"
        } else {
            return "table $tableName $alias"
        }
    }

    /**
     * Indicates whether some other object is "equal to" this table.
     * Two tables are equal only if they are the same instance.
     */
    final override fun equals(other: Any?): Boolean {
        return this === other
    }

    /**
     * Return a hash code value for this table.
     */
    final override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}
