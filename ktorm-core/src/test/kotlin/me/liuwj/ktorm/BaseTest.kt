package me.liuwj.ktorm

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.useConnection
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*
import org.junit.After
import org.junit.Before
import java.time.LocalDate

/**
 * Created by vince on Dec 07, 2018.
 */
open class BaseTest {

    open fun connect() {
        Database.connect(url = "jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    }

    @Before
    fun init() {
        connect()
        execSqlScript("init-data.sql")
    }

    @After
    fun destroy() {
        execSqlScript("drop-data.sql")
    }

    private fun execSqlScript(filename: String) {
        useConnection { conn ->
            conn.createStatement().use { statement ->
                javaClass.classLoader
                    .getResourceAsStream(filename)
                    .bufferedReader()
                    .use { reader ->
                        for (sql in reader.readText().split(';')) {
                            if (sql.any { it.isLetterOrDigit() }) {
                                statement.executeUpdate(sql)
                            }
                        }
                    }
            }
        }
    }

    interface Department : Entity<Department> {
        companion object : Entity.Factory<Department>()
        val id: Int
        var name: String
        var location: String
    }

    interface Employee : Entity<Employee> {
        companion object : Entity.Factory<Employee>()
        var id: Int?
        var name: String
        var job: String
        var manager: Employee?
        var hireDate: LocalDate
        var salary: Long
        var department: Department
    }

    open class Departments(alias: String?) : Table<Department>("t_department", alias) {
        companion object : Departments(null)
        override fun aliased(alias: String) = Departments(alias)

        val id by int("id").primaryKey().bindTo { it.id }
        val name by varchar("name").bindTo { it.name }
        val location by varchar("location").bindTo { it.location }
    }

    open class Employees(alias: String?) : Table<Employee>("t_employee", alias) {
        companion object : Employees(null)
        override fun aliased(alias: String) = Employees(alias)

        val id by int("id").primaryKey().bindTo { it.id }
        val name by varchar("name").bindTo { it.name }
        val job by varchar("job").bindTo { it.job }
        val managerId by int("manager_id").bindTo { it.manager?.id }
        val hireDate by date("hire_date").bindTo { it.hireDate }
        val salary by long("salary").bindTo { it.salary }
        val departmentId by int("department_id").references(Departments) { it.department }
    }
}