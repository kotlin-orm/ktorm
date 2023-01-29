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

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter
import kotlin.reflect.jvm.jvmErasure

/**
 * Return the corresponding Kotlin property of this method if exists and a flag indicates whether
 * it's a getter (true) or setter (false).
 */
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

/**
 * Return a default value for the class.
 */
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

/**
 * Check if this class is an inline class.
 */
internal val KClass<*>.isInline: Boolean get() = this.isValue && this.hasAnnotation<JvmInline>()

/**
 * Unbox the inline class value to the target type.
 */
internal fun Any.unboxTo(targetClass: Class<*>): Any? {
    var curr: Any? = this
    while (curr != null && curr::class.isInline && curr.javaClass != targetClass) {
        curr = curr.javaClass.getMethod("unbox-impl").invoke0(curr)
    }

    return curr
}

/**
 * Box the underlying value to an inline class value.
 */
internal fun KType.boxFrom(value: Any?): Any? {
    if (value == null && this.isMarkedNullable) {
        return null
    } else {
        return this.jvmErasure.boxFrom(value)
    }
}

/**
 * Box the underlying value to an inline class value.
 */
internal fun KClass<*>.boxFrom(value: Any?): Any? {
    if (!this.isInline || this.java == value?.javaClass) {
        return value
    }

    val method = this.java.methods.single { it.name == "box-impl" }
    if (value == null || method.parameterTypes[0].kotlin.isInstance(value)) {
        return method.invoke0(null, value)
    } else {
        return method.invoke0(null, method.parameterTypes[0].kotlin.boxFrom(value))
    }
}

/**
 * Call the [Method.invoke] method, catching [InvocationTargetException], and rethrowing the target exception.
 */
@Suppress("SwallowedException")
internal fun Method.invoke0(obj: Any?, vararg args: Any?): Any? {
    try {
        return this.invoke(obj, *args)
    } catch (e: InvocationTargetException) {
        throw e.targetException
    }
}
