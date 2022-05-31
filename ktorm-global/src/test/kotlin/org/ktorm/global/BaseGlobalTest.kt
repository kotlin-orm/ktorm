package org.ktorm.global

import org.ktorm.BaseTest
import org.ktorm.database.Database

/**
 * Created by vince at Apr 05, 2020.
 */
@Suppress("DEPRECATION")
open class BaseGlobalTest : BaseTest() {

    override fun init() {
        database = Database.connectGlobally("jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1", alwaysQuoteIdentifiers = true)
        execSqlScript("init-data.sql")
    }
}