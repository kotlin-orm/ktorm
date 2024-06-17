package org.ktorm.ksp.compiler.generator;

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest


class SuperTableClassTest : BaseKspTest() {

    @Test
    fun defaultSuperTableClass() = runKotlin(
        """
        import kotlin.reflect.full.isSubclassOf

        @Table
        interface User : Entity<User> {
            var id: Int
            var name: String
        }

        @Table
        data class Department(
            val id: Int,
            val name: String
        )

        fun run() {
            assert(Users::class.isSubclassOf(org.ktorm.schema.Table::class))
            assert(Departments::class.isSubclassOf( org.ktorm.schema.BaseTable::class))
            assert(!Departments::class.isSubclassOf(org.ktorm.schema.Table::class))
        }
    """.trimIndent()
    )

    @Test
    fun superTableClassOnClass() = kspFailing(
        "SuperTableClass annotation can only be used on interface.", """
        abstract class UserBaseTable<E : Entity<E>>(
            tableName: String,
            alias: String? = null,
            catalog: String? = null,
            schema: String? = null,
            entityClass: KClass<E>? = null,
        ) : org.ktorm.schema.Table<E>(
            tableName, alias, catalog, schema, entityClass
        )

        @Table
        @SuperTableClass(UserBaseTable::class)
        data class User(
            val id: Int,
            val name: String
        )
    """.trimIndent()
    )

    @Test
    fun directUseSuperTableClass() = runKotlin(
        """
        import kotlin.reflect.full.isSubclassOf

        abstract class UserBaseTable<E : Entity<E>>(
            tableName: String,
            alias: String? = null,
            catalog: String? = null,
            schema: String? = null,
            entityClass: KClass<E>? = null,
        ) : org.ktorm.schema.Table<E>(
            tableName, alias, catalog, schema, entityClass
        ){
            fun foo() = 0
            fun bar() = 1
        }

        @Table
        @SuperTableClass(UserBaseTable::class)
        interface User : Entity<User> {
            var id: Int
            var name: String
        }

        fun run() {
            assert(Users::class.isSubclassOf(UserBaseTable::class))
        }
    """.trimIndent()
    )

    @Test
    fun singleSuperTableClass() = runKotlin(
        """
        import kotlin.reflect.full.isSubclassOf
        import org.ktorm.schema.long

        @JvmInline
        value class UID(val value: Long)
        
        @SuperTableClass(UserBaseTable::class)
        interface UserBaseEntity<E : UserBaseEntity<E>> : Entity<E> {
            var uid: UID
        }

        abstract class UserBaseTable<E : UserBaseEntity<E>>(
            tableName: String,
            alias: String? = null,
            catalog: String? = null,
            schema: String? = null,
            entityClass: KClass<E>? = null,
        ) : org.ktorm.schema.Table<E>(
            tableName, alias, catalog, schema, entityClass
        ) {
            val uid = long("uid").transform({ UID(it) }, { it.value }).primaryKey().bindTo { it.uid }
        }

        @Table
        interface User : UserBaseEntity<User> {
            var name: String
        }

        fun run() {
            assert(Users::class.isSubclassOf(UserBaseTable::class))
        }
    """.trimIndent()
    )

    @Test
    fun multipleSuperClassTable() = runKotlin(
        """
        import org.ktorm.schema.int
        import kotlin.reflect.full.isSubclassOf

        @SuperTableClass(ATable::class)
        interface AEntity<E : AEntity<E>> : Entity<E> {
            var a: Int
        }
        abstract class ATable<E : AEntity<E>>(
            tableName: String,
            alias: String? = null,
            catalog: String? = null,
            schema: String? = null,
            entityClass: KClass<E>? = null,
        ) : org.ktorm.schema.Table<E>(
            tableName, alias, catalog, schema, entityClass
        ) {
            val a = int("a").bindTo { it.a }
        }

        @SuperTableClass(BTable::class)
        interface BEntity<E : BEntity<E>> : AEntity<E> {
            var b: Int
        }
        abstract class BTable<E : BEntity<E>>(
            tableName: String,
            alias: String? = null,
            catalog: String? = null,
            schema: String? = null,
            entityClass: KClass<E>? = null,
        ) : ATable<E>(
            tableName, alias, catalog, schema, entityClass
        ) {
            val b = int("b").bindTo { it.b }
        }

        @Table(className = "CTable")
        interface CEntity : BEntity<CEntity> {
            var c: Int
        }
        
        fun run() {
            assert(CTable::class.isSubclassOf(ATable::class))
            assert(CTable::class.isSubclassOf(BTable::class))
        }
    """.trimIndent()
    )

    @Test
    fun multipleSuperClassButHaveNotSameInheritanceHierarchy() = kspFailing(
        "the values of annotation are not in the same inheritance hierarchy.", """
        import org.ktorm.schema.int
        import kotlin.reflect.full.isSubclassOf

        @SuperTableClass(ATable::class)
        interface AEntity<E : AEntity<E>> : Entity<E> {
            var a: Int
        }
        abstract class ATable<E : AEntity<E>>(
            tableName: String,
            alias: String? = null,
            catalog: String? = null,
            schema: String? = null,
            entityClass: KClass<E>? = null,
        ) : org.ktorm.schema.Table<E>(
            tableName, alias, catalog, schema, entityClass
        ) {
            val a = int("a").bindTo { it.a }
        }

        @SuperTableClass(BTable::class)
        interface BEntity<E : BEntity<E>> : Entity<E> {
            var b: Int
        }
        abstract class BTable<E : BEntity<E>>(
            tableName: String,
            alias: String? = null,
            catalog: String? = null,
            schema: String? = null,
            entityClass: KClass<E>? = null,
        ) : org.ktorm.schema.Table<E>(
            tableName, alias, catalog, schema, entityClass
        ) {
            val b = int("b").bindTo { it.b }
        }

        @Table(className = "CTable")
        interface CEntity : AEntity<CEntity>, BEntity<CEntity> {
            var c: Int
        }
    """.trimIndent()
    )

    @Test
    fun minimumSuperTableClassParameter() = runKotlin(
        """
        import kotlin.reflect.full.isSubclassOf

        abstract class UserBaseTable<E : Entity<E>>(
            tableName: String,
            alias: String? = null,
        ) : org.ktorm.schema.Table<E>(
            tableName, alias
        )

        @Table
        @SuperTableClass(UserBaseTable::class)
        interface User : Entity<User> {
            var id: Int
            var name: String
        }

        fun run() {
            assert(Users::class.isSubclassOf(UserBaseTable::class))
        }
    """.trimIndent()
    )

    @Test
    fun lackPrimaryConstructor() = kspFailing(
        "should have a primary constructor with parameters tableName and alias.", """
        abstract class UserBaseTable<E : Entity<E>> : org.ktorm.schema.Table<E>("t_user")

        @Table
        @SuperTableClass(UserBaseTable::class)
        interface User : Entity<User> {
            var id: Int
            var name: String
        }
    """.trimIndent()
    )

    @Test
    fun lackAliasParameter() = kspFailing(
        "should have a primary constructor with parameters tableName and alias.", """
        abstract class UserBaseTable<E : Entity<E>>(tableName: String) : org.ktorm.schema.Table<E>(tableName)

        @Table
        @SuperTableClass(UserBaseTable::class)
        interface User : Entity<User> {
            var id: Int
            var name: String
        }
    """.trimIndent()
    )

    @Test
    fun lackTableNameParameter() = kspFailing(
        "should have a primary constructor with parameters tableName and alias.", """
        abstract class UserBaseTable<E : Entity<E>>(alias: String?) : org.ktorm.schema.Table<E>(alias = alias)

        @Table
        @SuperTableClass(UserBaseTable::class)
        interface User : Entity<User> {
            var id: Int
            var name: String
        }
    """.trimIndent()
    )

}

