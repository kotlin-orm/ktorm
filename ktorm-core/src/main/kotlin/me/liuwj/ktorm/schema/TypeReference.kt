package me.liuwj.ktorm.schema

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Base class used for obtaining full generic type information by subclassing.
 */
abstract class TypeReference<T> {

    /**
     * The actual type argument of subclass without erased.
     */
    val referencedType: Type by lazy { findSuperclassTypeArgument(javaClass) }

    /**
     * The actual kotlin type argument of subclass without erased.
     */
    val referencedKotlinType: KType by lazy { findSuperclassTypeArgument(javaClass.kotlin) }

    private fun findSuperclassTypeArgument(cls: Class<*>): Type {
        val genericSuperclass = cls.genericSuperclass

        if (genericSuperclass is Class<*>) {
            if (genericSuperclass != TypeReference::class.java) {
                // Try to climb up the hierarchy until meet something useful.
                return findSuperclassTypeArgument(genericSuperclass.superclass)
            } else {
                throw IllegalStateException("Could not find the referenced type of class $javaClass")
            }
        }

        return (genericSuperclass as ParameterizedType).actualTypeArguments[0]
    }

    private fun findSuperclassTypeArgument(cls: KClass<*>): KType {
        val supertype = cls.supertypes.first {
            val rawClass = it.classifier as? KClass<*>
            if (rawClass == null) {
                false
            } else {
                !rawClass.java.isInterface
            }
        }

        if (supertype.arguments.isEmpty()) {
            if (supertype.classifier != TypeReference::class) {
                // Try to climb up the hierarchy until meet something useful.
                return findSuperclassTypeArgument(supertype.classifier as KClass<*>)
            } else {
                throw IllegalStateException("Could not find the referenced type of class $javaClass")
            }
        }

        return supertype.arguments[0].type!!
    }
}

/**
 * Obtain the full generic type information via the reified type argument T, usage: typeOf<List<String>>()
 */
inline fun <reified T> typeOf(): Type {
    return object : TypeReference<T>() { }.referencedType
}

/**
 * Obtain the full generic type information via the reified type argument T, usage: kotlinTypeOf<List<String>>()
 *
 * Note: Do not use this function until the bug [KT-28616](https://youtrack.jetbrains.com/issue/KT-28616) fixed.
 */
@Deprecated("Do not use this function until the bug KT-28616 fixed", ReplaceWith("typeOf<T>()"))
inline fun <reified T> kotlinTypeOf(): KType {
    return object : TypeReference<T>() { }.referencedKotlinType
}