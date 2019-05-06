package me.liuwj.ktorm.schema

import me.liuwj.ktorm.entity.Entity
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

/**
 * Base class used for obtaining full generic type information by subclassing.
 */
abstract class TypeReference<T> {

    /**
     * The actual type argument of subclass without erased.
     */
    val referencedType: Type by lazy { findSuperclassTypeArgument(javaClass) }

    /**
     * The actual kotlin type argument of subclass without erased.
     */
    val referencedKotlinType: KType by lazy { findSuperclassTypeArgument(javaClass.kotlin) }

    private fun findSuperclassTypeArgument(cls: Class<*>): Type {
        val genericSuperclass = cls.genericSuperclass

        if (genericSuperclass is Class<*>) {
            if (genericSuperclass != TypeReference::class.java) {
                // Try to climb up the hierarchy until meet something useful.
                return findSuperclassTypeArgument(genericSuperclass.superclass)
            } else {
                throw IllegalStateException("Could not find the referenced type of class $javaClass")
            }
        }

        return (genericSuperclass as ParameterizedType).actualTypeArguments[0]
    }

    private fun findSuperclassTypeArgument(cls: KClass<*>): KType {
        val supertype = cls.supertypes.first { !it.jvmErasure.java.isInterface }

        if (supertype.arguments.isEmpty()) {
            if (supertype.jvmErasure != TypeReference::class) {
                // Try to climb up the hierarchy until meet something useful.
                return findSuperclassTypeArgument(supertype.jvmErasure)
            } else {
                throw IllegalStateException("Could not find the referenced type of class $javaClass")
            }
        }

        return supertype.arguments[0].type!!
    }
}

/**
 * Create a [TypeReference] object which references the reified type argument T.
 */
inline fun <reified T> typeRef(): TypeReference<T> {
    return object : TypeReference<T>() { }
}

/**
 * Obtain the full generic type information via the reified type argument T, usage: typeOf<List<String>>()
 */
inline fun <reified T> typeOf(): Type {
    return typeRef<T>().referencedType
}

/**
 * Obtain the full generic type information via the reified type argument T, usage: kotlinTypeOf<List<String>>()
 *
 * Note: Do not use this function until the bug [KT-28616](https://youtrack.jetbrains.com/issue/KT-28616) fixed.
 */
@Deprecated("Do not use this function until the bug KT-28616 fixed", ReplaceWith("typeOf<T>()"))
inline fun <reified T> kotlinTypeOf(): KType {
    return typeRef<T>().referencedKotlinType
}

internal val Method.kotlinProperty: Pair<KProperty1<*, *>, Boolean>? get() {
    for (prop in declaringClass.kotlin.declaredMemberProperties) {
        if (prop.javaGetter == this) {
            return prop to true
        }
        if (prop is KMutableProperty<*> && prop.javaSetter == this) {
            return prop to false
        }
    }
    return null
}

internal val KClass<*>.defaultValue: Any get() {
    val value: Any = try {
        when {
            this == Boolean::class -> false
            this == Char::class -> 0.toChar()
            this == Byte::class -> 0.toByte()
            this == Short::class -> 0.toShort()
            this == Int::class -> 0
            this == Long::class -> 0L
            this == Float::class -> 0.0F
            this == Double::class -> 0.0
            this == String::class -> ""
            this.isSubclassOf(Entity::class) -> Entity.create(this)
            this.java.isEnum -> this.java.enumConstants[0]
            this.java.isArray -> this.java.componentType.createArray(0)
            this == Set::class || this == MutableSet::class -> LinkedHashSet<Any?>()
            this == List::class || this == MutableList::class -> ArrayList<Any?>()
            this == Collection::class || this == MutableCollection::class -> ArrayList<Any?>()
            this == Map::class || this == MutableMap::class -> LinkedHashMap<Any?, Any?>()
            this == Queue::class || this == Deque::class -> LinkedList<Any?>()
            this == SortedSet::class || this == NavigableSet::class -> TreeSet<Any?>()
            this == SortedMap::class || this == NavigableMap::class -> TreeMap<Any?, Any?>()
            else -> this.createInstance()
        }
    } catch (e: Throwable) {
        throw IllegalArgumentException("Error creating a default value for non-null type: ${this.jvmName}", e)
    }

    if (this.isInstance(value)) {
        return value
    } else {
        // never happens...
        throw AssertionError("$value must be instance of $this")
    }
}

private fun Class<*>.createArray(length: Int): Any {
    return java.lang.reflect.Array.newInstance(this, length)
}