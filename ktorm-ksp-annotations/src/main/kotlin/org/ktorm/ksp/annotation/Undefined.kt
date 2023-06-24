/*
 * Copyright 2022-2023 the original author or authors.
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

@file:Suppress("NoMultipleSpaces")

package org.ktorm.ksp.annotation

import sun.misc.Unsafe
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility class that creates unique `undefined` values for any class.
 *
 * These `undefined` values are typically used as default values of parameters in ktorm-ksp generated pseudo
 * constructor functions for entities. Pseudo constructors check if the parameter is referential identical to
 * the `undefined` value to judge whether it is manually assigned by users or not. We don't use `null` in
 * this case because `null` can also be a valid value for entity properties.
 *
 * For example:
 *
 * ```kotlin
 * public fun Employee(name: String? = Undefined.of()): Employee {
 *     val entity = Entity.create<Employee>()
 *     if (name !== Undefined.of<String>()) {
 *         entity.name = name
 *     }
 *     return entity
 * }
 * ```
 *
 * In this example, `Employee("vince")` creates an employee named vince, `Employee(null)` creates an employee with
 * null name (can also be valid in some cases), and, in the meanwhile, `Employee()` creates an employee without giving
 * a name, which is different with `Employee(null)`.
 *
 * Note: `undefined` values created by this class can only be used for referential comparing, any method invocation
 * on these values can cause exceptions.
 */
public object Undefined {
    private val unsafe = getUnsafe()
    private val undefinedValuesCache = ConcurrentHashMap<Class<*>, Any>()

    /**
     * Return the `undefined` value for class [T]. See more details in the class level document.
     *
     * Note: this function never returns null, but we have to mark it as nullable to ensure we are always boxing
     * JVM primitive types and Kotlin inline classes.
     */
    public inline fun <reified T : Any> of(): T? {
        return getUndefinedValue(T::class.java) as T?
    }

    /**
     * Get or create the `undefined` value for class [cls].
     */
    @PublishedApi
    internal fun getUndefinedValue(cls: Class<*>): Any {
        return undefinedValuesCache.computeIfAbsent(cls) {
            if (cls.isArray) {
                java.lang.reflect.Array.newInstance(cls.componentType, 0)
            } else if (cls.isInterface) {
                createUndefinedValueByJdkProxy(cls)
            } else if (Modifier.isAbstract(cls.modifiers)) {
                createUndefinedValueBySubclassing(cls)
            } else {
                unsafe.allocateInstance(cls)
            }
        }
    }

    /**
     * Obtain the [Unsafe] instance by reflection.
     */
    private fun getUnsafe(): Unsafe {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        return field.get(null) as Unsafe
    }

    /**
     * Create the `undefined` value for interface by JDK dynamic proxy.
     */
    private fun createUndefinedValueByJdkProxy(cls: Class<*>): Any {
        return Proxy.newProxyInstance(cls.classLoader, arrayOf(cls)) { proxy, method, args ->
            when (method.declaringClass.kotlin) {
                Any::class -> {
                    when (method.name) {
                        "equals" -> proxy === args!![0]
                        "hashCode" -> System.identityHashCode(proxy)
                        "toString" -> "Ktorm undefined value proxy for ${cls.name}"
                        else -> throw UnsupportedOperationException("Method not supported: $method")
                    }
                }
                else -> {
                    throw UnsupportedOperationException("Method not supported: $method")
                }
            }
        }
    }

    /**
     * Create the `undefined` value for abstract class by generating a subclass dynamically.
     */
    private fun createUndefinedValueBySubclassing(cls: Class<*>): Any {
        val subclassName = if (cls.name.startsWith("java.")) "\$${cls.name}\$Undefined" else "${cls.name}\$Undefined"
        val classLoader = UndefinedClassLoader(cls.classLoader ?: Thread.currentThread().contextClassLoader)
        val subclass = Class.forName(subclassName, true, classLoader)
        return unsafe.allocateInstance(subclass)
    }

    /**
     * Class loader that generates `undefined` subclasses.
     *
     * Note:
     *
     * 1. Subclasses generated by this class loader doesn't have any constructors, so we have to use
     * [Unsafe.allocateInstance] to create instances.
     *
     * 2. Subclasses generated by this class loader doesn't implement any abstract methods from their super classes,
     * so any invocation on those abstract methods will cause [AbstractMethodError].
     */
    private class UndefinedClassLoader(parent: ClassLoader) : ClassLoader(parent) {

        override fun findClass(name: String): Class<*> {
            if (!name.endsWith("\$Undefined")) {
                throw ClassNotFoundException(name)
            }

            val className = name.replace(".", "/")
            val superClassName = className.removePrefix("\$").removeSuffix("\$Undefined")
            val bytes = generateByteCode(className.toByteArray(), superClassName.toByteArray())
            return defineClass(name, bytes, null)
        }

        @Suppress("MagicNumber")
        private fun generateByteCode(className: ByteArray, superClassName: ByteArray): ByteBuffer {
            val buf = ByteBuffer.allocate(1024)
            buf.putInt(0xCAFEBABE.toInt())                          // magic
            buf.putShort(0)                                         // minor version
            buf.putShort(52)                                        // major version, 52 for JDK1.8
            buf.putShort(5)                                         // constant pool count, 5 means 4 constants in all
            buf.put(1)                                              // #1, CONSTANT_Utf8_info
            buf.putShort(className.size.toShort())                  // length
            buf.put(className)                                      // class name
            buf.put(7)                                              // #2, CONSTANT_Class_info
            buf.putShort(1)                                         // name index, ref to constant #1
            buf.put(1)                                              // #3, CONSTANT_Utf8_info
            buf.putShort(superClassName.size.toShort())             // length
            buf.put(superClassName)                                 // super class name
            buf.put(7)                                              // #4, CONSTANT_Class_info
            buf.putShort(3)                                         // name index, ref to constant #3
            buf.putShort((0x0001 or 0x0020 or 0x1000).toShort())    // ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC
            buf.putShort(2)                                         // this class, ref to constant #2
            buf.putShort(4)                                         // super class, ref to constant #4
            buf.putShort(0)                                         // interfaces count
            buf.putShort(0)                                         // fields count
            buf.putShort(0)                                         // methods count
            buf.putShort(0)                                         // attributes count
            buf.flip()
            return buf
        }
    }
}
