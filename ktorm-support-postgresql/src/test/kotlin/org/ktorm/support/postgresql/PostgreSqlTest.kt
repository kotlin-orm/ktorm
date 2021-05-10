package org.ktorm.support.postgresql

import com.alibaba.druid.pool.DruidDataSource
import com.mchange.v2.c3p0.ComboPooledDataSource
import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.dbcp2.BasicDataSource
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.ClassRule
import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.ktorm.database.use
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.jackson.json
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.ktorm.schema.*
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Created by vince on Feb 13, 2019.
 */
class PostgreSqlTest : BaseTest() {

    companion object {
        class KPostgreSqlContainer : PostgreSQLContainer<KPostgreSqlContainer>("postgres:13-alpine")

        @ClassRule
        @JvmField
        val postgres = KPostgreSqlContainer()
    }

    override fun init() {
        database = Database.connect(
            url = postgres.jdbcUrl,
            driver = postgres.driverClassName,
            user = postgres.username,
            password = postgres.password,
            logger = ConsoleLogger(threshold = LogLevel.TRACE)
        )

        execSqlScript("init-postgresql-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-postgresql-data.sql")
    }

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
    fun testUpdate() {
        database.update(Employees) {
            set(it.job, "engineer")
            set(it.managerId, null)
            set(it.salary, 100)

            where {
                it.id eq 2
            }
        }

        val employee = database.employees.find { it.id eq 2 } ?: throw AssertionError()
        assert(employee.name == "marry")
        assert(employee.job == "engineer")
        assert(employee.manager == null)
        assert(employee.salary == 100L)
    }

    @Test
    fun testInsertOrUpdate() {
        database.insertOrUpdate(Employees) {
            set(it.id, 1)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict {
                set(it.salary, it.salary + 1000)
            }
        }
        database.insertOrUpdate(Employees.aliased("t")) {
            set(it.id, 5)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict(it.id) {
                set(it.salary, it.salary + 1000)
            }
        }

        assert(database.employees.find { it.id eq 1 }!!.salary == 1100L)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1000L)
    }

    @Test
    fun testInsertOrUpdateReturning() {
        database.insertOrUpdateReturning(Employees, Employees.id) {
            set(it.name, "pedro")
            set(it.job, "engineer")
            set(it.salary, 1500)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict {
                set(it.salary, it.salary + 900)
            }
        }.let { id ->
            assert(id == 5)
        }

        database.insertOrUpdateReturning(Employees, Pair(Employees.id, Employees.job)) {
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict {
                set(it.salary, it.salary + 900)
            }
        }.let { (id, job) ->
            assert(id == 6)
            assert(job == "engineer")
        }

        val t = Employees.aliased("t")
        database.insertOrUpdateReturning(t, Triple(t.id, t.job, t.salary)) {
            set(it.id, 6)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict(it.id) {
                set(it.salary, it.salary + 900)
            }
        }.let { (id, job, salary) ->
            assert(id == 6)
            assert(job == "engineer")
            assert(salary == 1900L)
        }

        database.insertOrUpdateReturning(t, Triple(t.id, t.job, t.salary)) {
            set(it.id, 6)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict(it.id) {
                doNothing()
            }
        }.let { (id, job, salary) ->
            assert(id == null)
            assert(job == null)
            assert(salary == null)
        }
    }

    @Test
    fun testInsertReturning() {
        database.insertReturning(Employees, Employees.id) {
            set(it.name, "pedro")
            set(it.job, "engineer")
            set(it.salary, 1500)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
        }.let { id ->
            assert(id == 5)
        }

        database.insertReturning(Employees, Pair(Employees.id, Employees.job)) {
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
        }.let { (id, job) ->
            assert(id == 6)
            assert(job == "engineer")
        }

        val t = Employees.aliased("t")
        database.insertReturning(t, Triple(t.id, t.job, t.salary)) {
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
        }.let { (id, job, salary) ->
            assert(id == 7)
            assert(job == "engineer")
            assert(salary == 1000L)
        }
    }

    @Test
    fun testBulkInsert() {
        database.bulkInsert(Employees) {
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 1)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 1)
            }
        }

        assert(database.employees.count() == 6)
    }

    @Test
    fun testBulkInsertReturning() {
        database.bulkInsertReturning(Employees, Employees.id) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(5, 6))
        }

        database.bulkInsertReturning(Employees, Pair(Employees.id, Employees.job)) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(Pair(7, "trainee"), Pair(8, "engineer")))
        }

        val t = Employees.aliased("t")
        database.bulkInsertReturning(t, Triple(t.id, t.job, t.salary)) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(Triple(9, "trainee", 1000L), Triple(10, "engineer", 1000L)))
        }
    }

    @Test
    fun testBulkInsertOrUpdate() {
        database.bulkInsertOrUpdate(Employees) {
            item {
                set(it.id, 1)
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.id, 5)
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            onConflict(it.id) {
                set(it.job, it.job)
                set(it.departmentId, excluded(it.departmentId))
                set(it.salary, it.salary + 1000)
            }
        }

        database.employees.find { it.id eq 1 }!!.let {
            assert(it.job == "engineer")
            assert(it.department.id == 2)
            assert(it.salary == 1100L)
        }

        database.employees.find { it.id eq 5 }!!.let {
            assert(it.job == "engineer")
            assert(it.department.id == 2)
            assert(it.salary == 1000L)
        }
    }

    @Test
    fun testBulkInsertOrUpdate1() {
        val bulkInsertWithUpdate = { ignoreErrors: Boolean ->
            database.bulkInsertOrUpdate(Employees) {
                item {
                    set(it.id, 5)
                    set(it.name, "vince")
                    set(it.job, "engineer")
                    set(it.salary, 1000)
                    set(it.hireDate, LocalDate.now())
                    set(it.departmentId, 1)
                }
                item {
                    set(it.id, 6)
                    set(it.name, "vince")
                    set(it.job, "engineer")
                    set(it.salary, 1000)
                    set(it.hireDate, LocalDate.now())
                    set(it.departmentId, 1)
                }
                onConflict {
                    if (ignoreErrors) doNothing() else set(it.salary, it.salary + 900)
                }
            }
        }

        bulkInsertWithUpdate(false)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1000L)
        assert(database.employees.find { it.id eq 6 }!!.salary == 1000L)

        bulkInsertWithUpdate(false)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1900L)
        assert(database.employees.find { it.id eq 6 }!!.salary == 1900L)

        bulkInsertWithUpdate(true)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1900L)
        assert(database.employees.find { it.id eq 6 }!!.salary == 1900L)
    }

    @Test
    fun testBulkInsertOrUpdateReturning() {
        database.bulkInsertOrUpdateReturning(Employees, Employees.id) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            onConflict {
                doNothing()
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(5, 6))
        }

        database.bulkInsertOrUpdateReturning(Employees, Pair(Employees.id, Employees.job)) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            onConflict {
                doNothing()
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(Pair(7, "trainee"), Pair(8, "engineer")))
        }

        val t = Employees.aliased("t")
        database.bulkInsertOrUpdateReturning(t, Triple(t.id, t.job, t.salary)) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            onConflict {
                doNothing()
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(Triple(9, "trainee", 1000L), Triple(10, "engineer", 1000L)))
        }

        database.bulkInsertOrUpdateReturning(t, Triple(t.id, t.job, t.salary)) {
            item {
                set(it.id, 10)
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.id, 11)
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            onConflict {
                doNothing()
            }
        }.let { results ->
            assert(results.size == 1)
            assert(results == listOf(Triple(11, "engineer", 1000L)))
        }
    }

    @Test
    fun testInsertAndGenerateKey() {
        val id = database.insertAndGenerateKey(Employees) {
            set(it.name, "Joe Friend")
            set(it.job, "Tester")
            set(it.managerId, null)
            set(it.salary, 50)
            set(it.hireDate, LocalDate.of(2020, 1, 10))
            set(it.departmentId, 1)
        } as Int

        assert(id > 4)

        assert(database.employees.count() == 5)
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

    object Metadatas : Table<Nothing>("t_metadata") {
        val id = int("id").primaryKey()
        val attributes = hstore("attrs")
        val numbers = textArray("numbers")
    }

    @Test
    fun testHStore() {
        val attributes = get { it.attributes } ?: error("Cannot get the attributes!")
        assertThat(attributes.size, equalTo(3))
        assertThat(attributes["a"], equalTo("1"))
        assertThat(attributes["b"], equalTo("2"))
        assertThat(attributes["c"], nullValue())
    }

    @Test
    fun testHStoreIsNull() {
        database.update(Metadatas) {
            set(it.attributes, null)
            where { it.id eq 1 }
        }

        val attributes = get { it.attributes }
        assertThat(attributes, nullValue())
    }

    @Test
    fun testHStoreGetValue() {
        assert(get { it.attributes["a"] } == "1")
        assert(get { it.attributes["b"] } == "2")
        assert(get { it.attributes["c"] } == null)
    }

    private inline fun <T : Any> get(op: (Metadatas) -> ColumnDeclaring<T>): T? {
        return database.sequenceOf(Metadatas).mapColumns { op(it) }.first()
    }

    @Test
    fun testHStoreGetValues() {
        val arrayOfAC: TextArray = arrayOf("a", "c")
        assertThat(get { it.attributes[arrayOfAC] }, equalTo(arrayOf("1", null)))

        val arrayOfBD: TextArray = arrayOf("b", "d")
        assertThat(get { it.attributes[arrayOfBD] }, equalTo(arrayOf("2", null)))
    }

    @Test
    fun testHStoreConcat() {
        database.update(Metadatas) {
            set(it.attributes, it.attributes + mapOf("d" to "4", "e" to null))
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        assertThat(updatedAttributes.size, equalTo(5))
        assertThat(updatedAttributes["a"], equalTo("1"))
        assertThat(updatedAttributes["b"], equalTo("2"))
        assertThat(updatedAttributes["c"], nullValue())
        assertThat(updatedAttributes["d"], equalTo("4"))
        assertThat(updatedAttributes["e"], nullValue())
    }

    @Test
    fun testHStoreContainsKey() {
        assert(get { it.attributes.containsKey("a") } == true)
        assert(get { it.attributes.containsKey("d") } == false)
    }

    @Test
    fun testHStoreContainsAll() {
        val arrayOfAC: TextArray = arrayOf("a", "c")
        assert(get { it.attributes.containsAll(arrayOfAC) } == true)

        val arrayOfBD: TextArray = arrayOf("b", "d")
        assert(get { it.attributes.containsAll(arrayOfBD) } == false)
    }

    @Test
    fun testHStoreContainsAny() {
        val arrayOfAC: TextArray = arrayOf("a", "c")
        assert(get { it.attributes.containsAny(arrayOfAC) } == true)

        val arrayOfBD: TextArray = arrayOf("b", "d")
        assert(get { it.attributes.containsAny(arrayOfBD) } == true)

        val arrayOfEF: TextArray = arrayOf("e", "f")
        assert(get { it.attributes.containsAny(arrayOfEF) } == false)
    }

    @Test
    fun testHStoreContains() {
        assert(get { it.attributes.contains(mapOf("a" to "1")) } == true)
        assert(get { it.attributes.contains(mapOf("a" to "1", "c" to null)) } == true)
        assert(get { it.attributes.contains(mapOf("a" to "1", "c" to "3")) } == false)
        assert(get { it.attributes.contains(mapOf("a" to "1", "d" to "4")) } == false)
    }

    @Test
    fun testHStoreContainedIn() {
        assert(get { it.attributes.containedIn(mapOf("a" to "1", "b" to "2", "c" to null)) } == true)
        assert(get { it.attributes.containedIn(mapOf("a" to "1", "b" to "2", "c" to null, "d" to "4")) } == true)
        assert(get { it.attributes.containedIn(mapOf("a" to "1")) } == false)
        assert(get { it.attributes.containedIn(mapOf("a" to "1", "b" to "2", "c" to "c")) } == false)
    }

    @Test
    fun testHStoreDeleteKey() {
        database.update(Metadatas) {
            set(it.attributes, it.attributes - "b")
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        assertThat(updatedAttributes, equalTo(mapOf("a" to "1", "c" to null)))
    }

    @Test
    fun testHStoreDeleteKeys() {
        database.update(Metadatas) {
            set(it.attributes, it.attributes - arrayOf("b", "c"))
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        assertThat(updatedAttributes, equalTo(mapOf<String, String?>("a" to "1")))
    }

    @Test
    fun testHStoreDeleteMatching() {
        database.update(Metadatas) {
            set(it.attributes, it.attributes - mapOf("a" to "1", "b" to "2", "c" to null))
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        assertThat(updatedAttributes, equalTo(emptyMap()))
    }

    @Test
    fun testTextArray() {
        database.update(Metadatas) {
            set(it.numbers, arrayOf("a", "b"))
            where { it.id eq 1 }
        }

        val numbers = get { it.numbers } ?: error("Cannot get the numbers!")
        assertThat(numbers, equalTo(arrayOf<String?>("a", "b")))
    }

    @Test
    fun testTextArrayIsNull() {
        database.update(Metadatas) {
            set(it.numbers, null)
            where { it.id eq 1 }
        }

        val numbers = get { it.numbers }
        assertThat(numbers, nullValue())
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
        val t = object : Table<Department>("t_department", catalog = postgres.databaseName, schema = "public") {
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

    @Test
    fun testJsonWithDefaultConnection() {
        testJson(database)
    }

    @Test
    fun testJsonWithHikariCP() {
        val ds = HikariDataSource()
        ds.jdbcUrl = postgres.jdbcUrl
        ds.driverClassName = postgres.driverClassName
        ds.username = postgres.username
        ds.password = postgres.password

        val database = Database.connect(ds, logger = ConsoleLogger(threshold = LogLevel.TRACE))
        testJson(database)
    }

    @Test
    fun testJsonWithC3P0() {
        val ds = ComboPooledDataSource()
        ds.jdbcUrl = postgres.jdbcUrl
        ds.driverClass = postgres.driverClassName
        ds.user = postgres.username
        ds.password = postgres.password

        val database = Database.connect(ds, logger = ConsoleLogger(threshold = LogLevel.TRACE))
        testJson(database)
    }

    @Test
    fun testJsonWithDBCP() {
        val ds = BasicDataSource()
        ds.url = postgres.jdbcUrl
        ds.driverClassName = postgres.driverClassName
        ds.username = postgres.username
        ds.password = postgres.password

        val database = Database.connect(ds, logger = ConsoleLogger(threshold = LogLevel.TRACE))
        testJson(database)
    }

    @Test
    fun testJsonWithDruid() {
        val ds = DruidDataSource()
        ds.url = postgres.jdbcUrl
        ds.driverClassName = postgres.driverClassName
        ds.username = postgres.username
        ds.password = postgres.password

        val database = Database.connect(ds, logger = ConsoleLogger(threshold = LogLevel.TRACE))
        testJson(database)
    }

    private fun testJson(database: Database) {
        val t = object : Table<Nothing>("t_json") {
            val obj = json<Employee>("obj")
            val arr = json<List<Int>>("arr")
        }

        database.insert(t) {
            set(it.obj, Employee { name = "vince"; salary = 100 })
            set(it.arr, listOf(1, 2, 3))
        }

        database.insert(t) {
            set(it.obj, null)
            set(it.arr, null)
        }

        database
            .from(t)
            .select(t.obj, t.arr)
            .forEach { row ->
                println("${row.getString(1)}:${row.getString(2)}")
            }
    }
}