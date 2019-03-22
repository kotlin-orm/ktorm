package me.liuwj.ktorm.entity

import me.liuwj.ktorm.BaseTest
import org.junit.Test

/**
 * Created by vince on Mar 22, 2019.
 */
class EntitySequenceTest : BaseTest() {

    @Test
    fun testIterateAll() {
        val employees = ArrayList<Employee>()

        for (employee in Employees) {
            employees += employee
        }

        assert(employees.size == 4)
        assert(employees[0].name == "vince")
        assert(employees[0].department.name == "tech")
    }
}