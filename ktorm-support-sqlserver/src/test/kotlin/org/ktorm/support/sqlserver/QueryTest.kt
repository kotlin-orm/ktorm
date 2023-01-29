package org.ktorm.support.sqlserver

import org.junit.Test
import org.ktorm.dsl.*
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class QueryTest : BaseSqlServerTest() {

    @Test
    fun testCast() {
        val salaries = database
            .from(Employees)
            .select(Employees.salary.toFloat())
            .where { Employees.salary eq 200 }
            .map { row -> row.getObject(1) }

        assertContentEquals(listOf(200.0), salaries)
    }

    @Test
    fun testUnion() {
        val query = database
            .from(Employees)
            .select(Employees.id)
            .union(
                database.from(Departments).select(Departments.id)
            )
            .union(
                database.from(Departments).select(Departments.id)
            )
            .orderBy(Employees.id.desc())
            .limit(0, 4)

        println(query.sql)

        val results = query.joinToString { row -> row.getString(1).orEmpty() }
        assertEquals("4, 3, 2, 1", results)
    }

    @Test
    fun testUnionAll() {
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

        println(query.sql)

        val results = query.joinToString { row -> row.getString(1).orEmpty() }
        assertEquals("4, 3, 2, 2, 2, 1, 1, 1", results)
    }
}
