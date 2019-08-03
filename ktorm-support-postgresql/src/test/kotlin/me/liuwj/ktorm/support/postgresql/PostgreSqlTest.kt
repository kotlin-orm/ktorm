package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.plus
import me.liuwj.ktorm.dsl.update
import me.liuwj.ktorm.entity.*
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
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
        Database.connect(
            url = postgres.jdbcUrl,
            driver = postgres.driverClassName,
            user = postgres.username,
            password = postgres.password,
            logger = ConsoleLogger(threshold = LogLevel.TRACE)
        )

        execSqlScript("init-postgresql-data.sql")
    }

    @Test
    fun testILike() {
        val names = Employees.asSequence().filter { it.name ilike "VINCE" }.mapColumns { it.name }
        println(names)
        assert(names.size == 1)
        assert(names[0] == "vince")
    }

    @Test
    fun testDropTake() {
        val employees = Employees.asSequence().drop(1).take(1).toList()
        println(employees)
        assert(employees.size == 1)
    }

    @Test
    fun testUpdate() {
        Employees.update {
            it.job to "engineer"
            it.managerId to null
            it.salary to 100

            where {
                it.id eq 2
            }
        }

        val employee = Employees.findById(2) ?: throw AssertionError()
        assert(employee.name == "marry")
        assert(employee.job == "engineer")
        assert(employee.manager == null)
        assert(employee.salary == 100L)
    }

    @Test
    fun testInsertOrUpdate() {
        Employees.insertOrUpdate {
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
        Employees.insertOrUpdate {
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

        assert(Employees.findById(1)!!.salary == 1000L)
        assert(Employees.findById(5)!!.salary == 1000L)
    }
}