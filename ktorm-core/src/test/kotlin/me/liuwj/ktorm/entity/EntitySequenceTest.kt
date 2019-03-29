package me.liuwj.ktorm.entity

import me.liuwj.ktorm.BaseTest
import org.junit.Test

/**
 * Created by vince on Mar 22, 2019.
 */
class EntitySequenceTest : BaseTest() {

    @Test
    fun testRealSequence() {
        //val sequence = listOf(1, 2, 3).asSequence()
        //sequence.mapIndexedTo()
    }

    @Test
    fun testToList() {
        val employees = Employees.toList()
        assert(employees.size == 4)
        assert(employees[0].name == "vince")
        assert(employees[0].department.name == "tech")
    }

    @Test
    fun testMap() {
        val names = Employees.map { it.name }.map { it.toUpperCase() }.toList()
        assert(names.size == 4)
        assert(names[0] == "VINCE")
        assert(names[1] == "MARRY")
    }

    @Test
    fun testMapIndexed() {
        val names = Employees.map { it.name }.mapIndexed { i, name -> "$i.$name" }.toList()
        assert(names.size == 4)
        assert(names[0] == "0.vince")
        assert(names[1] == "1.marry")
    }

    @Test
    fun testMapTo() {
        val names = Employees.map { it.name }.mapTo(ArrayList()) { it.toUpperCase() }
        assert(names.size == 4)
        assert(names[0] == "VINCE")
        assert(names[1] == "MARRY")
    }

    @Test
    fun testMapIndexedTo() {
        val names = Employees.map { it.name }.mapIndexedTo(ArrayList()) { i, name -> "$i.$name" }
        assert(names.size == 4)
        assert(names[0] == "0.vince")
        assert(names[1] == "1.marry")
    }
}