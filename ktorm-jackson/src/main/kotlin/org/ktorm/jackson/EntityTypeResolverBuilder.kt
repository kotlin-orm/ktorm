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

package org.ktorm.jackson

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder
import org.ktorm.entity.Entity

/**
 * Created by vince on Aug 13, 2018.
 */
internal class EntityTypeResolverBuilder(val delegate: StdTypeResolverBuilder) : StdTypeResolverBuilder() {
    companion object {
        private const val serialVersionUID = 1L
    }

    init {
        this._idType = delegate["_idType"] as JsonTypeInfo.Id?
        this._includeAs = delegate["_includeAs"] as JsonTypeInfo.As?
        this._typeProperty = delegate["_typeProperty"] as String?
        this._typeIdVisible = delegate["_typeIdVisible"] as Boolean
        this._defaultImpl = delegate["_defaultImpl"] as Class<*>?
        this._customIdResolver = delegate["_customIdResolver"] as TypeIdResolver?
    }

    override fun getDefaultImpl(): Class<*>? {
        return _defaultImpl
    }

    override fun buildTypeSerializer(
        config: SerializationConfig,
        baseType: JavaType,
        subtypes: MutableCollection<NamedType>?
    ): TypeSerializer? {

        if (baseType.isTypeOrSubTypeOf(Entity::class.java)) {
            // Always use type serialization for entity types...
            return super.buildTypeSerializer(config, baseType, subtypes)
        } else {
            return delegate.buildTypeSerializer(config, baseType, subtypes)
        }
    }

    override fun buildTypeDeserializer(
        config: DeserializationConfig,
        baseType: JavaType,
        subtypes: MutableCollection<NamedType>?
    ): TypeDeserializer? {

        if (baseType.isTypeOrSubTypeOf(Entity::class.java)) {
            // Always use type serialization for entity types...
            return super.buildTypeDeserializer(config, baseType, subtypes)
        } else {
            return delegate.buildTypeDeserializer(config, baseType, subtypes)
        }
    }

    override fun init(idType: JsonTypeInfo.Id, res: TypeIdResolver?): StdTypeResolverBuilder {
        delegate.init(idType, res)
        return super.init(idType, res)
    }

    override fun inclusion(includeAs: JsonTypeInfo.As): StdTypeResolverBuilder {
        delegate.inclusion(includeAs)
        return super.inclusion(includeAs)
    }

    override fun typeProperty(propName: String?): StdTypeResolverBuilder {
        delegate.typeProperty(propName)
        return super.typeProperty(propName)
    }

    override fun defaultImpl(defaultImpl: Class<*>?): StdTypeResolverBuilder {
        delegate.defaultImpl(defaultImpl)
        return super.defaultImpl(defaultImpl)
    }

    override fun typeIdVisibility(isVisible: Boolean): StdTypeResolverBuilder {
        delegate.typeIdVisibility(isVisible)
        return super.typeIdVisibility(isVisible)
    }

    private operator fun StdTypeResolverBuilder.get(fieldName: String): Any? {
        val field = StdTypeResolverBuilder::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(this)
    }
}
