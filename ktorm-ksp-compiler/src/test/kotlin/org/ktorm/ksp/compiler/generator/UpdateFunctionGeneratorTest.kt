package org.ktorm.ksp.compiler.generator

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest

class UpdateFunctionGeneratorTest : BaseKspTest() {

    @Test
    fun `sequence update function`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var age: Int,
        )
        
        fun run() {
            val user = database.users.first { it.id eq 1 }
            assert(user.username == "jack")
            
            user.username = "tom"
            database.users.update(user)
            
            val user0 = database.users.first { it.id eq 1 }
            assert(user0.username == "tom")
        }
    """.trimIndent())

    @Test
    fun `modified entity sequence call update fun`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var age: Int
        )
        
        fun run() {
            try {
                val users = database.users.filter { it.id eq 1 }
                users.update(User(1, "lucy", 10))
                throw AssertionError("fail")
            } catch (_: UnsupportedOperationException) {
            }
        }
    """.trimIndent())
}
