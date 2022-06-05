package org.ktorm.global

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.entity.*

/**
 * Created by vince at Apr 05, 2020.
 */
@Suppress("DEPRECATION")
class GlobalEntitySequenceTest : BaseGlobalTest() {

    @Test
    fun testAsSequence() {
        val employee = Employees
            .asSequence(withReferences = false)
            .filter { it.name eq "vince" }
            .single()

        println(employee)
        assert(employee.name == "vince")
        assert(employee.department.name.isEmpty())
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
        assert(Employees.asSequence().filter { it.departmentId eq 1 }.all { it.salary gt 49L })
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

    @Test
    fun testFindLast() {
        val employee = Employees.asSequence().elementAt(3)
        assert(employee.name == "penny")
        assert(Employees.asSequence().elementAtOrNull(4) == null)
    }

    @Test
    fun testFold() {
        val totalSalary = Employees.asSequence().fold(0L) { acc, employee -> acc + employee.salary }
        assert(totalSalary == 450L)
    }

    @Test
    fun testSorted() {
        val employee = Employees.asSequence().sortedByDescending { it.salary }.first()
        assert(employee.name == "tom")
    }

    @Test
    fun testFilterColumns() {
        val employee = Employees
            .asSequence()
            .filterColumns { it.columns + it.department.columns - it.department.location }
            .filter { it.department.id eq 1 }
            .first()

        assert(employee.department.location.underlying.isEmpty())
    }

    @Test
    fun testGroupBy() {
        val employees = Employees.asSequence().groupBy { it.department.id }
        println(employees)
        assert(employees.size == 2)
        assert(employees[1]!!.sumOf { it.salary.toInt() } == 150)
        assert(employees[2]!!.sumOf { it.salary.toInt() } == 300)
    }

    @Test
    fun testGroupingBy() {
        val salaries = Employees
            .asSequence()
            .groupingBy { it.departmentId * 2 }
            .fold(0L) { acc, employee ->
                acc + employee.salary
            }

        println(salaries)
        assert(salaries.size == 2)
        assert(salaries[2] == 150L)
        assert(salaries[4] == 300L)
    }

    @Test
    fun testEachCount() {
        val counts = Employees
            .asSequence()
            .filter { it.salary lt 100000L }
            .groupingBy { it.departmentId }
            .eachCount()

        println(counts)
        assert(counts.size == 2)
        assert(counts[1] == 2)
        assert(counts[2] == 2)
    }

    @Test
    fun testEachSum() {
        val sums = Employees
            .asSequence()
            .filter { it.salary lte 100000L }
            .groupingBy { it.departmentId }
            .eachSumBy { it.salary }

        println(sums)
        assert(sums.size == 2)
        assert(sums[1] == 150L)
        assert(sums[2] == 300L)
    }

    @Test
    fun testJoinToString() {
        val salaries = Employees.asSequence().joinToString { it.id.toString() }
        assert(salaries == "1, 2, 3, 4")
    }

    @Test
    fun testReduce() {
        val emp = Employees.asSequence().reduce { acc, employee -> acc.apply { salary += employee.salary } }
        assert(emp.salary == 450L)
    }

    @Test
    fun testSingle() {
        val employee = Employees.asSequence().singleOrNull { it.departmentId eq 1 }
        assert(employee == null)
    }

    @Test
    fun testMapColumns() {
        val names = Employees.asSequence().sortedBy { it.id }.mapColumns { it.name }

        println(names)
        assert(names.size == 4)
        assert(names[0] == "vince")
    }
}