package org.ktorm.global

import org.junit.Test
import org.ktorm.database.Database
import org.ktorm.database.asIterable
import org.ktorm.database.use
import org.ktorm.dsl.eq
import org.ktorm.schema.Table
import org.ktorm.schema.varchar

/**
 * Created by vince at Apr 05, 2020.
 */
@Suppress("DEPRECATION")
class GlobalDatabaseTest : BaseGlobalTest() {

    @Test
    fun testMetadata() {
        with(Database.global) {
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

        useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """CREATE TABLE T_CONFIG(KEY VARCHAR(128) PRIMARY KEY, VALUE VARCHAR(128))"""
                statement.executeUpdate(sql)
            }
        }

        configs.insert {
            set(it.key, "test")
            set(it.value, "test value")
        }

        assert(configs.count { it.key eq "test" } == 1)

        configs.delete { it.key eq "test" }
    }

    @Test
    fun testTransaction() {
        class DummyException : Exception()

        try {
            useTransaction {
                Departments.insert {
                    set(it.name, "administration")
                    set(it.location, LocationWrapper("Hong Kong"))
                }

                assert(Departments.count() == 3)

                throw DummyException()
            }

        } catch (e: DummyException) {
            assert(Departments.count() == 2)
        }
    }

    @Test
    fun testRawSql() {
        val names = useConnection { conn ->
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
