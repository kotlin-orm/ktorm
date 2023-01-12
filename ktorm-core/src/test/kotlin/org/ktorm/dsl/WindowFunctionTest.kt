package org.ktorm.dsl

import org.junit.Test
import org.ktorm.BaseTest
import kotlin.test.assertEquals

/**
 * Created by vince at Jan 12, 2023.
 */
class WindowFunctionTest : BaseTest() {

    @Test
    fun testWindowFunctions() {
        // for those that are aggregate functions
        val sums = database
            .from(Employees)
            .selectDistinct(Employees.departmentId, sum(Employees.salary).over { partitionBy(Employees.departmentId) })
            .associate { row ->
                Pair(row.getInt(1), row.getLong(2))
            }

        assertEquals(mapOf(1 to 150L, 2 to 300L), sums)

        // for those that are non-aggregate functions
        val ranks = database
            .from(Employees)
            .select(
                Employees.name,
                Employees.departmentId,
                rank().over { partitionBy(Employees.departmentId).orderBy(Employees.salary.desc()) }
            )
            .map { row ->
                Triple(row.getString(1), row.getInt(2), row.getInt(3))
            }

        assertEquals(setOf("vince", "tom"), ranks.filter { it.third == 1 }.map { it.first }.toSet())

        // for those non-aggregate functions that require parameters
        val groups = database
            .from(Employees)
            .select(Employees.id, ntile(2).over { orderBy(Employees.departmentId.asc()) })
            .associate { row ->
                Pair(row.getInt(1), row.getInt(2))
            }

        assertEquals(mapOf(1 to 1, 2 to 1, 3 to 2, 4 to 2), groups)
    }

    @Test
    fun testRowNumber() {
        val results = database
            .from(Employees)
            .select(Employees.name, rowNumber())
            .joinToString { row ->
                "${row.getString(1)}:${row.getInt(2)}"
            }

        assertEquals("vince:1, marry:2, tom:3, penny:4", results)
    }

    @Test
    fun testRank() {
        val results = database
            .from(Employees)
            .select(Employees.name, Employees.salary, rank().over { orderBy(Employees.salary.asc()) })
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}:${row.getInt(3)}"
            }

        assertEquals(setOf("marry:50:1", "vince:100:2", "penny:100:2", "tom:200:4"), results.toSet())
    }

    @Test
    fun testDenseRank() {
        val results = database
            .from(Employees)
            .select(Employees.name, Employees.salary, denseRank().over { orderBy(Employees.salary.asc()) })
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}:${row.getInt(3)}"
            }

        assertEquals(setOf("marry:50:1", "vince:100:2", "penny:100:2", "tom:200:3"), results.toSet())
    }

    @Test
    fun testPercentRank() {
        val results = database
            .from(Employees)
            .select(Employees.name, Employees.salary, percentRank().over { orderBy(Employees.salary.asc()) })
            .map { row ->
                String.format("%s:%d:%.2f", row.getString(1), row.getLong(2), row.getDouble(3))
            }

        assertEquals(setOf("marry:50:0.00", "vince:100:0.33", "penny:100:0.33", "tom:200:1.00"), results.toSet())
    }
}