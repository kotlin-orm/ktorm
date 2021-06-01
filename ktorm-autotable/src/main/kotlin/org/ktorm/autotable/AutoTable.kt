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

package org.ktorm.autotable

import org.ktorm.autotable.annotations.TableField
import org.ktorm.dsl.QueryRowSet
import org.ktorm.logging.Logger
import org.ktorm.logging.detectLoggerImplementation
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Simple create an ktorm BaseTable by entity class.
 */
@Suppress("unused", "LabeledExpression")
public open class AutoTable<T : Any> private constructor(
    entityClass: KClass<T>,
    tableName: String = entityClass.tableName,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null,
    private val unsafe: Boolean = true,
    private val fieldMap: Map<String, KProperty1<out T, *>>,
) : BaseTable<T>(tableName, alias, catalog, schema, entityClass) {
    public constructor(
        entityClass: KClass<T>,
        tableName: String = entityClass.tableName,
        alias: String? = null,
        catalog: String? = null,
        schema: String? = null,
        unsafe: Boolean = true,
    ) : this(
        entityClass, tableName, alias, catalog, schema, unsafe,
        entityClass.memberProperties.associateBy { it.tableFieldName },
    ) {
        entityClass.memberProperties.forEach {
            val field = it.javaField ?: return@forEach
            val tableField: TableField? = field.getAnnotation(TableField::class.java)
            if (tableField?.exist == false) return@forEach
            TypeAdapterFactory.register(this, it)
        }
    }

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): T {
        val instance = InstantAllocator[entityClass!!.java, unsafe]
        columns.forEach {
            val field = fieldMap[it.name] ?: return@forEach
            @Suppress("UNCHECKED_CAST")
            row[it]?.inject(instance, field as KProperty1<Any, Any?>)
        }
        return instance
    }

    /**
     * rebuild column map.
     */
    public fun rebuild() {
        val columnsField = BaseTable::class.java.getDeclaredField("_columns")
        columnsField.isAccessible = true
        (columnsField.get(this) as MutableMap<*, *>).clear()

        entityClass!!.memberProperties.forEach {
            try {
                val field = it.javaField ?: return@forEach
                val tableField: TableField? = field.getAnnotation(TableField::class.java)
                if (tableField?.exist == false) return@forEach
                TypeAdapterFactory.register(this, it)
            } catch (e: Exception) {
                logger.warn("an exception caused on rebuild property $it", e)
            }
        }
    }

    /**
     * get column by property.
     */
    @Suppress("UNCHECKED_CAST")
    public operator fun <R : Any> get(property: KProperty1<out T, R?>): Column<R> =
        this[property.tableFieldName] as Column<R>

    /**
     * get column by property.
     */
    @Suppress("UNCHECKED_CAST")
    public operator fun <R : Any> get(property: KProperty<R?>): Column<R> =
        this[property.tableFieldName] as Column<R>

    /**
     * create table field.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <V : Any> field(): FieldDelegation<T, V> = fieldProxyInstance as FieldDelegation<T, V>

    /**
     * create table field proxy by property.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <V : Any> field(
        property: KProperty0<*>,
    ): Column<V> = this[property.tableFieldName] as Column<V>

    /**
     * clone AutoTable instance.
     */
    public fun clone(
        entityClass: KClass<T> = super.entityClass!!,
        tableName: String = super.tableName,
        alias: String? = super.alias,
        catalog: String? = super.catalog,
        schema: String? = super.schema,
        unsafe: Boolean = this.unsafe,
    ): AutoTable<T> {
        val autoTable = AutoTable(
            entityClass,
            tableName,
            alias,
            catalog,
            schema,
            unsafe,
            fieldMap,
        )

        val columnsField = BaseTable::class.java.getDeclaredField("_columns")
        columnsField.isAccessible = true
        columnsField.set(autoTable, columnsField.get(this))

        return autoTable
    }

    public companion object {
        private val logger: Logger = detectLoggerImplementation()

        /**
         * get AutoTable instance.
         */
        public operator fun <T : Any> get(clazz: KClass<T>): AutoTable<T> =
            get(clazz.java)

        /**
         * get AutoTable instance.
         */
        public operator fun <T : Any> get(clazz: Class<T>): AutoTable<T> {
            var autoTable = autoTableMap[clazz]
            if (autoTable == null) {
                synchronized(autoTableMap) {
                    autoTable = autoTableMap[clazz]
                    if (autoTable == null) {
                        autoTable = AutoTable(clazz.kotlin)
                        @Suppress("UNCHECKED_CAST")
                        autoTableMap[clazz] = autoTable as AutoTable<T>
                    }
                }
            }
            @Suppress("UNCHECKED_CAST")
            return autoTable as AutoTable<T>
        }

        /**
         * get AutoTable instance.
         */
        public inline operator fun <reified T : Any> invoke(): AutoTable<T> = get(T::class.java)

        /**
         * table field delegation provider.
         */
        public class FieldDelegation<T : Any, V : Any> {
            /**
             * kotlin delegation operator to get value.
             */
            @Suppress("UNCHECKED_CAST")
            public operator fun getValue(
                autoTable: AutoTable<T>,
                property: KProperty<*>,
            ): Column<V> = autoTable[property.tableFieldName] as Column<V>
        }

        private val fieldProxyInstance = FieldDelegation<Any, Any>()
        private val autoTableMap = ConcurrentHashMap<Class<*>, AutoTable<*>>()
    }
}
