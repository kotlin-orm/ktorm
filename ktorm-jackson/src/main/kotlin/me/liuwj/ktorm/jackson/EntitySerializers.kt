package me.liuwj.ktorm.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleSerializers
import me.liuwj.ktorm.entity.Entity
import kotlin.reflect.full.isSubclassOf
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

        if (type.rawClass.kotlin.isSubclassOf(Entity::class)) {
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
            val typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(entity, entity.entityClass.java, JsonToken.START_OBJECT))

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