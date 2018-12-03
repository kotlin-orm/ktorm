package me.liuwj.ktorm

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*
import java.time.LocalDate

interface Department : Entity<Department> {
    val id: Int
    var name: String
    var location: String
}

object Departments : Table<Department>("t_departments") {
    val id by int("id").primaryKey().bindTo(Department::id)
    val name by varchar("name").bindTo(Department::name)
    val location by varchar("location").bindTo(Department::location)
}

interface Employee : Entity<Employee> {
    val id: Int
    var name: String
    var job: String
    var manager: Employee
    var hireDate: LocalDate
    var salary: Long
    var department: Department
}

object Employees : Table<Employee>("t_employee") {
    val id by int("id").primaryKey().bindTo(Employee::id)
    val name by varchar("name").bindTo(Employee::name)
    val job by varchar("job").bindTo(Employee::job)
    val managerId by int("manager_id").bindTo(Employee::manager, Employee::id)
    val hireDate by date("hire_date").bindTo(Employee::hireDate)
    val salary by long("salary").bindTo(Employee::salary)
    val departmentId by int("department_id").references(Departments, onProperty = Employee::department)
}