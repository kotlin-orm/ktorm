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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken.START_OBJECT
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleSerializers
import org.ktorm.entity.Entity
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Created by vince on Aug 13, 2018.
 */
internal class EntitySerializers : SimpleSerializers() {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun findSerializer(
        config: SerializationConfig,
        type: JavaType,
        beanDesc: BeanDescription
    ): JsonSerializer<*>? {
        if (type.isTypeOrSubTypeOf(Entity::class.java)) {
            return SerializerImpl
        } else {
            return super.findSerializer(config, type, beanDesc)
        }
    }

    private object SerializerImpl : JsonSerializer<Entity<*>>() {

        override fun serialize(
            entity: Entity<*>,
            gen: JsonGenerator,
            serializers: SerializerProvider
        ) {
            gen.configureIndentOutputIfEnabled()
            gen.writeStartObject()
            serializeProperties(entity, gen, serializers)
            gen.writeEndObject()
        }

        private fun findReadableProperties(entity: Entity<*>): Map<String, KProperty1<*, *>> {
            return entity.entityClass.memberProperties
                .asSequence()
                .filter { it.isAbstract }
                .filter { it.name != "entityClass" && it.name != "properties" }
                .filter { it.findAnnotationForSerialization<JsonIgnore>() == null }
                .filter { prop ->
                    val jsonProperty = prop.findAnnotationForSerialization<JsonProperty>()
                    jsonProperty == null || jsonProperty.access != Access.WRITE_ONLY
                }
                .associateBy {
                    it.name
                }
        }

        private fun serializeProperties(
            entity: Entity<*>,
            gen: JsonGenerator,
            serializers: SerializerProvider
        ) {
            val properties = findReadableProperties(entity)

            for ((name, value) in entity.properties) {
                val prop = properties[name] ?: continue

                gen.writeFieldName(gen.codec.serializeNameForProperty(prop, serializers.config))

                if (value == null) {
                    gen.writeNull()
                } else {
                    val propType = serializers.constructType(prop.getPropertyType())
                    val ser = serializers.findTypedValueSerializer(propType, true, null)
                    ser.serialize(value, gen, serializers)
                }
            }
        }

        override fun serializeWithType(
            entity: Entity<*>,
            gen: JsonGenerator,
            serializers: SerializerProvider,
            typeSer: TypeSerializer
        ) {
            gen.configureIndentOutputIfEnabled()

            val typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(entity, entity.entityClass.java, START_OBJECT))
            serializeProperties(entity, gen, serializers)
            typeSer.writeTypeSuffix(gen, typeId)
        }
    }
}
