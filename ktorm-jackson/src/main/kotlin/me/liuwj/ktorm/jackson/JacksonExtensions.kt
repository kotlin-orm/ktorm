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

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

internal fun JsonGenerator.configureIndentOutputIfEnabled() {
    val codec = this.codec
    if (codec is ObjectMapper) {
        val config = codec.serializationConfig
        if (config.isEnabled(SerializationFeature.INDENT_OUTPUT) && prettyPrinter == null) {
            prettyPrinter = config.constructDefaultPrettyPrinter()
        }
    }
}

/**
 * it may has multi alias name when property annotated with JsonAlias.
 *
 * [JsonAlias] and [JsonProperty] on getter method both don't work when deserialize, so we only need find them
 * on setter method, see JacksonAnnotationTest.testAliasDeserializeJsonPropertyDontWork.
 */
internal fun ObjectCodec.deserializeNameForProperty(prop: KProperty1<*, *>, config: MapperConfig<*>): List<String> {
    if (this is ObjectMapper) {
        if (prop is KMutableProperty<*>) {
            val nameList: MutableList<String> = ArrayList()

            val jsonAlias = prop.javaSetter!!.annotations.find { it is JsonAlias } as JsonAlias?
            if (jsonAlias != null && jsonAlias.value.isNotEmpty()) {
                nameList.addAll(jsonAlias.value)
            }

            val jsonProperty = prop.javaSetter!!.annotations.find { it is JsonProperty } as JsonProperty?
            // according to JacksonAnnotationTest.testDeserializeAliasName2
            // prop with `JsonProperty`, alias name don't contains prop's strategy name
            if (jsonProperty != null) {
                if (jsonProperty.value.isNotEmpty()) {
                    nameList.add(jsonProperty.value)
                }
            } else {
                this.propertyNamingStrategy?.let { strategy ->
                    val getter = AnnotatedMethod(null, prop.javaGetter!!, null, null)
                    nameList.add(strategy.nameForGetterMethod(config, getter, prop.name))
                }
            }

            if (nameList.isNotEmpty()) {
                return nameList
            }
        }

        this.propertyNamingStrategy?.let { strategy ->
            val getter = AnnotatedMethod(null, prop.javaGetter!!, null, null)
            return listOf(strategy.nameForGetterMethod(config, getter, prop.name))
        }
    }

    return listOf(prop.name)
}

internal fun ObjectCodec.serializeNameForProperty(prop: KProperty1<*, *>, config: MapperConfig<*>): String {
    if (this is ObjectMapper) {
        val alias = prop.findAnnotationGetterFirst(JsonProperty::class.java)
        if (alias != null && alias.value.isNotEmpty()) {
            return alias.value
        }
        val strategy = this.propertyNamingStrategy
        if (strategy != null) {
            val getter = AnnotatedMethod(null, prop.javaGetter!!, null, null)
            return strategy.nameForGetterMethod(config, getter, prop.name)
        }
    }

    return prop.name
}

internal inline fun <reified T : Any> KProperty1<*, *>.findAnnotationGetterFirst(annotation: Class<T>): T? {
    if (this is KMutableProperty<*>) {
        return (this.javaGetter!!.annotations.find { annotation.isInstance(it) } ?:
            this.javaSetter!!.annotations.find { annotation.isInstance(it) }) as T?
    }
    return this.javaGetter!!.annotations.find { annotation.isInstance(it) } as T?
}

internal inline fun <reified T : Any> KProperty1<*, *>.findAnnotationSetterFirst(annotation: Class<T>): T? {
    if (this is KMutableProperty<*>) {
        return (this.javaSetter!!.annotations.find { annotation.isInstance(it) } ?:
            this.javaGetter!!.annotations.find { annotation.isInstance(it) }) as T?
    }
    return this.javaGetter!!.annotations.find { annotation.isInstance(it) } as T?
}
