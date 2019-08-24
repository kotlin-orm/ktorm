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

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.expression.TableExpression
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure

/**
 * Base class of Ktorm's table objects, represents relational tables in the database.
 *
 * This class provides the basic ability of table and column definition but doesn't support any binding mechanisms.
 * If you need the binding support to [Entity] interfaces, use [Table] instead.
 *
 * There is an abstract function [doCreateEntity]. Subclasses should implement this function, creating an entity object
 * from the result set returned by a query, using the binding rules defined by themselves. Here, the type of the entity
 * object could be an interface extending from [Entity], or a data class, POJO, or any kind of classes.
 *
 * Here is an example defining an entity as data class. The full documentation can be found at:
 * https://ktorm.liuwj.me/en/define-entities-as-any-kind-of-classes.html
 *
 * ```kotlin
 * data class Staff(
 *     val id: Int,
 *     val name: String,
 *     val job: String,
 *     val hireDate: LocalDate
 * )
 * object Staffs : BaseTable<Staff>("t_employee") {
 *     val id by int("id").primaryKey()
 *     val name by varchar("name")
 *     val job by varchar("job")
 *     val hireDate by date("hire_date")
 *     override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Staff(
 *         id = row[id] ?: 0,
 *         name = row[name].orEmpty(),
 *         job = row[job].orEmpty(),
 *         hireDate = row[hireDate] ?: LocalDate.now()
 *     )
 * }
 * ```
 *
 * @since 2.5
 */
@Suppress("CanBePrimaryConstructorProperty", "UNCHECKED_CAST")
abstract class BaseTable<E : Any>(
    tableName: String,
    alias: String? = null,
    entityClass: KClass<E>? = null
) : TypeReference<E>() {

    private val _refCounter = AtomicInteger()
    private val _columns = LinkedHashMap<String, Column<*>>()
    private var _primaryKeyName: String? = null

    /**
     * The table's name.
     */
    val tableName: String = tableName

    /**
     * The table's alias.
     */
    val alias: String? = alias

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
    operator fun get(name: String): Column<*> {
        return _columns[name] ?: throw NoSuchElementException(name)
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
     * The generated SQL is like: `select name as label, name as aliased_label from dual`.
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
     * Mark the registered column as the primary key.
     */
    fun <C : Any> ColumnRegistration<C>.primaryKey(): ColumnRegistration<C> {
        val column = getColumn()
        if (column is AliasedColumn) {
            throw UnsupportedOperationException("Cannot set aliased column ${column.alias} as a primary key.")
        }

        _primaryKeyName = column.name
        return this
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
    inner class ColumnRegistration<C : Any>(private val key: String) : ReadOnlyProperty<BaseTable<E>, Column<C>> {

        /**
         * Return the registered column.
         */
        override operator fun getValue(thisRef: BaseTable<E>, property: KProperty<*>): Column<C> {
            check(thisRef === this@BaseTable)
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
         * Configure the binding of the registered column. Note that this function is only used internally by the Ktorm
         * library and its extension modules. Others should not use this function directly.
         */
        fun doBindInternal(binding: ColumnBinding): ColumnRegistration<C> {
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

        private fun checkCircularReference(root: BaseTable<*>, stack: LinkedList<String> = LinkedList()) {
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
     * Return a new-created table object with all properties (including the table name and columns and so on) being
     * copied from this table, but applying a new alias given by the parameter.
     *
     * Usually, table objects are defined as Kotlin singleton objects or subclasses extending from [Table]. But limited
     * to the Kotlin language, although the default implementation of this function can create a copied table object
     * with a specific alias, it's return type cannot be the same as the caller's type but only [Table].
     *
     * So we recommend that if we need to use table aliases, please don't define tables as Kotlin's singleton objects,
     * please use classes instead, and override this [aliased] function to return the same type as the concrete table
     * classes.
     *
     * More details can be found on our website: https://ktorm.liuwj.me/en/joining.html#Self-Joining-amp-Table-Aliases
     */
    open fun aliased(alias: String): BaseTable<E> {
        throw UnsupportedOperationException("The function 'aliased' is not supported by $javaClass")
    }

    /**
     * Copy column definitions from [src] to this table.
     */
    protected fun copyDefinitionsFrom(src: BaseTable<*>) {
        rewriteDefinitions(src.columns, src._primaryKeyName, copyReferences = true)
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

    private fun copyReference(table: BaseTable<*>): BaseTable<*> {
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
     * Create an entity object from the specific row of query results.
     *
     * This function uses the binding configurations of this table object, filling columns' values into corresponding
     * entities' properties. And if there are any reference bindings to other tables, it will also create the referenced
     * entity objects recursively.
     */
    fun createEntity(row: QueryRowSet): E {
        val entity = doCreateEntity(row, withReferences = true)

        val logger = Database.global.logger
        if (logger != null && logger.isTraceEnabled()) {
            logger.trace("Entity: $entity")
        }

        return entity
    }

    /**
     * Create an entity object from the specific row without obtaining referenced entities' data automatically.
     *
     * Similar to [createEntity], this function uses the binding configurations of this table object, filling
     * columns' values into corresponding entities' properties. But differently, it treats all reference bindings
     * as nested bindings to the referenced entities’ primary keys.
     *
     * For example the binding `c.references(Departments) { it.department }`, it is equivalent to
     * `c.bindTo { it.department.id }` for this function, that avoids unnecessary object creations and
     * some exceptions raised by conflict column names.
     */
    fun createEntityWithoutReferences(row: QueryRowSet): E {
        val entity = doCreateEntity(row, withReferences = false)

        val logger = Database.global.logger
        if (logger != null && logger.isTraceEnabled()) {
            logger.trace("Entity: $entity")
        }

        return entity
    }

    /**
     * Create an entity object from the specific row of query results.
     *
     * This function is called by [createEntity] and [createEntityWithoutReferences]. Subclasses should override it
     * and implement the actual logic of retrieving an entity object from the query results.
     */
    protected abstract fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): E

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
