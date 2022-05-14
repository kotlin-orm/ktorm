package org.ktorm.dsl

import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.entity.filter
import org.ktorm.entity.first
import org.ktorm.entity.forUpdate
import org.ktorm.entity.sequenceOf
import org.ktorm.expression.ScalarExpression
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.random.Random

/**
 * Created by vince on Dec 07, 2018.
 */
class QueryTest : BaseTest() {

    @Test
    fun testSelect() {
        val query = database.from(Departments).select()
        assert(query.rowSet.size() == 2)

        for (row in query) {
            println(row[Departments.name] + ": " + row[Departments.location])
        }
    }

    @Test
    fun testSelectDistinct() {
        val ids = database
            .from(Employees)
            .selectDistinct(Employees.departmentId)
            .map { it.getInt(1) }
            .sortedDescending()

        assert(ids.size == 2)
        assert(ids[0] == 2)
        assert(ids[1] == 1)
    }

    @Test
    fun testWhere() {
        val name = database
            .from(Employees)
            .select(Employees.name)
            .where { Employees.managerId.isNull() and (Employees.departmentId eq 1) }
            .map { it.getString(1) }
            .first()

        assert(name == "vince")
    }

    @Test
    fun testWhereWithConditions() {
        val t = Employees.aliased("t")

        val name = database
            .from(t)
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
    fun testWhereWithOrConditionsNoStackOverflow() {
        val t = Employees.aliased("t")

        val query = database
            .from(t)
            .select(t.name)
            .whereWithOrConditions { where ->
                repeat(100_000) {
                    where += (t.id eq Random.nextInt()) and (t.departmentId eq Random.nextInt())
                }
            }

        // very large SQL doesn't cause stackoverflow
        println(query.sql)
        assert(true)
    }

    @Test
    fun testCombineConditions() {
        val t = Employees.aliased("t")

        val names = database
            .from(t)
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
        val names = database
            .from(Employees)
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

        val salaries = database
            .from(t)
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

        val salaries = database
            .from(t)
            .select(t.departmentId, avg(t.salary))
            .groupBy(t.departmentId)
            .having(avg(t.salary).greater(100.0))
            .associate { it.getInt(1) to it.getDouble(2) }

        println(salaries)
        assert(salaries.size == 1)
        assert(salaries.keys.first() == 2)
    }

    @Test
    fun testColumnAlias() {
        val deptId = Employees.departmentId.aliased("dept_id")
        val salaryAvg = avg(Employees.salary).aliased("salary_avg")

        val salaries = database
            .from(Employees)
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

        val salaries = database
            .from(Employees)
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
            val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(0, 2)
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
        val names = database
            .from(Employees)
            .select(Employees.name)
            .where { Employees.salary between 100L..200L }
            .map { it.getString(1) }

        assert(names.size == 3)
        println(names)
    }

    @Test
    fun testInList() {
        val query = database
            .from(Employees)
            .select()
            .where { Employees.id.inList(1, 2, 3) }

        assert(query.rowSet.size() == 3)
    }

    @Test
    fun testInNestedQuery() {
        val departmentIds = database.from(Departments).selectDistinct(Departments.id)

        val query = database
            .from(Employees)
            .select()
            .where { Employees.departmentId inList departmentIds }

        assert(query.rowSet.size() == 4)

        println(query.sql)
    }

    @Test
    fun testExists() {
        val query = database
            .from(Employees)
            .select()
            .where {
                Employees.id.isNotNull() and exists(
                    database
                        .from(Departments)
                        .select()
                        .where { Departments.id eq Employees.departmentId }
                )
            }

        assert(query.rowSet.size() == 4)
        println(query.sql)
    }

    @Test
    fun testUnion() {
        val query = database
            .from(Employees)
            .select(Employees.id)
            .unionAll(
                database.from(Departments).select(Departments.id)
            )
            .unionAll(
                database.from(Departments).select(Departments.id)
            )
            .orderBy(Employees.id.desc())

        assert(query.rowSet.size() == 8)

        println(query.sql)
    }

    @Test
    fun testMod() {
        val query = database.from(Employees).select().where { Employees.id % 2 eq 1 }
        assert(query.rowSet.size() == 2)
        println(query.sql)
    }

    @Test
    @Suppress("DEPRECATION")
    fun testSelectForUpdate() {
        database.useTransaction {
            val employee = database
                .sequenceOf(Employees, withReferences = false)
                .filter { it.id eq 1 }
                .forUpdate()
                .first()

            val future = Executors.newSingleThreadExecutor().submit {
                employee.name = "vince"
                employee.flushChanges()
            }

            try {
                future.get(5, TimeUnit.SECONDS)
                throw AssertionError()
            } catch (e: ExecutionException) {
                // Expected, the record is locked.
                e.printStackTrace()
            } catch (e: TimeoutException) {
                // Expected, the record is locked.
                e.printStackTrace()
            }
        }
    }

    @Test
    fun testFlatMap() {
        val names = database
            .from(Employees)
            .select(Employees.name)
            .where { Employees.departmentId eq 1 }
            .orderBy(Employees.salary.desc())
            .flatMapIndexed { index, row -> listOf("$index:${row.getString(1)}") }

        assert(names.size == 2)
        assert(names[0] == "0:vince")
        assert(names[1] == "1:marry")
    }
}
