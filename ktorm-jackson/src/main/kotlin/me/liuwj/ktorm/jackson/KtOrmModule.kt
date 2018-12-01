package me.liuwj.ktorm.jackson

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder

/**
 * 用于适配 Jackson 框架的 Module，由于框架要求我们的实体类都使用 interface 而不是 data class 来声明，
 * 因此 Jackson 无法将我们的实体对象进行序列化和反序列化。
 *
 * 在 Jackson 中注册此 Module 后，即可正常将实体对象序列化成 json，以支持 RPC 传输，或存储在数据库中。
 * 可使用 objectMapper.registerModule(JacksonOrmModule()) 手动注册，
 * 也可以使用 objectMapper.findAndRegisterModules 自动扫描并注册
 *
 * Created by vince on Aug 13, 2018.
 *
 * @see ObjectMapper.registerModule
 * @see ObjectMapper.findAndRegisterModules
 */
class KtOrmModule : Module() {
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