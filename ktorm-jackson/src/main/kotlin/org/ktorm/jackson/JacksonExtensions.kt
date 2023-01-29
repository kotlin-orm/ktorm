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
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.*

internal fun JsonGenerator.configureIndentOutputIfEnabled() {
    val codec = this.codec
    if (codec is ObjectMapper) {
        val config = codec.serializationConfig
        if (config.isEnabled(SerializationFeature.INDENT_OUTPUT) && prettyPrinter == null) {
            prettyPrinter = config.constructDefaultPrettyPrinter()
        }
    }
}

internal fun ObjectCodec.deserializeNamesForProperty(prop: KProperty1<*, *>, config: MapperConfig<*>): List<String> {
    val names = ArrayList<String>()

    val jsonProperty = prop.findAnnotationForDeserialization<JsonProperty>()
    if (jsonProperty != null && jsonProperty.value.isNotEmpty()) {
        names += jsonProperty.value
    } else {
        val strategy = (this as? ObjectMapper)?.propertyNamingStrategy
        if (strategy == null) {
            names += prop.name
        } else {
            val getter = AnnotatedMethod(null, prop.javaGetter, null, null)
            names += strategy.nameForGetterMethod(config, getter, prop.name)
        }
    }

    val jsonAlias = prop.findAnnotationForDeserialization<JsonAlias>()
    if (jsonAlias != null && jsonAlias.value.isNotEmpty()) {
        names += jsonAlias.value
    }

    return names
}

internal fun ObjectCodec.serializeNameForProperty(prop: KProperty1<*, *>, config: MapperConfig<*>): String {
    val jsonProperty = prop.findAnnotationForSerialization<JsonProperty>()
    if (jsonProperty != null && jsonProperty.value.isNotEmpty()) {
        return jsonProperty.value
    }

    if (this is ObjectMapper) {
        val strategy = this.propertyNamingStrategy
        if (strategy != null) {
            val getter = AnnotatedMethod(null, prop.javaGetter, null, null)
            return strategy.nameForGetterMethod(config, getter, prop.name)
        }
    }

    return prop.name
}

internal inline fun <reified T : Annotation> KProperty1<*, *>.findAnnotationForSerialization(): T? {
    var annotation = javaGetter?.getAnnotation(T::class.java)

    if (annotation == null && this is KMutableProperty<*>) {
        annotation = javaSetter?.getAnnotation(T::class.java)
    }

    if (annotation == null) {
        annotation = javaField?.getAnnotation(T::class.java)
    }

    return annotation
}

internal inline fun <reified T : Annotation> KProperty1<*, *>.findAnnotationForDeserialization(): T? {
    var annotation: T? = null

    if (this is KMutableProperty<*>) {
        annotation = javaSetter?.getAnnotation(T::class.java)
    }

    if (annotation == null) {
        annotation = javaGetter?.getAnnotation(T::class.java)
    }

    if (annotation == null) {
        annotation = javaField?.getAnnotation(T::class.java)
    }

    return annotation
}

internal fun KProperty1<*, *>.getPropertyType(): java.lang.reflect.Type {
    val cls = returnType.jvmErasure
    if (cls.isValue && cls.hasAnnotation<JvmInline>()) {
        return cls.java
    } else {
        return returnType.javaType
    }
}
