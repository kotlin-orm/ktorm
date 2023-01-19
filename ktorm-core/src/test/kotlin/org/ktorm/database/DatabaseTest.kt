package org.ktorm.database

import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.dsl.*
import org.ktorm.entity.count
import org.ktorm.entity.defaultValue
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * Created by vince on Dec 02, 2018.
 */
class DatabaseTest : BaseTest() {

    @Test
    fun testMetadata() {
        with(database) {
            println(url)
            println(name)
            println(productName)
            println(productVersion)
            println(keywords.toString())
            println(identifierQuoteString)
            println(extraNameCharacters)
        }
    }

    @Test
    fun testKeywordWrapping() {
        val configs = object : Table<Nothing>("T_CONFIG") {
            val key = varchar("KEY").primaryKey()
            val value = varchar("VALUE")
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """CREATE TABLE T_CONFIG(KEY VARCHAR(128) PRIMARY KEY, VALUE VARCHAR(128))"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(configs) {
            set(it.key, "test")
            set(it.value, "test value")
        }

        assert(database.from(configs).select(count()).where(configs.key eq "test").map { it.getInt(1) }[0] == 1)

        database.delete(configs) { it.key eq "test" }
    }

    @Test
    fun testTransaction() {
        class DummyException : Exception()

        try {
            database.useTransaction {
                database.insert(Departments) {
                    set(it.name, "administration")
                    set(it.location, LocationWrapper("Hong Kong"))
                }

                assert(database.departments.count() == 3)

                throw DummyException()
            }

        } catch (e: DummyException) {
            assert(database.departments.count() == 2)
        }
    }

    @Test
    fun testRawSql() {
        val names = database.useConnection { conn ->
            val sql = """
                select "name" from "t_employee"
                where "department_id" = ?
                order by "id"
            """

            conn.prepareStatement(sql).use { statement ->
                statement.setInt(1, 1)
                statement.executeQuery().asIterable().map { it.getString(1) }
            }
        }

        assert(names.size == 2)
        assert(names[0] == "vince")
        assert(names[1] == "marry")
    }

    @Test
    fun testDefaultValueReferenceEquality() {
        assert(Boolean::class.javaPrimitiveType!!.defaultValue === Boolean::class.javaPrimitiveType!!.defaultValue)
        assert(Char::class.javaPrimitiveType!!.defaultValue === Char::class.javaPrimitiveType!!.defaultValue)
        assert(Byte::class.javaPrimitiveType!!.defaultValue === Byte::class.javaPrimitiveType!!.defaultValue)
        assert(Short::class.javaPrimitiveType!!.defaultValue === Short::class.javaPrimitiveType!!.defaultValue)
        assert(Int::class.javaPrimitiveType!!.defaultValue === Int::class.javaPrimitiveType!!.defaultValue)
        assert(Long::class.javaPrimitiveType!!.defaultValue === Long::class.javaPrimitiveType!!.defaultValue)
        assert(Float::class.javaPrimitiveType!!.defaultValue !== Float::class.javaPrimitiveType!!.defaultValue)
        assert(Double::class.javaPrimitiveType!!.defaultValue !== Double::class.javaPrimitiveType!!.defaultValue)
        assert(String::class.java.defaultValue === String::class.java.defaultValue)
        assert(UByte::class.java.defaultValue !== UByte::class.java.defaultValue)
        assert(UShort::class.java.defaultValue !== UShort::class.java.defaultValue)
        assert(UInt::class.java.defaultValue !== UInt::class.java.defaultValue)
        assert(ULong::class.java.defaultValue !== ULong::class.java.defaultValue)
    }

    enum class Status(val code: Int) {
        ONE(1), TWO(2), THREE(3);

        companion object {
            fun forCode(code: Int): Status {
                return values().first { it.code == code }
            }
        }
    }

    @Test
    fun testTransformingNullValues() {
        val t = object : Table<Nothing>("T_TEST_TRANSFORMING_NULL_VALUES") {
            val status = int("STATUS").transform({ Status.forCode(it) }, { it.code })
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """CREATE TABLE T_TEST_TRANSFORMING_NULL_VALUES(STATUS INT)"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(t) {
            set(it.status, null)
        }

        val query = database.from(t).select(t.status)
        assert(query.map { row -> row[t.status] }.first() == null)
    }

}