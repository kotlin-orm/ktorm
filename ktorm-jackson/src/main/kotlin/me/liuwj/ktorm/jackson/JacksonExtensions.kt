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
 */
internal fun ObjectCodec.deserializeNameForProperty(prop: KProperty1<*, *>, config: MapperConfig<*>): List<String> {
    if (this is ObjectMapper) {
        if (prop is KMutableProperty<*>) {
            val nameList: MutableList<String> = ArrayList()
            prop.javaSetter!!.annotations.forEach { annotation ->
                if (annotation is JsonProperty && annotation.value.isNotEmpty()) {
                    nameList.add(annotation.value)
                }
                if (annotation is JsonAlias && annotation.value.isNotEmpty()) {
                    nameList.addAll(annotation.value)
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
        val alias = prop.javaGetter!!.annotations.find { it is JsonProperty } as JsonProperty?
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
