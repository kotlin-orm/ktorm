package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.BaseTest
import org.junit.Test

/**
 * Created by vince on Dec 09, 2018.
 */
class AggregationTest : BaseTest() {

    @Test
    fun testCount() {
        val count = Employees.count { it.departmentId eq 1 }
        assert(count == 2)
    }

    @Test
    fun testCountAll() {
        val count = Employees.count()
        assert(count == 4)
    }

    @Test
    fun testSum() {
        val sum = Employees.sumBy { it.salary + 1 }
        assert(sum == 454L)
    }

    @Test
    fun testMax() {
        val max = Employees.maxBy { it.salary - 1 }
        assert(max == 199L)
    }

    @Test
    fun testMin() {
        val min = Employees.minBy { it.salary }
        assert(min == 50L)
    }

    @Test
    fun testAvg() {
        val avg = Employees.avgBy { it.salary }
        println(avg)
    }

    @Test
    fun testNone() {
        assert(Employees.none { it.salary greater 200L })
    }

    @Test
    fun testAny() {
        assert(!Employees.any { it.salary greater 200L })
    }

    @Test
    fun testAll() {
        assert(Employees.all { it.salary greater 0L })
    }
}