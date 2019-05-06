package me.liuwj.ktorm.entity

import me.liuwj.ktorm.schema.TypeReference
import me.liuwj.ktorm.schema.Table
import java.io.Serializable
import java.lang.reflect.Proxy
import java.sql.SQLException
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

/**
 * 实体类必须实现的接口，为实体类注入通用的操作方法
 *
 * 在框架中，实体类必须声明为 interface(not data class) 并且继承 [Entity]。
 * 在从查询结果中创建实体对象时，框架会使用 JDK 动态代理生成实体类接口的实现，并创建一个对象，
 * 因此在实体里上进行的任何操作都会代理到框架的 [EntityImplementation] 类中，从而使得框架能够检测到实体对象中的任何变化
 *
 * Created by vince on Jun 18, 2018.
 */
interface Entity<E : Entity<E>> : Serializable {

    /**
     * Return this entity's [KClass] instance, which must be an interface.
     */
    val entityClass: KClass<E>

    /**
     * Return the immutable view of this entity's all properties.
     */
    val properties: Map<String, Any?>

    /**
     * 将实体对象中变化的字段保存到数据库，返回受影响的记录数
     */
    @Throws(SQLException::class)
    fun flushChanges(): Int

    /**
     * 清除实体对象中的所有变化数据，清除后调用 [flushChanges] 不会有任何效果
     */
    fun discardChanges()

    /**
     * 在数据库中删除此实体对象所代表的记录，返回受影响的记录数
     */
    @Throws(SQLException::class)
    fun delete(): Int

    /**
     * 使用属性名获取对象中的属性值
     */
    operator fun get(name: String): Any?

    /**
     * 为对象中指定名称的属性设值
     */
    operator fun set(name: String, value: Any?)

    /**
     * 复制一个当前实体对象的拷贝，返回的对象具有与当前对象完全相同的属性值和状态
     */
    fun copy(): E

    companion object {

        /**
         * 创建实体类对象，此方法仅限框架内部使用
         */
        internal fun create(entityClass: KClass<*>, parent: EntityImplementation? = null, fromTable: Table<*>? = parent?.fromTable): Entity<*> {
            if (!entityClass.isSubclassOf(Entity::class)) {
                throw IllegalArgumentException("An entity class must be subclass of Entity.")
            }
            if (!entityClass.java.isInterface) {
                throw IllegalArgumentException("An entity class must be defined as an interface.")
            }

            val classLoader = Thread.currentThread().contextClassLoader
            val handler = EntityImplementation(entityClass, fromTable, parent)
            return Proxy.newProxyInstance(classLoader, arrayOf(entityClass.java), handler) as Entity<*>
        }

        /**
         * 创建实体类对象
         */
        fun create(entityClass: KClass<*>): Entity<*> {
            if (!entityClass.isSubclassOf(Entity::class)) {
                throw IllegalArgumentException("An entity class must be subclass of Entity.")
            }
            if (!entityClass.java.isInterface) {
                throw IllegalArgumentException("An entity class must be defined as an interface.")
            }

            val classLoader = Thread.currentThread().contextClassLoader
            val handler = EntityImplementation(entityClass, null, null)
            return Proxy.newProxyInstance(classLoader, arrayOf(entityClass.java), handler) as Entity<*>
        }

        /**
         * 创建实体类对象
         */
        inline fun <reified E : Entity<E>> create(): E {
            return create(E::class) as E
        }
    }

    /**
     * 用于方便创建实体类对象的抽象工厂，一般作为实体类的伴随对象声明，以支持 EntityClass() 类似普通对象创建的语法
     */
    abstract class Factory<E : Entity<E>> : TypeReference<E>() {

        /**
         * 使用 EntityClass() 创建实体对象，虽然实体类接口没有构造方法，但是创建对象的语法却是相同的
         */
        @Suppress("UNCHECKED_CAST")
        operator fun invoke(): E {
            return create(referencedKotlinType.jvmErasure, null, null) as E
        }

        /**
         * 使用 EntityClass { } 创建实体对象并进行初始化操作
         */
        inline operator fun invoke(init: E.() -> Unit): E {
            return invoke().apply(init)
        }
    }
}
