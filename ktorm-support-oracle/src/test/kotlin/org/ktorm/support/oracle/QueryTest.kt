package org.ktorm.support.oracle

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.schema.IntSqlType
import java.math.BigDecimal
import kotlin.test.assertContentEquals

class QueryTest : BaseOracleTest() {

    @Test
    fun testCast() {
        val mixedCases = database
            .from(Departments)
            .select(Departments.mixedCase.cast(IntSqlType))
            .where { Departments.mixedCase eq "123" }
            .map { it.getObject(1)}

        assertContentEquals(listOf(BigDecimal(123)), mixedCases)
    }
}
