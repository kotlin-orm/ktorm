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

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder
import org.ktorm.entity.Entity
import java.util.*

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
public class KtormModule : Module() {
    private companion object {
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

        try {
            val codec = context.getOwner<ObjectCodec>()
            if (codec is ObjectMapper) {
                val objectType = codec.constructType(Any::class.java)

                val serializerTyper = codec.serializationConfig.getDefaultTyper(objectType)
                if (serializerTyper != null && serializerTyper is DefaultTypeResolverBuilder) {
                    codec.setConfig(codec.serializationConfig.with(EntityTypeResolverBuilder(serializerTyper)))
                }

                val deserializerTyper = codec.deserializationConfig.getDefaultTyper(objectType)
                if (deserializerTyper != null && deserializerTyper is DefaultTypeResolverBuilder) {
                    codec.setConfig(codec.deserializationConfig.with(EntityTypeResolverBuilder(deserializerTyper)))
                }
            }
        } catch (e: Throwable) {
            // Some reflection operation (setAccessible) may fail in JDK 9 or above.
            if (e.javaClass.name == "java.lang.reflect.InaccessibleObjectException") {
                @Suppress("MaxLineLength")
                val msg = "" +
                    "Default typing is not supported because some hacking magic based on reflection failed. " +
                    "Please disable the default typing functionality by ObjectMapper.disableDefaultTyping(), " +
                    "or add the following VM arguments: " +
                    "--add-opens com.fasterxml.jackson.databind/com.fasterxml.jackson.databind=ktorm.jackson " +
                    "--add-opens com.fasterxml.jackson.databind/com.fasterxml.jackson.databind.jsontype.impl=ktorm.jackson "
                throw UnsupportedOperationException(msg, e)
            } else {
                throw e
            }
        }
    }
}
