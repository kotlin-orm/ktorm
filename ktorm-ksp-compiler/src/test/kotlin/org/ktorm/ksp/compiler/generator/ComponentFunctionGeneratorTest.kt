package org.ktorm.ksp.compiler.generator

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest

class ComponentFunctionGeneratorTest : BaseKspTest() {

    @Test
    fun `interface entity component function`() = runKotlin("""
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
            
            val employee = Entity.create<Employee>()
            employee.id = 1
            employee.name = "name"
            employee.job = "job"
            employee.hireDate = today
            
            val (id, name, job, hireDate) = employee
            assert(id == 1)
            assert(name == "name")
            assert(job == "job")
            assert(hireDate == today)
        }
    """.trimIndent())
}
