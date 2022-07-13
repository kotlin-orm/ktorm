package org.ktorm.dsl

import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.entity.*

/**
 * Created by vince on Dec 09, 2018.
 */
class AggregationTest : BaseTest() {

    @Test
    fun testCount() {
        val count = database.employees.count { it.departmentId eq 1 }
        assert(count == 2)
    }

    @Test
    fun testCountAll() {
        val count = database.employees.count()
        assert(count == 4)
    }

    @Test
    fun testSum() {
        val sum = database.employees.sumBy { it.salary + 1 }
        assert(sum == 454L)
    }

    @Test
    fun testMax() {
        val max = database.employees.maxBy { it.salary - 1 }
        assert(max == 199L)
    }

    @Test
    fun testMin() {
        val min = database.employees.minBy { it.salary }
        assert(min == 50L)
    }

    @Test
    fun testAvg() {
        val avg = database.employees.averageBy { it.salary }
        println(avg)
    }

    @Test
    fun testNone() {
        assert(database.employees.none { it.salary gt 200L })
    }

    @Test
    fun testAny() {
        assert(!database.employees.any { it.salary gt 200L })
    }

    @Test
    fun testAll() {
        assert(database.employees.all { it.salary gt 0L })
    }

    @Test
    fun testAggregate() {
        val result = database.employees.aggregateColumns { max(it.salary) - min(it.salary) }
        println(result)
        assert(result == 150L)
    }

    @Test
    fun testAggregate2() {
        val (max, min) = database.employees.aggregateColumns { tupleOf(max(it.salary), min(it.salary)) }
        assert(max == 200L)
        assert(min == 50L)
    }

    @Test
    fun testGroupAggregate3() {
        database.employees
            .groupingBy { it.departmentId }
            .aggregateColumns { tupleOf(max(it.salary), min(it.salary)) }
            .forEach { departmentId, (max, min) ->
                println("$departmentId:$max:$min")
            }
    }
}