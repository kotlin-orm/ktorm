package org.ktorm.conditional

import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.forEach
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

/**
 *
 * @author ForteScarlet
 */
class ConditionalTableTest : BaseTest() {

    @Test
    fun testCondition() {
        println("a")
        println("a")
        println("a")
        println("a")
        println("a")
        println("a")
        println("a")
        val entity1 = Department {
            name = "te"
        }

        database.departmentsWithCondition
            .filterBy(entity1)
            .forEach {
                println(it)
                assert(it.name.startsWith(entity1.name))
                assert(it.mixedCase != null)
            }

        database.from(DepartmentsWithCondition)
            .select()
            .whereBy(DepartmentsWithCondition, entity1)
            .forEach {
                assert(it[DepartmentsWithCondition.name]!!.startsWith(entity1.name))
                assert(it[DepartmentsWithCondition.mixedCase] != null)
            }


    }


    open class DepartmentsWithCondition(alias: String?) : ConditionalTable<Department>("t_department", alias) {
        companion object : DepartmentsWithCondition(null)

        override fun aliased(alias: String) = DepartmentsWithCondition(alias)

        val id = int("id").primaryKey().bindTo { it.id }.conditionNotNullOn { _, c, v ->
            c eq v
        }
        val name = varchar("name").bindTo { it.name }.conditionNotNullOn { _, c, v ->
            c like "$v%"
        }

        val location = varchar("location").transform({ LocationWrapper(it) }, { it.underlying }).bindTo { it.location }
            .conditionOn { _, column, locationWrapper ->
                if (locationWrapper == null) {
                    (column.table as DepartmentsWithCondition).mixedCase.isNotNull()
                } else {
                    column eq locationWrapper
                }

            }
        val mixedCase = varchar("mixedCase").bindTo { it.mixedCase }
    }

    open class EmployeesWithCondition(alias: String?) : ConditionalTable<Employee>("t_employee", alias) {
        companion object : EmployeesWithCondition(null)

        override fun aliased(alias: String) = EmployeesWithCondition(alias)

        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").bindTo { it.name }
        val job = varchar("job").bindTo { it.job }
        val managerId = int("manager_id").bindTo { it.manager?.id }
        val hireDate = date("hire_date").bindTo { it.hireDate }
        val salary = long("salary").bindTo { it.salary }
        val departmentId = int("department_id").references(Departments) { it.department }
        val department = departmentId.referenceTable as Departments
    }

    open class CustomersWithC(alias: String?) : ConditionalTable<Customer>("t_customer", alias, schema = "company") {
        companion object : CustomersWithC(null)

        override fun aliased(alias: String) = CustomersWithC(alias)

        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").bindTo { it.name }
        val email = varchar("email").bindTo { it.email }
        val phoneNumber = varchar("phone_number").bindTo { it.phoneNumber }
    }

    val Database.departmentsWithCondition get() = this.sequenceOf(DepartmentsWithCondition)

    val Database.employeesWithCondition get() = this.sequenceOf(EmployeesWithCondition)

    val Database.customersWithC get() = this.sequenceOf(CustomersWithC)
}