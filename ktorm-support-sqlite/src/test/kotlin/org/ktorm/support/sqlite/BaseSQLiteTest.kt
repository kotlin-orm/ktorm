package org.ktorm.support.sqlite

import org.ktorm.BaseTest
import org.ktorm.database.Database
import java.sql.Connection
import java.sql.DriverManager

abstract class BaseSQLiteTest : BaseTest() {
    lateinit var connection: Connection

    override fun init() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")

        database = Database.connect {
            object : Connection by connection {
                override fun close() {
                    // do nothing...
                }
            }
        }

        execSqlScript("init-sqlite-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-sqlite-data.sql")
        connection.close()
    }
}