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

import org.ktorm.database.Database
import org.ktorm.schema.Table
import org.ktorm.schema.TypeReference
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

/**
 * The super interface of all entity classes in Ktorm. This interface injects many useful functions into entities.
 *
 * Ktorm requires us to define entity classes as interfaces extending from this interface. A simple example is
 * given as follows:
 *
 * ```kotlin
 * interface Department : Entity<Department> {
 *     val id: Int
 *     var name: String
 *     var location: String
 * }
 * ```
 *
 * ### Creating Entity Objects
 *
 * As everyone knows, interfaces cannot be instantiated, so Ktorm provides a [Entity.create] function for us to
 * create entity objects. This function will generate implementations for entity interfaces via JDK dynamic proxy
 * and create their instances.
 *
 * In case you don't like creating objects by [Entity.create], Ktorm also provides an abstract factory class
 * [Entity.Factory]. This class overloads the `invoke` operator of Kotlin, so we just need to add a companion
 * object to our entity class extending from [Entity.Factory], then entity objects can be created just like there
 * is a constructor: `val department = Department()`.
 *
 * ### Getting and Setting Properties
 *
 * Entity objects in Ktorm are proxies, that's why Ktorm can intercept all the invocations on entities and listen
 * the status changes of them. Behind those entity objects, there is a value table that holds all the values of the
 * properties for each entity. Any operation of getting or setting a property is actually operating the underlying
 * value table. However, what if the value doesn't exist while we are getting a property? Ktorm defines a set of
 * rules for this situation:
 *
 * - If the value doesn't exist and the property’s type is nullable (e.g. `var name: String?`), then we’ll return null.
 * - If the value doesn't exist and the property’s type is not nullable (e.g. `var name: String`), then we can not
 * return null anymore, because the null value here can cause an unexpected null pointer exception, we’ll return the
 * type’s default value instead.
 *
 * The default values of different types are well-defined:
 *
 * - For [Boolean] type, the default value is `false`.
 * - For [Char] type, the default value is `\u0000`.
 * - For number types (such as [Int], [Long], [Double], etc), the default value is zero.
 * - For [String] type, the default value is an empty string.
 * - For entity types, the default value is a new-created entity object which is empty.
 * - For enum types, the default value is the first value of the enum, whose ordinal is 0.
 * - For array types, the default value is a new-created empty array.
 * - For collection types (such as [Set], [List], [Map], etc), the default value is a new created mutable collection
 * of the concrete type.
 * - For any other types, the default value is an instance created by its no-args constructor. If the constructor
 * doesn't exist, an exception is thrown.
 *
 * Moreover, there is a cache mechanism for default values, that ensures a property always returns the same default
 * value instance even if it’s called twice or more. This can avoid some counterintuitive bugs.
 *
 * ### Non-abstract members
 *
 * If we are using domain driven design, then entities are not only data containers that hold property values, there
 * are also some behaviors, so we need to add some business functions to our entities. Fortunately, Kotlin allows us
 * to define non-abstract functions in interfaces, that’s why we don’t lose anything even if Ktorm’s entity classes
 * are all interfaces. Here is an example:
 *
 * ```kotlin
 * interface Foo : Entity<Foo> {
 *     companion object : Entity.Factory<Foo>()
 *     val name: String
 *     fun printName() {
 *         println(name)
 *     }
 * }
 * ```
 *
 * Then if we call `Foo().printName()`, the value of the property `name` will be printed.
 *
 * Besides of non-abstract functions, Kotlin also allows us to define properties with custom getters or setters in
 * interfaces. For example, in the following code, if we call `Foo().upperName`, then the value of the `name` property
 * will be returned in upper case:
 *
 * ```kotlin
 * interface Foo : Entity<Foo> {
 *     companion object : Entity.Factory<Foo>()
 *     val name: String
 *     val upperName get() = name.toUpperCase()
 * }
 * ```
 *
 * More details can be found in our website: https://www.ktorm.org/en/entities-and-column-binding.html#More-About-Entities
 *
 * ### Serialization
 *
 * The [Entity] interface extends from [Serializable], so all entity objects are serializable by default. We can save
 * them to our disks, or transfer them between systems through networks.
 *
 * Note that Ktorm only saves entities’ property values when serialization, any other data that used to track entity
 * status are lost (marked as transient). So we can not obtain an entity object from one system, then flush its changes
 * into the database in another system.
 *
 * Java uses [ObjectOutputStream] to serialize objects, and uses [ObjectInputStream] to deserialize them, you can
 * refer to their documentation for more details.
 *
 * Besides of JDK serialization, the ktorm-jackson module also supports serializing entities in JSON format. This
 * module provides an extension for Jackson, the famous JSON framework in Java word. It supports serializing entity
 * objects into JSON format and parsing JSONs as entity objects. More details can be found in its documentation.
 */
public interface Entity<E : Entity<E>> : Serializable {

    /**
     * Return this entity's [KClass] instance, which must be an interface.
     */
    public val entityClass: KClass<E>

    /**
     * Return the immutable view of this entity's all properties.
     */
    public val properties: Map<String, Any?>

    /**
     * Update the property changes of this entity into the database and return the affected record number.
     *
     * Using this function, we need to note that:
     *
     * 1. This function requires a primary key specified in the table object via [Table.primaryKey],
     * otherwise Ktorm doesn't know how to identify entity objects and will throw an exception.
     *
     * 2. The entity object calling this function must be ATTACHED to the database first. In Ktorm’s implementation,
     * every entity object holds a reference `fromDatabase`. For entity objects obtained by sequence APIs, their
     * `fromDatabase` references point to the database they are obtained from. For entity objects created by
     * [Entity.create] or [Entity.Factory], their `fromDatabase` references are `null` initially, so we can not call
     * [flushChanges] on them. But once we use them with [add] or [update] function, `fromDatabase` will be modified
     * to the current database, so we will be able to call [flushChanges] on them afterwards.
     *
     * @see add
     * @see update
     */
    public fun flushChanges(): Int

    /**
     * Clear the tracked property changes of this entity.
     *
     * After calling this function, the [flushChanges] doesn't do anything anymore because the property changes
     * are discarded.
     */
    public fun discardChanges()

    /**
     * Delete this entity in the database and return the affected record number.
     *
     * Similar to [flushChanges], we need to note that:
     *
     * 1. The function requires a primary key specified in the table object via [Table.primaryKey],
     * otherwise, Ktorm doesn't know how to identify entity objects.
     *
     * 2. The entity object calling this function must be ATTACHED to the database first.
     *
     * @see add
     * @see update
     * @see flushChanges
     */
    public fun delete(): Int

    /**
     * Obtain a property's value by its name.
     *
     * Note that this function doesn't follow the rules of default values discussed in the class level documentation.
     * If the value doesn't exist, we will return `null` simply.
     */
    public operator fun get(name: String): Any?

    /**
     * Modify a property's value by its name.
     */
    public operator fun set(name: String, value: Any?)

    /**
     * Return a deep copy of this entity, which has the same property values and tracked statuses.
     */
    public fun copy(): E

    /**
     * Indicate whether some other object is "equal to" this entity.
     * Two entities are equal only if they have the same [entityClass] and [properties].
     *
     * @since 3.4.0
     */
    public override fun equals(other: Any?): Boolean

    /**
     * Return a hash code value for this entity.
     *
     * @since 3.4.0
     */
    public override fun hashCode(): Int

    /**
     * Return a string representation of this table.
     * The format is like `Employee{id=1, name=Eric, job=contributor, hireDate=2021-05-05, salary=50}`.
     */
    public override fun toString(): String

    /**
     * Companion object provides functions to create entity instances.
     */
    public companion object {

        /**
         * Create an entity object. This function is used by Ktorm internal.
         */
        internal fun create(
            entityClass: KClass<*>,
            parent: EntityImplementation? = null,
            fromDatabase: Database? = parent?.fromDatabase,
            fromTable: Table<*>? = parent?.fromTable
        ): Entity<*> {
            if (!entityClass.isSubclassOf(Entity::class)) {
                throw IllegalArgumentException("An entity class must be subclass of Entity.")
            }
            if (!entityClass.java.isInterface) {
                throw IllegalArgumentException("An entity class must be defined as an interface.")
            }

            val handler = EntityImplementation(entityClass, fromDatabase, fromTable, parent)
            return Proxy.newProxyInstance(entityClass.java.classLoader, arrayOf(entityClass.java), handler) as Entity<*>
        }

        /**
         * Create an entity object by JDK dynamic proxy.
         */
        public fun create(entityClass: KClass<*>): Entity<*> {
            return create(entityClass, null, null, null)
        }

        /**
         * Create an entity object by JDK dynamic proxy.
         */
        public inline fun <reified E : Entity<E>> create(): E {
            return create(E::class) as E
        }
    }

    /**
     * Abstract factory used to create entity objects, typically declared as companion objects of entity classes.
     */
    public abstract class Factory<E : Entity<E>> : TypeReference<E>() {

        /**
         * Overload the `invoke` operator, creating an entity object just like there is a constructor.
         */
        @Suppress("UNCHECKED_CAST")
        public operator fun invoke(): E {
            return create(referencedKotlinType.jvmErasure) as E
        }

        /**
         * Overload the `invoke` operator, creating an entity object and call the [init] function.
         */
        public inline operator fun invoke(init: E.() -> Unit): E {
            return invoke().apply(init)
        }
    }
}
