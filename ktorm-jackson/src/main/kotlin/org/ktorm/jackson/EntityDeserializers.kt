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
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.module.SimpleDeserializers
import org.ktorm.entity.Entity
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

/**
 * Created by vince on Aug 13, 2018.
 */
internal class EntityDeserializers : SimpleDeserializers() {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun findBeanDeserializer(
        type: JavaType,
        config: DeserializationConfig,
        beanDesc: BeanDescription
    ): JsonDeserializer<*>? {
        val ktClass = type.rawClass.kotlin
        if (ktClass.isSubclassOf(Entity::class)) {
            return DeserializerImpl(ktClass)
        } else {
            return super.findBeanDeserializer(type, config, beanDesc)
        }
    }

    private class DeserializerImpl(val entityClass: KClass<*>) : JsonDeserializer<Entity<*>>() {

        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Entity<*> {
            val entity = Entity.create(entityClass)
            deserialize(parser, ctxt, entity)
            return entity
        }

        private fun findWritableProperties(
            parser: JsonParser,
            ctx: DeserializationContext
        ): Map<String, KProperty1<*, *>> {
            return entityClass.memberProperties
                .asSequence()
                .filter { it.isAbstract }
                .filter { it.name != "entityClass" && it.name != "properties" }
                .filter { it.findAnnotationForDeserialization<JsonIgnore>() == null }
                .filter { prop ->
                    val jsonProperty = prop.findAnnotationForDeserialization<JsonProperty>()
                    jsonProperty == null || jsonProperty.access != Access.READ_ONLY
                }
                .flatMap { prop ->
                    val names = parser.codec.deserializeNamesForProperty(prop, ctx.config)
                    names.map { it to prop }.asSequence()
                }
                .toMap()
        }

        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext, intoValue: Entity<*>): Entity<*> {
            val properties = findWritableProperties(parser, ctxt)

            if (parser.currentToken == JsonToken.START_OBJECT) {
                parser.nextToken()
            }

            while (parser.currentToken != JsonToken.END_OBJECT) {
                if (parser.currentToken != JsonToken.FIELD_NAME) {
                    ctxt.reportWrongTokenException(entityClass.java, JsonToken.FIELD_NAME, null)
                }

                val prop = properties[parser.currentName]

                parser.nextToken() // skip to field value

                if (prop != null) {
                    val propType = ctxt.constructType(prop.getPropertyType())
                    intoValue[prop.name] = parser.codec.readValue(parser, propType)
                } else {
                    if (parser.currentToken.isStructStart) {
                        parser.skipChildren()
                    }
                }

                parser.nextToken()
            }

            return intoValue
        }

        override fun deserializeWithType(
            parser: JsonParser,
            ctxt: DeserializationContext,
            typeDeserializer: TypeDeserializer
        ): Any {
            return typeDeserializer.deserializeTypedFromObject(parser, ctxt)
        }
    }
}
