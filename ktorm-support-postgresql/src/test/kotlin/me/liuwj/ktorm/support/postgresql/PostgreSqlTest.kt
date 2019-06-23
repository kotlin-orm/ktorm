package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.select
import me.liuwj.ktorm.dsl.where
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import org.junit.Test

/**
 * Created by vince on Feb 13, 2019.
 */
class PostgreSqlTest : BaseTest() {

    override fun connect() {
        Database.connect(
            url = "jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            logger = ConsoleLogger(threshold = LogLevel.TRACE)
        )
    }

    @Test
    fun testILike() {
        val query = Employees.select().where { Employees.name ilike "vince" }
        println(query.sql)
    }
}