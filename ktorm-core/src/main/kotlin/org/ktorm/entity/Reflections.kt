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
import kotlin.reflect.KClass
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
        try {
            curr = curr.javaClass.getMethod("unbox-impl").invoke(curr)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    return curr
}
