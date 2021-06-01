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

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.reflections.Reflections
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

/**
 * An factory to register TypeAdapter and Column.
 */
public object TypeAdapterFactory {
    private val adapterMap = ConcurrentSkipListMap<Int, MutableSet<TypeAdapter<*>>>()

    init {
        scanPackage(TypeAdapterFactory::class.java.`package`.name + ".typeadapter")
    }

    private fun getAdapterQueue(priority: Int): MutableCollection<TypeAdapter<*>> {
        var set = adapterMap[-priority]
        if (set == null) {
            synchronized(this) {
                adapterMap[-priority] = CopyOnWriteArraySet()
                set = adapterMap[-priority]!!
            }
        }
        return set!!
    }

    /**
     * 扫描包并注册适配器.
     */
    public fun scanPackage(pkg: String) {
        Reflections(pkg).getSubTypesOf(TypeAdapter::class.java).forEach { jClazz ->
            try {
                val clazz = jClazz.kotlin
                if (clazz.isSubclassOf(TypeAdapter::class)) {
                    @Suppress("UNCHECKED_CAST")
                    val adapter = InstantAllocator[clazz] as TypeAdapter<*>
                    registerAdapter(adapter)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 注册适配器实例.
     */
    public fun registerAdapter(adapter: TypeAdapter<*>) {
        getAdapterQueue(adapter.priority).add(adapter)
    }

    public fun register(
        table: BaseTable<*>,
        field: KProperty1<*, *>,
    ): Column<*>? {
        adapterMap.forEach { (_, queue) ->
            queue.forEach {
                @Suppress("UNCHECKED_CAST")
                val column = it.register(table as BaseTable<Any>, field as KProperty1<Any, Nothing>)
                if (column != null) {
                    return column
                }
            }
        }
        return null
    }

    override fun toString(): String = adapterMap.toString()
}
