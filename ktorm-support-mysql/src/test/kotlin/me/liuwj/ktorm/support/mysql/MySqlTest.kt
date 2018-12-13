package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.useConnection
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.sumBy
import org.junit.Test

/**
 * Created by vince on Dec 12, 2018.
 */
class MySqlTest : BaseTest() {

    override fun connect() {
        Database.connect(
            url = "jdbc:mysql://127.0.0.1:3306/ktorm",
            driver = "com.mysql.jdbc.Driver",
            user = "root",
            dialect = MySqlDialect
        )
    }

    @Test
    fun testLimit() {
        val query = Employees.select().orderBy(Employees.id.desc()).limit(0, 2)
        assert(query.totalRecords == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids[0] == 4)
        assert(ids[1] == 3)
    }

    @Test
    fun testAcceptChanges() {
        val query = Employees.select(Employees.columns)

        for (row in query.rowSet) {
            row.updateLong(Employees.salary.label, 200)
            row.updateRow()
        }

        useConnection { conn ->
            conn.autoCommit = false
            query.rowSet.acceptChanges(conn)
        }

        assert(Employees.sumBy { it.salary } == 800L)
    }
}