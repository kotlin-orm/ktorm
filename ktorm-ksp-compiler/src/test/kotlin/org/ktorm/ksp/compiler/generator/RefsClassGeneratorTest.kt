package org.ktorm.ksp.compiler.generator

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest

/**
 * Created by vince at Jul 16, 2023.
 */
class RefsClassGeneratorTest : BaseKspTest() {

    @Test
    fun testRefs() = runKotlin(
        """
        @Table
        interface Employee: Entity<Employee> {
            @PrimaryKey
            var id: Int
            var name: String
            var job: String
            var managerId: Int?
            var hireDate: LocalDate
            var salary: Long
            @References
            var department: Department
        }
        
        @Table
        interface Department: Entity<Department> {
            @PrimaryKey
            var id: Int
            var name: String
            var location: String
        }
        
        fun run() {
            val employees = database.employees
                .filter { it.refs.department.name eq "tech" }
                .filter { it.refs.department.location eq "Guangzhou" }
                .sortedBy { it.id }
                .toList()
            
            assert(employees.size == 2)
            assert(employees[0].name == "vince")
            assert(employees[1].name == "marry")
        }
    """.trimIndent())
}