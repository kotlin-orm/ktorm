/*
 * Copyright 2018-2020 the original author or authors.
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
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter
import kotlin.reflect.jvm.jvmErasure

@PublishedApi
internal class ColumnBindingHandler(val properties: MutableList<KProperty1<*, *>>) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        when (method.declaringClass.kotlin) {
            Any::class, Entity::class -> {
                error("Unsupported method: $method")
            }
            else -> {
                val (prop, isGetter) = method.kotlinProperty ?: error("Unsupported method: $method")
                if (!prop.isAbstract) {
                    error("Cannot bind a column to a non-abstract property: $prop")
                }
                if (!isGetter) {
                    error("Cannot modify a property while we are binding a column to it, property: $prop")
                }

                properties += prop

                val returnType = prop.returnType.jvmErasure
                return when {
                    returnType.isSubclassOf(Entity::class) -> createProxy(returnType, properties)
                    returnType.java.isPrimitive -> returnType.defaultValue
                    else -> null
                }
            }
        }
    }

    private fun error(msg: String): Nothing {
        throw UnsupportedOperationException(msg)
    }

    companion object {

        fun createProxy(entityClass: KClass<*>, properties: MutableList<KProperty1<*, *>>): Entity<*> {
            val handler = ColumnBindingHandler(properties)
            return Proxy.newProxyInstance(entityClass.java.classLoader, arrayOf(entityClass.java), handler) as Entity<*>
        }
    }
}

internal val Method.kotlinProperty: Pair<KProperty1<*, *>, Boolean>? get() {
    for (prop in declaringClass.kotlin.declaredMemberProperties) {
        if (prop.javaGetter == this) {
            return Pair(prop, true)
        }
        if (prop is KMutableProperty<*> && prop.javaSetter == this) {
            return Pair(prop, false)
        }
    }
    return null
}

internal val KClass<*>.defaultValue: Any get() {
    val value = when {
        this == Boolean::class -> false
        this == Char::class -> 0.toChar()
        this == Byte::class -> 0.toByte()
        this == Short::class -> 0.toShort()
        this == Int::class -> 0
        this == Long::class -> 0L
        this == Float::class -> 0.0F
        this == Double::class -> 0.0
        this == String::class -> ""
        this.isSubclassOf(Entity::class) -> Entity.create(this)
        this.java.isEnum -> this.java.enumConstants[0]
        this.java.isArray -> this.java.componentType.createArray(0)
        this == Set::class || this == MutableSet::class -> LinkedHashSet<Any?>()
        this == List::class || this == MutableList::class -> ArrayList<Any?>()
        this == Collection::class || this == MutableCollection::class -> ArrayList<Any?>()
        this == Map::class || this == MutableMap::class -> LinkedHashMap<Any?, Any?>()
        this == Queue::class || this == Deque::class -> LinkedList<Any?>()
        this == SortedSet::class || this == NavigableSet::class -> TreeSet<Any?>()
        this == SortedMap::class || this == NavigableMap::class -> TreeMap<Any?, Any?>()
        else -> this.createInstance()
    }

    if (this.isInstance(value)) {
        return value
    } else {
        // never happens...
        throw AssertionError("$value must be instance of $this")
    }
}

private fun Class<*>.createArray(length: Int): Any {
    return java.lang.reflect.Array.newInstance(this, length)
}
