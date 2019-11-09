/*
 * Copyright 2018-2019 the original author or authors.
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

package me.liuwj.ktorm.entity

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.defaultValue
import me.liuwj.ktorm.schema.kotlinProperty
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlinFunction

internal class EntityImplementation(
    var entityClass: KClass<*>,
    @Transient var fromDatabase: Database?,
    @Transient var fromTable: Table<*>?,
    @Transient var parent: EntityImplementation?
) : InvocationHandler, Serializable {

    var values = LinkedHashMap<String, Any?>()
    @Transient var changedProperties = LinkedHashSet<String>()

    companion object {
        private const val serialVersionUID = 1L
        private val defaultImplsCache: MutableMap<Method, Method> = Collections.synchronizedMap(WeakHashMap())
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        return when (method.declaringClass.kotlin) {
            Any::class -> {
                when (method.name) {
                    "equals" -> this == args!![0]
                    "hashCode" -> this.hashCode()
                    "toString" -> this.toString()
                    else -> throw IllegalStateException("Unrecognized method: $method")
                }
            }
            Entity::class -> {
                when (method.name) {
                    "getEntityClass" -> this.entityClass
                    "getProperties" -> Collections.unmodifiableMap(this.values)
                    "flushChanges" -> this.doFlushChanges()
                    "discardChanges" -> this.doDiscardChanges()
                    "delete" -> this.doDelete()
                    "get" -> this.getProperty(args!![0] as String)
                    "set" -> this.setProperty(args!![0] as String, args[1])
                    "copy" -> this.copy()
                    else -> throw IllegalStateException("Unrecognized method: $method")
                }
            }
            else -> {
                handleMethodCall(proxy, method, args)
            }
        }
    }

    private fun handleMethodCall(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val ktProp = method.kotlinProperty
        if (ktProp != null) {
            val (prop, isGetter) = ktProp
            if (prop.isAbstract) {
                if (isGetter) {
                    val result = this.getProperty(prop.name)
                    if (result != null || prop.returnType.isMarkedNullable) {
                        return result
                    } else {
                        return prop.defaultValue.also { this.setProperty(prop.name, it) }
                    }
                } else {
                    this.setProperty(prop.name, args!![0])
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

    private val KProperty1<*, *>.defaultValue: Any get() {
        try {
            return returnType.jvmErasure.defaultValue
        } catch (e: Throwable) {
            val msg =
                "The value of non-null property [$this] doesn't exist, " +
                "an error occurred while trying to create a default one. " +
                "Please ensure its value exists, or you can mark the return type nullable [${this.returnType}?]"
            throw IllegalStateException(msg, e)
        }
    }

    @Suppress("SwallowedException")
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
        }
    }

    fun getProperty(name: String): Any? {
        return values[name]
    }

    fun setProperty(name: String, value: Any?, forceSet: Boolean = false) {
        if (!forceSet && isPrimaryKey(name) && name in values) {
            val msg = "Cannot modify the primary key value because it's already set to ${values[name]}"
            throw UnsupportedOperationException(msg)
        }

        values[name] = value
        changedProperties.add(name)
    }

    private fun copy(): Entity<*> {
        val entity = Entity.create(entityClass, parent, fromDatabase, fromTable)
        entity.implementation.values.putAll(values)
        entity.implementation.changedProperties.addAll(changedProperties)
        return entity
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
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is EntityImplementation -> this === other
            is Entity<*> -> this === other.implementation
            else -> false
        }
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    override fun toString(): String {
        return entityClass.simpleName + values
    }
}
