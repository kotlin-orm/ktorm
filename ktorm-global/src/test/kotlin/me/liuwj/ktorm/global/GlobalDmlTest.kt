package me.liuwj.ktorm.global

import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.insertTo
import me.liuwj.ktorm.dsl.plus
import me.liuwj.ktorm.dsl.where
import org.junit.Test
import java.time.LocalDate

/**
 * Created by vince at Apr 05, 2020.
 */
class GlobalDmlTest : BaseGlobalTest() {

    @Test
    fun testUpdate() {
        Employees.update {
            it.job to "engineer"
            it.managerId to null
            it.salary to 100

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
                    it.location to LocationWrapper("Hong Kong")
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
            it.salary to it.salary + 1
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
            it.name to "jerry"
            it.job to "trainee"
            it.managerId to 1
            it.hireDate to LocalDate.now()
            it.salary to 50
            it.departmentId to 1
        }

        assert(Employees.count() == 5)
    }

    @Test
    fun testBatchInsert() {
        Employees.batchInsert {
            item {
                it.name to "jerry"
                it.job to "trainee"
                it.managerId to 1
                it.hireDate to LocalDate.now()
                it.salary to 50
                it.departmentId to 1
            }
            item {
                it.name to "linda"
                it.job to "assistant"
                it.managerId to 3
                it.hireDate to LocalDate.now()
                it.salary to 100
                it.departmentId to 2
            }
        }

        assert(Employees.count() == 6)
    }

    @Test
    fun testInsertAndGenerateKey() {
        val today = LocalDate.now()

        val id = Employees.insertAndGenerateKey {
            it.name to "jerry"
            it.job to "trainee"
            it.managerId to 1
            it.hireDate to today
            it.salary to 50
            it.departmentId to 1
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