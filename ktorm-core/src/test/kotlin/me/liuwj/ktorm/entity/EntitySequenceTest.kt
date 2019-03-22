package me.liuwj.ktorm.entity

import me.liuwj.ktorm.BaseTest
import org.junit.Test

/**
 * Created by vince on Mar 22, 2019.
 */
class EntitySequenceTest : BaseTest() {

    @Test
    fun testToList() {
        val employees = Employees.toList()
        assert(employees.size == 4)
        assert(employees[0].name == "vince")
        assert(employees[0].department.name == "tech")
    }
}