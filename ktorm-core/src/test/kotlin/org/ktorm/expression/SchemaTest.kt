package org.ktorm.expression

import org.junit.Before
import org.junit.Test
import org.ktorm.database.Database
import org.ktorm.database.SqlDialect
import org.ktorm.dsl.default
import org.ktorm.dsl.unique
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.ktorm.schema.*
import java.io.Serializable
import java.time.LocalDate

/**
 * Created by vince on Dec 07, 2018.
 */
open class SchemaTest {
    lateinit var database: Database

    @Before
    open fun init() {
        database = Database.connect(
            dialect = object : SqlDialect {
                override fun createSqlFormatter(
                    database: Database,
                    beautifySql: Boolean,
                    indentSize: Int
                ): SqlFormatter {
                    return TestFormatter(database, beautifySql, indentSize)
                }
            },
            url = "jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            logger = ConsoleLogger(threshold = LogLevel.TRACE),
            alwaysQuoteIdentifiers = true
        )
    }

    @Test
    fun create(){
        val fmt = TestFormatter(database, true, 2)
        database.executeUpdate(Departments.createTable())
        database.executeUpdate(Employees.createTable())
    }

    class TestFormatter(
        database: Database,
        beautifySql: Boolean,
        indentSize: Int
    ) : SqlSchemaFormatter(database, beautifySql, indentSize) {
        override fun writePagination(expr: QueryExpression) {
            TODO("Not yet implemented")
        }
    }


    data class LocationWrapper(val underlying: String = ""): Serializable

    interface Department : Entity<Department> {
        companion object : Entity.Factory<Department>()
        val id: Int
        var name: String
        var location: LocationWrapper
        var mixedCase: String?
    }

    interface Employee : Entity<Employee> {
        companion object : Entity.Factory<Employee>()
        var id: Int
        var name: String
        var job: String
        var manager: Employee?
        var hireDate: LocalDate
        var salary: Long
        var department: Department

        val upperName get() = name.toUpperCase()
        fun upperName() = name.toUpperCase()
    }

    interface Customer : Entity<Customer> {
        companion object : Entity.Factory<Customer>()
        var id: Int
        var name: String
        var email: String
        var phoneNumber: String
    }

    open class Departments(alias: String?) : Table<Department>("t_department", alias) {
        companion object : Departments(null)
        override fun aliased(alias: String) = Departments(alias)

        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").bindTo { it.name }
        val location = varchar("location").transform({ LocationWrapper(it) }, { it.underlying }).bindTo { it.location }
        val mixedCase = varchar("mixedCase").bindTo { it.mixedCase }
    }

    open class Employees(alias: String?) : Table<Employee>("t_employee", alias) {
        companion object : Employees(null)
        override fun aliased(alias: String) = Employees(alias)

        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").unique().bindTo { it.name }
        val job = varchar("job").default("Minion").bindTo { it.job }
        val managerId = int("manager_id").bindTo { it.manager?.id }
        val hireDate = date("hire_date").bindTo { it.hireDate }
        val salary = long("salary").bindTo { it.salary }
        val departmentId = int("department_id").references(Departments) { it.department }
        val department = departmentId.referenceTable as Departments
    }

    open class Customers(alias: String?) : Table<Customer>("t_customer", alias, schema = "company") {
        companion object : Customers(null)
        override fun aliased(alias: String) = Customers(alias)

        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").bindTo { it.name }
        val email = varchar("email").bindTo { it.email }
        val phoneNumber = varchar("phone_number").bindTo { it.phoneNumber }
    }

    val Database.departments get() = this.sequenceOf(Departments)

    val Database.employees get() = this.sequenceOf(Employees)

    val Database.customers get() = this.sequenceOf(Customers)
}