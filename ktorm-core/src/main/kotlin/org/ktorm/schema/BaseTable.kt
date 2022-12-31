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

import org.ktorm.dsl.QueryRowSet
import org.ktorm.entity.Entity
import org.ktorm.expression.TableExpression
import java.util.*
import kotlin.collections.LinkedHashSet
import kotlin.reflect.KClass
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
 * https://www.ktorm.org/en/define-entities-as-any-kind-of-classes.html
 *
 * ```kotlin
 * data class Staff(
 *     val id: Int,
 *     val name: String,
 *     val job: String,
 *     val hireDate: LocalDate
 * )
 * object Staffs : BaseTable<Staff>("t_employee") {
 *     val id = int("id").primaryKey()
 *     val name = varchar("name")
 *     val job = varchar("job")
 *     val hireDate = date("hire_date")
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
public abstract class BaseTable<E : Any>(
    tableName: String,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null,
    entityClass: KClass<E>? = null
) : TypeReference<E>() {

    private val _refCounter = RefCounter.getCounter()
    private val _columns = LinkedHashMap<String, Column<*>>()
    private val _primaryKeyNames = LinkedHashSet<String>()

    /**
     * The table's name.
     */
    @Suppress("CanBePrimaryConstructorProperty")
    public val tableName: String = tableName

    /**
     * The table's alias.
     */
    @Suppress("CanBePrimaryConstructorProperty")
    public val alias: String? = alias

    /**
     * The table's catalog.
     *
     * @since 3.1.0
     */
    @Suppress("CanBePrimaryConstructorProperty")
    public val catalog: String? = catalog

    /**
     * The table's schema.
     *
     * @since 3.1.0
     */
    @Suppress("CanBePrimaryConstructorProperty")
    public val schema: String? = schema

    /**
     * The entity class this table is bound to.
     */
    @Suppress("UNCHECKED_CAST")
    public val entityClass: KClass<E>? =
        (entityClass ?: referencedKotlinType.jvmErasure as KClass<E>).takeIf { it != Nothing::class }

    /**
     * Return all columns of the table.
     */
    public val columns: List<Column<*>> get() = _columns.values.toList()

    /**
     * The primary key columns of this table.
     */
    public val primaryKeys: List<Column<*>> get() = _primaryKeyNames.map { this[it] }

    /**
     * Obtain the single primary key or throw an exception.
     */
    internal inline fun singlePrimaryKey(errMsg: () -> String): Column<*> {
        val primaryKeys = primaryKeys
        if (primaryKeys.isEmpty()) {
            error("Table '$this' doesn't have a primary key.")
        }
        if (primaryKeys.size > 1) {
            error(errMsg())
        }
        return primaryKeys[0]
    }

    /**
     * Obtain a column from this table by the name.
     */
    public operator fun get(name: String): Column<*> {
        return _columns[name] ?: throw NoSuchElementException(name)
    }

    /**
     * Register a column to this table with the given [name] and [sqlType].
     *
     * This function returns the registered column, we can perform more modifications to the column such as configure
     * a binding, mark it as a primary key, and so on. But please note that [Column] is immutable, those modification
     * functions will create new [Column] instances and replace the origin registered ones.
     */
    public fun <C : Any> registerColumn(name: String, sqlType: SqlType<C>): Column<C> {
        if (name in _columns) {
            throw IllegalStateException("Duplicate column name: $name")
        }

        val column = Column(this, name, sqlType = sqlType)
        _columns[name] = column
        return column
    }

    /**
     * Mark the registered column as a primary key.
     */
    public fun <C : Any> Column<C>.primaryKey(): Column<C> {
        checkRegistered()
        _primaryKeyNames += name
        return this
    }

    private fun <C : Any> Column<C>.checkRegistered() {
        if (name !in _columns) {
            throw IllegalStateException("The column $name was not registered to table '$this'.")
        }
    }

    private fun <C : Any> Column<C>.checkTransformable() {
        if (binding != null || extraBindings.isNotEmpty()) {
            throw IllegalStateException("Cannot transform a column after its bindings are configured.")
        }
    }

    /**
     * Transform the registered column's [SqlType] to another. The transformed [SqlType] has the same `typeCode` and
     * `typeName` as the underlying one, and performs the specific transformations on column values.
     *
     * This enables a user-friendly syntax to extend more data types. For example, the following code defines a column
     * of type `Column<UserRole>`, based on the existing column definition function [int]:
     *
     * ```kotlin
     * val role = int("role").transform({ UserRole.fromCode(it) }, { it.code })
     * ```
     *
     * Note: Since [Column] is immutable, this function will create a new [Column] instance and replace the origin
     * registered one.
     *
     * @param fromUnderlyingValue a function that transforms a value of underlying type to the user's type.
     * @param toUnderlyingValue a function that transforms a value of user's type to the underlying type.
     * @return the new [Column] instance with its type changed to [R].
     * @see SqlType.transform
     */
    public fun <C : Any, R : Any> Column<C>.transform(
        fromUnderlyingValue: (C) -> R,
        toUnderlyingValue: (R) -> C
    ): Column<R> {
        checkRegistered()
        checkTransformable()

        val result = Column(table, name, sqlType = sqlType.transform(fromUnderlyingValue, toUnderlyingValue))
        _columns[name] = result
        return result
    }

    /**
     * Configure the binding of the registered column. Note that this function is only used internally by the Ktorm
     * library and its extension modules. Others should not use this function directly.
     */
    @PublishedApi
    internal fun <C : Any> Column<C>.doBindInternal(binding: ColumnBinding): Column<C> {
        checkRegistered()
        checkConflictBinding(binding)

        val b = when (binding) {
            is NestedBinding -> binding
            is ReferenceBinding -> {
                checkReferencePrimaryKey(binding.referenceTable)
                checkCircularReference(binding.referenceTable)
                ReferenceBinding(copyReferenceTable(binding.referenceTable), binding.onProperty)
            }
        }

        val result = if (this.binding == null) this.copy(binding = b) else this.copy(extraBindings = extraBindings + b)
        _columns[name] = result
        return result
    }

    private fun <C : Any> Column<C>.checkConflictBinding(binding: ColumnBinding) {
        for (column in _columns.values) {
            val hasConflict = when (binding) {
                is NestedBinding ->
                    column.allBindings.asSequence()
                        .filterIsInstance<NestedBinding>()
                        .filter { it.properties == binding.properties }
                        .any()
                is ReferenceBinding ->
                    column.allBindings.asSequence()
                        .filterIsInstance<ReferenceBinding>()
                        .filter { it.referenceTable.tableName == binding.referenceTable.tableName }
                        .filter { it.referenceTable.catalog == binding.referenceTable.catalog }
                        .filter { it.referenceTable.schema == binding.referenceTable.schema }
                        .filter { it.onProperty == binding.onProperty }
                        .any()
            }

            if (hasConflict) {
                throw IllegalStateException(
                    "Column '$name' and '${column.name}' are bound to the same property. Please check your code."
                )
            }
        }
    }

    private fun checkReferencePrimaryKey(refTable: BaseTable<*>) {
        val primaryKeys = refTable.primaryKeys
        if (primaryKeys.isEmpty()) {
            throw IllegalStateException(
                "Cannot reference the table '$refTable' because it doesn't have a primary key."
            )
        }
        if (primaryKeys.size > 1) {
            throw IllegalStateException(
                "Cannot reference the table '$refTable' because it has compound primary keys."
            )
        }
    }

    private fun checkCircularReference(root: BaseTable<*>, stack: LinkedList<String> = LinkedList()) {
        stack.push(root.toString(withAlias = false))

        if (tableName == root.tableName && catalog == root.catalog && schema == root.schema) {
            throw IllegalStateException(
                "Circular reference detected, current table: '$this', reference route: ${stack.asReversed()}"
            )
        }

        for (column in root.columns) {
            val ref = column.referenceTable
            if (ref != null) {
                checkCircularReference(ref, stack)
            }
        }

        stack.pop()
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
     * More details can be found on our website: https://www.ktorm.org/en/joining.html#Self-Joining-amp-Table-Aliases
     */
    public open fun aliased(alias: String): BaseTable<E> {
        throw UnsupportedOperationException("The function 'aliased' is not supported by $javaClass")
    }

    /**
     * Copy column definitions from [src] to this table.
     */
    protected fun copyDefinitionsFrom(src: BaseTable<*>) {
        if (_columns.isNotEmpty()) {
            throw IllegalStateException("Cannot update the column definitions since the table is already initialized.")
        }

        _primaryKeyNames.addAll(src._primaryKeyNames)

        for ((name, column) in src._columns) {
            val binding = column.binding?.let { copyBinding(it) }
            val extraBindings = column.extraBindings.map { copyBinding(it) }
            _columns[name] = column.copy(table = this, binding = binding, extraBindings = extraBindings)
        }
    }

    private fun copyReferenceTable(table: BaseTable<*>): BaseTable<*> {
        RefCounter.setContextCounter(_refCounter)
        return table.aliased("_ref${_refCounter.getAndIncrement()}")
    }

    private fun copyBinding(binding: ColumnBinding): ColumnBinding {
        if (binding is ReferenceBinding) {
            return binding.copy(referenceTable = copyReferenceTable(binding.referenceTable))
        } else {
            return binding
        }
    }

    /**
     * Create an entity object from the specific row of query results. This function uses the binding configurations
     * of the table, filling column values into corresponding properties of the returned entity.
     *
     * If the [withReferences] flag is set to true and there are any reference bindings to other tables, this function
     * will create the referenced entity objects by recursively calling [createEntity] itself.
     *
     * Otherwise, if the [withReferences] flag is set to false, it will treat all reference bindings as nested bindings
     * to the referenced entities' primary keys. For example the binding `c.references(Departments) { it.department }`,
     * it is equivalent to `c.bindTo { it.department.id }` in this case, that avoids unnecessary object creations
     * and some exceptions raised by conflict column names.
     */
    public fun createEntity(row: QueryRowSet, withReferences: Boolean = true): E {
        val entity = doCreateEntity(row, withReferences)

        val logger = row.query.database.logger
        if (logger.isTraceEnabled()) {
            logger.trace("Entity: $entity")
        }

        return entity
    }

    /**
     * Create an entity object from the specific row of query results.
     *
     * This function is called by [createEntity]. Subclasses should override it and implement the actual logic of
     * retrieving an entity object from the query results.
     */
    protected abstract fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): E

    /**
     * Convert this table to a [TableExpression].
     */
    public fun asExpression(): TableExpression {
        return TableExpression(tableName, alias, catalog, schema)
    }

    /**
     * Return a string representation of this table.
     */
    override fun toString(): String {
        return toString(withAlias = true)
    }

    private fun toString(withAlias: Boolean) = buildString {
        if (!catalog.isNullOrBlank()) {
            append("$catalog.")
        }
        if (!schema.isNullOrBlank()) {
            append("$schema.")
        }

        append(tableName)

        if (withAlias && !alias.isNullOrBlank()) {
            append(" $alias")
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
