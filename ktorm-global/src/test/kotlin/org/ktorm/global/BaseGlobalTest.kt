package me.liuwj.ktorm.global

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel

/**
 * Created by vince at Apr 05, 2020.
 */
open class BaseGlobalTest : BaseTest() {

    override fun init() {
        database = Database.connectGlobally(
            url = "jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            logger = ConsoleLogger(threshold = LogLevel.TRACE),
            alwaysQuoteIdentifiers = true
        )

        execSqlScript("init-data.sql")
    }
}