package org.ktorm.support.oracle

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.dsl.WindowFrames.currentRow
import org.ktorm.dsl.WindowFrames.following
import org.ktorm.dsl.WindowFrames.preceding
import org.ktorm.dsl.WindowFrames.unboundedFollowing
import org.ktorm.dsl.WindowFrames.unboundedPreceding
import kotlin.test.assertEquals

/**
 * Created by vince at Jan 26, 2023.
 */
class WindowFunctionTest2 : BaseOracleTest() {

    @Test
    fun testMultiPartitionBy() {
        val results = database
            .from(Employees)
            .select(Employees.name, max(Employees.salary).over { partitionBy(Employees.id, Employees.name) })
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:100", "marry:50", "tom:200", "penny:100"), results.toSet())
    }

    @Test
    fun testMultiOrderBy() {
        val results = database
            .from(Employees)
            .select(Employees.name, sum(Employees.salary).over { orderBy(Employees.id.asc(), Employees.name.desc()) })
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:100", "marry:150", "tom:350", "penny:450"), results.toSet())
    }

    @Test
    fun testRows() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                sum(Employees.salary).over { orderBy(Employees.id.asc()).rows(preceding(1)) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:100", "marry:150", "tom:250", "penny:300"), results.toSet())
    }

    @Test
    fun testRowsBetween() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                sum(Employees.salary).over { orderBy(Employees.id.asc()).rowsBetween(currentRow(), following(1)) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:150", "marry:250", "tom:300", "penny:100"), results.toSet())
    }

    @Test
    fun testRange() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                sum(Employees.salary).over { orderBy(Employees.salary.asc()).range(preceding(100)) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("marry:50", "vince:250", "penny:250", "tom:400"), results.toSet())
    }

    @Test
    fun testRangeBetween() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                sum(Employees.salary).over { orderBy(Employees.id.asc()).rangeBetween(unboundedPreceding(), unboundedFollowing()) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:450", "marry:450", "tom:450", "penny:450"), results.toSet())
    }

    @Test
    fun testTotalRecords() {
        val query = database
            .from(Employees)
            .select(Employees.name, Employees.salary, rank().over { orderBy(Employees.salary.asc(), Employees.id.asc()) })
            .orderBy(Employees.salary.asc(), Employees.id.asc())
            .limit(0, 2)

        val results = query.map { row -> "${row.getString(1)}:${row.getLong(2)}:${row.getInt(3)}" }
        assertEquals(setOf("marry:50:1", "vince:100:2"), results.toSet())
        assertEquals(4, query.totalRecordsInAllPages)
    }
}