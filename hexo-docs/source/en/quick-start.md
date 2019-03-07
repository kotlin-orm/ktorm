---
title: Quick Start
lang: en
related_path: zh-cn/quick-start.html
---

# Quick Start

Ktorm was deployed to maven central and jcenter, so you just need to add a dependency to your `pom.xml` file if you are using maven: 

````xml
<dependency>
    <groupId>me.liuwj.ktorm</groupId>
    <artifactId>ktorm-core</artifactId>
    <version>${ktorm.version}</version>
</dependency>
````

Or Gradle: 

````groovy
compile "me.liuwj.ktorm:ktorm-core:${ktorm.version}"
````

Firstly, create Kotlin objects to describe your table schema: 

````kotlin
object Departments : Table<Nothing>("t_department") {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val location by varchar("location")
}

object Employees : Table<Nothing>("t_employee") {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val job by varchar("job")
    val managerId by int("manager_id")
    val hireDate by date("hire_date")
    val salary by long("salary")
    val departmentId by int("department_id")
}
````

Then, connect to your database and write a simple query: 

````kotlin
fun main() {
    Database.connect("jdbc:mysql://localhost:3306/ktorm", driver = "com.mysql.jdbc.Driver")

    for (row in Employees.select()) {
        println(row[Employees.name])
    }
}
````

Now you can run this program, Ktorm will generate a SQL `select * from t_employee`, selecting all employees in the table and printing their names. You can use for-each loop because the query object returned by the `select` function implements the `Iterable<T>` interface. Any other extension functions on `Iterable<T>` is also available, eg. map/filter/reduce provided by Kotlin standard lib.

## SQL DSL

Let's add some filter conditions to the query: 

````kotlin
val names = Employees
    .select(Employees.name)
    .where { (Employees.departmentId eq 1) and (Employees.name like "%vince%") }
    .map { row -> row[Employees.name] }
println(names)
````

Generated SQL: 

````sql
select t_employee.name as t_employee_name 
from t_employee 
where (t_employee.department_id = ?) and (t_employee.name like ?) 
````

That's the magic of Kotlin, writing a query with Ktorm is easy and natural, the generated SQL is exactly corresponding to the origin Kotlin code. And moreover, it's strong typed, the compiler will check your codes before it runs, and you will be benefit from the IDE's intelligent sense and code completion.

Dynamic query based on conditions: 

```kotlin
val names = Employees
    .select(Employees.name)
    .whereWithConditions {
        if (someCondition) {
            it += Employees.managerId.isNull()
        }
        if (otherCondition) {
            it += Employees.departmentId eq 1
        }
    }
    .map { it.getString(1) }
```

Aggregation: 

```kotlin
val t = Employees
val salaries = t
    .select(t.departmentId, avg(t.salary))
    .groupBy(t.departmentId)
    .having { avg(t.salary) greater 100.0 }
    .associate { it.getInt(1) to it.getDouble(2) }
```

Some other convenient aggregation functions: 

```kotlin
Employees.count { it.departmentId eq 1 }
Employees.sumBy { it.salary }
Employees.maxBy { it.salary }
Employees.minBy { it.salary }
Employees.avgBy { it.salary }
Employees.any { it.salary greater 200L }
Employees.none { it.salary greater 200L }
Employees.all { it.salary lessEq 1000L }
```

Union: 

```kotlin
Employees
    .select(Employees.id)
    .unionAll(
        Departments.select(Departments.id)
    )
    .unionAll(
        Departments.select(Departments.id)
    )
    .orderBy(Employees.id.desc())
```

Joining: 

```kotlin
data class Names(val name: String, val managerName: String?, val departmentName: String)

val emp = Employees.aliased("emp")
val mgr = Employees.aliased("mgr")
val dept = Departments.aliased("dept")

val results = emp
    .leftJoin(dept, on = emp.departmentId eq dept.id)
    .leftJoin(mgr, on = emp.managerId eq mgr.id)
    .select(emp.name, mgr.name, dept.name)
    .orderBy(emp.id.asc())
    .map {
        Names(
            name = it.getString(1),
            managerName = it.getString(2),
            departmentName = it.getString(3)
        )
    }
```

Insert: 

```kotlin
Employees.insert {
    it.name to "jerry"
    it.job to "trainee"
    it.managerId to 1
    it.hireDate to LocalDate.now()
    it.salary to 50
    it.departmentId to 1
}
```

Update: 

```kotlin
Employees.update {
    it.job to "engineer"
    it.managerId to null
    it.salary to 100

    where {
        it.id eq 2
    }
}
```

Delete: 

```kotlin
Employees.delete { it.id eq 4 }
```

Refer to detailed documentations for more usage about SQL DSL.

## Entity API

Entity objects is also supported just like other ORM frameworks do. In Ktorm, we define entities as interfaces extending from `Entity<E>`: 

````kotlin
interface Department : Entity<Department> {
    val id: Int
    var name: String
    var location: String
}

interface Employee : Entity<Employee> {
    val id: Int?
    var name: String
    var job: String
    var manager: Employee?
    var hireDate: LocalDate
    var salary: Long
    var department: Department
}
````

Modify the table objects above, binding database columns to entity properties:  

````kotlin
object Departments : Table<Department>("t_department") {
    val id by int("id").primaryKey().bindTo(Department::id)
    val name by varchar("name").bindTo(Department::name)
    val location by varchar("location").bindTo(Department::location)
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
````

Finding an employee by name: 

```kotlin
val vince = Employees.findOne { it.name eq "vince" }
println(vince)
```

The `findOne` function accepts a lambda expression, generating a select sql with the condition returned by the lambda, auto left joining the referenced table `t_department` . Generated SQL: 

````sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where t_employee.name = ?
````

> Naming Strategy: It's highly recommended to name your entity classes by singular nouns, name table objects by plurals (eg. Employee/Employees, Department/Departments). 

Some other `find*` functions: 

```kotlin
Employees.findAll()
Employees.findById(1)
Employees.findListByIds(listOf(1))
Employees.findMapByIds(listOf(1))
Employees.findList { it.departmentId eq 1 }
Employees.findOne { it.name eq "vince" }
```

Return entity instances by query DSL: 

```kotlin
val employees = Employees
    .joinReferencesAndSelect()
    .whereWithConditions {
        if (someCondition) {
            it += Employees.managerId.isNull()
        }
        if (otherCondition) {
            it += Employees.departmentId eq 1
        }
    }
    .orderBy(Employees.id.asc())
    .limit(0, 10)
    .map { Employees.createEntity(it) }
```

Save entities to database: 

```kotlin
val employee = Employee {
    name = "jerry"
    job = "trainee"
    manager = Employees.findOne { it.name eq "vince" }
    hireDate = LocalDate.now()
    salary = 50
    department = Departments.findOne { it.name eq "tech" } ?: throw AssertionError()
}

Employees.add(employee)
```

Flush property changes in memory to database: 

```kotlin
val employee = Employees.findById(2) ?: throw AssertionError()
employee.job = "engineer"
employee.salary = 100
employee.flushChanges()
```

Delete a entity from database: 

```kotlin
val employee = Employees.findById(2) ?: throw AssertionError()
employee.delete()
```

Refer to detailed documentations for more usage about entity APIs.