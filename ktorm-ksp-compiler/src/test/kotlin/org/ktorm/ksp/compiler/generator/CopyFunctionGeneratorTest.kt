package org.ktorm.ksp.compiler.generator

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest

class CopyFunctionGeneratorTest : BaseKspTest() {

    @Test
    fun `interface entity copy function`() = runKotlin("""
        @Table
        interface Employee: Entity<Employee> {
            @PrimaryKey
            var id: Int?
            var name: String
            var job: String
            @Ignore
            var hireDate: LocalDate
        }
        
        fun run() {
            val today = LocalDate.now()
            
            val jack = Employee(name = "jack", job = "programmer", hireDate = today)
            val tom = jack.copy(name = "tom")
            
            assert(tom != jack)
            assert(tom !== jack)
            assert(tom.name == "tom")
            assert(tom.job == "programmer")
            assert(tom.hireDate == today)
            
            with(EntityExtensionsApi()) {
                assert(!tom.hasColumnValue(Employees.id.binding!!))
            }
        }
    """.trimIndent())
}
