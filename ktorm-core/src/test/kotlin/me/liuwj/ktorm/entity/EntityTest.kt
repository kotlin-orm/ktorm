package me.liuwj.ktorm.entity

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.dsl.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDate
import java.util.*

/**
 * Created by vince on Dec 09, 2018.
 */
class EntityTest : BaseTest() {

    @Test
    fun testTypeReference() {
        println(Employee)
        println(Employee.referencedKotlinType)
        assert(Employee.referencedKotlinType.classifier == Employee::class)

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
            manager = Employees.findOne { it.name eq "vince" }
            hireDate = LocalDate.now()
            salary = 50
            department = Departments.findOne { it.name eq "tech" } ?: throw AssertionError()
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

        val str = "rO0ABXN9AAAAAQAgbWUubGl1d2oua3Rvcm0uQmFzZVRlc3QkRW1wbG95ZWV4cgAXamF2YS5sYW5nLnJlZmxlY3QuUHJveHnhJ9ogzBBDywIAAUwAAWh0ACVMamF2YS9sYW5nL3JlZmxlY3QvSW52b2NhdGlvbkhhbmRsZXI7eHBzcgAgbWUubGl1d2oua3Rvcm0uZW50aXR5LkVudGl0eUltcGwAAAAAAAAAAQMAA0wAC2VudGl0eUNsYXNzdAAXTGtvdGxpbi9yZWZsZWN0L0tDbGFzcztMAA9ob2xkZXJGaWVsZE5hbWV0ABJMamF2YS9sYW5nL1N0cmluZztMAAZ2YWx1ZXN0ABlMamF2YS91dGlsL0xpbmtlZEhhc2hNYXA7eHB3IgAgbWUubGl1d2oua3Rvcm0uQmFzZVRlc3QkRW1wbG95ZWVwc3IAF2phdmEudXRpbC5MaW5rZWRIYXNoTWFwNMBOXBBswPsCAAFaAAthY2Nlc3NPcmRlcnhyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAGdAAEbmFtZXQABWplcnJ5dAADam9idAAHdHJhaW5lZXQAB21hbmFnZXJzcQB+AABzcQB+AAR3IgAgbWUubGl1d2oua3Rvcm0uQmFzZVRlc3QkRW1wbG95ZWVwc3EAfgAJP0AAAAAAAAx3CAAAABAAAAAHdAACaWRzcgARamF2YS5sYW5nLkludGVnZXIS4qCk94GHOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAAAXQABG5hbWV0AAV2aW5jZXQAA2pvYnQACGVuZ2luZWVydAAHbWFuYWdlcnNxAH4AAHNxAH4ABHciACBtZS5saXV3ai5rdG9ybS5CYXNlVGVzdCRFbXBsb3llZXEAfgAcc3EAfgAJP0AAAAAAAAx3CAAAABAAAAABcQB+ABRweAB4dAAIaGlyZURhdGVzcgANamF2YS50aW1lLlNlcpVdhLobIkiyDAAAeHB3BwMAAAfiAQF4dAAGc2FsYXJ5c3IADmphdmEubGFuZy5Mb25nO4vkkMyPI98CAAFKAAV2YWx1ZXhxAH4AFgAAAAAAAABkdAAKZGVwYXJ0bWVudHN9AAAAAQAibWUubGl1d2oua3Rvcm0uQmFzZVRlc3QkRGVwYXJ0bWVudHhxAH4AAXNxAH4ABHckACJtZS5saXV3ai5rdG9ybS5CYXNlVGVzdCREZXBhcnRtZW50cHNxAH4ACT9AAAAAAAAMdwgAAAAQAAAAA3EAfgAUcQB+ABdxAH4AGHQABHRlY2h0AAhsb2NhdGlvbnQACUd1YW5nemhvdXgAeHgAeHQACGhpcmVEYXRlc3EAfgAhdwcDAAAH4gwJeHQABnNhbGFyeXNxAH4AJAAAAAAAAAAydAAKZGVwYXJ0bWVudHNxAH4AJ3NxAH4ABHckACJtZS5saXV3ai5rdG9ybS5CYXNlVGVzdCREZXBhcnRtZW50cHNxAH4ACT9AAAAAAAAMdwgAAAAQAAAAA3EAfgAUcQB+ABdxAH4AGHEAfgArcQB+ACxxAH4ALXgAeHgAeA=="
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
        val employee = Employees.findById(1) ?: throw AssertionError()
        println(employee)

        assert(employee.name == "vince")
        assert(employee.job == "engineer")
    }

    @Test
    fun testFindWithReference() {
        val employees = Employees
            .findList {
                val dept = it.departmentId.referenceTable as Departments
                dept.location like "%Guangzhou%"
            }
            .sortedBy { it.id }

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
    fun testFlushChanges() {
        var employee = Employees.findById(2) ?: throw AssertionError()
        employee.job = "engineer"
        employee.salary = 100
        employee.flushChanges()
        employee.flushChanges()

        employee = Employees.findById(2) ?: throw AssertionError()
        assert(employee.job == "engineer")
        assert(employee.salary == 100L)
    }

    @Test
    fun testDeleteEntity() {
        val employee = Employees.findById(2) ?: throw AssertionError()
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

        Employees.add(employee)
        println(employee)

        employee = Employees.findById(5) ?: throw AssertionError()
        assert(employee.name == "jerry")
        assert(employee.department.name == "tech")

        employee.job = "engineer"
        employee.salary = 100
        employee.flushChanges()

        employee = Employees.findById(5) ?: throw AssertionError()
        assert(employee.job == "engineer")
        assert(employee.salary == 100L)

        employee.delete()
        assert(Employees.count() == 4)
    }

    @Test
    fun testFindMapById() {
        val employees = Employees.findMapByIds(listOf(1, 2))
        assert(employees.size == 2)
        assert(employees[1]!!.name == "vince")
        assert(employees[2]!!.name == "marry")
    }
}