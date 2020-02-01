package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.entity.*
import org.junit.Test
import java.time.LocalDate

/**
 * Created by vince on Dec 08, 2018.
 */
class DmlTest : BaseTest() {

    @Test
    fun testUpdate() {
        database.update(Employees) {
            it.job to "engineer"
            it.managerId to null
            it.salary to 100

            where {
                it.id eq 2
            }
        }

        val employee = database.sequenceOf(Employees).find { it.id eq 2 } ?: throw AssertionError()
        assert(employee.name == "marry")
        assert(employee.job == "engineer")
        assert(employee.manager == null)
        assert(employee.salary == 100L)
    }

    @Test
    fun testBatchUpdate() {
        database.batchUpdate(Departments) {
            for (i in 1..2) {
                item {
                    it.location to LocationWrapper("Hong Kong")
                    where {
                        it.id eq i
                    }
                }
            }
        }

        val departments = database.sequenceOf(Departments).toList()
        assert(departments.size == 2)

        for (dept in departments) {
            assert(dept.location.underlying == "Hong Kong")
        }
    }

    @Test
    fun testSelfIncrement() {
        database.update(Employees) {
            it.salary to it.salary + 1
            where { it.id eq 1 }
        }

        val salary = database
            .from(Employees)
            .select(Employees.salary)
            .where { Employees.id eq 1 }
            .map { it.getLong(1) }
            .first()

        assert(salary == 101L)
    }

    @Test
    fun testInsert() {
        database.insert(Employees) {
            it.name to "jerry"
            it.job to "trainee"
            it.managerId to 1
            it.hireDate to LocalDate.now()
            it.salary to 50
            it.departmentId to 1
        }

        assert(database.sequenceOf(Employees).count() == 5)
    }

    @Test
    fun testBatchInsert() {
        database.batchInsert(Employees) {
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

        assert(database.sequenceOf(Employees).count() == 6)
    }

    @Test
    fun testInsertAndGenerateKey() {
        val today = LocalDate.now()

        val id = database.insertAndGenerateKey(Employees) {
            it.name to "jerry"
            it.job to "trainee"
            it.managerId to 1
            it.hireDate to today
            it.salary to 50
            it.departmentId to 1
        }

        val employee = database.sequenceOf(Employees).find { it.id eq (id as Int) } ?: throw AssertionError()
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

        assert(database.sequenceOf(Departments).count() == 3)
    }

    @Test
    fun testDelete() {
        database.delete(Employees) { it.id eq 4 }
        assert(database.sequenceOf(Employees).count() == 3)
    }
}