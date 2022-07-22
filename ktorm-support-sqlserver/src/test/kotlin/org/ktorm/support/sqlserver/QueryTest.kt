package org.ktorm.support.sqlserver

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.schema.FloatSqlType
import kotlin.test.assertContentEquals

class QueryTest : BaseSqlServerTest() {

    @Test
    fun testCast() {
        val salaries = database
            .from(Employees)
            .select(Employees.salary.cast(FloatSqlType))
            .where { Employees.salary eq 200 }
            .map { it.getObject(1) }

        assertContentEquals(listOf(200.0), salaries)
    }
}
