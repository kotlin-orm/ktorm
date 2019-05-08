package me.liuwj.ktorm.jackson

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder
import me.liuwj.ktorm.entity.Entity

/**
 * Created by vince on Aug 13, 2018.
 */
internal class EntityTypeResolverBuilder(val src: StdTypeResolverBuilder) : StdTypeResolverBuilder() {
    companion object {
        private const val serialVersionUID = 1L
    }

    init {
        this._idType = src["_idType"] as JsonTypeInfo.Id?
        this._includeAs = src["_includeAs"] as JsonTypeInfo.As?
        this._typeProperty = src["_typeProperty"] as String?
        this._typeIdVisible = src["_typeIdVisible"] as Boolean
        this._defaultImpl = src["_defaultImpl"] as Class<*>?
        this._customIdResolver = src["_customIdResolver"] as TypeIdResolver?
    }

    override fun getDefaultImpl(): Class<*>? {
        return _defaultImpl
    }

    override fun buildTypeSerializer(
        config: SerializationConfig,
        baseType: JavaType,
        subtypes: MutableCollection<NamedType>?
    ): TypeSerializer? {

        if (baseType.isTypeOrSubTypeOf(Entity::class.java)) {
            // Always use type serialization for entity types...
            return super.buildTypeSerializer(config, baseType, subtypes)
        } else {
            return src.buildTypeSerializer(config, baseType, subtypes)
        }
    }

    override fun buildTypeDeserializer(
        config: DeserializationConfig,
        baseType: JavaType,
        subtypes: MutableCollection<NamedType>?
    ): TypeDeserializer? {

        if (baseType.isTypeOrSubTypeOf(Entity::class.java)) {
            // Always use type serialization for entity types...
            return super.buildTypeDeserializer(config, baseType, subtypes)
        } else {
            return src.buildTypeDeserializer(config, baseType, subtypes)
        }
    }

    override fun init(idType: JsonTypeInfo.Id, res: TypeIdResolver?): StdTypeResolverBuilder {
        src.init(idType, res)
        return super.init(idType, res)
    }

    override fun inclusion(includeAs: JsonTypeInfo.As): StdTypeResolverBuilder {
        src.inclusion(includeAs)
        return super.inclusion(includeAs)
    }

    override fun typeProperty(propName: String?): StdTypeResolverBuilder {
        src.typeProperty(propName)
        return super.typeProperty(propName)
    }

    override fun defaultImpl(defaultImpl: Class<*>?): StdTypeResolverBuilder {
        src.defaultImpl(defaultImpl)
        return super.defaultImpl(defaultImpl)
    }

    override fun typeIdVisibility(isVisible: Boolean): StdTypeResolverBuilder {
        src.typeIdVisibility(isVisible)
        return super.typeIdVisibility(isVisible)
    }

    private operator fun StdTypeResolverBuilder.get(fieldName: String): Any? {
        val field = StdTypeResolverBuilder::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(this)
    }
}
