package org.ktorm.entity

import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDate
import java.util.*
import kotlin.reflect.jvm.jvmErasure
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        val employee = Employee { name = "vince" }
        println(employee)

        assert(employee["name"] == "vince")
        assert(employee.name == "vince")

        assert(employee["job"] == null)
        assert(employee.job == "")
    }

    @Test
    fun testDefaultMethod() {
        for (method in Employee::class.java.methods) {
            println(method)
        }

        val employee = Employee { name = "vince" }
        println(employee)

        assert(employee.upperName == "VINCE")
        assert(employee.upperName() == "VINCE")
        assert(employee.nameWithPrefix(":") == ":vince")
        assert(employee.nameWithSuffix(":") == "vince:")
    }

    interface Animal<T : Animal<T>> : Entity<T> {
        fun say1() = "animal1"
        fun say2() = "animal2"
        fun say3() = "animal3"
    }

    interface Dog : Animal<Dog> {
        override fun say1() = "${super.say1()} --> dog1"
        override fun say2() = "${super.say2()} --> dog2"
        override fun say3() = "${super.say3()} --> dog3"
    }

    @Test
    fun testDefaultMethodOverride() {
        val dog = Entity.create<Dog>()
        assert(dog.say1() == "animal1 --> dog1")
        assert(dog.say2() == "animal2 --> dog2")
        assert(dog.say3() == "animal3 --> dog3")
    }

    @Test
    fun testSerialize() {
        val employee = Employee {
            name = "jerry"
            job = "trainee"
            manager = database.employees.find { it.name eq "vince" }
            hireDate = LocalDate.now()
            salary = 50
            department = database.departments.find { it.name eq "tech" } ?: throw AssertionError()
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

        val str = "rO0ABXN9AAAAAQAbb3JnLmt0b3JtLkJhc2VUZXN0JEVtcGxveWVleHIAF2phdmEubGFuZy5yZWZsZWN0LlByb3h54SfaIMwQQ8sCAAFMAAFodAAlTGphdmEvbGFuZy9yZWZsZWN0L0ludm9jYXRpb25IYW5kbGVyO3hwc3IAJW9yZy5rdG9ybS5lbnRpdHkuRW50aXR5SW1wbGVtZW50YXRpb24AAAAAAAAAAQMAAkwAC2VudGl0eUNsYXNzdAAXTGtvdGxpbi9yZWZsZWN0L0tDbGFzcztMAAZ2YWx1ZXN0ABlMamF2YS91dGlsL0xpbmtlZEhhc2hNYXA7eHB3HQAbb3JnLmt0b3JtLkJhc2VUZXN0JEVtcGxveWVlc3IAF2phdmEudXRpbC5MaW5rZWRIYXNoTWFwNMBOXBBswPsCAAFaAAthY2Nlc3NPcmRlcnhyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAGdAAEbmFtZXQABWplcnJ5dAADam9idAAHdHJhaW5lZXQAB21hbmFnZXJzcQB+AABzcQB+AAR3HQAbb3JnLmt0b3JtLkJhc2VUZXN0JEVtcGxveWVlc3EAfgAIP0AAAAAAAAx3CAAAABAAAAAGdAACaWRzcgARamF2YS5sYW5nLkludGVnZXIS4qCk94GHOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAAAXEAfgALdAAFdmluY2VxAH4ADXQACGVuZ2luZWVydAAIaGlyZURhdGVzcgANamF2YS50aW1lLlNlcpVdhLobIkiyDAAAeHB3BwMAAAfiAQF4dAAGc2FsYXJ5c3IADmphdmEubGFuZy5Mb25nO4vkkMyPI98CAAFKAAV2YWx1ZXhxAH4AFQAAAAAAAABkdAAKZGVwYXJ0bWVudHN9AAAAAQAdb3JnLmt0b3JtLkJhc2VUZXN0JERlcGFydG1lbnR4cQB+AAFzcQB+AAR3HwAdb3JnLmt0b3JtLkJhc2VUZXN0JERlcGFydG1lbnRzcQB+AAg/QAAAAAAADHcIAAAAEAAAAAN0AAJpZHEAfgAWdAAEbmFtZXQABHRlY2h0AAhsb2NhdGlvbnNyACJvcmcua3Rvcm0uQmFzZVRlc3QkTG9jYXRpb25XcmFwcGVykiiIyygSeecCAAFMAAp1bmRlcmx5aW5ndAASTGphdmEvbGFuZy9TdHJpbmc7eHB0AAlHdWFuZ3pob3V4AHh4AHhxAH4AGXNxAH4AGncHAwAAB+QJGnhxAH4AHHNxAH4AHQAAAAAAAAAycQB+AB9zcQB+ACBzcQB+AAR3HwAdb3JnLmt0b3JtLkJhc2VUZXN0JERlcGFydG1lbnRzcQB+AAg/QAAAAAAADHcIAAAAEAAAAANxAH4AJHEAfgAWcQB+ACVxAH4AJnEAfgAnc3EAfgAocQB+ACt4AHh4AHg="
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
        val employee = database.employees.find { it.id eq 1 } ?: throw AssertionError()
        println(employee)

        assert(employee.name == "vince")
        assert(employee.job == "engineer")
    }

    @Test
    fun testFindWithReference() {
        val employees = database.employees
            .filter { it.department.location like "%Guangzhou%" }
            .sortedBy { it.id }
            .toList()

        assert(employees.size == 2)
        assert(employees[0].name == "vince")
        assert(employees[1].name == "marry")
    }

    @Test
    fun testCreateEntity() {
        val employees = database
            .from(Employees)
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
        database.employees.update(employee)

        employee = database.employees.find { it.id eq 2 } ?: throw AssertionError()
        assert(employee.job == "engineer")
        assert(employee.salary == 100L)
        assert(employee.manager?.id == 1)
    }

    @Test
    fun testFlushChanges() {
        var employee = database.employees.find { it.id eq 2 } ?: throw AssertionError()
        employee.job = "engineer"
        employee.salary = 100
        employee.manager = null
        employee.flushChanges()
        employee.flushChanges()

        employee = database.employees.find { it.id eq 2 } ?: throw AssertionError()
        assert(employee.job == "engineer")
        assert(employee.salary == 100L)
        assert(employee.manager == null)
    }

    @Test
    fun testDeleteEntity() {
        val employee = database.employees.find { it.id eq 2 } ?: throw AssertionError()
        employee.delete()

        assert(database.employees.count() == 3)
    }

    @Test
    fun testSaveEntity() {
        var employee = Employee {
            name = "jerry"
            job = "trainee"
            manager = null
            hireDate = LocalDate.now()
            salary = 50
            department = database.departments.find { it.name eq "tech" } ?: throw AssertionError()
        }

        database.employees.add(employee)
        println(employee)

        employee = database.employees.find { it.id eq 5 } ?: throw AssertionError()
        assert(employee.name == "jerry")
        assert(employee.department.name == "tech")

        employee.job = "engineer"
        employee.salary = 100
        employee.flushChanges()

        employee = database.employees.find { it.id eq 5 } ?: throw AssertionError()
        assert(employee.job == "engineer")
        assert(employee.salary == 100L)

        employee.delete()
        assert(database.employees.count() == 4)
    }

    @Test
    fun testFindMapById() {
        val employees = database.employees.filter { it.id.inList(1, 2) }.associateBy { it.id }
        assert(employees.size == 2)
        assert(employees[1]?.name == "vince")
        assert(employees[2]?.name == "marry")
    }

    interface Parent : Entity<Parent> {
        companion object : Entity.Factory<Parent>()
        var child: Child?
    }

    interface Child : Entity<Child> {
        companion object : Entity.Factory<Child>()
        var grandChild: GrandChild?
    }

    interface GrandChild : Entity<GrandChild> {
        companion object : Entity.Factory<GrandChild>()
        var id: Int?
        var name: String?
    }

    object Parents : Table<Parent>("t_employee") {
        val id = int("id").primaryKey().bindTo { it.child?.grandChild?.id }
    }

    @Test
    fun testHasColumnValue() {
        val p1 = Parent()
        assert(!p1.implementation.hasColumnValue(Parents.id.binding!!))
        assert(p1.implementation.getColumnValue(Parents.id.binding!!) == null)

        val p2 = Parent {
            child = null
        }
        assert(p2.implementation.hasColumnValue(Parents.id.binding!!))
        assert(p2.implementation.getColumnValue(Parents.id.binding!!) == null)

        val p3 = Parent {
            child = Child()
        }
        assert(!p3.implementation.hasColumnValue(Parents.id.binding!!))
        assert(p3.implementation.getColumnValue(Parents.id.binding!!) == null)

        val p4 = Parent {
            child = Child {
                grandChild = null
            }
        }
        assert(p4.implementation.hasColumnValue(Parents.id.binding!!))
        assert(p4.implementation.getColumnValue(Parents.id.binding!!) == null)

        val p5 = Parent {
            child = Child {
                grandChild = GrandChild()
            }
        }
        assert(!p5.implementation.hasColumnValue(Parents.id.binding!!))
        assert(p5.implementation.getColumnValue(Parents.id.binding!!) == null)

        val p6 = Parent {
            child = Child {
                grandChild = GrandChild {
                    id = null
                }
            }
        }
        assert(p6.implementation.hasColumnValue(Parents.id.binding!!))
        assert(p6.implementation.getColumnValue(Parents.id.binding!!) == null)

        val p7 = Parent {
            child = Child {
                grandChild = GrandChild {
                    id = 6
                }
            }
        }
        assert(p7.implementation.hasColumnValue(Parents.id.binding!!))
        assert(p7.implementation.getColumnValue(Parents.id.binding!!) == 6)
    }

    @Test
    fun testHasColumnValueAttached() {
        val sofiaDepartment = Department {
            name = "Sofia Office"
            location = LocationWrapper("Sofia")
        }

        database.departments.add(sofiaDepartment)

        val now = LocalDate.now()
        val employeeManager = Employee {
            name = "Simpson"
            job = "Manager"
            hireDate = now
            department = sofiaDepartment
            salary = 100
        }

        database.employees.add(employeeManager)

        val employee1 = Employee {
            name = "McDonald"
            job = "Engineer"
            hireDate = now
            department = sofiaDepartment
            salary = 100
        }

        val e1 = with(database.employees) {
            add(employee1)
            find { it.id eq employee1.id }
        }

        assertNotNull(e1)
        assert(!e1.implementation.hasColumnValue(Employees.managerId.binding!!))
        assertNull(e1.implementation.getColumnValue(Employees.managerId.binding!!))

        val employee2 = Employee {
            name = "Smith"
            job = "Engineer"
            hireDate = now
            department = sofiaDepartment
            manager = null
            salary = 100
        }

        val e2 = with(database.employees) {
            add(employee2)
            find { it.id eq employee2.id }
        }

        assertNotNull(e2)
        assert(!e2.implementation.hasColumnValue(Employees.managerId.binding!!))
        assertNull(e2.implementation.getColumnValue(Employees.managerId.binding!!))

        val employee3 = Employee {
            name = "Dennis"
            job = "Engineer"
            hireDate = now
            department = sofiaDepartment
            manager = employeeManager
            salary = 100
        }

        val e3 = with(database.employees) {
            add(employee3)
            find { it.id eq employee3.id }
        }

        assertNotNull(e3)
        assert(e3.implementation.hasColumnValue(Employees.managerId.binding!!))
        assertNotNull(e3.implementation.getColumnValue(Employees.managerId.binding!!))
    }

    @Test
    fun testUpdatePrimaryKey() {
        try {
            val parent = database.sequenceOf(Parents).find { it.id eq 1 } ?: throw AssertionError()
            assert(parent.child?.grandChild?.id == 1)

            parent.child?.grandChild?.id = 2
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
        val e = database.sequenceOf(EmployeeTestForReferencePrimaryKeys).find { it.id eq 2 } ?: return
        e.manager.employee = database.sequenceOf(Employees).find { it.id eq 1 } ?: return

        try {
            e.employee = database.sequenceOf(Employees).find { it.id eq 1 } ?: return
            throw AssertionError()
        } catch (e: UnsupportedOperationException) {
            // expected
            println(e.message)
        }

        e.flushChanges()
    }

    @Test
    fun testForeignKeyValue() {
        val employees = database
            .from(Employees)
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
        val employees = database
            .from(Employees)
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
        var department = database.departments.find { it.id eq 2 } ?: return
        department.name = "tech"

        val employee = Employee()
        employee.department = department
        employee.name = "jerry"
        employee.job = "trainee"
        employee.manager = database.employees.find { it.name eq "vince" }
        employee.hireDate = LocalDate.now()
        employee.salary = 50
        database.employees.add(employee)

        department.location = LocationWrapper("Guangzhou")
        department.flushChanges()

        department = database.departments.find { it.id eq 2 } ?: return
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

    val Database.emps get() = this.sequenceOf(Emps)

    @Test
    fun testCheckUnexpectedFlush() {
        val emp1 = database.emps.find { it.id eq 1 } ?: return
        emp1.employee.name = "jerry"
        // emp1.flushChanges()

        val emp2 = Emp {
            employee = emp1.employee
            hireDate = LocalDate.now()
            salary = 100
            departmentId = 1
        }

        try {
            database.emps.add(emp2)
            throw AssertionError("failed")

        } catch (e: IllegalStateException) {
            assert(e.message == "this.employee.name may be unexpectedly discarded, please save it to database first.")
        }
    }

    @Test
    fun testCheckUnexpectedFlush0() {
        val emp1 = database.emps.find { it.id eq 1 } ?: return
        emp1.employee.name = "jerry"
        // emp1.flushChanges()

        val emp2 = database.emps.find { it.id eq 2 } ?: return
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
        val employee = database.employees.find { it.id eq 1 } ?: return
        employee.name = "jerry"
        // employee.flushChanges()

        val emp = database.emps.find { it.id eq 2 } ?: return
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
        var emp = database.emps.find { it.id eq 1 } ?: return
        emp.manager.id = 2
        emp.flushChanges()

        emp = database.emps.find { it.id eq 1 } ?: return
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
        var employee = database.employees.find { it.id eq 2 }?.copy() ?: return
        employee.name = "jerry"
        employee.manager?.id = 3
        employee.flushChanges()

        employee = database.employees.find { it.id eq 2 } ?: return
        assert(employee.name == "jerry")
        assert(employee.manager?.id == 3)
    }

    @Test
    fun testDeepCopy() {
        val employee = database.employees.find { it.id eq 2 } ?: return
        val copy = employee.copy()

        assert(employee == copy)
        assert(employee !== copy)
        assert(employee.hireDate !== copy.hireDate) // should not be the same instance because of deep copy.
        assert(copy.manager?.implementation?.parent === copy.implementation) // should keep the parent relationship.
    }

    @Test
    fun testRemoveIf() {
        database.employees.removeIf { it.departmentId eq 1 }
        assert(database.employees.count() == 2)
    }

    @Test
    fun testClear() {
        database.employees.clear()
        assert(database.employees.isEmpty())
    }

    @Test
    fun testAddAndFlushChanges() {
        var employee = Employee {
            name = "jerry"
            job = "trainee"
            manager = database.employees.find { it.name eq "vince" }
            hireDate = LocalDate.now()
            salary = 50
            department = database.departments.find { it.name eq "tech" } ?: throw AssertionError()
        }

        database.employees.add(employee)

        employee.job = "engineer"
        employee.flushChanges()

        employee = database.employees.find { it.id eq employee.id } ?: throw AssertionError()
        assert(employee.job == "engineer")
    }

    @Test
    fun testValueEquality() {
        val now = LocalDate.now()
        val employee1 = Employee {
            id = 1
            name = "Eric"
            job = "contributor"
            hireDate = now
            salary = 50
        }

        val employee2 = Employee {
            id = 1
            name = "Eric"
            job = "contributor"
            hireDate = now
            salary = 50
        }

        println(employee1)
        println(employee2)
        println(employee1.hashCode())
        assert(employee1 == employee2)
        assert(employee2 == employee1)
        assert(employee1 !== employee2)
        assert(employee1.hashCode() == employee2.hashCode())
    }

    @Test
    fun testDifferentClassesSameValuesNotEqual() {
        val employee = Employee {
            name = "name"
        }

        val department = Department {
            name = "name"
        }

        println(employee)
        println(department)
        println(employee.hashCode())
        println(department.hashCode())
        assert(employee != department)
    }

    @Test
    fun testEqualsWithNullValues() {
        val e1 = Employee {
            id = 1
            name = "vince"
        }

        val e2 = Employee {
            id = 1
            name = "vince"
            manager = null
        }

        println(e1)
        println(e2)
        println(e1.hashCode())
        assert(e1 == e2)
        assert(e2 == e1)
        assert(e1 !== e2)
        assert(e1.hashCode() == e2.hashCode())
    }

    @Test
    fun testEqualsForNestedEntities() {
        val p1 = Parent {
            child = Child {
                grandChild = GrandChild {
                    id = 1
                }
            }
        }

        val p2 = Parent {
            child = Child {
                grandChild = GrandChild {
                    id = 1
                    name = null
                }
            }
        }

        println(p1)
        println(p2)
        println(p1.hashCode())
        assert(p1 == p2)
        assert(p2 == p1)
        assert(p1 !== p2)
        assert(p1.hashCode() == p2.hashCode())
    }

    @Test
    fun testValueNullEquality() {
        val departmentTransient = Department {
            name = "Sofia Office"
            location = LocationWrapper("Sofia")
            mixedCase = null // explicitly initialized to null
        }

        database.departments.add(departmentTransient)

        val departmentAttached = database.departments.find { it.name eq "Sofia Office" }
        assertNotNull(departmentAttached)

        println(departmentTransient)
        println(departmentAttached)
        println(departmentTransient.hashCode())
        assert(departmentTransient == departmentAttached)
        assert(departmentAttached == departmentTransient)
        assert(departmentTransient !== departmentAttached)
        assert(departmentTransient.hashCode() == departmentAttached.hashCode())
    }
}