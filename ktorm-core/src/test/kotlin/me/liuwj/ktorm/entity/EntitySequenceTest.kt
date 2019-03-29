package me.liuwj.ktorm.entity

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.isNull
import org.junit.Test

/**
 * Created by vince on Mar 22, 2019.
 */
class EntitySequenceTest : BaseTest() {

    @Test
    fun testRealSequence() {
        val sequence = listOf(1, 2, 3).asSequence()
        sequence.filter { it > 0 }
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
}