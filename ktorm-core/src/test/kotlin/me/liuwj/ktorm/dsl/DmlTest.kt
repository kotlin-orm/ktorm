package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.entity.findAll
import me.liuwj.ktorm.entity.findById
import org.junit.Test
import java.time.LocalDate

/**
 * Created by vince on Dec 08, 2018.
 */
class DmlTest : BaseTest() {

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

        val employee = Employees.findById(2) ?: throw AssertionError()
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
                    it.location to "Hong Kong"
                    where {
                        it.id eq i
                    }
                }
            }
        }

        val departments = Departments.findAll()
        assert(departments.size == 2)

        for (dept in departments) {
            assert(dept.location == "Hong Kong")
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

        val employee = Employees.findById(id) ?: throw AssertionError()
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