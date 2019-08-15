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

import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.entity.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmErasure

/**
 * Base class of Ktorm's table objects. This class extends from [BaseTable] and supports bindings to [Entity] classes.
 */
@Suppress("UNCHECKED_CAST")
open class Table<E : Entity<E>>(
    tableName: String,
    alias: String? = null,
    entityClass: KClass<E>? = null
) : BaseTable<E>(tableName, alias, entityClass) {

    /**
     * Bind the column to nested properties, eg. `employee.manager.department.id`.
     *
     * @param selector a lambda in which we should return the property we want to bind.
     * For example: `val name by varchar("name").bindTo { it.name }`.
     *
     * @return this column registration.
     */
    inline fun <C : Any> ColumnRegistration<C>.bindTo(selector: (E) -> C?): ColumnRegistration<C> {
        val properties = detectBindingProperties(selector)
        return doBindInternal(NestedBinding(properties))
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
     * @see createEntity
     */
    inline fun <C : Any, R : Entity<R>> ColumnRegistration<C>.references(
        referenceTable: Table<R>,
        selector: (E) -> R?
    ): ColumnRegistration<C> {
        val properties = detectBindingProperties(selector)

        if (properties.size > 1) {
            throw IllegalArgumentException("Reference binding doesn't support nested properties.")
        } else {
            return doBindInternal(ReferenceBinding(referenceTable, properties[0]))
        }
    }

    @PublishedApi
    internal inline fun detectBindingProperties(selector: (E) -> Any?): List<KProperty1<*, *>> {
        val entityClass = this.entityClass ?: error("No entity class configured for table: $tableName")
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
        val result = Table(tableName, alias, entityClass)
        result.copyDefinitionsFrom(this)
        return result
    }

    final override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): E {
        val entityClass = this.entityClass ?: error("No entity class configured for table: $tableName")
        val entity = Entity.create(entityClass, fromTable = this) as E

        for (column in columns) {
            try {
                row.retrieveColumn(column, intoEntity = entity, withReferences = withReferences)
            } catch (e: Throwable) {
                throw IllegalStateException("Error retrieving column: $column, binding: ${column.binding}", e)
            }
        }

        return entity.apply { clearChangesRecursively() }
    }

    private fun QueryRowSet.retrieveColumn(column: Column<*>, intoEntity: E, withReferences: Boolean) {
        val columnValue = this[column] ?: return
        val binding = column.binding ?: return

        when (binding) {
            is ReferenceBinding -> {
                val refTable = binding.referenceTable as Table<*>
                val primaryKey = refTable.primaryKey ?: error("Table ${refTable.tableName} doesn't have a primary key.")

                if (withReferences) {
                    val child = refTable.doCreateEntity(this, withReferences = true)
                    child.implementation.setColumnValue(primaryKey, columnValue, forceSet = true)
                    intoEntity[binding.onProperty.name] = child
                } else {
                    val child = Entity.create(binding.onProperty.returnType.jvmErasure, fromTable = refTable)
                    child.implementation.setColumnValue(primaryKey, columnValue)
                    intoEntity[binding.onProperty.name] = child
                }
            }
            is NestedBinding -> {
                intoEntity.implementation.setColumnValue(column, columnValue)
            }
        }
    }
}
