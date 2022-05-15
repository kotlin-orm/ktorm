package org.ktorm.entity

import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.database.use
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import org.ktorm.schema.Table
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class InlineClassTest : BaseTest() {

    fun BaseTable<*>.ulong(name: String): Column<ULong> {
        return registerColumn(name, object : SqlType<ULong>(Types.BIGINT, "bigint unsigned") {
            override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: ULong) {
                ps.setLong(index, parameter.toLong())
            }

            override fun doGetResult(rs: ResultSet, index: Int): ULong? {
                return rs.getLong(index).toULong()
            }
        })
    }

    interface TestUnsigned : Entity<TestUnsigned> {
        companion object : Entity.Factory<TestUnsigned>()
        var id: ULong
    }

    @Test
    fun testUnsigned() {
        val t = object : Table<TestUnsigned>("T_TEST_UNSIGNED") {
            val id = ulong("ID").primaryKey().bindTo { it.id }
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """CREATE TABLE T_TEST_UNSIGNED(ID BIGINT UNSIGNED NOT NULL PRIMARY KEY)"""
                statement.executeUpdate(sql)
            }
        }

        val unsigned = TestUnsigned { id = 5UL }
        assert(unsigned.id == 5UL)
        database.sequenceOf(t).add(unsigned)

        val ids = database.sequenceOf(t).toList().map { it.id }
        println(ids)
        assert(ids == listOf(5UL))

        database.insert(t) {
            set(it.id, 6UL)
        }

        val ids2 = database.from(t).select(t.id).map { row -> row[t.id] }
        println(ids2)
        assert(ids2 == listOf(5UL, 6UL))

        assert(TestUnsigned().id == 0UL)
    }

    interface TestUnsignedNullable : Entity<TestUnsignedNullable> {
        companion object : Entity.Factory<TestUnsignedNullable>()
        var id: ULong?
    }

    @Test
    fun testUnsignedNullable() {
        val t = object : Table<TestUnsignedNullable>("T_TEST_UNSIGNED_NULLABLE") {
            val id = ulong("ID").primaryKey().bindTo { it.id }
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """CREATE TABLE T_TEST_UNSIGNED_NULLABLE(ID BIGINT UNSIGNED NOT NULL PRIMARY KEY)"""
                statement.executeUpdate(sql)
            }
        }

        val unsigned = TestUnsignedNullable { id = 5UL }
        assert(unsigned.id == 5UL)
        database.sequenceOf(t).add(unsigned)

        val ids = database.sequenceOf(t).toList().map { it.id }
        println(ids)
        assert(ids == listOf(5UL))

        assert(TestUnsignedNullable().id == null)
    }
}