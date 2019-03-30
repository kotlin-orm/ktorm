package me.liuwj.ktorm.entity

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.greater
import me.liuwj.ktorm.dsl.isNull
import org.junit.Test

/**
 * Created by vince on Mar 22, 2019.
 */
class EntitySequenceTest : BaseTest() {

    @Test
    fun testRealSequence() {
        val sequence = listOf(1, 2, 3).asSequence()
        sequence.take(0)
    }

    @Test
    fun testToList() {
        val employees = Employees.asSequence().toList()
        assert(employees.size == 4)
        assert(employees[0].name == "vince")
        assert(employees[0].department.name == "tech")
    }

    @Test
    fun testFilter() {
        val names = Employees
            .asSequence()
            .filter { it.departmentId eq 1 }
            .filterNot { it.managerId.isNull() }
            .toList()
            .map { it.name }

        assert(names.size == 1)
        assert(names[0] == "marry")
    }

    @Test
    fun testFilterTo() {
        val names = Employees
            .asSequence()
            .filter { it.departmentId eq 1 }
            .filterTo(ArrayList()) { it.managerId.isNull() }
            .map { it.name }

        assert(names.size == 1)
        assert(names[0] == "vince")
    }

    @Test
    fun testCount() {
        assert(Employees.asSequence().filter { it.departmentId eq 1 }.count() == 2)
        assert(Employees.asSequence().count { it.departmentId eq 1 } == 2)
    }

    @Test
    fun testAll() {
        assert(Employees.asSequence().filter { it.departmentId eq 1 }.all { it.salary greater 49L })
    }

    @Test
    fun testAssociate() {
        val employees = Employees.asSequence().filter { it.departmentId eq 1 }.associateBy { it.id }
        assert(employees.size == 2)
        assert(employees[1]!!.name == "vince")
    }

    @Test
    fun testDrop() {
        try {
            val employees = Employees.asSequence().drop(3).toList()
            assert(employees.size == 1)
            assert(employees[0].name == "penny")
        } catch (e: UnsupportedOperationException) {
            // Expected, pagination should be provided by dialects...
        }
    }

    @Test
    fun testTake() {
        try {
            val employees = Employees.asSequence().take(1).toList()
            assert(employees.size == 1)
            assert(employees[0].name == "vince")
        } catch (e: UnsupportedOperationException) {
            // Expected, pagination should be provided by dialects...
        }
    }
}