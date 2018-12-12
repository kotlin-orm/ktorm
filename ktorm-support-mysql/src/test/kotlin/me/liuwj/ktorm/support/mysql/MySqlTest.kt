package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.desc
import me.liuwj.ktorm.dsl.limit
import me.liuwj.ktorm.dsl.orderBy
import me.liuwj.ktorm.dsl.select
import org.junit.Test

/**
 * Created by vince on Dec 12, 2018.
 */
class MySqlTest : BaseTest() {

    override fun connect() {
        Database.connect(
            url = "jdbc:mysql://localhost:3306/ktorm",
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
}