package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.use
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.int
import me.liuwj.ktorm.schema.varchar
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate

/**
 * Created by vince on Feb 13, 2019.
 */
class PostgreSqlTest : BaseTest() {

    companion object {
        class KPostgreSqlContainer : PostgreSQLContainer<KPostgreSqlContainer>()

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
            it.key to "test"
            it.value to "test value"
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
            it.job to "engineer"
            it.managerId to null
            it.salary to 100

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
            it.id to 1
            it.name to "vince"
            it.job to "engineer"
            it.salary to 1000
            it.hireDate to LocalDate.now()
            it.departmentId to 1

            onDuplicateKey {
                it.salary to it.salary + 900
            }
        }
        database.insertOrUpdate(Employees) {
            it.id to 5
            it.name to "vince"
            it.job to "engineer"
            it.salary to 1000
            it.hireDate to LocalDate.now()
            it.departmentId to 1

            onDuplicateKey(it.id) {
                it.salary to it.salary + 900
            }
        }

        assert(database.employees.find { it.id eq 1 }!!.salary == 1000L)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1000L)
    }

    @Test
    fun testInsertAndGenerateKey() {
        val id = database.insertAndGenerateKey(Employees) {
            it.name to "Joe Friend"
            it.job to "Tester"
            it.managerId to null
            it.salary to 50
            it.hireDate to LocalDate.of(2020, 1, 10)
            it.departmentId to 1
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
                it.name to "dept name"
                it.location to LocationWrapper("dept location")
                it.mixedCase to "value for mixed case"
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
            it.attributes to null
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
            it.attributes to (it.attributes + mapOf("d" to "4", "e" to null))
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
            it.attributes to (it.attributes - "b")
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        assertThat(updatedAttributes, equalTo(mapOf("a" to "1", "c" to null)))
    }

    @Test
    fun testHStoreDeleteKeys() {
        database.update(Metadatas) {
            it.attributes to (it.attributes - arrayOf<String?>("b", "c"))
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        assertThat(updatedAttributes, equalTo(mapOf<String, String?>("a" to "1")))
    }

    @Test
    fun testHStoreDeleteMatching() {
        database.update(Metadatas) {
            it.attributes to (it.attributes - mapOf("a" to "1", "b" to "2", "c" to null))
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        assertThat(updatedAttributes, equalTo(emptyMap()))
    }

    @Test
    fun testTextArray() {
        database.update(Metadatas) {
            it.numbers to arrayOf("a", "b")
            where { it.id eq 1 }
        }

        val numbers = get { it.numbers } ?: error("Cannot get the numbers!")
        assertThat(numbers, equalTo(arrayOf<String?>("a", "b")))
    }

    @Test
    fun testTextArrayIsNull() {
        database.update(Metadatas) {
            it.numbers to null
            where { it.id eq 1 }
        }

        val numbers = get { it.numbers }
        assertThat(numbers, nullValue())
    }
}