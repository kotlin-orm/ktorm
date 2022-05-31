package org.ktorm.global

import org.junit.Test
import org.ktorm.dsl.*

/**
 * Created by vince at Apr 05, 2020.
 */
@Suppress("DEPRECATION")
class GlobalJoinTest : BaseGlobalTest() {

    @Test
    fun testCrossJoin() {
        val query = Employees.crossJoin(Departments).select()
        assert(query.rowSet.size() == 8)
    }

    @Test
    fun testJoinWithConditions() {
        val names = Employees
            .leftJoin(Departments, on = Employees.departmentId eq Departments.id)
            .select(Employees.name, Departments.name)
            .where { Employees.managerId.isNull() }
            .associate { it.getString(1) to it.getString(2) }

        assert(names.size == 2)
        assert(names["vince"] == "tech")
        assert(names["tom"] == "finance")
    }

    @Test
    fun testMultiJoin() {
        data class Names(val name: String, val managerName: String, val departmentName: String)

        val emp = Employees.aliased("emp")
        val mgr = Employees.aliased("mgr")
        val dept = Departments.aliased("dept")

        val results = emp
            .leftJoin(dept, on = emp.departmentId eq dept.id)
            .leftJoin(mgr, on = emp.managerId eq mgr.id)
            .select(emp.name, mgr.name, dept.name)
            .orderBy(emp.id.asc())
            .map { row ->
                Names(
                    name = row[emp.name].orEmpty(),
                    managerName = row[mgr.name].orEmpty(),
                    departmentName = row[dept.name].orEmpty()
                )
            }

        assert(results.size == 4)
        assert(results[0] == Names(name = "vince", managerName = "", departmentName = "tech"))
        assert(results[1] == Names(name = "marry", managerName = "vince", departmentName = "tech"))
        assert(results[2] == Names(name = "tom", managerName = "", departmentName = "finance"))
        assert(results[3] == Names(name = "penny", managerName = "tom", departmentName = "finance"))
    }

    @Test
    fun testHasColumn() {
        data class Names(val name: String, val managerName: String, val departmentName: String)

        val emp = Employees.aliased("emp")
        val mgr = Employees.aliased("mgr")
        val dept = Departments.aliased("dept")

        val results = emp
            .select(emp.name)
            .map {
                Names(
                    name = it[emp.name].orEmpty(),
                    managerName = it[mgr.name].orEmpty(),
                    departmentName = it[dept.name].orEmpty()
                )
            }

        results.forEach(::println)
        assert(results.all { it.managerName == "" })
        assert(results.all { it.departmentName == "" })
    }
}