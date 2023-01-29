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

package org.ktorm.schema

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

/**
 * Base class used to obtain full generic type information by subclassing.
 */
@Suppress("UnnecessaryAbstractClass")
public abstract class TypeReference<T> {

    /**
     * The actual type argument of subclass without erased.
     */
    public val referencedType: Type by lazy { findSuperclassTypeArgument(javaClass) }

    /**
     * The actual kotlin type argument of subclass without erased.
     */
    public val referencedKotlinType: KType by lazy { findSuperclassTypeArgument(javaClass.kotlin) }

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
        val supertype = cls.supertypes.first { !it.jvmErasure.java.isInterface }

        if (supertype.arguments.isEmpty()) {
            if (supertype.jvmErasure != TypeReference::class) {
                // Try to climb up the hierarchy until meet something useful.
                return findSuperclassTypeArgument(supertype.jvmErasure)
            } else {
                throw IllegalStateException("Could not find the referenced type of class $javaClass")
            }
        }

        return supertype.arguments[0].type!!
    }
}

/**
 * Create a [TypeReference] object which references the reified type argument [T].
 */
public inline fun <reified T> typeRef(): TypeReference<T> {
    return object : TypeReference<T>() { }
}

/**
 * Obtain the full generic type information of the reified type argument [T], usage: `typeOf<List<String>>()`.
 */
public inline fun <reified T> typeOf(): Type {
    return typeRef<T>().referencedType
}

/**
 * Obtain the full generic type information of the reified type argument [T], usage: `kotlinTypeOf<List<String>>()`.
 */
public inline fun <reified T> kotlinTypeOf(): KType {
    // Compiler bug: https://youtrack.jetbrains.com/issue/KT-28616
    // return typeRef<T>().referencedKotlinType
    return kotlin.reflect.typeOf<T>()
}
