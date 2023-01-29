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

package org.ktorm.jackson

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder
import org.ktorm.entity.Entity

/**
 * Created by vince on Aug 13, 2018.
 */
internal class EntityTypeResolverBuilder(
    src: DefaultTypeResolverBuilder
) : DefaultTypeResolverBuilder(src._appliesFor, src._subtypeValidator) {

    companion object {
        private const val serialVersionUID = 2L
    }

    init {
        this._idType = src._idType
        this._includeAs = src._includeAs
        this._typeProperty = src._typeProperty
        this._typeIdVisible = src._typeIdVisible
        this._defaultImpl = src._defaultImpl
        this._customIdResolver = src._customIdResolver
    }

    override fun useForType(t: JavaType): Boolean {
        if (t.isTypeOrSubTypeOf(Entity::class.java)) {
            // Always use type serialization for entity types...
            return true
        } else {
            return super.useForType(t)
        }
    }
}

@Suppress("ObjectPropertyName")
private val StdTypeResolverBuilder._idType: JsonTypeInfo.Id? get() {
    val field = StdTypeResolverBuilder::class.java.getDeclaredField("_idType")
    field.isAccessible = true
    return field.get(this) as JsonTypeInfo.Id?
}

@Suppress("ObjectPropertyName")
private val StdTypeResolverBuilder._includeAs: JsonTypeInfo.As? get() {
    val field = StdTypeResolverBuilder::class.java.getDeclaredField("_includeAs")
    field.isAccessible = true
    return field.get(this) as JsonTypeInfo.As?
}

@Suppress("ObjectPropertyName")
private val StdTypeResolverBuilder._typeProperty: String? get() {
    val field = StdTypeResolverBuilder::class.java.getDeclaredField("_typeProperty")
    field.isAccessible = true
    return field.get(this) as String?
}

@Suppress("ObjectPropertyName")
private val StdTypeResolverBuilder._typeIdVisible: Boolean get() {
    val field = StdTypeResolverBuilder::class.java.getDeclaredField("_typeIdVisible")
    field.isAccessible = true
    return field.get(this) as Boolean
}

@Suppress("ObjectPropertyName")
private val StdTypeResolverBuilder._defaultImpl: Class<*>? get() {
    val field = StdTypeResolverBuilder::class.java.getDeclaredField("_defaultImpl")
    field.isAccessible = true
    return field.get(this) as Class<*>?
}

@Suppress("ObjectPropertyName")
private val StdTypeResolverBuilder._customIdResolver: TypeIdResolver? get() {
    val field = StdTypeResolverBuilder::class.java.getDeclaredField("_customIdResolver")
    field.isAccessible = true
    return field.get(this) as TypeIdResolver?
}

@Suppress("ObjectPropertyName")
private val DefaultTypeResolverBuilder._appliesFor: ObjectMapper.DefaultTyping get() {
    val field = DefaultTypeResolverBuilder::class.java.getDeclaredField("_appliesFor")
    field.isAccessible = true
    return field.get(this) as ObjectMapper.DefaultTyping
}

@Suppress("ObjectPropertyName")
private val DefaultTypeResolverBuilder._subtypeValidator: PolymorphicTypeValidator get() {
    val field = DefaultTypeResolverBuilder::class.java.getDeclaredField("_subtypeValidator")
    field.isAccessible = true
    return field.get(this) as PolymorphicTypeValidator
}
