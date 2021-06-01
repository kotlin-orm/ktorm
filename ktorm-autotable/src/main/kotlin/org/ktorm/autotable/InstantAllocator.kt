/*
 * Copyright 2018-2021 the original author or authors.
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

package org.ktorm.autotable

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

internal object InstantAllocator {
    enum class AllocateFunction { INSTANCE, UNSAFE, KOBJECT, NONE }

    private val allocateFunctionMap = ConcurrentHashMap<Class<*>, AllocateFunction>()

    operator fun <T> invoke(
        clazz: Class<out T>,
        unsafe: Boolean = true,
    ): T = get(clazz, unsafe)

    inline operator fun <reified T : Any> invoke(
        unsafe: Boolean = true,
    ): T = get(T::class.java, unsafe)

    operator fun <T : Any> get(
        clazz: KClass<out T>,
        unsafe: Boolean = true,
    ): T = get(clazz.java, unsafe)

    operator fun <T> get(clazz: Class<out T>, unsafe: Boolean = true): T {
        return when (allocateFunctionMap[clazz]) {
            null -> try {
                val newInstance = clazz.newInstance()
                allocateFunctionMap[clazz] = AllocateFunction.INSTANCE
                newInstance
            } catch (e: Exception) {
                val kClass = clazz.kotlin
                val objectInstance = kClass.objectInstance
                when {
                    objectInstance != null -> {
                        allocateFunctionMap[clazz] = AllocateFunction.KOBJECT
                        objectInstance
                    }
                    unsafe -> try {
                        allocateFunctionMap[clazz] = AllocateFunction.UNSAFE
                        @Suppress("UNCHECKED_CAST")
                        Unsafe.theUnsafe.allocateInstance(clazz) as T
                    } catch (e: Exception) {
                        allocateFunctionMap[clazz] = AllocateFunction.NONE
                        throwNoSuchMethodException(clazz)
                    }
                    else -> throwNoSuchMethodException(clazz, e)
                }
            }
            AllocateFunction.INSTANCE -> clazz.newInstance()
            AllocateFunction.UNSAFE -> if (unsafe) {
                @Suppress("UNCHECKED_CAST")
                Unsafe.theUnsafe.allocateInstance(clazz) as T
            } else {
                throwNoSuchMethodException(clazz)
            }
            AllocateFunction.KOBJECT -> clazz.kotlin.objectInstance!!
            AllocateFunction.NONE -> throwNoSuchMethodException(clazz)
        }
    }

    private fun throwNoSuchMethodException(clazz: Class<*>, e: Exception? = null): Nothing {
        throw NoSuchMethodException("${clazz.name}:<init>()").initCause(e)
    }
}
