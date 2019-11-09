package me.liuwj.ktorm.entity

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.schema.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDate
import java.util.*
import kotlin.reflect.jvm.jvmErasure

/**
 * Created by vince on Dec 09, 2018.
 */
class EntityTest : BaseTest() {

    @Test
    fun testTypeReference() {
        println(Employee)
        println(Employee.referencedKotlinType)
        assert(Employee.referencedKotlinType.jvmErasure == Employee::class)

        println(Employees)
        println(Employees.entityClass)
        assert(Employees.entityClass == Employee::class)

        println(Employees.aliased("t"))
        println(Employees.aliased("t").entityClass)
        assert(Employees.aliased("t").entityClass == Employee::class)
    }

    @Test
    fun testEntityProperties() {
        val employee = Employee {
            name = "vince"
        }

        println(employee)

        assert(employee["name"] == "vince")
        assert(employee.name == "vince")

        assert(employee["job"] == null)
        assert(employee.job == "")
    }

    @Test
    fun testSerialize() {
        val employee = Employee {
            name = "jerry"
            job = "trainee"
            manager = db.sequenceOf(Employees).find { it.name eq "vince" }
            hireDate = LocalDate.now()
            salary = 50
            department = db.sequenceOf(Departments).find { it.name eq "tech" } ?: throw AssertionError()
        }

        val bytes = serialize(employee)
        println(Base64.getEncoder().encodeToString(bytes))
    }

    @Test
    fun testDeserialize() {
        Department {
            name = "test"
            println(this.javaClass)
            println(this)
        }

        Employee {
            name = "test"
            println(this.javaClass)
            println(this)
        }

        val str = "rO0ABXN9AAAAAQAgbWUubGl1d2oua3Rvcm0uQmFzZVRlc3QkRW1wbG95ZWV4cgAXamF2YS5sYW5nLnJlZmxlY3QuUHJveHnhJ9ogzBBDywIAAUwAAWh0ACVMamF2YS9sYW5nL3JlZmxlY3QvSW52b2NhdGlvbkhhbmRsZXI7eHBzcgAqbWUubGl1d2oua3Rvcm0uZW50aXR5LkVudGl0eUltcGxlbWVudGF0aW9uAAAAAAAAAAEDAAJMAAtlbnRpdHlDbGFzc3QAF0xrb3RsaW4vcmVmbGVjdC9LQ2xhc3M7TAAGdmFsdWVzdAAZTGphdmEvdXRpbC9MaW5rZWRIYXNoTWFwO3hwdyIAIG1lLmxpdXdqLmt0b3JtLkJhc2VUZXN0JEVtcGxveWVlc3IAF2phdmEudXRpbC5MaW5rZWRIYXNoTWFwNMBOXBBswPsCAAFaAAthY2Nlc3NPcmRlcnhyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAGdAAEbmFtZXQABWplcnJ5dAADam9idAAHdHJhaW5lZXQAB21hbmFnZXJzcQB+AABzcQB+AAR3IgAgbWUubGl1d2oua3Rvcm0uQmFzZVRlc3QkRW1wbG95ZWVzcQB+AAg/QAAAAAAADHcIAAAAEAAAAAZ0AAJpZHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAABdAAEbmFtZXQABXZpbmNldAADam9idAAIZW5naW5lZXJ0AAhoaXJlRGF0ZXNyAA1qYXZhLnRpbWUuU2VylV2EuhsiSLIMAAB4cHcHAwAAB+IBAXh0AAZzYWxhcnlzcgAOamF2YS5sYW5nLkxvbmc7i+SQzI8j3wIAAUoABXZhbHVleHEAfgAVAAAAAAAAAGR0AApkZXBhcnRtZW50c30AAAABACJtZS5saXV3ai5rdG9ybS5CYXNlVGVzdCREZXBhcnRtZW50eHEAfgABc3EAfgAEdyQAIm1lLmxpdXdqLmt0b3JtLkJhc2VUZXN0JERlcGFydG1lbnRzcQB+AAg/QAAAAAAADHcIAAAAEAAAAANxAH4AE3EAfgAWcQB+ABd0AAR0ZWNodAAIbG9jYXRpb250AAlHdWFuZ3pob3V4AHh4AHh0AAhoaXJlRGF0ZXNxAH4AHHcHAwAAB+MDHHh0AAZzYWxhcnlzcQB+AB8AAAAAAAAAMnQACmRlcGFydG1lbnRzcQB+ACJzcQB+AAR3JAAibWUubGl1d2oua3Rvcm0uQmFzZVRlc3QkRGVwYXJ0bWVudHNxAH4ACD9AAAAAAAAMdwgAAAAQAAAAA3EAfgATcQB+ABZxAH4AF3EAfgAmcQB+ACdxAH4AKHgAeHgAeA=="
        val bytes = Base64.getDecoder().decode(str)

        val employee = deserialize(bytes) as Employee
        println(employee.javaClass)
        println(employee)

        assert(employee.name == "jerry")
        assert(employee.job == "trainee")
        assert(employee.manager?.name == "vince")
        assert(employee.salary == 50L)
        assert(employee.department.name == "tech")
    }

    private fun serialize(obj: Any): ByteArray {
        ByteArrayOutputStream().use { buffer ->
            ObjectOutputStream(buffer).use { output ->
                output.writeObject(obj)
                output.flush()
                return buffer.toByteArray()
            }
        }
    }

    private fun deserialize(bytes: ByteArray): Any {
        ByteArrayInputStream(bytes).use { buffer ->
            ObjectInputStream(buffer).use { input ->
                return input.readObject()
            }
        }
    }

    @Test
    fun testFind() {
        val employee = db.sequenceOf(Employees).find { it.id eq 1 } ?: throw AssertionError()
        println(employee)

        assert(employee.name == "vince")
        assert(employee.job == "engineer")
    }

    @Test
    fun testFindWithReference() {
        val employees = db.sequenceOf(Employees)
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
        val employees = db.from(Employees)
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
    fun testFlushChanges() {
        var employee = db.sequenceOf(Employees).find { it.id eq 2 } ?: throw AssertionError()
        employee.job = "engineer"
        employee.salary = 100
        employee.flushChanges()
        employee.flushChanges()

        employee = db.sequenceOf(Employees).find { it.id eq 2 } ?: throw AssertionError()
        assert(employee.job == "engineer")
        assert(employee.salary == 100L)
    }

    @Test
    fun testDeleteEntity() {
        val employee = db.sequenceOf(Employees).find { it.id eq 2 } ?: throw AssertionError()
        employee.delete()

        assert(db.sequenceOf(Employees).count() == 3)
    }

    @Test
    fun testSaveEntity() {
        var employee = Employee {
            name = "jerry"
            job = "trainee"
            manager = db.sequenceOf(Employees).find { it.name eq "vince" }
            hireDate = LocalDate.now()
            salary = 50
            department = db.sequenceOf(Departments).find { it.name eq "tech" } ?: throw AssertionError()
        }

        db.sequenceOf(Employees).add(employee)
        println(employee)

        employee = db.sequenceOf(Employees).find { it.id eq 5 } ?: throw AssertionError()
        assert(employee.name == "jerry")
        assert(employee.department.name == "tech")

        employee.job = "engineer"
        employee.salary = 100
        employee.flushChanges()

        employee = db.sequenceOf(Employees).find { it.id eq 5 } ?: throw AssertionError()
        assert(employee.job == "engineer")
        assert(employee.salary == 100L)

        employee.delete()
        assert(db.sequenceOf(Employees).count() == 4)
    }

    @Test
    fun testFindMapById() {
        val employees = db.sequenceOf(Employees).filter { it.id inList listOf(1, 2) }.associateBy { it.id }
        assert(employees.size == 2)
        assert(employees[1]!!.name == "vince")
        assert(employees[2]!!.name == "marry")
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
        val id by int("id").primaryKey().bindTo { it.child.grandChild.id }
    }

    @Test
    fun testUpdatePrimaryKey() {
        try {
            val parent = db.sequenceOf(Parents).find { it.id eq 1 } ?: throw AssertionError()
            assert(parent.child.grandChild.id == 1)

            parent.child.grandChild.id = 2
            throw AssertionError()

        } catch (e: UnsupportedOperationException) {
            // expected
            println(e.message)
        }
    }

    @Test
    fun testForeignKeyValue() {
        val employees = db.from(Employees)
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
        val employees = db.from(Employees)
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
        var department = db.sequenceOf(Departments).find { it.id eq 2 } ?: return
        department.name = "tech"

        val employee = Employee()
        employee.department = department
        employee.name = "jerry"
        employee.job = "trainee"
        employee.manager = db.sequenceOf(Employees).find { it.name eq "vince" }
        employee.hireDate = LocalDate.now()
        employee.salary = 50
        db.sequenceOf(Employees).add(employee)

        department.location = "Guangzhou"
        department.flushChanges()

        department = db.sequenceOf(Departments).find { it.id eq 2 } ?: return
        assert(department.name == "tech")
        assert(department.location == "Guangzhou")
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
        val id by int("id").primaryKey().bindTo { it.id }
        val name by varchar("name").bindTo { it.employee.name }
        val job by varchar("job").bindTo { it.employee.job }
        val managerId by int("manager_id").bindTo { it.manager.id }
        val hireDate by date("hire_date").bindTo { it.hireDate }
        val salary by long("salary").bindTo { it.salary }
        val departmentId by int("department_id").bindTo { it.departmentId }
    }

    @Test
    fun testCheckUnexpectedFlush() {
        val emp1 = db.sequenceOf(Emps).find { it.id eq 1 } ?: return
        emp1.employee.name = "jerry"
        // emp1.flushChanges()

        val emp2 = Emp {
            employee = emp1.employee
            hireDate = LocalDate.now()
            salary = 100
            departmentId = 1
        }

        try {
            db.sequenceOf(Emps).add(emp2)
            throw AssertionError("failed")

        } catch (e: IllegalStateException) {
            assert(e.message == "this.employee.name may be unexpectedly discarded, please save it to database first.")
        }
    }

    @Test
    fun testCheckUnexpectedFlush0() {
        val emp1 = db.sequenceOf(Emps).find { it.id eq 1 } ?: return
        emp1.employee.name = "jerry"
        // emp1.flushChanges()

        val emp2 = db.sequenceOf(Emps).find { it.id eq 2 } ?: return
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
        val employee = db.sequenceOf(Employees).find { it.id eq 1 } ?: return
        employee.name = "jerry"
        // employee.flushChanges()

        val emp = db.sequenceOf(Emps).find { it.id eq 2 } ?: return
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
        var emp = db.sequenceOf(Emps).find { it.id eq 1 } ?: return
        emp.manager.id = 2
        emp.flushChanges()

        emp = db.sequenceOf(Emps).find { it.id eq 1 } ?: return
        assert(emp.manager.id == 2)
    }

    @Test
    fun testCopy() {
        var employee = db.sequenceOf(Employees).find { it.id eq 1 }?.copy() ?: return
        employee.name = "jerry"
        employee.flushChanges()

        employee = db.sequenceOf(Employees).find { it.id eq 1 } ?: return
        assert(employee.name == "jerry")
    }
}