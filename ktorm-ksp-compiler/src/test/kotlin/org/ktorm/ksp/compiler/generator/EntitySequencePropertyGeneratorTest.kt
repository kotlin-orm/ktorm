package org.ktorm.ksp.compiler.generator

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest

class EntitySequencePropertyGeneratorTest : BaseKspTest() {

    @Test
    fun `sequenceOf function`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var age: Int
        )
        
        @Table
        interface Employee: Entity<Employee> {
            @PrimaryKey
            var id: Int
            var name: String
            var job: String
            var hireDate: LocalDate
        }
        
        fun run() {
            val users = database.users.toList()
            assert(users.size == 3)
            assert(users[0] == User(id = 1, username = "jack", age = 20))
            assert(users[1] == User(id = 2, username = "lucy", age = 22))
            assert(users[2] == User(id = 3, username = "mike", age = 22))
            
            val employees = database.employees.toList()
            assert(employees.size == 4)
            assert(employees[0] == Employee(id = 1, name = "vince", job = "engineer", hireDate = LocalDate.of(2018, 1, 1)))
            assert(employees[1] == Employee(id = 2, name = "marry", job = "trainee", hireDate = LocalDate.of(2019, 1, 1)))
            assert(employees[2] == Employee(id = 3, name = "tom", job = "director", hireDate = LocalDate.of(2018, 1, 1)))
            assert(employees[3] == Employee(id = 4, name = "penny", job = "assistant", hireDate = LocalDate.of(2019, 1, 1)))
        }
    """.trimIndent())

    @Test
    fun `custom sequence name`() = runKotlin("""
        @Table(entitySequenceName = "aUsers")
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var age: Int
        )
        
        fun run() {
            val users = database.aUsers.toList()
            assert(users.size == 3)
            assert(users[0] == User(id = 1, username = "jack", age = 20))
            assert(users[1] == User(id = 2, username = "lucy", age = 22))
            assert(users[2] == User(id = 3, username = "mike", age = 22))
        }
    """.trimIndent())
}
