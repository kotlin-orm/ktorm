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

package org.ktorm.entity

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodHandles.Lookup
import java.lang.invoke.MethodHandles.Lookup.*
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*

internal class DefaultMethodHandler(
    private val javaDefaultMethodHandle: MethodHandle? = null,
    private val kotlinDefaultImplMethod: Method? = null
) {

    fun invoke(proxy: Any, args: Array<out Any>?): Any? {
        if (javaDefaultMethodHandle != null) {
            if (args == null) {
                return javaDefaultMethodHandle.bindTo(proxy).invokeWithArguments()
            } else {
                return javaDefaultMethodHandle.bindTo(proxy).invokeWithArguments(*args)
            }
        }

        if (kotlinDefaultImplMethod != null) {
            if (args == null) {
                return kotlinDefaultImplMethod.invoke0(null, proxy)
            } else {
                return kotlinDefaultImplMethod.invoke0(null, proxy, *args)
            }
        }

        throw AssertionError("Non-abstract method in an interface must be a JVM default method or have DefaultImpls")
    }

    companion object {
        private val handlersCache = Collections.synchronizedMap(WeakHashMap<Method, DefaultMethodHandler>())
        private val privateLookupIn: Method?
        private val lookupConstructor: Constructor<Lookup>?

        init {
            privateLookupIn = initPrivateLookupInMethod()
            lookupConstructor = if (privateLookupIn == null) initLookupConstructor() else null
        }

        @Suppress("SwallowedException")
        private fun initPrivateLookupInMethod(): Method? {
            try {
                return MethodHandles::class.java.getMethod("privateLookupIn", Class::class.java, Lookup::class.java)
            } catch (e: NoSuchMethodException) {
                // MethodHandles.privateLookupIn(Class, MethodHandles.Lookup) doesn't exist in JDK 1.8.
                return null
            }
        }

        private fun initLookupConstructor(): Constructor<Lookup>? {
            try {
                // This branch only runs in JDK 1.8, so the reflection operation (setAccessible) is safe.
                val c = Lookup::class.java.getDeclaredConstructor(Class::class.java, Int::class.javaPrimitiveType)
                c.isAccessible = true
                return c
            } catch (e: NoSuchMethodException) {
                val msg = "" +
                    "Cannot find constructor MethodHandles.Lookup(Class, int), " +
                    "please ensure you are using JDK 1.8 or above."
                throw IllegalStateException(msg, e)
            }
        }

        @Suppress("SwallowedException")
        private fun unreflectSpecial(method: Method): MethodHandle {
            // For JDK 9 or above.
            if (privateLookupIn != null) {
                val lookup = privateLookupIn.invoke0(null, method.declaringClass, MethodHandles.lookup()) as Lookup
                return lookup.unreflectSpecial(method, method.declaringClass)
            }

            // For JDK 1.8.
            if (lookupConstructor != null) {
                try {
                    val allModes = PUBLIC or PRIVATE or PROTECTED or PACKAGE
                    val lookup = lookupConstructor.newInstance(method.declaringClass, allModes)
                    return lookup.unreflectSpecial(method, method.declaringClass)
                } catch (e: InvocationTargetException) {
                    throw e.targetException
                }
            }

            // Throws error for JDK version lower than 1.8.
            val msg = "" +
                "Cannot find constructor MethodHandles.Lookup(Class, int), " +
                "please ensure you are using JDK 1.8 or above."
            throw AssertionError(msg)
        }

        fun forMethod(method: Method): DefaultMethodHandler {
            // Workaround for the compiler bug, see https://youtrack.jetbrains.com/issue/KT-34826
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
            val cache = handlersCache as java.util.Map<Method, DefaultMethodHandler>

            return cache.computeIfAbsent(method) {
                if (method.isDefault) {
                    val handle = unreflectSpecial(method)
                    DefaultMethodHandler(javaDefaultMethodHandle = handle)
                } else {
                    val classLoader = method.declaringClass.classLoader
                    val cls = Class.forName(method.declaringClass.name + "\$DefaultImpls", true, classLoader)
                    val impl = cls.getMethod(method.name, method.declaringClass, *method.parameterTypes)
                    DefaultMethodHandler(kotlinDefaultImplMethod = impl)
                }
            }
        }
    }
}
