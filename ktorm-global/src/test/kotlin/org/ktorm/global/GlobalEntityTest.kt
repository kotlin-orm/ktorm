package org.ktorm.global

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.*
import java.time.LocalDate

/**
 * Created by vince at Apr 05, 2020.
 */
@Suppress("DEPRECATION")
class GlobalEntityTest : BaseGlobalTest() {

    @Test
    fun testFind() {
        val employee = Employees.findOne { it.id eq 1 } ?: throw AssertionError()
        println(employee)

        assert(employee.name == "vince")
        assert(employee.job == "engineer")
    }

    @Test
    fun testFindWithReference() {
        val employees = Employees
            .asSequence()
            .filter {
                val dept = it.departmentId.referenceTable as Departments
                dept.location like "%Guangzhou%"
            }
            .sortedBy { it.id }
            .toList()

        assert(employees.size == 2)
        assert(employees[0].name == "vince")
        assert(employees[1].name == "marry")
    }

    @Test
    fun testCreateEntity() {
        val employees = Employees
            .joinReferencesAndSelect()
            .where {
                val dept = Employees.departmentId.referenceTable as Departments
                dept.location like "%Guangzhou%"
            }
            .orderBy(Employees.id.asc())
            .map { Employees.createEntity(it) }

        assert(employees.size == 2)
        assert(employees[0].name == "vince")
        assert(employees[1].name == "marry")
    }

    @Test
    fun testUpdate() {
        var employee = Employee()
        employee.id = 2
        employee.job = "engineer"
        employee.salary = 100
        // employee.manager = null
        Employees.updateEntity(employee)

        employee = Employees.findOne { it.id eq 2 } ?: throw AssertionError()
        assert(employee.job == "engineer")
        assert(employee.salary == 100L)
        assert(employee.manager?.id == 1)
    }

    @Test
    fun testFlushChanges() {
        var employee = Employees.findOne { it.id eq 2 } ?: throw AssertionError()
        employee.job = "engineer"
        employee.salary = 100
        employee.flushChanges()
        employee.flushChanges()

        employee = Employees.findOne { it.id eq 2 } ?: throw AssertionError()
        assert(employee.job == "engineer")
        assert(employee.salary == 100L)
    }

    @Test
    fun testDeleteEntity() {
        val employee = Employees.findOne { it.id eq 2 } ?: throw AssertionError()
        employee.delete()

        assert(Employees.count() == 3)
    }

    @Test
    fun testSaveEntity() {
        var employee = Employee {
            name = "jerry"
            job = "trainee"
            manager = Employees.findOne { it.name eq "vince" }
            hireDate = LocalDate.now()
            salary = 50
            department = Departments.findOne { it.name eq "tech" } ?: throw AssertionError()
        }

        Employees.addEntity(employee)
        println(employee)

        employee = Employees.findOne { it.id eq 5 } ?: throw AssertionError()
        assert(employee.name == "jerry")
        assert(employee.department.name == "tech")

        employee.job = "engineer"
        employee.salary = 100
        employee.flushChanges()

        employee = Employees.findOne { it.id eq 5 } ?: throw AssertionError()
        assert(employee.job == "engineer")
        assert(employee.salary == 100L)

        employee.delete()
        assert(Employees.count() == 4)
    }

    @Test
    fun testFindMapById() {
        val employees = Employees.asSequence().filter { it.id.inList(1, 2) }.associateBy { it.id }
        assert(employees.size == 2)
        assert(employees[1]?.name == "vince")
        assert(employees[2]?.name == "marry")
    }

    interface Parent : Entity<Parent> {
        var child: Child
    }

    interface Child : Entity<Child> {
        var grandChild: GrandChild
    }

    interface GrandChild : Entity<GrandChild> {
        var id: Int
    }

    object Parents : Table<Parent>("t_employee") {
        val id = int("id").primaryKey().bindTo { it.child.grandChild.id }
    }

    @Test
    fun testUpdatePrimaryKey() {
        try {
            val parent = Parents.findOne { it.id eq 1 } ?: throw AssertionError()
            assert(parent.child.grandChild.id == 1)

            parent.child.grandChild.id = 2
            throw AssertionError()

        } catch (e: UnsupportedOperationException) {
            // expected
            println(e.message)
        }
    }

    interface EmployeeTestForReferencePrimaryKey : Entity<EmployeeTestForReferencePrimaryKey> {
        var employee: Employee
        var manager: EmployeeManagerTestForReferencePrimaryKey
    }

    interface EmployeeManagerTestForReferencePrimaryKey : Entity<EmployeeManagerTestForReferencePrimaryKey> {
        var employee: Employee
    }

    object EmployeeTestForReferencePrimaryKeys : Table<EmployeeTestForReferencePrimaryKey>("t_employee0") {
        val id = int("id").primaryKey().references(Employees) { it.employee }
        val managerId = int("manager_id").bindTo { it.manager.employee.id }
    }

    @Test
    fun testUpdateReferencesPrimaryKey() {
        val e = EmployeeTestForReferencePrimaryKeys.findOne { it.id eq 2 } ?: return
        e.manager.employee = Employees.findOne { it.id eq 1 } ?: return

        try {
            e.employee = Employees.findOne { it.id eq 1 } ?: return
            throw AssertionError()
        } catch (e: UnsupportedOperationException) {
            // expected
            println(e.message)
        }

        e.flushChanges()
    }

    @Test
    fun testForeignKeyValue() {
        val employees = Employees
            .select()
            .orderBy(Employees.id.asc())
            .map { Employees.createEntity(it) }

        val vince = employees[0]
        assert(vince.manager == null)
        assert(vince.department.id == 1)

        val marry = employees[1]
        assert(marry.manager?.id == 1)
        assert(marry.department.id == 1)

        val tom = employees[2]
        assert(tom.manager == null)
        assert(tom.department.id == 2)

        val penny = employees[3]
        assert(penny.manager?.id == 3)
        assert(penny.department.id == 2)
    }

    @Test
    fun testCreateEntityWithoutReferences() {
        val employees = Employees
            .leftJoin(Departments, on = Employees.departmentId eq Departments.id)
            .select(Employees.columns + Departments.columns)
            .map { Employees.createEntity(it, withReferences = false) }

        employees.forEach { println(it) }

        assert(employees.size == 4)
        assert(employees[0].department.id == 1)
        assert(employees[1].department.id == 1)
        assert(employees[2].department.id == 2)
        assert(employees[3].department.id == 2)
    }

    @Test
    fun testAutoDiscardChanges() {
        var department = Departments.findOne { it.id eq 2 } ?: return
        department.name = "tech"

        val employee = Employee()
        employee.department = department
        employee.name = "jerry"
        employee.job = "trainee"
        employee.manager = Employees.findOne { it.name eq "vince" }
        employee.hireDate = LocalDate.now()
        employee.salary = 50
        Employees.addEntity(employee)

        department.location = LocationWrapper("Guangzhou")
        department.flushChanges()

        department = Departments.findOne { it.id eq 2 } ?: return
        assert(department.name == "tech")
        assert(department.location.underlying == "Guangzhou")
    }

    interface Emp : Entity<Emp> {
        companion object : Entity.Factory<Emp>()
        val id: Int
        var employee: Employee
        var manager: Employee
        var hireDate: LocalDate
        var salary: Long
        var departmentId: Int
    }

    object Emps : Table<Emp>("t_employee") {
        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").bindTo { it.employee.name }
        val job = varchar("job").bindTo { it.employee.job }
        val managerId = int("manager_id").bindTo { it.manager.id }
        val hireDate = date("hire_date").bindTo { it.hireDate }
        val salary = long("salary").bindTo { it.salary }
        val departmentId = int("department_id").bindTo { it.departmentId }
    }

    @Test
    fun testCheckUnexpectedFlush() {
        val emp1 = Emps.findOne { it.id eq 1 } ?: return
        emp1.employee.name = "jerry"
        // emp1.flushChanges()

        val emp2 = Emp {
            employee = emp1.employee
            hireDate = LocalDate.now()
            salary = 100
            departmentId = 1
        }

        try {
            Emps.addEntity(emp2)
            throw AssertionError("failed")

        } catch (e: IllegalStateException) {
            assert(e.message == "this.employee.name may be unexpectedly discarded, please save it to database first.")
        }
    }

    @Test
    fun testCheckUnexpectedFlush0() {
        val emp1 = Emps.findOne { it.id eq 1 } ?: return
        emp1.employee.name = "jerry"
        // emp1.flushChanges()

        val emp2 = Emps.findOne { it.id eq 2 } ?: return
        emp2.employee = emp1.employee

        try {
            emp2.flushChanges()
            throw AssertionError("failed")

        } catch (e: IllegalStateException) {
            assert(e.message == "this.employee.name may be unexpectedly discarded, please save it to database first.")
        }
    }

    @Test
    fun testCheckUnexpectedFlush1() {
        val employee = Employees.findOne { it.id eq 1 } ?: return
        employee.name = "jerry"
        // employee.flushChanges()

        val emp = Emps.findOne { it.id eq 2 } ?: return
        emp.employee = employee

        try {
            emp.flushChanges()
            throw AssertionError("failed")

        } catch (e: IllegalStateException) {
            assert(e.message == "this.employee.name may be unexpectedly discarded, please save it to database first.")
        }
    }

    @Test
    fun testFlushChangesForDefaultValues() {
        var emp = Emps.findOne { it.id eq 1 } ?: return
        emp.manager.id = 2
        emp.flushChanges()

        emp = Emps.findOne { it.id eq 1 } ?: return
        assert(emp.manager.id == 2)
    }

    @Test
    fun testDefaultValuesCache() {
        val department = Department()
        assert(department.id == 0)
        assert(department["id"] == null)
    }

    @Test
    fun testCopyStatus() {
        var employee = Employees.findOne { it.id eq 2 }?.copy() ?: return
        employee.name = "jerry"
        employee.manager?.id = 3
        employee.flushChanges()

        employee = Employees.findOne { it.id eq 2 } ?: return
        assert(employee.name == "jerry")
        assert(employee.manager?.id == 3)
    }

    @Test
    fun testRemoveIf() {
        val sequence = Employees.asSequence()
        sequence.removeIf { it.departmentId eq 1 }
        assert(sequence.count() == 2)
    }

    @Test
    fun testClear() {
        val sequence = Employees.asSequence()
        sequence.clear()
        assert(sequence.isEmpty())
    }

    @Test
    fun testAddAndFlushChanges() {
        val sequence = Employees.asSequence()

        var employee = Employee {
            name = "jerry"
            job = "trainee"
            manager = sequence.find { it.name eq "vince" }
            hireDate = LocalDate.now()
            salary = 50
            department = Departments.findOne { it.name eq "tech" } ?: throw AssertionError()
        }

        sequence.add(employee)

        employee.job = "engineer"
        employee.flushChanges()

        employee = sequence.find { it.id eq employee.id } ?: throw AssertionError()
        assert(employee.job == "engineer")
    }
}