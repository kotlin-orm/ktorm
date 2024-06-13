package org.ktorm.ksp.compiler.generator

import org.junit.Test
import org.ktorm.entity.Entity
import org.ktorm.ksp.compiler.BaseKspTest
import kotlin.reflect.KClass

class TableClassGeneratorTest : BaseKspTest() {

    @Test
    fun `dataClass entity`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int? = null,
            var username: String,
            var age: Int = 0
        )
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "username", "age"))
        }
    """.trimIndent())

    @Test
    fun `data class keyword identifier`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var `class`: String,
            var operator: String,
        ) {
            var `interface`: String = ""
            var constructor: String = ""
        }
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "class", "operator", "interface", "constructor"))
        }
    """.trimIndent())

    @Test
    fun `table annotation`() = runKotlin("""
        @Table(
            name = "t_user", 
            alias = "t_user_alias", 
            catalog = "catalog", 
            schema = "schema", 
            className = "UserTable", 
            entitySequenceName = "userTable", 
            ignoreProperties = ["age"]
        )
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
        ) {
            var age: Int = 10
        }
        
        fun run() {
            assert(UserTable.tableName == "t_user")
            assert(UserTable.alias == "t_user_alias")
            assert(UserTable.catalog == "catalog")
            assert(UserTable.schema == "schema")
            assert(UserTable.columns.map { it.name }.toSet() == setOf("id", "username"))
            println(database.userTable)
        }
    """.trimIndent())

    @Test
    fun `data class constructor with default parameters column`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var phone: String? = "12345"
        )
        
        fun run() {
            val user = database.users.first { it.id eq 1 }
            assert(user.username == "jack")
            assert(user.phone == null)
        }
    """.trimIndent())

    @Test
    fun `data class constructor with default parameters column allowing reflection`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var phone: String? = "12345"
        )
        
        fun run() {
            val user = database.users.first { it.id eq 1 }
            assert(user.username == "jack")
            assert(user.phone == "12345")
        }
    """.trimIndent(), emptyList(), "ktorm.allowReflection" to "true")

    @Test
    fun `ignore properties`() = runKotlin("""
        @Table(ignoreProperties = ["email"])
        data class User(
            @PrimaryKey
            var id: Int,
            var age: Int,
            @Ignore
            var username: String = ""
        ) {
            var email: String = ""
        }
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "age"))
        }
    """.trimIndent())

    @Test
    fun `column has no backingField`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var age: Int
        ) {
            val username: String get() = "username"
        }
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "age"))
        }
    """.trimIndent())

    @Test
    fun `column reference`() = runKotlin("""
        @Table
        interface User: Entity<User> {
            @PrimaryKey
            var id: Int
            var username: String
            var age: Int
            @References
            var firstSchool: School
            @References("second_school_identity")
            var secondSchool: School
        }
        
        @Table
        interface School: Entity<School> {
            @PrimaryKey
            var id: Int
            var schoolName: String
        }
        
        fun run() {
            assert(Users.firstSchoolId.referenceTable is Schools)
            assert(Users.firstSchoolId.name == "first_school_id")
            assert(Users.secondSchoolId.referenceTable is Schools)
            assert(Users.secondSchoolId.name == "second_school_identity")
        }
    """.trimIndent())

    @Test
    fun `interface entity`() = runKotlin("""
        @Table
        interface User: Entity<User> {
            @PrimaryKey
            var id: Int
            var username: String
            var age: Int
        }
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "username", "age"))
        }
    """.trimIndent())

    @Test
    fun `interface entity keyword identifier`() = runKotlin("""
        @Table
        interface User: Entity<User> {
            @PrimaryKey
            var id: Int
            var `class`: String
            var operator: String
        }
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "class", "operator"))
        }
    """.trimIndent())

    @Test
    fun `super class`() = runKotlin("""
        @Table(superClass = CstmTable::class)
        interface User: Entity<User> {
            @PrimaryKey
            var id: Int
            var `class`: String
            var operator: String
        }

        fun run() {
            assert(CstmTable::class.isSubclassOf(CstmTable::class))
        }
    """.trimIndent(), listOf("org.ktorm.ksp.compiler.generator.CstmTable", "kotlin.reflect.full.*"))
}

abstract class CstmTable<E: Entity<E>>(
    tableName: String,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null,
    entityClass: KClass<E>? = null
) : org.ktorm.schema.Table<E>(
    tableName, alias, catalog, schema, entityClass
)
