package org.ktorm.support.sqlserver

import org.junit.Test
import org.ktorm.dsl.*
import kotlin.test.assertContentEquals

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
}
