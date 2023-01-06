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

package org.ktorm.expression

import org.ktorm.entity.DefaultMethodHandler
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * Interceptor that can intercept the visit functions for [SqlExpressionVisitor] and its sub-interfaces.
 *
 * @since 3.6.0
 */
public interface SqlExpressionVisitorInterceptor {

    /**
     * Intercept the visit functions.
     *
     * If a non-null result is returned, this result will be used as the visit result, the origin visit function
     * will be skipped. Otherwise, if null is returned, the origin visit function will be executed, because null
     * value means that we don't want to intercept the logic.
     */
    public fun intercept(expr: SqlExpression, visitor: SqlExpressionVisitor): SqlExpression?
}

/**
 * Create a default visitor instance for [this] interface using the specific [interceptor].
 *
 * @since 3.6.0
 */
@Suppress("UNCHECKED_CAST")
public fun <T : SqlExpressionVisitor> KClass<T>.newVisitorInstance(interceptor: SqlExpressionVisitorInterceptor): T {
    val c = this.java
    if (!c.isInterface) {
        throw IllegalArgumentException("${c.name} is not an interface.")
    }
    if (this.members.any { it.isAbstract }) {
        throw IllegalArgumentException("${c.name} cannot have any abstract members.")
    }

    return Proxy.newProxyInstance(c.classLoader, arrayOf(c), VisitorInvocationHandler(interceptor)) as T
}

/**
 * Visitor invocation handler with intercepting ability.
 */
private class VisitorInvocationHandler(val interceptor: SqlExpressionVisitorInterceptor) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (method.declaringClass.kotlin == Any::class) {
            return when (method.name) {
                "equals" -> proxy === args!![0]
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "Proxy\$${proxy.javaClass.interfaces[0].simpleName}(interceptor=$interceptor)"
                else -> throw IllegalStateException("Unrecognized method: $method")
            }
        }

        if (canIntercept(method)) {
            val r = interceptor.intercept(args!![0] as SqlExpression, proxy as SqlExpressionVisitor)
            if (r != null) {
                return r
            }
        }

        return DefaultMethodHandler.forMethod(method).invoke(proxy, args)
    }

    private fun canIntercept(method: Method): Boolean {
        return method.name.startsWith("visit")
            && method.parameterCount == 1
            && method.parameterTypes[0] == method.returnType
            && SqlExpression::class.java.isAssignableFrom(method.returnType)
    }
}
