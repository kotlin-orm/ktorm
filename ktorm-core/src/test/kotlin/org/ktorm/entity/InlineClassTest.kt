package org.ktorm.entity

import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.database.use
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.schema.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import kotlin.test.assertEquals

class InlineClassTest : BaseTest() {

    fun BaseTable<*>.ulong(name: String): Column<ULong> {
        return registerColumn(name, object : SqlType<ULong>(Types.BIGINT, "bigint unsigned") {
            override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: ULong) {
                ps.setLong(index, parameter.toLong())
            }

            override fun doGetResult(rs: ResultSet, index: Int): ULong {
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
        for (method in TestUnsigned::class.java.methods) {
            println(method)
        }

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
        val unsigned2 = TestUnsignedNullable { id = null }
        assert(unsigned2.id == null)

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

    interface Case1 {
        @JvmInline
        value class ICPrimitive(val x: Int)
    }

    @Test
    fun testUnboxCase1() {
        val c = Case1.ICPrimitive(1)
        assertEquals(c.unboxTo(Int::class.java), 1)
        assertEquals(c.unboxTo(Case1.ICPrimitive::class.java), Case1.ICPrimitive(1))
    }

    @Test
    fun testBoxCase1() {
        assertEquals(Case1.ICPrimitive::class.boxFrom(1), Case1.ICPrimitive(1))
        assertEquals(Case1.ICPrimitive::class.boxFrom(Case1.ICPrimitive(1)), Case1.ICPrimitive(1))
    }

    interface Case2 {
        @JvmInline
        value class ICReference(val s: String)
    }

    @Test
    fun testUnboxCase2() {
        val c = Case2.ICReference("hello")
        assertEquals(c.unboxTo(String::class.java), "hello")
        assertEquals(c.unboxTo(Case2.ICReference::class.java), Case2.ICReference("hello"))
    }

    @Test
    fun testBoxCase2() {
        assertEquals(Case2.ICReference::class.boxFrom("hello"), Case2.ICReference("hello"))
        assertEquals(Case2.ICReference::class.boxFrom(Case2.ICReference("hello")), Case2.ICReference("hello"))
    }

    interface Case3 {
        @JvmInline
        value class ICNullable(val s: String?)
    }

    @Test
    fun testUnboxCase3() {
        val c1 = Case3.ICNullable("hello")
        assertEquals(c1.unboxTo(String::class.java), "hello")
        assertEquals(c1.unboxTo(Case3.ICNullable::class.java), Case3.ICNullable("hello"))

        val c2 = Case3.ICNullable(null)
        assertEquals(c2.unboxTo(String::class.java), null)
        assertEquals(c2.unboxTo(Case3.ICNullable::class.java), Case3.ICNullable(null))
    }

    @Test
    fun testBoxCase3() {
        assertEquals(Case3.ICNullable::class.boxFrom(null), Case3.ICNullable(null))
        assertEquals(Case3.ICNullable::class.boxFrom("hello"), Case3.ICNullable("hello"))
        assertEquals(Case3.ICNullable::class.boxFrom(Case3.ICNullable("hello")), Case3.ICNullable("hello"))
    }

    interface Case4 {
        @JvmInline
        value class IC1(val s: String)
        @JvmInline
        value class IC2(val ic1: IC1?)
        @JvmInline
        value class IC3(val ic2: IC2)
    }

    @Test
    fun testUnboxCase4() {
        val c1 = Case4.IC3(Case4.IC2(Case4.IC1("hello")))
        assertEquals(c1.unboxTo(String::class.java), "hello")
        assertEquals(c1.unboxTo(Case4.IC3::class.java), Case4.IC3(Case4.IC2(Case4.IC1("hello"))))

        val c2 = Case4.IC3(Case4.IC2(null))
        assertEquals(c2.unboxTo(String::class.java), null)
        assertEquals(c2.unboxTo(Case4.IC3::class.java), Case4.IC3(Case4.IC2(null)))
    }

    @Test
    fun testBoxCase4() {
        assertEquals(Case4.IC3::class.boxFrom(null), Case4.IC3(Case4.IC2(null)))
        assertEquals(Case4.IC3::class.boxFrom("hello"), Case4.IC3(Case4.IC2(Case4.IC1("hello"))))
        assertEquals(Case4.IC3::class.boxFrom(Case4.IC3(Case4.IC2(Case4.IC1("hello")))), Case4.IC3(Case4.IC2(Case4.IC1("hello"))))
    }

    interface Case5 {
        @JvmInline
        value class IC1(val s: String?)
        @JvmInline
        value class IC2(val ic1: IC1?)
        @JvmInline
        value class IC3(val ic2: IC2)

        interface TestEntity : Entity<TestEntity> {
            companion object : Entity.Factory<TestEntity>()
            var id: IC3
        }
    }

    fun BaseTable<*>.case5(name: String): Column<Case5.IC3> {
        return registerColumn(name, object : SqlType<Case5.IC3>(Types.VARCHAR, "varchar") {
            override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Case5.IC3) {
                val s = parameter.ic2.ic1?.s
                if (s == null) {
                    ps.setNull(index, typeCode)
                } else {
                    ps.setString(index, s)
                }
            }

            override fun doGetResult(rs: ResultSet, index: Int): Case5.IC3 {
                return Case5.IC3(Case5.IC2(Case5.IC1(rs.getString(index))))
            }
        })
    }

    @Test
    fun testUnboxCase5() {
        val c1 = Case5.IC3(Case5.IC2(Case5.IC1("hello")))
        assertEquals(c1.unboxTo(String::class.java), "hello")
        assertEquals(c1.unboxTo(Case5.IC1::class.java), Case5.IC1("hello"))
        assertEquals(c1.unboxTo(Case5.IC3::class.java), Case5.IC3(Case5.IC2(Case5.IC1("hello"))))

        val c2 = Case5.IC3(Case5.IC2(null))
        assertEquals(c2.unboxTo(String::class.java), null)
        assertEquals(c2.unboxTo(Case5.IC1::class.java), null)
        assertEquals(c2.unboxTo(Case5.IC3::class.java), Case5.IC3(Case5.IC2(null)))

        val c3 = Case5.IC3(Case5.IC2(Case5.IC1(null)))
        assertEquals(c3.unboxTo(String::class.java), null)
        assertEquals(c3.unboxTo(Case5.IC1::class.java), Case5.IC1(null))
        assertEquals(c3.unboxTo(Case5.IC3::class.java), Case5.IC3(Case5.IC2(Case5.IC1(null))))
    }

    @Test
    fun testBoxCase5() {
        assertEquals(Case5.IC3::class.boxFrom(null), Case5.IC3(Case5.IC2(null)))
        assertEquals(Case5.IC3::class.boxFrom(Case5.IC1(null)), Case5.IC3(Case5.IC2(Case5.IC1(null))))
        assertEquals(Case5.IC3::class.boxFrom("hello"), Case5.IC3(Case5.IC2(Case5.IC1("hello"))))
        assertEquals(Case5.IC3::class.boxFrom(Case5.IC1("hello")), Case5.IC3(Case5.IC2(Case5.IC1("hello"))))
        assertEquals(Case5.IC3::class.boxFrom(Case5.IC3(Case5.IC2(Case5.IC1("hello")))), Case5.IC3(Case5.IC2(Case5.IC1("hello"))))
    }

    @Test
    fun testNestedInlineClassCase5() {
        for (method in Case5.TestEntity::class.java.methods) {
            println(method)
        }

        val t = object : Table<Case5.TestEntity>("T_TEST_NESTED_INLINE_CLASS_CASE5") {
            val id = case5("ID").primaryKey().bindTo { it.id }
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """CREATE TABLE T_TEST_NESTED_INLINE_CLASS_CASE5(ID VARCHAR NOT NULL PRIMARY KEY)"""
                statement.executeUpdate(sql)
            }
        }

        val entity = Case5.TestEntity { id = Case5.IC3(Case5.IC2(Case5.IC1("hello"))) }
        assert(entity.id == Case5.IC3(Case5.IC2(Case5.IC1("hello"))))
        database.sequenceOf(t).add(entity)

        val ids = database.sequenceOf(t).toList().map { it.id }
        println(ids)
        assert(ids == listOf(Case5.IC3(Case5.IC2(Case5.IC1("hello")))))
    }
}