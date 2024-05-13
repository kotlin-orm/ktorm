package org.ktorm.ksp.compiler.generator

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest

class PseudoConstructorFunctionGeneratorTest : BaseKspTest() {

    @Test
    fun `interface entity constructor function`() = runKotlin("""
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
            assert(Employee().toString() == "Employee()")
            assert(Employee(id = null).toString() == "Employee(id=null)")
            assert(Employee(id = null, name = "").toString() == "Employee(id=null, name=)")
            assert(Employee(1, "vince", "engineer", LocalDate.of(2023, 1, 1)).toString() == "Employee(id=1, name=vince, job=engineer, hireDate=2023-01-01)")
        }
    """.trimIndent())
}
