package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*
import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import me.liuwj.ktorm.schema.BooleanSqlType
import me.liuwj.ktorm.schema.IntSqlType
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.MySQLContainer
import java.time.LocalDate

/**
 * Created by vince on Dec 12, 2018.
 */
class MySqlTest : BaseTest() {

    companion object {
        class KMySqlContainer : MySQLContainer<KMySqlContainer>()

        @ClassRule
        @JvmField
        val mysql = KMySqlContainer()
    }

    override fun init() {
        database = Database.connect(
            url = mysql.jdbcUrl,
            driver = mysql.driverClassName,
            user = mysql.username,
            password = mysql.password,
            logger = ConsoleLogger(threshold = LogLevel.TRACE)
        )

        execSqlScript("init-mysql-data.sql")
    }

    @Test
    fun testLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(0, 2)
        assert(query.totalRecords == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids[0] == 4)
        assert(ids[1] == 3)
    }

    @Test
    fun testBulkInsert() {
        database.bulkInsert(Employees) {
            item {
                it.name to "jerry"
                it.job to "trainee"
                it.managerId to 1
                it.hireDate to LocalDate.now()
                it.salary to 50
                it.departmentId to 1
            }
            item {
                it.name to "linda"
                it.job to "assistant"
                it.managerId to 3
                it.hireDate to LocalDate.now()
                it.salary to 100
                it.departmentId to 2
            }
            onDuplicateKey {
                it.name to it.name
                it.job to values(it.job)
            }
        }

        assert(database.employees.count() == 6)
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

        assert(database.employees.find { it.id eq 1 }!!.salary == 1000L)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1000L)
    }

    @Test
    fun testNaturalJoin() {
        val query = database.from(Employees).naturalJoin(Departments).select()
        assert(query.rowSet.size() == 0)
    }

    @Test
    fun testPagingSql() {
        var query = database
            .from(Employees)
            .leftJoin(Departments, on = Employees.departmentId eq Departments.id)
            .select()
            .orderBy(Departments.id.desc())
            .limit(0, 1)

        assert(query.totalRecords == 4)

        query = database
            .from(Employees)
            .select(Employees.name)
            .orderBy((Employees.id + 1).desc())
            .limit(0, 1)

        assert(query.totalRecords == 4)

        query = database
            .from(Employees)
            .select(Employees.departmentId, avg(Employees.salary))
            .groupBy(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecords == 2)

        query = database
            .from(Employees)
            .selectDistinct(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecords == 2)

        query = database
            .from(Employees)
            .select(max(Employees.salary))
            .limit(0, 1)

        assert(query.totalRecords == 1)

        query = database
            .from(Employees)
            .select(Employees.name)
            .limit(0, 1)

        assert(query.totalRecords == 4)
    }

    @Test
    fun testDrop() {
        val employees = database.employees.drop(1).drop(1).drop(1).toList()
        assert(employees.size == 1)
        assert(employees[0].name == "penny")
    }

    @Test
    fun testTake() {
        val employees = database.employees.take(2).take(1).toList()
        assert(employees.size == 1)
        assert(employees[0].name == "vince")
    }

    @Test
    fun testElementAt() {
        val employee = database.employees.drop(2).elementAt(1)

        assert(employee.name == "penny")
        assert(database.employees.elementAtOrNull(4) == null)
    }

    @Test
    fun testMapColumns3() {
        database.employees
            .filter { it.departmentId eq 1 }
            .mapColumns3 { tupleOf(it.id, it.name, dateDiff(LocalDate.now(), it.hireDate)) }
            .forEach { (id, name, days) ->
                println("$id:$name:$days")
            }
    }

    @Test
    fun testMatchAgainst() {
        val employees = database.employees.filterTo(ArrayList()) {
            match(it.name, it.job).against("vince", SearchModifier.IN_NATURAL_LANGUAGE_MODE)
        }

        employees.forEach { println(it) }
    }

    @Test
    fun testReplace() {
        val names = database.employees.mapColumns { it.name.replace("vince", "VINCE") }
        println(names)
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
    fun testToUpperCase() {
        val name = database.employees
            .filter { it.id eq 1 }
            .mapColumns { it.name.toUpperCase() }
            .first()

        assert(name == "VINCE")
    }

    @Test
    fun testSumIf() {
        val countRich = database.from(Employees)
                .select(
                        sum(`if`((Employees.salary greaterEq 100L).cast(BooleanSqlType),
                                then = ArgumentExpression(1, IntSqlType),
                                otherwise = ArgumentExpression(0, IntSqlType)))
                )
                .map { row -> row.getInt(1) }
        assert(countRich.size == 1)
        assert(countRich.first() == 3)
    }

    @Test
    fun testSum() {
        val countRich = database.from(Employees)
                .select(
                        sum(Employees.salary.greaterEq(100L).toInt())
                )
                .map { row -> row.getInt(1) }
        assert(countRich.size == 1)
        assert(countRich.first() == 3)
    }
}
