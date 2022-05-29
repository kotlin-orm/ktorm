package org.ktorm.support.postgresql

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.ktorm.database.use
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.Table
import org.ktorm.schema.enum
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Created by vince on Feb 13, 2019.
 */
class CommonPostgreSqlTest : BasePostgreSqlTest() {

    @Test
    fun testKeywordWrapping() {
        val configs = object : Table<Nothing>("t_config") {
            val key = varchar("key").primaryKey()
            val value = varchar("value")
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table t_config(key varchar(128) primary key, value varchar(128))"""
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
    fun testILike() {
        val names = database.employees.filter { it.name ilike "VINCE" }.mapColumns { it.name }
        println(names)
        assert(names.size == 1)
        assert(names[0] == "vince")
    }

    @Test
    fun testDropTake() {
        val employees = database.employees.drop(1).take(1).toList()
        println(employees)
        assert(employees.size == 1)
    }

    @Test
    fun testReturnInTransactionBlock() {
        insertTransactional()
        assert(database.departments.count() == 3)
    }

    private fun insertTransactional(): Int {
        database.useTransaction {
            return database.insert(Departments) {
                set(it.name, "dept name")
                set(it.location, LocationWrapper("dept location"))
                set(it.mixedCase, "value for mixed case")
            }
        }
    }

    @Test
    fun testSelectForUpdate() {
        database.useTransaction {
            val emp = Employees.aliased("emp")

            val employee = database
                .sequenceOf(emp, withReferences = false)
                .filter { it.id eq 1 }
                .locking(LockingMode.FOR_UPDATE, tables = listOf(emp), wait = LockingWait.SKIP_LOCKED)
                .first()

            val future = Executors.newSingleThreadExecutor().submit {
                employee.name = "vince"
                employee.flushChanges()
            }

            try {
                future.get(5, TimeUnit.SECONDS)
                throw AssertionError()
            } catch (e: ExecutionException) {
                // Expected, the record is locked.
                e.printStackTrace()
            } catch (e: TimeoutException) {
                // Expected, the record is locked.
                e.printStackTrace()
            }
        }
    }

    @Test
    fun testSchema() {
        val t = object : Table<Department>("t_department", catalog = databaseName, schema = "public") {
            val id = int("id").primaryKey().bindTo { it.id }
            val name = varchar("name").bindTo { it.name }
        }

        database.update(t) {
            set(it.name, "test")
            where {
                it.id eq 1
            }
        }

        assert(database.sequenceOf(t).filter { it.id eq 1 }.mapTo(HashSet()) { it.name } == setOf("test"))
        assert(database.sequenceOf(t.aliased("t")).mapTo(HashSet()) { it.name } == setOf("test", "finance"))
    }

    @Test
    fun testMaxColumnNameLength() {
        val t = object : Table<Nothing>("t_long_name") {
            val col = varchar("a".repeat(database.maxColumnNameLength))
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table t_long_name(${t.col.name} varchar(128))"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(t) {
            set(it.col, "test")
        }

        try {
            val name = database.from(t).select(t.col).map { it[t.col] }.first()
            println(name)
            throw java.lang.AssertionError("unexpected.")
        } catch (e: IllegalStateException) {
            println(e.message)
            assert("too long" in e.message!!)
        }

    }

    enum class Mood {
        HAPPY,
        SAD
    }

    object TableWithEnum : Table<Nothing>("t_enum") {
        val id = int("id").primaryKey()
        val current_mood = enum<Mood>("current_mood")
    }

    @Test
    fun testEnum() {
        database.insert(TableWithEnum) {
            set(it.current_mood, Mood.SAD)
        }

        val count = database.sequenceOf(TableWithEnum).count { it.current_mood eq Mood.SAD }
        assertThat(count, equalTo(1))

        val mood = database.sequenceOf(TableWithEnum).filter { it.id eq 1 }.mapColumns { it.current_mood }.first()
        assertThat(mood, equalTo(Mood.HAPPY))

        database.insert(TableWithEnum) {
            set(it.current_mood, null)
        }

        val mood1 = database.sequenceOf(TableWithEnum).filter { it.id eq 3 }.mapColumns { it.current_mood }.first()
        assertThat(mood1, equalTo(null))
    }
}