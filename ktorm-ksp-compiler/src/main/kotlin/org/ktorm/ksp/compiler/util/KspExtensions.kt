/*
 * Copyright 2018-2024 the original author or authors.
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

@file:OptIn(KspExperimental::class)

package org.ktorm.ksp.compiler.util

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSValidateVisitor

/**
 * Return the resolved [KSType] of this property.
 */
@Suppress("ObjectPropertyName", "TopLevelPropertyNaming")
internal val KSPropertyDeclaration._type: KSType get() = type.resolve()

/**
 * Return the resolved [KSType] of this parameter.
 */
@Suppress("ObjectPropertyName", "TopLevelPropertyNaming")
internal val KSValueParameter._type: KSType get() = type.resolve()

/**
 * Return the resolved [KSType] of this annotation.
 */
@Suppress("ObjectPropertyName", "TopLevelPropertyNaming")
internal val KSAnnotation._annotationType: KSType get() = annotationType.resolve()

/**
 * Check if this type is an inline class.
 */
internal fun KSType.isInline(): Boolean {
    val declaration = declaration as KSClassDeclaration
    return declaration.isAnnotationPresent(JvmInline::class) && declaration.modifiers.contains(Modifier.VALUE)
}

/**
 * Check if this class is a subclass of [T].
 */
internal inline fun <reified T : Any> KSClassDeclaration.isSubclassOf(): Boolean {
    return findSuperTypeReference(T::class.qualifiedName!!) != null
}

/**
 * Check if this class is a subclass of declaration.
 */
internal fun KSClassDeclaration.isSubclassOf(declaration: KSClassDeclaration): Boolean {
    return findSuperTypeReference(declaration.qualifiedName!!.asString()) != null
}

/**
 * Find the specific super type reference for this class.
 */
internal fun KSClassDeclaration.findSuperTypeReference(name: String): KSTypeReference? {
    for (superType in this.superTypes) {
        val ksType = superType.resolve()

        if (ksType.declaration.qualifiedName?.asString() == name) {
            return superType
        }

        val result = (ksType.declaration as KSClassDeclaration).findSuperTypeReference(name)
        if (result != null) {
            return result
        }
    }

    return null
}

/**
 * Find all annotations with the given name in the inheritance hierarchy of this class.
 *
 * @param name the qualified name of the annotation.
 */
internal fun KSClassDeclaration.findAllAnnotationsInInheritanceHierarchy(name: String): List<Pair<KSClassDeclaration, KSAnnotation>> {
    val rst = mutableListOf<Pair<KSClassDeclaration, KSAnnotation>>()

    fun KSClassDeclaration.collectAnnotations() {
        rst += annotations.filter { it.annotationType.resolve().declaration.qualifiedName?.asString() == name }.map { this to it }
        superTypes.forEach { (it.resolve().declaration as KSClassDeclaration).collectAnnotations() }
    }

    collectAnnotations()
    return rst
}


/**
 * Check if the given symbol is valid.
 */
internal fun KSNode.isValid(): Boolean {
    // Custom visitor to avoid stack overflow, see https://github.com/google/ksp/issues/1114
    val visitor = object : KSValidateVisitor({ _, _ -> true }) {
        private val stack = LinkedHashSet<KSType>()

        private fun validateType(type: KSType): Boolean {
            if (!stack.add(type)) {
                // Skip if the type already in the stack, avoid infinite recursion.
                return true
            }

            try {
                return !type.isError && !type.arguments.any { it.type?.accept(this, null) == false }
            } finally {
                stack.remove(type)
            }
        }

        override fun visitTypeReference(typeReference: KSTypeReference, data: KSNode?): Boolean {
            return validateType(typeReference.resolve())
        }

        override fun visitValueArgument(valueArgument: KSValueArgument, data: KSNode?): Boolean {
            fun visitValue(value: Any?): Boolean = when (value) {
                is KSType -> this.validateType(value)
                is KSAnnotation -> this.visitAnnotation(value, data)
                is List<*> -> value.all { visitValue(it) }
                else -> true
            }

            return visitValue(valueArgument.value)
        }
    }

    return this.accept(visitor, null)
}
