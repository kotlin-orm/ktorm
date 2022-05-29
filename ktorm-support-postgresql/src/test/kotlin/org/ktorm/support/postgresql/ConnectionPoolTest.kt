package org.ktorm.support.postgresql

import com.alibaba.druid.pool.DruidDataSource
import com.mchange.v2.c3p0.ComboPooledDataSource
import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.dbcp2.BasicDataSource
import org.junit.Test
import org.ktorm.database.Database
import org.ktorm.dsl.forEach
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.select
import org.ktorm.jackson.json
import org.ktorm.schema.Table

@Suppress("ConvertTryFinallyToUseCall")
class ConnectionPoolTest : BasePostgreSqlTest() {

    @Test
    fun testJsonWithDefaultConnection() {
        testJson(database)
    }

    @Test
    fun testJsonWithHikariCP() {
        val ds = HikariDataSource()
        ds.jdbcUrl = jdbcUrl
        ds.driverClassName = driverClassName
        ds.username = username
        ds.password = password

        try {
            testJson(Database.connect(ds))
        } finally {
            ds.close()
        }
    }

    @Test
    fun testJsonWithC3P0() {
        val ds = ComboPooledDataSource()
        ds.jdbcUrl = jdbcUrl
        ds.driverClass = driverClassName
        ds.user = username
        ds.password = password

        try {
            testJson(Database.connect(ds))
        } finally {
            ds.close()
        }
    }

    @Test
    fun testJsonWithDBCP() {
        val ds = BasicDataSource()
        ds.url = jdbcUrl
        ds.driverClassName = driverClassName
        ds.username = username
        ds.password = password

        try {
            testJson(Database.connect(ds))
        } finally {
            ds.close()
        }
    }

    @Test
    fun testJsonWithDruid() {
        val ds = DruidDataSource()
        ds.url = jdbcUrl
        ds.driverClassName = driverClassName
        ds.username = username
        ds.password = password

        try {
            testJson(Database.connect(ds))
        } finally {
            ds.close()
        }
    }

    private fun testJson(database: Database) {
        val t = object : Table<Nothing>("t_json") {
            val obj = json<Employee>("obj")
            val arr = json<List<Int>>("arr")
        }

        database.insert(t) {
            set(it.obj, Employee { name = "vince"; salary = 100 })
            set(it.arr, listOf(1, 2, 3))
        }

        database.insert(t) {
            set(it.obj, null)
            set(it.arr, null)
        }

        database
            .from(t)
            .select(t.obj, t.arr)
            .forEach { row ->
                println("${row.getString(1)}:${row.getString(2)}")
            }
    }
}