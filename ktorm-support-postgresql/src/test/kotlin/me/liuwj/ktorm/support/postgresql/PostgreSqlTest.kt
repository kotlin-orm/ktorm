package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*
import me.liuwj.ktorm.expression.ScalarExpression
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.int
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
    fun testILike() {
        val names = database.sequenceOf(Employees).filter { it.name ilike "VINCE" }.mapColumns { it.name }
        println(names)
        assert(names.size == 1)
        assert(names[0] == "vince")
    }

    @Test
    fun testDropTake() {
        val employees = database.sequenceOf(Employees).drop(1).take(1).toList()
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

        val employee = database.sequenceOf(Employees).find { it.id eq 2 } ?: throw AssertionError()
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

            onDuplicateKey {
                it.salary to it.salary + 900
            }
        }

        assert(database.sequenceOf(Employees).find { it.id eq 1 }!!.salary == 1000L)
        assert(database.sequenceOf(Employees).find { it.id eq 5 }!!.salary == 1000L)
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

        assert(database.sequenceOf(Employees).count() == 5)
    }

    @Test
    fun testReturnInTransactionBlock() {
        insertTransactional()
        assert(database.sequenceOf(Departments).count() == 3)
    }

    private fun insertTransactional(): Int {
        database.useTransaction {
            return database.insert(Departments) {
                it.name to "dept name"
                it.location to LocationWrapper("dept location")
            }
        }
    }

    interface Metadata : Entity<Metadata> {
        companion object : Entity.Factory<Metadata>()

        val id: Int
        var attributes: Hstore
    }

    open class Metadatas(alias: String?) : Table<Metadata>("t_metadata", alias) {
        companion object : Metadatas(null)

        override fun aliased(alias: String) = Metadatas(alias)

        val id by int("id").primaryKey().bindTo { it.id }
        val attributes by hstore("attrs").bindTo { it.attributes }
    }

    @Test
    fun testHstore() {
        val allMetadatas = database.sequenceOf(Metadatas).toList()
        assert(allMetadatas.size == 1)
        val attributes = allMetadatas[0].attributes
        assertThat(attributes.size, equalTo(3))
        assertThat(attributes["a"], equalTo("1"))
        assertThat(attributes["b"], equalTo("2"))
        assertThat(attributes["c"], nullValue())
    }

    @Test
    fun testHstoreGetValue() {
        testHstoreGetValue("a", "1")
        testHstoreGetValue("b", "2")
        testHstoreGetValue("c", null)
    }

    private fun testHstoreGetValue(key: String, expectedValue: String?) = testHstoreOperator({ column, param -> column[param] }, key, expectedValue)

    private fun <ParamT : Any, ResultT : Any> testHstoreOperator(
        operator: (ColumnDeclaring<Hstore>, ParamT) -> ScalarExpression<ResultT>,
        parameter: ParamT,
        expectedValue: ResultT?
    ) {
        val aliasedColumn = operator(Metadatas.attributes, parameter).aliased("label")
        val value = database.from(Metadatas)
            .select(aliasedColumn)
            .map { it[aliasedColumn] }
            .first()
        assertThat(value, equalTo(expectedValue))
    }

    @Test
    fun testHstoreGetValues() {
        val arrayOfAC: TextArray = arrayOf("a", "c")
        testHstoreOperator({ column, param -> column[param] }, arrayOfAC, arrayOf("1", null))
        val arrayOfBD: TextArray = arrayOf("b", "d")
        testHstoreOperator({ column, param -> column[param] }, arrayOfBD, arrayOf("2", null))
    }

    @Test
    fun testHstoreConcat() {
        database.update(Metadatas) {
            Metadatas.attributes to (Metadatas.attributes + mapOf(Pair("d", "4"), Pair("e", null)))
            where { it.id eq 1 }
        }
        val updatedAttributes = database.sequenceOf(Metadatas).find { it.id eq 1 }!!.attributes
        assertThat(updatedAttributes.size, equalTo(5))
        assertThat(updatedAttributes["a"], equalTo("1"))
        assertThat(updatedAttributes["b"], equalTo("2"))
        assertThat(updatedAttributes["c"], nullValue())
        assertThat(updatedAttributes["d"], equalTo("4"))
        assertThat(updatedAttributes["e"], nullValue())
    }

    @Test
    fun testHstoreContainsKey() {
        testHstoreOperator(ColumnDeclaring<Hstore>::containsKey, "a", true)
        testHstoreOperator(ColumnDeclaring<Hstore>::containsKey, "d", false)
    }

    @Test
    fun testHstoreContainsAll() {
        val arrayOfAC: TextArray = arrayOf("a", "c")
        testHstoreOperator(ColumnDeclaring<Hstore>::containsAll, arrayOfAC, true)
        val arrayOfBD: TextArray = arrayOf("b", "d")
        testHstoreOperator(ColumnDeclaring<Hstore>::containsAll, arrayOfBD, false)
    }

    @Test
    fun testHstoreContainsAny() {
        val arrayOfAC: TextArray = arrayOf("a", "c")
        testHstoreOperator(ColumnDeclaring<Hstore>::containsAny, arrayOfAC, true)
        val arrayOfBD: TextArray = arrayOf("b", "d")
        testHstoreOperator(ColumnDeclaring<Hstore>::containsAny, arrayOfBD, true)
        val arrayOfEF: TextArray = arrayOf("e", "f")
        testHstoreOperator(ColumnDeclaring<Hstore>::containsAny, arrayOfEF, false)
    }

    @Test
    fun testHstoreContains() {
        testHstoreOperator(ColumnDeclaring<Hstore>::contains, mapOf(Pair("a", "1")), true)
        testHstoreOperator(ColumnDeclaring<Hstore>::contains, mapOf(Pair("a", "1"), Pair("c", null)), true)
        testHstoreOperator(ColumnDeclaring<Hstore>::contains, mapOf(Pair("a", "1"), Pair("c", "3")), false)
        testHstoreOperator(ColumnDeclaring<Hstore>::contains, mapOf(Pair("a", "1"), Pair("d", "4")), false)
    }

    @Test
    fun testHstoreContainedIn() {
        testHstoreOperator(ColumnDeclaring<Hstore>::containedIn, mapOf(Pair("a", "1"), Pair("b", "2"), Pair("c", null)), true)
        testHstoreOperator(ColumnDeclaring<Hstore>::containedIn, mapOf(Pair("a", "1"), Pair("b", "2"), Pair("c", null), Pair("d", "4")), true)
        testHstoreOperator(ColumnDeclaring<Hstore>::containedIn, mapOf(Pair("a", "1")), false)
        testHstoreOperator(ColumnDeclaring<Hstore>::containedIn, mapOf(Pair("a", "1"), Pair("b", "2"), Pair("c", "3")), false)
    }

    @Test
    fun testHstoreDeleteKey() {
        database.update(Metadatas) {
            Metadatas.attributes to (Metadatas.attributes - "b")
            where { it.id eq 1 }
        }
        val updatedAttributes = database.sequenceOf(Metadatas).find { it.id eq 1 }!!.attributes
        assertThat(updatedAttributes, equalTo(mapOf(Pair("a", "1"), Pair("c", null))))
    }

    @Test
    fun testHstoreDeleteKeys() {
        database.update(Metadatas) {
            val keysToDelete = arrayOf<String?>("b", "c")
            Metadatas.attributes to (Metadatas.attributes - keysToDelete)
            where { it.id eq 1 }
        }
        val updatedAttributes = database.sequenceOf(Metadatas).find { it.id eq 1 }!!.attributes
        assertThat(updatedAttributes, equalTo(mapOf<String, String?>(Pair("a", "1"))))
    }
}