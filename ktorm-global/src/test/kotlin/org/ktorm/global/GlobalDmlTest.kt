package org.ktorm.global

import org.junit.Test
import org.ktorm.dsl.*
import java.time.LocalDate

/**
 * Created by vince at Apr 05, 2020.
 */
@Suppress("DEPRECATION")
class GlobalDmlTest : BaseGlobalTest() {

    @Test
    fun testUpdate() {
        Employees.update {
            set(it.job, "engineer")
            set(it.managerId, null)
            set(it.salary, 100)

            where {
                it.id eq 2
            }
        }

        val employee = Employees.findOne { it.id eq 2 } ?: throw AssertionError()
        assert(employee.name == "marry")
        assert(employee.job == "engineer")
        assert(employee.manager == null)
        assert(employee.salary == 100L)
    }

    @Test
    fun testBatchUpdate() {
        Departments.batchUpdate {
            for (i in 1..2) {
                item {
                    set(it.location, LocationWrapper("Hong Kong"))
                    where {
                        it.id eq i
                    }
                }
            }
        }

        val departments = Departments.findAll()
        assert(departments.size == 2)

        for (dept in departments) {
            assert(dept.location.underlying == "Hong Kong")
        }
    }

    @Test
    fun testSelfIncrement() {
        Employees.update {
            set(it.salary, it.salary + 1)
            where { it.id eq 1 }
        }

        val salary = Employees
            .select(Employees.salary)
            .where { Employees.id eq 1 }
            .map { it.getLong(1) }
            .first()

        assert(salary == 101L)
    }

    @Test
    fun testInsert() {
        Employees.insert {
            set(it.name, "jerry")
            set(it.job, "trainee")
            set(it.managerId, 1)
            set(it.hireDate, LocalDate.now())
            set(it.salary, 50)
            set(it.departmentId, 1)
        }

        assert(Employees.count() == 5)
    }

    @Test
    fun testBatchInsert() {
        Employees.batchInsert {
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

        assert(Employees.count() == 6)
    }

    @Test
    fun testInsertAndGenerateKey() {
        val today = LocalDate.now()

        val id = Employees.insertAndGenerateKey {
            set(it.name, "jerry")
            set(it.job, "trainee")
            set(it.managerId, 1)
            set(it.hireDate, today)
            set(it.salary, 50)
            set(it.departmentId, 1)
        }

        val employee = Employees.findOne { it.id eq (id as Int) } ?: throw AssertionError()
        assert(employee.name == "jerry")
        assert(employee.hireDate == today)
    }

    @Test
    fun testInsertFromSelect() {
        Departments
            .select(Departments.name, Departments.location)
            .where { Departments.id eq 1 }
            .insertTo(Departments, Departments.name, Departments.location)

        assert(Departments.count() == 3)
    }

    @Test
    fun testDelete() {
        Employees.delete { it.id eq 4 }
        assert(Employees.count() == 3)
    }
}