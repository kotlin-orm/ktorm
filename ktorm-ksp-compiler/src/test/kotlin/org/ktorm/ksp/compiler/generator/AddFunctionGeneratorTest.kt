package org.ktorm.ksp.compiler.generator

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest

class AddFunctionGeneratorTest : BaseKspTest() {

    @Test
    fun testGenerateKey() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var age: Int,
        )
        
        fun run() {
            val user = User(0, "test", 100)
            database.users.add(user, useGeneratedKey = true)
            assert(user.id == 4)
        
            val users = database.users.toList()
            assert(users.size == 4)
            assert(users[0] == User(id = 1, username = "jack", age = 20))
            assert(users[1] == User(id = 2, username = "lucy", age = 22))
            assert(users[2] == User(id = 3, username = "mike", age = 22))
            assert(users[3] == User(id = 4, username = "test", age = 100))
        }
    """.trimIndent())

    @Test
    fun testNoGenerateKey() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var age: Int,
        )
        
        fun run() {
            database.users.add(User(99, "test", 100))
        
            val users = database.users.toList()
            assert(users.size == 4)
            assert(users[0] == User(id = 1, username = "jack", age = 20))
            assert(users[1] == User(id = 2, username = "lucy", age = 22))
            assert(users[2] == User(id = 3, username = "mike", age = 22))
            assert(users[3] == User(id = 99, username = "test", age = 100))
        }
    """.trimIndent())

    @Test
    fun testNoGenerateKey1() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            val id: Int,
            val username: String,
            val age: Int,
        )
        
        fun run() {
            database.users.add(User(99, "test", 100))
        
            val users = database.users.toList()
            assert(users.size == 4)
            assert(users[0] == User(id = 1, username = "jack", age = 20))
            assert(users[1] == User(id = 2, username = "lucy", age = 22))
            assert(users[2] == User(id = 3, username = "mike", age = 22))
            assert(users[3] == User(id = 99, username = "test", age = 100))
        }
    """.trimIndent())

    @Test
    fun testSequenceModified() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            val id: Int,
            val username: String,
            val age: Int,
        )
        
        fun run() {
            try {
                val users = database.users.filter { it.id eq 1 }
                users.add(User(99, "lucy", 10))
                throw AssertionError("fail")
            } catch (_: UnsupportedOperationException) {
            }
        }
    """.trimIndent())
}
