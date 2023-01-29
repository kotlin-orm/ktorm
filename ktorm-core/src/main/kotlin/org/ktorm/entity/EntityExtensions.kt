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

package org.ktorm.entity

import org.ktorm.schema.*
import java.lang.reflect.Proxy
import java.util.*
import kotlin.reflect.jvm.jvmErasure

internal fun EntityImplementation.hasPrimaryKeyValue(fromTable: Table<*>): Boolean {
    val pk = fromTable.singlePrimaryKey { "Table '$fromTable' has compound primary keys." }
    if (pk.binding == null) {
        error("Primary column $pk has no bindings to any entity field.")
    } else {
        return hasColumnValue(pk.binding)
    }
}

internal fun EntityImplementation.hasColumnValue(binding: ColumnBinding): Boolean {
    when (binding) {
        is ReferenceBinding -> {
            if (!this.hasProperty(binding.onProperty)) {
                return false
            }

            val child = this.getProperty(binding.onProperty) as Entity<*>?
            if (child == null) {
                // null is also a legal column value.
                return true
            } else {
                return child.implementation.hasPrimaryKeyValue(binding.referenceTable as Table<*>)
            }
        }
        is NestedBinding -> {
            var curr: EntityImplementation = this

            for ((i, prop) in binding.properties.withIndex()) {
                if (i != binding.properties.lastIndex) {
                    if (!curr.hasProperty(prop)) {
                        return false
                    }

                    val child = curr.getProperty(prop) as Entity<*>?
                    if (child == null) {
                        // null is also a legal column value.
                        return true
                    } else {
                        curr = child.implementation
                    }
                }
            }

            return curr.hasProperty(binding.properties.last())
        }
    }
}

internal fun EntityImplementation.getPrimaryKeyValue(fromTable: Table<*>): Any? {
    val pk = fromTable.singlePrimaryKey { "Table '$fromTable' has compound primary keys." }
    if (pk.binding == null) {
        error("Primary column $pk has no bindings to any entity field.")
    } else {
        return getColumnValue(pk.binding)
    }
}

internal fun EntityImplementation.getColumnValue(binding: ColumnBinding): Any? {
    when (binding) {
        is ReferenceBinding -> {
            val child = this.getProperty(binding.onProperty) as Entity<*>?
            return child?.implementation?.getPrimaryKeyValue(binding.referenceTable as Table<*>)
        }
        is NestedBinding -> {
            var curr: EntityImplementation? = this
            for ((i, prop) in binding.properties.withIndex()) {
                if (i != binding.properties.lastIndex) {
                    val child = curr?.getProperty(prop) as Entity<*>?
                    curr = child?.implementation
                }
            }
            return curr?.getProperty(binding.properties.last())
        }
    }
}

internal fun EntityImplementation.setPrimaryKeyValue(
    fromTable: Table<*>,
    value: Any?,
    forceSet: Boolean = false,
    useExtraBindings: Boolean = false
) {
    val pk = fromTable.singlePrimaryKey { "Table '$fromTable' has compound primary keys." }
    if (pk.binding == null) {
        error("Primary column $pk has no bindings to any entity field.")
    } else {
        setColumnValue(pk.binding, value, forceSet)
    }

    if (useExtraBindings) {
        for (extraBinding in pk.extraBindings) {
            setColumnValue(extraBinding, value, forceSet)
        }
    }
}

internal fun EntityImplementation.setColumnValue(binding: ColumnBinding, value: Any?, forceSet: Boolean = false) {
    when (binding) {
        is ReferenceBinding -> {
            var child = this.getProperty(binding.onProperty) as Entity<*>?
            if (child == null) {
                child = Entity.create(
                    entityClass = binding.onProperty.returnType.jvmErasure,
                    fromDatabase = this.fromDatabase,
                    fromTable = binding.referenceTable as Table<*>
                )
                this.setProperty(binding.onProperty, child, forceSet)
            }

            val refTable = binding.referenceTable as Table<*>
            child.implementation.setPrimaryKeyValue(refTable, value, forceSet, useExtraBindings = true)
        }
        is NestedBinding -> {
            var curr: EntityImplementation = this
            for ((i, prop) in binding.properties.withIndex()) {
                if (i != binding.properties.lastIndex) {
                    var child = curr.getProperty(prop) as Entity<*>?
                    if (child == null) {
                        child = Entity.create(prop.returnType.jvmErasure, parent = curr)
                        curr.setProperty(prop, child, forceSet)
                    }

                    curr = child.implementation
                }
            }

            curr.setProperty(binding.properties.last(), value, forceSet)
        }
    }
}

internal fun EntityImplementation.isPrimaryKey(name: String): Boolean {
    for (pk in this.fromTable?.primaryKeys.orEmpty()) {
        val binding = pk.binding ?: continue
        when (binding) {
            is ReferenceBinding -> {
                if (parent == null && binding.onProperty.name == name) {
                    return true
                }
            }
            is NestedBinding -> {
                val namesPath = LinkedList<Set<String>>()
                namesPath.addFirst(setOf(name))

                var curr: EntityImplementation = this
                while (true) {
                    val parent = curr.parent ?: break
                    val children = parent.values.filterValues { it is Entity<*> && it.implementation === curr }

                    if (children.isEmpty()) {
                        break
                    } else {
                        namesPath.addFirst(children.keys)
                        curr = parent
                    }
                }

                if (namesPath.withIndex().all { (i, names) -> binding.properties[i].name in names }) {
                    return true
                }
            }
        }
    }

    return false
}

internal val Entity<*>.implementation: EntityImplementation get() {
    return Proxy.getInvocationHandler(this) as EntityImplementation
}
