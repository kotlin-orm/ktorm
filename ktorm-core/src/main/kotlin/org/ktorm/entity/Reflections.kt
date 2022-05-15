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
