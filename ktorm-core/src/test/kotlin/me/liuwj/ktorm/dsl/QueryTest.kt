package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.expression.ScalarExpression
import org.junit.Test

/**
 * Created by vince on Dec 07, 2018.
 */
class QueryTest : BaseTest() {

    @Test
    fun testSelect() {
        val query = Departments.select()
        assert(query.rowSet.size() == 2)

        for (row in query) {
            println(row[Departments.name] + ": " + row[Departments.location])
        }
    }

    @Test
    fun testSelectDistinct() {
        val ids = Employees
            .selectDistinct(Employees.departmentId)
            .map { it.getInt(1) }
            .sortedDescending()

        assert(ids.size == 2)
        assert(ids[0] == 2)
        assert(ids[1] == 1)
    }

    @Test
    fun testWhere() {
        val name = Employees
            .select(Employees.name)
            .where { Employees.managerId.isNull() and (Employees.departmentId eq 1) }
            .map { it.getString(1) }
            .first()

        assert(name == "vince")
    }

    @Test
    fun testWhereWithConditions() {
        val t = Employees.aliased("t")

        val name = t
            .select(t.name)
            .whereWithConditions {
                it += t.managerId.isNull()
                it += t.departmentId eq 1
            }
            .map { it.getString(1) }
            .first()

        assert(name == "vince")
    }

    @Test
    fun testCombineConditions() {
        val t = Employees.aliased("t")

        val names = t
            .select(t.name)
            .where { emptyList<ScalarExpression<Boolean>>().combineConditions() }
            .orderBy(t.id.asc())
            .map { it.getString(1) }

        assert(names.size == 4)
        assert(names[0] == "vince")
        assert(names[1] == "marry")
    }

    @Test
    fun testOrderBy() {
        val names = Employees
            .select(Employees.name)
            .where { Employees.departmentId eq 1 }
            .orderBy(Employees.salary.desc())
            .map { it.getString(1) }

        assert(names.size == 2)
        assert(names[0] == "vince")
        assert(names[1] == "marry")
    }

    @Test
    fun testAggregation() {
        val t = Employees

        val salaries = t
            .select(t.departmentId, sum(t.salary))
            .groupBy(t.departmentId)
            .associate { it.getInt(1) to it.getLong(2) }

        assert(salaries.size == 2)
        assert(salaries[1]!! == 150L)
        assert(salaries[2]!! == 300L)
    }

    @Test
    fun testHaving() {
        val t = Employees

        val salaries = t
            .select(t.departmentId, avg(t.salary))
            .groupBy(t.departmentId)
            .having { avg(t.salary) greater 100.0 }
            .associate { it.getInt(1) to it.getDouble(2) }

        println(salaries)
        assert(salaries.size == 1)
        assert(salaries.keys.first() == 2)
    }

    @Test
    fun testColumnAlias() {
        val deptId = Employees.departmentId.aliased("dept_id")
        val salaryAvg = avg(Employees.salary).aliased("salary_avg")

        Employees
            .select(deptId, salaryAvg)
            .groupBy(deptId)
            .having { salaryAvg greater 100.0 }
            .forEach { row ->
                println("${row[deptId]}:${row[salaryAvg]}")
            }

        val salaries = Employees
            .select(deptId, salaryAvg)
            .groupBy(deptId)
            .having { salaryAvg greater 100.0 }
            .associate { row ->
                row[deptId] to row[salaryAvg]
            }

        println(salaries)
        assert(salaries.size == 1)
        assert(salaries.keys.first() == 2)
        assert(salaries.values.first() == 150.0)
    }

    @Test
    fun testColumnAlias1() {
        val salary = (Employees.salary + 100).aliased(null)

        val salaries = Employees
            .select(salary)
            .where { salary greater 200L }
            .map { it.getLong(1) }

        println(salaries)
        assert(salaries.size == 1)
        assert(salaries.first() == 300L)
    }

    @Test
    fun testLimit() {
        try {
            val query = Employees.select().orderBy(Employees.id.desc()).limit(0, 2)
            assert(query.totalRecords == 4)

            val ids = query.map { it[Employees.id] }
            assert(ids[0] == 4)
            assert(ids[1] == 3)

        } catch (e: UnsupportedOperationException) {
            // Expected, pagination should be provided by dialects...
        }
    }

    @Test
    fun testBetween() {
        val names = Employees
            .select(Employees.name)
            .where { Employees.salary between 100L..200L }
            .map { it.getString(1) }

        assert(names.size == 3)
        println(names)
    }

    @Test
    fun testInList() {
        val query = Employees
            .select()
            .where { Employees.id inList listOf(1, 2, 3) }

        assert(query.count() == 3)
    }

    @Test
    fun testInNestedQuery() {
        val departmentIds = Departments.selectDistinct(Departments.id)

        val query = Employees
            .select()
            .where { Employees.departmentId inList departmentIds }

        assert(query.count() == 4)

        println(query.sql)
    }

    @Test
    fun testExists() {
        val query = Employees
            .select()
            .where {
                Employees.id.isNotNull() and exists(
                    Departments
                        .select()
                        .where { Departments.id eq Employees.departmentId }
                )
            }

        assert(query.count() == 4)
        println(query.sql)
    }

    @Test
    fun testUnion() {
        val query = Employees
            .select(Employees.id)
            .unionAll(
                Departments.select(Departments.id)
            )
            .unionAll(
                Departments.select(Departments.id)
            )
            .orderBy(Employees.id.desc())

        assert(query.count() == 8)

        println(query.sql)
    }

    @Test
    fun testMod() {
        val query = Employees.select().where { Employees.id % 2 eq 1 }
        assert(query.count() == 2)
        println(query.sql)
    }
}