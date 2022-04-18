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

package org.ktorm.schema

import org.ktorm.entity.Entity
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

                val returnType = method.returnType
                return when {
                    returnType.kotlin.isSubclassOf(Entity::class) -> createProxy(returnType.kotlin, properties)
                    returnType.isPrimitive -> returnType.defaultValue
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

@OptIn(ExperimentalUnsignedTypes::class)
internal val Class<*>.defaultValue: Any get() {
    val value = when {
        this == Boolean::class.javaPrimitiveType -> false
        this == Char::class.javaPrimitiveType -> 0.toChar()
        this == Byte::class.javaPrimitiveType -> 0.toByte()
        this == Short::class.javaPrimitiveType -> 0.toShort()
        this == Int::class.javaPrimitiveType -> 0
        this == Long::class.javaPrimitiveType -> 0L
        this == Float::class.javaPrimitiveType -> 0.0F
        this == Double::class.javaPrimitiveType -> 0.0
        this == String::class.java -> ""
        this == UByte::class.java -> 0.toUByte()
        this == UShort::class.java -> 0.toUShort()
        this == UInt::class.java -> 0U
        this == ULong::class.java -> 0UL
        this == UByteArray::class.java -> ubyteArrayOf()
        this == UShortArray::class.java -> ushortArrayOf()
        this == UIntArray::class.java -> uintArrayOf()
        this == ULongArray::class.java -> ulongArrayOf()
        this == Set::class.java -> LinkedHashSet<Any?>()
        this == List::class.java -> ArrayList<Any?>()
        this == Collection::class.java -> ArrayList<Any?>()
        this == Map::class.java -> LinkedHashMap<Any?, Any?>()
        this == Queue::class.java || this == Deque::class.java -> LinkedList<Any?>()
        this == SortedSet::class.java || this == NavigableSet::class.java -> TreeSet<Any?>()
        this == SortedMap::class.java || this == NavigableMap::class.java -> TreeMap<Any?, Any?>()
        this.isEnum -> this.enumConstants[0]
        this.isArray -> java.lang.reflect.Array.newInstance(this.componentType, 0)
        this.kotlin.isSubclassOf(Entity::class) -> Entity.create(this.kotlin)
        else -> this.kotlin.createInstance()
    }

    if (this.kotlin.isInstance(value)) {
        return value
    } else {
        // never happens...
        throw AssertionError("$value must be instance of $this")
    }
}
