package me.liuwj.ktorm.schema

import me.liuwj.ktorm.entity.Entity
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Hold a entity class instance.
 */
abstract class EntityClassHolder<E : Entity<E>>(entityClass: KClass<E>? = null) {

    @Suppress("UNCHECKED_CAST")
    val entityClass: KClass<E>? by lazy {
        val result = entityClass ?: findSuperClassTypeArgument(javaClass.kotlin).classifier as? KClass<E>
        result?.takeIf { it != Nothing::class }
    }

    private fun findSuperClassTypeArgument(cls: KClass<*>): KType {
        val superType = cls.supertypes.first {
            val rawClass = it.classifier as? KClass<*>
            if (rawClass == null) {
                false
            } else {
                !rawClass.java.isInterface
            }
        }

        if (superType.arguments.isEmpty()) {
            if (superType.classifier != EntityClassHolder::class) {
                return findSuperClassTypeArgument(superType.classifier as KClass<*>)
            } else {
                throw IllegalStateException("Could not find the referenced type of class ${javaClass.kotlin}")
            }
        }

        return superType.arguments[0].type!!
    }
}