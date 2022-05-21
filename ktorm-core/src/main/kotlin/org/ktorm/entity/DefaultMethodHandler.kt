/*
 * Copyright 2018-2022 the original author or authors.
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
import java.lang.reflect.Method
import java.util.*

internal class DefaultMethodHandler(
    private val kotlinDefaultImplMethod: Method? = null,
    private val javaDefaultMethodHandle: MethodHandle? = null
) {

    fun invoke(proxy: Any, args: Array<out Any>?): Any? {
        if (kotlinDefaultImplMethod != null) {
            if (args == null) {
                return kotlinDefaultImplMethod.invoke0(null, proxy)
            } else {
                return kotlinDefaultImplMethod.invoke0(null, proxy, *args)
            }
        }

        if (javaDefaultMethodHandle != null) {
            if (args == null) {
                return javaDefaultMethodHandle.bindTo(proxy).invokeWithArguments()
            } else {
                return javaDefaultMethodHandle.bindTo(proxy).invokeWithArguments(*args)
            }
        }

        throw AssertionError("Non-abstract method in an interface must be a JVM default method or have DefaultImpls")
    }

    companion object {
        private val handlersCache: MutableMap<Method, DefaultMethodHandler> = Collections.synchronizedMap(WeakHashMap())

        fun forMethod(method: Method): DefaultMethodHandler {
            return handlersCache.computeIfAbsent(method) {
                val defaultImpl = getKotlinDefaultImplMethod(method)
                if (defaultImpl != null) {
                    DefaultMethodHandler(kotlinDefaultImplMethod = defaultImpl)
                } else {
                    DefaultMethodHandler(javaDefaultMethodHandle = getJavaDefaultMethodHandle(method))
                }
            }
        }

        private fun getKotlinDefaultImplMethod(method: Method): Method? {
            try {
                val cls = Class.forName(method.declaringClass.name + "\$DefaultImpls")
                return cls.getMethod(method.name, method.declaringClass, *method.parameterTypes)
            } catch (e: ClassNotFoundException) {
                return null
            } catch (e: NoSuchMethodException) {
                return null
            }
        }

        private fun getJavaDefaultMethodHandle(method: Method): MethodHandle? {
            return MethodHandles.lookup().unreflectSpecial(method, method.declaringClass)
        }
    }
}