package org.ktorm.support.oracle

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.schema.IntSqlType
import java.math.BigDecimal
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class QueryTest : BaseOracleTest() {

    @Test
    fun testCast() {
        val mixedCases = database
            .from(Departments)
            .select(Departments.mixedCase.cast(IntSqlType))
            .where { Departments.mixedCase eq "123" }
            .map { row -> row.getObject(1)}

        assertContentEquals(listOf(BigDecimal(123)), mixedCases)
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
    fun testUnionWithoutOrderBy() {
        val query = database
            .from(Employees)
            .select(Employees.id)
            .union(
                database.from(Departments).select(Departments.id)
            )
            .union(
                database.from(Departments).select(Departments.id)
            )

        println(query.sql)

        val results = query.map { row -> row.getInt(1) }
        assertEquals(setOf(1, 2, 3, 4), results.toSet())
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
        assertEquals("4, 3, 3, 3, 2, 2, 2, 1, 1, 1", results)
    }
}
