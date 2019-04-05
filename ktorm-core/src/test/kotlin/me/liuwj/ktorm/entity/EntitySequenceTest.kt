package me.liuwj.ktorm.entity

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.dsl.*
import org.junit.Test

/**
 * Created by vince on Mar 22, 2019.
 */
class EntitySequenceTest : BaseTest() {

    @Test
    fun testRealSequence() {
        val sequence = listOf(1, 2, 3).asSequence()
        sequence.withIndex()
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

    @Test
    fun testFindLast() {
        val employee = Employees
            .asSequence()
            .elementAt(3)

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

        assert(employee.department.location.isEmpty())
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
            .filter { it.salary less 100000L }
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
            .filter { it.salary lessEq 100000L }
            .groupingBy { it.departmentId }
            .eachSumBy { it.salary }

        println(sums)
        assert(sums.size == 2)
        assert(sums[1] == 150L)
        assert(sums[2] == 300L)
    }
}