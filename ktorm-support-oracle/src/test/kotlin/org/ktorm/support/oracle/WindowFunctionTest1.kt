package org.ktorm.support.oracle

import org.junit.Test
import org.ktorm.dsl.*
import kotlin.test.assertEquals

/**
 * Created by vince at Jan 26, 2023.
 */
class WindowFunctionTest1 : BaseOracleTest() {
    @Test
    fun testMin() {
        val results = database
            .from(Employees)
            .select(Employees.name, min(Employees.salary).over())
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:50", "marry:50", "tom:50", "penny:50"), results.toSet())
    }

    @Test
    fun testRunningMin() {
        val results = database
            .from(Employees)
            .select(Employees.name, min(Employees.salary).over { orderBy(Employees.id.asc()) })
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:100", "marry:50", "tom:50", "penny:50"), results.toSet())
    }

    @Test
    fun testMax() {
        val results = database
            .from(Employees)
            .select(Employees.name, max(Employees.salary).over())
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:200", "marry:200", "tom:200", "penny:200"), results.toSet())
    }

    @Test
    fun testRunningMax() {
        val results = database
            .from(Employees)
            .select(Employees.name, max(Employees.salary).over { orderBy(Employees.id.asc()) })
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:100", "marry:100", "tom:200", "penny:200"), results.toSet())
    }

    @Test
    fun testAvg() {
        val results = database
            .from(Employees)
            .select(Employees.name, avg(Employees.salary).over())
            .map { row ->
                String.format("%s:%.2f", row.getString(1), row.getDouble(2))
            }

        assertEquals(setOf("vince:112.50", "marry:112.50", "tom:112.50", "penny:112.50"), results.toSet())
    }

    @Test
    fun testRunningAvg() {
        val results = database
            .from(Employees)
            .select(Employees.name, avg(Employees.salary).over { orderBy(Employees.id.asc()) })
            .map { row ->
                String.format("%s:%.2f", row.getString(1), row.getDouble(2))
            }

        assertEquals(setOf("vince:100.00", "marry:75.00", "tom:116.67", "penny:112.50"), results.toSet())
    }

    @Test
    fun testSum() {
        val results = database
            .from(Employees)
            .select(Employees.name, sum(Employees.salary).over())
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:450", "marry:450", "tom:450", "penny:450"), results.toSet())
    }

    @Test
    fun testRunningSum() {
        val results = database
            .from(Employees)
            .select(Employees.name, sum(Employees.salary).over { orderBy(Employees.id.asc()) })
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:100", "marry:150", "tom:350", "penny:450"), results.toSet())
    }

    @Test
    fun testCount() {
        val results = database
            .from(Employees)
            .select(Employees.name, count(Employees.salary).over())
            .map { row ->
                "${row.getString(1)}:${row.getInt(2)}"
            }

        assertEquals(setOf("vince:4", "marry:4", "tom:4", "penny:4"), results.toSet())
    }

    @Test
    fun testRunningCount() {
        val results = database
            .from(Employees)
            .select(Employees.name, count(Employees.salary).over { orderBy(Employees.id.asc()) })
            .map { row ->
                "${row.getString(1)}:${row.getInt(2)}"
            }

        assertEquals(setOf("vince:1", "marry:2", "tom:3", "penny:4"), results.toSet())
    }
}