package org.ktorm.global

import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel

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