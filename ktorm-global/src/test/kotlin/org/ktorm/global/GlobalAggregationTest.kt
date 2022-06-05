package org.ktorm.global

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.entity.aggregateColumns
import org.ktorm.entity.groupingBy
import org.ktorm.entity.tupleOf

/**
 * Created by vince at Apr 05, 2020.
 */
@Suppress("DEPRECATION")
class GlobalAggregationTest : BaseGlobalTest() {

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
        val avg = Employees.averageBy { it.salary }
        println(avg)
    }

    @Test
    fun testNone() {
        assert(Employees.none { it.salary gt 200L })
    }

    @Test
    fun testAny() {
        assert(!Employees.any { it.salary gt 200L })
    }

    @Test
    fun testAll() {
        assert(Employees.all { it.salary gt 0L })
    }

    @Test
    fun testAggregate() {
        val result = Employees.asSequence().aggregateColumns { max(it.salary) - min(it.salary) }
        println(result)
        assert(result == 150L)
    }

    @Test
    fun testAggregate2() {
        val (max, min) = Employees.asSequence().aggregateColumns { tupleOf(max(it.salary), min(it.salary)) }
        assert(max == 200L)
        assert(min == 50L)
    }

    @Test
    fun testGroupAggregate3() {
        Employees
            .asSequence()
            .groupingBy { it.departmentId }
            .aggregateColumns { tupleOf(max(it.salary), min(it.salary)) }
            .forEach { departmentId, (max, min) ->
                println("$departmentId:$max:$min")
            }
    }
}