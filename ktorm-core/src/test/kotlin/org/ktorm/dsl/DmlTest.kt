package org.ktorm.dsl

import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.entity.*
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * Created by vince on Dec 08, 2018.
 */
class DmlTest : BaseTest() {

    @Test
    fun testUpdate() {
        database.update(Employees) {
            set(it.job, "engineer")
            set(it.managerId, null)
            set(it.salary, 100)

            where {
                it.id eq 2
            }
        }

        val employee = database.employees.find { it.id eq 2 } ?: throw AssertionError()
        assert(employee.name == "marry")
        assert(employee.job == "engineer")
        assert(employee.manager == null)
        assert(employee.salary == 100L)
    }

    @Test
    fun testUpdateWithNestedQuery() {
        val query = database
            .from(Employees)
            .innerJoin(Departments, on = Employees.departmentId eq Departments.id)
            .select(Employees.id)
            .where(Employees.id gte 3)

        database.update(Employees) {
            set(it.salary, 1000)
            where {
                it.id inList query
            }
        }

        val sum = database.employees.sumBy { it.salary }
        assertEquals(2150, sum)
    }

    @Test
    fun testBatchUpdate() {
        database.batchUpdate(Departments) {
            for (i in 1..2) {
                item {
                    set(it.location, LocationWrapper("Hong Kong"))
                    where {
                        it.id eq i
                    }
                }
            }
        }

        val departments = database.departments.toList()
        assert(departments.size == 2)

        for (dept in departments) {
            assert(dept.location.underlying == "Hong Kong")
        }
    }

    @Test
    fun testSelfIncrement() {
        database.update(Employees) {
            set(it.salary, it.salary + 1)
            where { it.id eq 1 }
        }

        val salary = database
            .from(Employees)
            .select(Employees.salary)
            .where(Employees.id.eq(1))
            .map { it.getLong(1) }
            .first()

        assert(salary == 101L)
    }

    @Test
    fun testInsert() {
        database.insert(Employees) {
            set(it.name, "jerry")
            set(it.job, "trainee")
            set(it.managerId, 1)
            set(it.hireDate, LocalDate.now())
            set(it.salary, 50)
            set(it.departmentId, 1)
        }

        assert(database.employees.count() == 5)
    }

    @Test
    fun testInsertWithSchema() {
        database.insert(Customers) {
            set(it.name, "steve")
            set(it.email, "steve@job.com")
            set(it.phoneNumber, "0123456789")
        }

        assert(database.customers.count() == 1)
    }

    @Test
    fun testBatchInsert() {
        database.batchInsert(Employees) {
            item {
                set(it.name, "jerry")
                set(it.job, "trainee")
                set(it.managerId, 1)
                set(it.hireDate, LocalDate.now())
                set(it.salary, 50)
                set(it.departmentId, 1)
            }
            item {
                set(it.name, "linda")
                set(it.job, "assistant")
                set(it.managerId, 3)
                set(it.hireDate, LocalDate.now())
                set(it.salary, 100)
                set(it.departmentId, 2)
            }
        }

        assert(database.employees.count() == 6)
    }

    @Test
    fun testInsertAndGenerateKey() {
        val today = LocalDate.now()

        val id = database.insertAndGenerateKey(Employees) {
            set(it.name, "jerry")
            set(it.job, "trainee")
            set(it.managerId, 1)
            set(it.hireDate, today)
            set(it.salary, 50)
            set(it.departmentId, 1)
        }

        val employee = database.employees.find { it.id eq (id as Int) } ?: throw AssertionError()
        assert(employee.name == "jerry")
        assert(employee.hireDate == today)
    }

    @Test
    fun testInsertFromSelect() {
        database
            .from(Departments)
            .select(Departments.name, Departments.location)
            .where { Departments.id eq 1 }
            .insertTo(Departments, Departments.name, Departments.location)

        assert(database.departments.count() == 3)
    }

    @Test
    fun testDelete() {
        database.delete(Employees) { it.id eq 4 }
        assert(database.employees.count() == 3)
    }
}