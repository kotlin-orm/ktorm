package me.liuwj.ktorm.database

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.count
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.varchar
import org.junit.Test
import java.lang.Exception

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

        assert(database.sequenceOf(configs).count { it.key eq "test" } == 1)

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
}