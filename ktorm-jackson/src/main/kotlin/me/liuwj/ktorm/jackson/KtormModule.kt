package me.liuwj.ktorm.jackson

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder
import java.util.ServiceLoader
import me.liuwj.ktorm.entity.Entity

/**
 * Jackson [Module] implementation that supports serializing Ktorm's entity objects in JSON format.
 *
 * [Entity] classes in Ktorm are defined as interfaces, and entity objects are created by JDK dynamic proxy.
 * That's why Jackson cannot serialize entity objects by default (because they are not normal Java classes).
 * This module provides the Jackson serialization support for Ktorm.
 *
 * To enable this module, you need to call the [ObjectMapper.registerModule] method to register it to Jackson.
 * You can also call [ObjectMapper.findAndRegisterModules] to automatically find and register it using JDK
 * [ServiceLoader] facility. For more details, please see Jackson's documentation.
 *
 * @see Entity
 * @see ObjectMapper.registerModule
 * @see ObjectMapper.findAndRegisterModules
 */
class KtormModule : Module() {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun getModuleName(): String {
        return PackageVersion.VERSION.artifactId
    }

    override fun version(): Version {
        return PackageVersion.VERSION
    }

    override fun setupModule(context: SetupContext) {
        context.addSerializers(EntitySerializers())
        context.addDeserializers(EntityDeserializers())

        val codec = context.getOwner<ObjectCodec>()
        if (codec is ObjectMapper) {
            val objectType = codec.constructType(Any::class.java)

            val serializerTyper = codec.serializationConfig.getDefaultTyper(objectType)
            if (serializerTyper != null && serializerTyper is StdTypeResolverBuilder) {
                codec.setConfig(codec.serializationConfig.with(EntityTypeResolverBuilder(serializerTyper)))
            }

            val deserializerTyper = codec.deserializationConfig.getDefaultTyper(objectType)
            if (deserializerTyper != null && deserializerTyper is StdTypeResolverBuilder) {
                codec.setConfig(codec.deserializationConfig.with(EntityTypeResolverBuilder(deserializerTyper)))
            }
        }
    }
}
