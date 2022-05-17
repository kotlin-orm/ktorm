/*
 * Copyright 2018-2022 the original author or authors.
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
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.hasAnnotation

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
        return (this.classifier as KClass<*>).boxFrom(value)
    }
}

/**
 * Box the underlying value to an inline class value
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
