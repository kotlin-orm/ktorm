/*
 * Copyright 2018-2020 the original author or authors.
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

package me.liuwj.ktorm.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken.START_OBJECT
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleSerializers
import me.liuwj.ktorm.entity.Entity
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

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

            val properties = entity.entityClass.memberProperties.associateBy { it.name }

            gen.writeStartObject()

            for ((name, value) in entity.properties) {
                val prop = properties[name] ?: continue
                val propType = serializers.constructType(prop.javaGetter!!.genericReturnType)
                val ser = serializers.findTypedValueSerializer(propType, true, null)

                gen.writeFieldName(gen.codec.nameForProperty(prop, serializers.config))

                if (value == null) {
                    gen.writeNull()
                } else {
                    ser.serialize(value, gen, serializers)
                }
            }

            gen.writeEndObject()
        }

        override fun serializeWithType(
            entity: Entity<*>,
            gen: JsonGenerator,
            serializers: SerializerProvider,
            typeSer: TypeSerializer
        ) {
            gen.configureIndentOutputIfEnabled()

            val properties = entity.entityClass.memberProperties.associateBy { it.name }
            val typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(entity, entity.entityClass.java, START_OBJECT))

            for ((name, value) in entity.properties) {
                val prop = properties[name] ?: continue
                val propType = serializers.constructType(prop.javaGetter!!.genericReturnType)
                val ser = serializers.findTypedValueSerializer(propType, true, null)

                gen.writeFieldName(gen.codec.nameForProperty(prop, serializers.config))

                if (value == null) {
                    gen.writeNull()
                } else {
                    ser.serialize(value, gen, serializers)
                }
            }

            typeSer.writeTypeSuffix(gen, typeId)
        }
    }
}
