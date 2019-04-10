package me.liuwj.ktorm.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaGetter

val sharedObjectMapper: ObjectMapper = ObjectMapper().registerModule(KtormModule())

internal fun JsonGenerator.configureIndentOutputIfEnabled() {
    val codec = this.codec
    if (codec is ObjectMapper) {
        val config = codec.serializationConfig
        if (config.isEnabled(SerializationFeature.INDENT_OUTPUT) && prettyPrinter == null) {
            prettyPrinter = config.constructDefaultPrettyPrinter()
        }
    }
}

internal fun ObjectCodec.nameForProperty(prop: KProperty1<*, *>, config: MapperConfig<*>): String {
    if (this is ObjectMapper) {
        val strategy = this.propertyNamingStrategy
        if (strategy != null) {
            val getter = AnnotatedMethod(null, prop.javaGetter!!, null, null)
            return strategy.nameForGetterMethod(config, getter, prop.name)
        }
    }

    return prop.name
}
