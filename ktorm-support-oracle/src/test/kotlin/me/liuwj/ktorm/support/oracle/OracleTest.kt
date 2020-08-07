package me.liuwj.ktorm.support.oracle

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.use
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.count
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.varchar
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.OracleContainer

/**
 * Created by vince at Aug 01, 2020.
 */
class OracleTest : BaseTest() {

    companion object {
        @ClassRule
        @JvmField
        val oracle: OracleContainer = OracleContainer("zerda/oracle-database:11.2.0.2-xe")
            .withCreateContainerCmdModifier { cmd -> cmd.hostConfig?.withShmSize(1 * 1024 * 1024 * 1024) }
    }

    override fun init() {
        database = Database.connect(
            url = oracle.jdbcUrl,
            driver = oracle.driverClassName,
            user = oracle.username,
            password = oracle.password,
            logger = ConsoleLogger(threshold = LogLevel.TRACE),
            alwaysQuoteIdentifiers = true
        )

        execSqlScript("init-oracle-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-oracle-data.sql")
    }

    @Test
    fun testKeywordWrapping() {
        val configs = object : Table<Nothing>("T_CONFIG") {
            val key = varchar("KEY").primaryKey()
            val value = varchar("VALUE")
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """CREATE TABLE T_CONFIG("KEY" VARCHAR(128) PRIMARY KEY, "VALUE" VARCHAR(128))"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(configs) {
            set(it.key, "test")
            set(it.value, "test value")
        }

        assert(database.sequenceOf(configs).count { it.key eq "test" } == 1)

        database.delete(configs) { it.key eq "test" }
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
    fun testSequence() {
        for (employee in database.employees) {
            println(employee)
        }
    }
}