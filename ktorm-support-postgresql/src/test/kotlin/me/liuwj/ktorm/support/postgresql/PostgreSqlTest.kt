package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.select
import me.liuwj.ktorm.dsl.where
import me.liuwj.ktorm.entity.asSequence
import me.liuwj.ktorm.entity.drop
import me.liuwj.ktorm.entity.take
import me.liuwj.ktorm.entity.toList
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import org.junit.Test

/**
 * Created by vince on Feb 13, 2019.
 */
class PostgreSqlTest : BaseTest() {

    override fun connect() {
        Database.connect(
            url = "jdbc:postgresql://127.0.0.1:5432/ktorm",
            driver = "org.postgresql.Driver",
            user = "postgres",
            logger = ConsoleLogger(threshold = LogLevel.TRACE)
        )
    }

    override fun init() {
        connect()
        execSqlScript("init-postgresql-data.sql")
    }

    @Test
    fun testILike() {
        val names = Employees.select().where { Employees.name ilike "VINCE" }.map { it[Employees.name] }
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
}