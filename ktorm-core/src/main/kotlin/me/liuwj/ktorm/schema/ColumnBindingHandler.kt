package me.liuwj.ktorm.schema

import me.liuwj.ktorm.entity.Entity
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

/* internal */
class ColumnBindingHandler(val properties: MutableList<KProperty1<*, *>>) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        when (method.declaringClass.kotlin) {
            Any::class, Entity::class -> {
                throw UnsupportedOperationException("Unsupported method: $method")
            }
            else -> {
                val (prop, isGetter) = method.kotlinProperty ?: throw UnsupportedOperationException("Unsupported method: $method")
                if (!prop.isAbstract) {
                    throw UnsupportedOperationException("Cannot bind a column to a non-abstract property: $prop")
                }
                if (!isGetter) {
                    throw UnsupportedOperationException("Cannot modify a property while we are binding a column to it, property: $prop")
                }

                properties += prop

                val returnType = prop.returnType.classifier as KClass<*>
                return when {
                    returnType.isSubclassOf(Entity::class) -> createProxy(returnType, properties)
                    returnType.java.isPrimitive -> returnType.defaultValue
                    else -> null
                }
            }
        }
    }

    companion object {

        fun createProxy(entityClass: KClass<*>, properties: MutableList<KProperty1<*, *>>): Entity<*> {
            val classLoader = Thread.currentThread().contextClassLoader
            val handler = ColumnBindingHandler(properties)
            return Proxy.newProxyInstance(classLoader, arrayOf(entityClass.java), handler) as Entity<*>
        }
    }
}