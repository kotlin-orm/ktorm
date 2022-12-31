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

package org.ktorm.schema

import org.ktorm.entity.Entity
import org.ktorm.entity.defaultValue
import org.ktorm.entity.kotlinProperty
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

@PublishedApi
internal class ColumnBindingHandler(val properties: MutableList<KProperty1<*, *>>) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        when (method.declaringClass.kotlin) {
            Any::class, Entity::class -> {
                error("Unsupported method: $method")
            }
            else -> {
                val (prop, isGetter) = method.kotlinProperty ?: error("Unsupported method: $method")
                if (!prop.isAbstract) {
                    error("Cannot bind a column to a non-abstract property: $prop")
                }
                if (!isGetter) {
                    error("Cannot modify a property while we are binding a column to it, property: $prop")
                }

                properties += prop

                val returnType = method.returnType
                return when {
                    returnType.kotlin.isSubclassOf(Entity::class) -> createProxy(returnType.kotlin, properties)
                    returnType.isPrimitive -> returnType.defaultValue
                    else -> null
                }
            }
        }
    }

    private fun error(msg: String): Nothing {
        throw UnsupportedOperationException(msg)
    }

    companion object {

        fun createProxy(entityClass: KClass<*>, properties: MutableList<KProperty1<*, *>>): Entity<*> {
            val handler = ColumnBindingHandler(properties)
            return Proxy.newProxyInstance(entityClass.java.classLoader, arrayOf(entityClass.java), handler) as Entity<*>
        }
    }
}
