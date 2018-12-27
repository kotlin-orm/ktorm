package me.liuwj.ktorm.entity

import me.liuwj.ktorm.schema.NestedBinding
import me.liuwj.ktorm.schema.ReferenceBinding
import me.liuwj.ktorm.schema.SimpleBinding
import me.liuwj.ktorm.schema.Table
import org.slf4j.LoggerFactory
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlinFunction

/**
 * Created by vince on Jun 18, 2018.
 * todo: make this class internal
 */
class EntityImpl(
    var entityClass: KClass<*>,
    @Transient var fromTable: Table<*>?,
    @Transient var parent: Entity<*>?
) : InvocationHandler, Entity<EntityImpl> {

    var values = LinkedHashMap<String, Any?>()
    @Transient var changedProperties = LinkedHashSet<String>()
    @Transient var defaultValuesCache = HashMap<KClass<*>, Any>()

    companion object {
        private const val serialVersionUID = 1L
        private val logger = LoggerFactory.getLogger(EntityImpl::class.java)
        private val defaultImplsCache: MutableMap<Method, Method> = Collections.synchronizedMap(WeakHashMap())
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? = when (method.name) {
        "equals" -> this == args!![0]
        "hashCode" -> this.hashCode()
        "toString" -> this.toString()
        "flushChanges" -> this.flushChanges()
        "discardChanges" -> this.discardChanges()
        "delete" -> this.delete()
        "get" -> this[args!![0] as String]
        "set" -> this[args!![0] as String] = args[1]
        else -> handleMethodCall(proxy, method, args)
    }

    private fun handleMethodCall(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val ktProp = method.kotlinProperty
        if (ktProp != null) {
            val (prop, isGetter) = ktProp
            if (prop.isAbstract) {
                if (isGetter) {
                    val result = this[prop.name]
                    if (result != null || prop.returnType.isMarkedNullable) {
                        return result
                    } else {
                        return (prop.returnType.classifier as KClass<*>).defaultValue
                    }
                } else {
                    this[prop.name] = args!![0]
                    return null
                }
            } else {
                return callDefaultImpl(proxy, method, args)
            }
        } else {
            val func = method.kotlinFunction
            if (func != null && !func.isAbstract) {
                return callDefaultImpl(proxy, method, args)
            } else {
                throw IllegalStateException("Unrecognized method: $method")
            }
        }
    }

    private fun callDefaultImpl(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val impl = defaultImplsCache.computeIfAbsent(method) {
            val cls = Class.forName(method.declaringClass.name + "\$DefaultImpls")
            cls.getMethod(method.name, method.declaringClass, *method.parameterTypes)
        }

        try {
            if (args == null) {
                return impl.invoke(null, proxy)
            } else {
                return impl.invoke(null, proxy, *args)
            }

        } catch (e: InvocationTargetException) {
            throw e.targetException

        } catch (e: Throwable) {
            logger.error("proxy: ${proxy.javaClass}: $proxy")
            logger.error("method: $method")
            logger.error("args: ${args?.contentToString()}")
            logger.error("arg types: ${args?.map { it.javaClass }}")
            logger.error("impl: $impl")
            logger.error("cache: $defaultImplsCache")
            throw e
        }
    }

    private val Method.kotlinProperty: Pair<KProperty<*>, Boolean>? get() {
        for (prop in entityClass.memberProperties) {
            if (prop.javaGetter == this) {
                return prop to true
            }
            if (prop is KMutableProperty<*> && prop.javaSetter == this) {
                return prop to false
            }
        }
        return null
    }

    private val KClass<*>.defaultValue: Any get() {
        return defaultValuesCache.computeIfAbsent(this) {
            val value: Any = when {
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

            if (this.isInstance(value)) {
                value
            } else {
                throw AssertionError("$value must be instance of $this")
            }
        }
    }

    private fun Class<*>.createArray(length: Int): Any {
        return java.lang.reflect.Array.newInstance(this, length)
    }

    override fun flushChanges(): Int {
        return doFlushChanges()
    }

    override fun discardChanges() {
        changedProperties.clear()

        for ((_, value) in values) {
            if (value is Entity<*>) {
                value.discardChanges()
            }
        }
    }

    override fun delete(): Int {
        return doDelete()
    }

    override fun get(name: String): Any? {
        return values[name]
    }

    override fun set(name: String, value: Any?) {
        val binding = this.fromTable?.primaryKey?.binding

        val isPrimaryKey = when (binding) {
            null -> false
            is SimpleBinding -> {
                binding.property.name == name
            }
            is NestedBinding -> {
                val p = parent
                if (p == null) {
                    binding.property1.name == name
                } else {
                    p[binding.property1.name] == this && binding.property2.name == name
                }
            }
            is ReferenceBinding -> {
                binding.onProperty.name == name
            }
        }

        if (isPrimaryKey && name in values) {
            throw UnsupportedOperationException("Cannot modify the primary key value because it's already set to ${values[name]}")
        }

        values[name] = value
        changedProperties.add(name)
    }

    private fun writeObject(output: ObjectOutputStream) {
        output.writeUTF(entityClass.jvmName)
        output.writeObject(values)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readObject(input: ObjectInputStream) {
        entityClass = Class.forName(input.readUTF()).kotlin
        values = input.readObject() as LinkedHashMap<String, Any?>

        changedProperties = LinkedHashSet()
        defaultValuesCache = HashMap()
    }

    override fun equals(other: Any?): Boolean {
        return this === (other as? Entity<*>)?.impl
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    override fun toString(): String {
        return entityClass.simpleName + values
    }
}