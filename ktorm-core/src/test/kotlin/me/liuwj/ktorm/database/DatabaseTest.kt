package me.liuwj.ktorm.database

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.dsl.*
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
        Database.global {
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
        val configs = object : Table<Nothing>("t_config") {
            val key by varchar("key").primaryKey()
            val value by varchar("value")
        }

        useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table t_config(`key` varchar(128) primary key, "value" varchar(128))"""
                statement.executeUpdate(sql)
            }
        }

        configs.insert {
            it.key to "test"
            it.value to "test value"
        }

        assert(configs.count { it.key eq "test" } == 1)

        configs.delete { it.key eq "test" }
    }

    @Test
    fun testTransaction() {
        class DummyException : Exception()

        try {
            transactional {
                Departments.insert {
                    it.name to "administration"
                    it.location to "Hong Kong"
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
                select name from t_employee
                where department_id = ?
                order by id
            """

            conn.prepareStatement(sql).use { statement ->
                statement.setInt(1, 1)
                statement.executeQuery().iterable().map { it.getString(1) }
            }
        }

        assert(names.size == 2)
        assert(names[0] == "vince")
        assert(names[1] == "marry")
    }
}