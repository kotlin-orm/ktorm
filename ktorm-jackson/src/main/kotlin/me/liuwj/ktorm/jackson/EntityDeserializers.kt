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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.module.SimpleDeserializers
import me.liuwj.ktorm.entity.Entity
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

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
        companion object {
            private val entityPrivateProperties = setOf("properties", "entityClass")
            private val propCache: MutableMap<String, Map<String, KProperty1<*, *>>> = HashMap()
        }

        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Entity<*> {
            val entity = Entity.create(entityClass)
            deserialize(parser, ctxt, entity)
            return entity
        }

        private fun findWritableProperties(
            parser: JsonParser,
            ctx: DeserializationContext
        ): Map<String, KProperty1<*, *>> {
            return propCache.computeIfAbsent(entityClass.qualifiedName!!) { _ ->
                entityClass.memberProperties.filter { kp ->
                    !entityPrivateProperties.contains(entityClass.qualifiedName!!)
                            && kp.annotations.find { annotation -> annotation is JacksonIgnore } == null
                }.filter { kp ->
                    val annotation = kp.annotations.find { it is JacksonProperty } as JacksonProperty?
                    annotation == null || annotation.access != JsonProperty.Access.READ_ONLY
                }.associateBy { parser.codec.nameForProperty(it, ctx.config) }
            }
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

                val name = parser.currentName
                val prop = properties[name]

                parser.nextToken() // skip to field value

                if (prop != null) {
                    val propType = ctxt.constructType(prop.javaGetter!!.genericReturnType)
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
