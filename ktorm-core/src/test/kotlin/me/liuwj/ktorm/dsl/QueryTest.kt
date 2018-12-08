package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.BaseTest
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
        val name = Employees
            .select(Employees.name)
            .whereWithConditions {
                it += Employees.managerId.isNull()
                it += Employees.departmentId eq 1
            }
            .map { it.getString(1) }
            .first()

        assert(name == "vince")
    }
}