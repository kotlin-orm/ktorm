package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.useConnection
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.findById
import org.junit.Test
import java.time.LocalDate

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

    @Test
    fun testBulkInsert() {
        Employees.bulkInsert {
            item {
                it.name to "jerry"
                it.job to "trainee"
                it.managerId to 1
                it.hireDate to LocalDate.now()
                it.salary to 50
                it.departmentId to 1
            }
            item {
                it.name to "linda"
                it.job to "assistant"
                it.managerId to 3
                it.hireDate to LocalDate.now()
                it.salary to 100
                it.departmentId to 2
            }
        }

        assert(Employees.count() == 6)
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