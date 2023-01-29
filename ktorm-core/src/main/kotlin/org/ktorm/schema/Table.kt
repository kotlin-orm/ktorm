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
import org.ktorm.entity.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmErasure

/**
 * Base class of Ktorm's table objects. This class extends from [BaseTable], additionally providing a binding mechanism
 * with [Entity] interfaces based on functions such as [bindTo], [references].
 *
 * [Table] implements the [doCreateEntity] function from the parent class. The function automatically creates an
 * entity object using the binding configuration specified by [bindTo] and [references], reading columns’ values from
 * the result set and filling them into corresponding entity properties.
 *
 * To use this class, we need to define our entities as interfaces extending from [Entity]. Here is an example. More
 * documents can be found at https://www.ktorm.org/en/entities-and-column-binding.html
 *
 * ```kotlin
 * interface Department : Entity<Department> {
 *    val id: Int
 *    var name: String
 *    var location: String
 * }
 * object Departments : Table<Department>("t_department") {
 *    val id = int("id").primaryKey().bindTo { it.id }
 *    val name = varchar("name").bindTo { it.name }
 *    val location = varchar("location").bindTo { it.location }
 * }
 * ```
 */
@Suppress("UNCHECKED_CAST")
public open class Table<E : Entity<E>>(
    tableName: String,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null,
    entityClass: KClass<E>? = null
) : BaseTable<E>(tableName, alias, catalog, schema, entityClass) {

    /**
     * Bind the column to nested properties, eg. `employee.manager.department.id`.
     *
     * Note: Since [Column] is immutable, this function will create a new [Column] instance and replace the origin
     * registered one.
     *
     * @param selector a lambda in which we should return the property we want to bind.
     * For example: `val name = varchar("name").bindTo { it.name }`.
     *
     * @return the new [Column] instance.
     */
    public inline fun <C : Any> Column<C>.bindTo(selector: (E) -> C?): Column<C> {
        val properties = detectBindingProperties(selector)
        return doBindInternal(NestedBinding(properties))
    }

    /**
     * Bind the column to a reference table, equivalent to a foreign key in relational databases.
     * Entity sequence APIs would automatically left-join all references (recursively) by default.
     *
     * Note: Since [Column] is immutable, this function will create a new [Column] instance and replace
     * the origin registered one.
     *
     * @param referenceTable the reference table, will be copied by calling its [aliased] function with
     * an alias like `_refN`.
     *
     * @param selector a lambda in which we should return the property used to hold the referenced entities.
     * For example: `val departmentId = int("department_id").references(Departments) { it.department }`.
     *
     * @return the new [Column] instance.
     *
     * @see org.ktorm.entity.sequenceOf
     * @see createEntity
     */
    public inline fun <C : Any, R : Entity<R>> Column<C>.references(
        referenceTable: Table<R>,
        selector: (E) -> R?
    ): Column<C> {
        val properties = detectBindingProperties(selector)

        if (properties.size > 1) {
            throw IllegalArgumentException("Reference binding doesn't support nested properties.")
        } else {
            return doBindInternal(ReferenceBinding(referenceTable, properties[0]))
        }
    }

    @PublishedApi
    internal inline fun detectBindingProperties(selector: (E) -> Any?): List<KProperty1<*, *>> {
        val entityClass = this.entityClass ?: error("No entity class configured for table: '$this'")
        val properties = ArrayList<KProperty1<*, *>>()

        val proxy = ColumnBindingHandler.createProxy(entityClass, properties)
        selector(proxy as E)

        if (properties.isEmpty()) {
            throw IllegalArgumentException("No binding properties found.")
        } else {
            return properties
        }
    }

    override fun aliased(alias: String): Table<E> {
        val result = Table(tableName, alias, catalog, schema, entityClass)
        result.copyDefinitionsFrom(this)
        return result
    }

    final override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): E {
        val entityClass = this.entityClass ?: error("No entity class configured for table: '$this'")
        val entity = Entity.create(entityClass, fromDatabase = row.query.database, fromTable = this) as E

        for (column in columns) {
            row.retrieveColumn(column, intoEntity = entity, withReferences = withReferences)
        }

        return entity.apply { clearChangesRecursively() }
    }

    private fun QueryRowSet.retrieveColumn(column: Column<*>, intoEntity: E, withReferences: Boolean) {
        val columnValue = this[column] ?: return

        for (binding in column.allBindings) {
            when (binding) {
                is ReferenceBinding -> {
                    val refTable = binding.referenceTable as Table<*>
                    val pk = refTable.singlePrimaryKey {
                        "Cannot reference the table '$refTable' as there is compound primary keys."
                    }

                    if (withReferences) {
                        val child = refTable.doCreateEntity(this, withReferences = true)
                        child.implementation.setColumnValue(pk, columnValue, forceSet = true)
                        intoEntity[binding.onProperty.name] = child
                    } else {
                        val entityClass = binding.onProperty.returnType.jvmErasure
                        val child = Entity.create(entityClass, fromDatabase = query.database, fromTable = refTable)
                        child.implementation.setColumnValue(pk, columnValue)
                        intoEntity[binding.onProperty.name] = child
                    }
                }
                is NestedBinding -> {
                    intoEntity.implementation.setColumnValue(binding, columnValue)
                }
            }
        }
    }

    private fun EntityImplementation.setColumnValue(column: Column<*>, value: Any?, forceSet: Boolean = false) {
        for (binding in column.allBindings) {
            this.setColumnValue(binding, value, forceSet)
        }
    }
}
