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
    fun testCondition1() {
        val entity = Department {
            name = "te"
        }
        
        database.departmentsWithCondition
            .filterBy(entity)
            .forEach {
                assert(it.name.startsWith(entity.name))
                assert(it.mixedCase != null)
            }
        
        database.from(DepartmentsWithCondition)
            .select()
            .whereBy(DepartmentsWithCondition, entity)
            .forEach {
                assert(it[DepartmentsWithCondition.name]!!.startsWith(entity.name))
                assert(it[DepartmentsWithCondition.mixedCase] != null)
            }
    }
    
    @Test
    fun testCondition2() {
        val entity = Department {
            name = "te"
            location = LocationWrapper("Guangzhou")
        }
        
        database.departmentsWithCondition
            .filterBy(entity)
            .forEach {
                assert(it.name.startsWith(entity.name))
                assert(it.location.underlying == entity.location.underlying)
            }
        
        database.from(DepartmentsWithCondition)
            .select()
            .whereBy(DepartmentsWithCondition, entity)
            .forEach {
                assert(it[DepartmentsWithCondition.name]!!.startsWith(entity.name))
                assert(it[DepartmentsWithCondition.location]?.underlying == entity.location.underlying)
            }
    }
    
    @Test
    fun testCondition3() {
        val entityManagerId = 1
        val entity = Employee {
            manager = Employee {
                id = entityManagerId
            }
        }
        
        database.employeesWithCondition
            .filterBy(entity)
            .forEach {
                assert(it.manager?.id == entityManagerId)
            }
        
        database.from(EmployeesWithCondition)
            .select()
            .whereBy(EmployeesWithCondition, entity)
            .forEach {
                assert(it[EmployeesWithCondition.managerId] == entityManagerId)
            }
    }
    
    @Test
    fun testCondition4() {
        val entity = Employee {
            salary = 50
            department = Department {
                name = "dep"
            }
        }
        database.employeesWithCondition
            .filterBy(entity)
            .forEach {
                assert(it.salary > entity.salary)
            }
        
        database.from(EmployeesWithCondition)
            .select()
            .whereBy(EmployeesWithCondition, entity)
            .forEach {
                assert(it[EmployeesWithCondition.salary]!! > entity.salary)
            }
    }
    
    
    @Test
    fun testConditionAndThen1() {
        val locationWrapper = LocationWrapper("Guangzhou")
        
        val entity = Department {
            name = "te"
        }
        
        database.departmentsWithCondition
            .filterBy(entity) { table, condition ->
                condition and (table.location eq locationWrapper)
            }
            .forEach {
                assert(it.name.startsWith(entity.name))
                assert(it.mixedCase != null)
                assert(it.location.underlying != locationWrapper.underlying)
            }
        
        database.from(DepartmentsWithCondition)
            .select()
            .whereBy(DepartmentsWithCondition, entity) {
                it and (DepartmentsWithCondition.location eq locationWrapper)
            }
            .forEach {
                assert(it[DepartmentsWithCondition.name]!!.startsWith(entity.name))
                assert(it[DepartmentsWithCondition.mixedCase] != null)
                assert(it[DepartmentsWithCondition.location]?.underlying != locationWrapper.underlying)
            }
    }
    
    @Test
    fun testConditionAndThen2() {
        val locationWrapper = LocationWrapper("Guangzhou")
        
        val entity = Department {
            name = "te"
        }
        
        database.departmentsWithCondition
            .filterByOr(entity) { table, condition ->
                val extraCondition = table.location eq locationWrapper
                condition?.and(extraCondition) ?: extraCondition
            }
            .forEach {
                assert(it.name.startsWith(entity.name))
                assert(it.location.underlying == entity.location.underlying)
            }
        
        database.from(DepartmentsWithCondition)
            .select()
            .whereByOr(DepartmentsWithCondition, entity) {
                it?.and(DepartmentsWithCondition.location eq locationWrapper) // or null.
            }
            .forEach {
                assert(it[DepartmentsWithCondition.name]!!.startsWith(entity.name))
                assert(it[DepartmentsWithCondition.location]?.underlying == entity.location.underlying)
            }
    }
    
    
    open class DepartmentsWithCondition(alias: String?) : ConditionalTable<Department>("t_department", alias) {
        companion object : DepartmentsWithCondition(null)
        
        override fun aliased(alias: String) = DepartmentsWithCondition(alias)
        
        val id = int("id").primaryKey().bindTo { it.id }.conditionNotNullOn { c, v ->
            c eq v
        }
        val name = varchar("name").bindTo { it.name }.conditionNotNullOn { c, v ->
            c like "$v%"
        }
        
        val location = varchar("location").transform({ LocationWrapper(it) }, { it.underlying }).bindTo { it.location }
            .conditionOn { column, locationWrapper ->
                println("location wrapper: $locationWrapper")
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
        val managerId = int("manager_id").bindTo { it.manager?.id }.conditionNotNullOn { column, i ->
            column eq i
        }
        val hireDate = date("hire_date").bindTo { it.hireDate }
        val salary = long("salary").bindTo { it.salary }.conditionOn { column, value ->
            column greater (value ?: 0)
        }
        val departmentId = int("department_id").references(Departments) { it.department }
        val department = departmentId.referenceTable as Departments
    }
    
    open class CustomersWithC(alias: String?) : ConditionalTable<Customer>("t_customer", alias, schema = "company") {
        companion object : CustomersWithC(null)
        
        override fun aliased(alias: String) = CustomersWithC(alias)
        
        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").bindTo { it.name }
        @Suppress("unused")
        val email = varchar("email").bindTo { it.email }
        @Suppress("unused")
        val phoneNumber = varchar("phone_number").bindTo { it.phoneNumber }
    }
    
    private val Database.departmentsWithCondition get() = this.sequenceOf(DepartmentsWithCondition)
    
    private val Database.employeesWithCondition get() = this.sequenceOf(EmployeesWithCondition)
    
}