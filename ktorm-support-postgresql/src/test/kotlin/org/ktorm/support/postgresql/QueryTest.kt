package org.ktorm.support.postgresql

import org.junit.Test
import org.ktorm.database.use
import org.ktorm.dsl.*
import org.ktorm.schema.TextSqlType
import java.sql.Clob
import kotlin.test.assertEquals

class QueryTest : BasePostgreSqlTest() {

    @Test
    fun testCast() {
        val salaries = database
            .from(Employees)
            .select(Employees.salary.cast(TextSqlType))
            .where { Employees.salary eq 200 }
            .map { row ->
                when(val value = row.getObject(1)) {
                    is Clob -> value.characterStream.use { it.readText() }
                    else -> value
                }
            }

        assertEquals(listOf("200"), salaries)
    }
}
