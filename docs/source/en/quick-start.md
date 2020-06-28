---
title: Quick Start
lang: en
related_path: zh-cn/quick-start.html
---

# Quick Start

Ktorm was deployed to maven central and jcenter, so you just need to add a dependency to your `pom.xml` file if you are using maven: 

```xml
<dependency>
    <groupId>me.liuwj.ktorm</groupId>
    <artifactId>ktorm-core</artifactId>
    <version>${ktorm.version}</version>
</dependency>
```

Or Gradle: 

```groovy
compile "me.liuwj.ktorm:ktorm-core:${ktorm.version}"
```

Firstly, create Kotlin objects to [describe your table schemas](./schema-definition.html): 

```kotlin
object Departments : Table<Nothing>("t_department") {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val location = varchar("location")
}

object Employees : Table<Nothing>("t_employee") {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val job = varchar("job")
    val managerId = int("manager_id")
    val hireDate = date("hire_date")
    val salary = long("salary")
    val departmentId = int("department_id")
}
```

Then, connect to your database and write a simple query: 

```kotlin
fun main() {
    val database = Database.connect("jdbc:mysql://localhost:3306/ktorm?user=root&password=***")

    for (row in database.from(Employees).select()) {
        println(row[Employees.name])
    }
}
```

Now you can run this program, Ktorm will generate a SQL `select * from t_employee`, selecting all employees in the table and printing their names. You can use the for-each loop here because the query object returned by the `select` function overloads the iteration operator. 

## SQL DSL

Let's add some filter conditions to the query: 

```kotlin
database
    .from(Employees)
    .select(Employees.name)
    .where { (Employees.departmentId eq 1) and (Employees.name like "%vince%") }
    .forEach { row -> 
        println(row[Employees.name])
    }
```

Generated SQL: 

```sql
select t_employee.name as t_employee_name 
from t_employee 
where (t_employee.department_id = ?) and (t_employee.name like ?) 
```

That's the magic of Kotlin, writing a query with Ktorm is easy and natural, the generated SQL is exactly corresponding to the origin Kotlin code. And moreover, it's strong-typed, the compiler will check your code before it runs, and you will be benefited from the IDE's intelligent sense and code completion.

Dynamic query that will apply different filter conditions in different situations: 

```kotlin
val query = database
    .from(Employees)
    .select(Employees.name)
    .whereWithConditions {
        if (someCondition) {
            it += Employees.managerId.isNull()
        }
        if (otherCondition) {
            it += Employees.departmentId eq 1
        }
    }
```

Aggregation: 

```kotlin
val t = Employees.aliased("t")
database
    .from(t)
    .select(t.departmentId, avg(t.salary))
    .groupBy(t.departmentId)
    .having { avg(t.salary) greater 100.0 }
    .forEach { row -> 
        println("${row.getInt(1)}:${row.getDouble(2)}")
    }
```

Union: 

```kotlin
val query = database
    .from(Employees)
    .select(Employees.id)
    .unionAll(
        database.from(Departments).select(Departments.id)
    )
    .unionAll(
        database.from(Departments).select(Departments.id)
    )
    .orderBy(Employees.id.desc())
```

Joining: 

```kotlin
data class Names(val name: String?, val managerName: String?, val departmentName: String?)

val emp = Employees.aliased("emp")
val mgr = Employees.aliased("mgr")
val dept = Departments.aliased("dept")

val results = database
    .from(emp)
    .leftJoin(dept, on = emp.departmentId eq dept.id)
    .leftJoin(mgr, on = emp.managerId eq mgr.id)
    .select(emp.name, mgr.name, dept.name)
    .orderBy(emp.id.asc())
    .map { row -> 
        Names(
            name = row[emp.name],
            managerName = row[mgr.name],
            departmentName = row[dept.name]
        )
    }
```

Insert: 

```kotlin
database.insert(Employees) {
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
database.update(Employees) {
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
database.delete(Employees) { it.id eq 4 }
```

Refer to [detailed documentation](./query.html) for more usages about SQL DSL.

## Entities & Column Binding

In addition to SQL DSL, entity objects are also supported just like other ORM frameworks do. We need to define entity classes firstly and bind table objects to them. In Ktorm, entity classes are defined as interfaces extending from `Entity<E>`: 

```kotlin
interface Department : Entity<Department> {
    companion object : Entity.Factory<Department>()
    val id: Int
    var name: String
    var location: String
}

interface Employee : Entity<Employee> {
    companion object : Entity.Factory<Employee>()
    val id: Int
    var name: String
    var job: String
    var manager: Employee?
    var hireDate: LocalDate
    var salary: Long
    var department: Department
}
```

Modify the table objects above, binding database columns to entity properties:  

```kotlin
object Departments : Table<Department>("t_department") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val location = varchar("location").bindTo { it.location }
}

object Employees : Table<Employee>("t_employee") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val job = varchar("job").bindTo { it.job }
    val managerId = int("manager_id").bindTo { it.manager.id }
    val hireDate = date("hire_date").bindTo { it.hireDate }
    val salary = long("salary").bindTo { it.salary }
    val departmentId = int("department_id").references(Departments) { it.department }
}
```

> Naming Strategy: It's highly recommended to name your entity classes by singular nouns, name table objects by plurals (eg. Employee/Employees, Department/Departments). 

Now that column bindings are configured, so we can use [sequence APIs](#Entity-Sequence-APIs) to perform many operations on entities. Just like the following code, firstly we create a sequence object via `sequenceOf`, then we call the `find` function to obtain an employee by its name: 

```kotlin
val sequence = database.sequenceOf(Employees)
val employee = sequence.find { it.name eq "vince" }
```

We can also filter the sequence by the function `filter`. For example, obtaining all the employees whose names are vince: 

```kotlin
val employees = sequence.filter { it.name eq "vince" }.toList()
```

The `find` and `filter` functions both accept a lambda expression, generating a select sql with the condition returned by the lambda. The generated SQL auto left joins the referenced table `t_department`: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where t_employee.name = ?
```

Save entities to database: 

```kotlin
val employee = Employee {
    name = "jerry"
    job = "trainee"
    hireDate = LocalDate.now()
    salary = 50
    department = database.sequenceOf(Departments).find { it.name eq "tech" }
}

sequence.add(employee)
```

Flush property changes in memory to database: 

```kotlin
val employee = sequence.find { it.id eq 2 } ?: return
employee.job = "engineer"
employee.salary = 100
employee.flushChanges()
```

Delete a entity from database: 

```kotlin
val employee = sequence.find { it.id eq 2 } ?: return
employee.delete()
```

Detailed usages of entity APIs can be found in the documentation of [column binding](./entities-and-column-binding.html) and [entity query](./entity-finding.html).

## Entity Sequence APIs

Ktorm provides a set of APIs named *Entity Sequence*, which can be used to obtain entity objects from databases. As the name implies, its style and use pattern are highly similar to the sequence APIs in Kotlin standard lib, as it provides many extension functions with the same names, such as `filter`, `map`, `reduce`, etc.

Most of the entity sequence APIs are provided as extension functions, which can be divided into two groups, they are intermediate operations and terminal operations. 

### Intermediate Operations

These functions don’t execute the internal queries but return new-created sequence objects applying some modifications. For example, the `filter` function creates a new sequence object with the filter condition given by its parameter. The following code obtains all the employees in department 1 by using `filter`:

```kotlin
val employees = database.sequenceOf(Employees).filter { it.departmentId eq 1 }.toList()
```

We can see that the usage is almost the same as `kotlin.sequences`, the only difference is the `==` in the lambda is replaced by the `eq` function. The `filter` function can also be called continuously, as all the filter conditions are combined with the `and` operator. 

```kotlin
val employees = database
    .sequenceOf(Employees)
    .filter { it.departmentId eq 1 }
    .filter { it.managerId.isNotNull() }
    .toList()
```

Generated SQL: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where (t_employee.department_id = ?) and (t_employee.manager_id is not null)
```

Use `sortedBy` or `soretdByDescending` to sort entities in a sequence: 

```kotlin
val employees = database.sequenceOf(Employees).sortedBy { it.salary }.toList()
```

Use `drop` and `take` for pagination: 

```kotlin
val employees = database.sequenceOf(Employees).drop(1).take(1).toList()
```

### Terminal Operations

Terminal operations of entity sequences execute the queries right now, then obtain the query results and perform some calculations on them. The for-each loop is a typical terminal operation, and the following code uses it to print all employees in the sequence: 

```kotlin
for (employee in database.sequenceOf(Employees)) {
    println(employee)
}
```

Generated SQL: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id
```

The `toCollection` functions (including `toList`, `toSet`, etc.) are used to collect all the elements into a collection: 

```kotlin
val employees = database.sequenceOf(Employees).toCollection(ArrayList())
```

The `mapColumns` function is used to obtain the results of a column: 

```kotlin
val names = database.sequenceOf(Employees).mapColumns { it.name }
```

Additionally, if we want to select two or more columns, we can change to `mapColumns2` or `mapColumns3`, then we need to wrap our selected columns by `Pair` or `Triple` in the closure, and the function’s return type becomes `List<Pair<C1?, C2?>>` or `List<Triple<C1?, C2?, C3?>>`. 

```kotlin
database
    .sequenceOf(Employees)
    .filter { it.departmentId eq 1 }
    .mapColumns2 { Pair(it.id, it.name) }
    .forEach { (id, name) ->
        println("$id:$name")
    }
```

Generated SQL: 

```sql
select t_employee.id, t_employee.name
from t_employee 
where t_employee.department_id = ?
```

Other familiar functions are also supported, such as `fold`, `reduce`, `forEach`, etc. The following code calculates the total salary of all employees: 

```kotlin
val totalSalary = database.sequenceOf(Employees).fold(0L) { acc, employee -> acc + employee.salary }
```

### Sequence Aggregation

The entity sequence APIs not only allow us to obtain entities from databases just like using `kotlin.sequences`, but they also provide rich support for aggregations, so we can conveniently count the columns, sum them, or calculate their averages, etc.

The following code obtains the max salary in department 1:

```kotlin
val max = database
    .sequenceOf(Employees)
    .filter { it.departmentId eq 1 }
    .aggregateColumns { max(it.salary) }
```

Also, if we want to aggregate two or more columns, we can change to `aggregateColumns2` or `aggregateColumns3`, then we need to wrap our aggregate expressions by `Pair` or `Triple` in the closure, and the function’s return type becomes `Pair<C1?, C2?>` or `Triple<C1?, C2?, C3?>`. The example below obtains the average and the range of salaries in department 1: 

```kotlin
val (avg, diff) = database
    .sequenceOf(Employees)
    .filter { it.departmentId eq 1 }
    .aggregateColumns2 { Pair(avg(it.salary), max(it.salary) - min(it.salary)) }
```

Generated SQL: 

```sql
select avg(t_employee.salary), max(t_employee.salary) - min(t_employee.salary) 
from t_employee 
where t_employee.department_id = ?
```

Ktorm also provides many convenient helper functions implemented based on `aggregateColumns`, they are `count`, `any`, `none`, `all`, `sumBy`, `maxBy`, `minBy`, `averageBy`. 

The following code obtains the max salary in department 1 using `maxBy` instead: 

```kotlin
val max = database
    .sequenceOf(Employees)
    .filter { it.departmentId eq 1 }
    .maxBy { it.salary }
```

Additionally, grouping aggregations are also supported, we just need to call `groupingBy` before calling `aggregateColumns`. The following code obtains the average salaries for each department. Here, the result's type is `Map<Int?, Double?>`, in which the keys are departments' IDs, and the values are the average salaries of the departments. 

```kotlin
val averageSalaries = database
    .sequenceOf(Employees)
    .groupingBy { it.departmentId }
    .aggregateColumns { avg(it.salary) }
```

Generated SQL: 

```sql
select t_employee.department_id, avg(t_employee.salary) 
from t_employee 
group by t_employee.department_id
```

Ktorm also provides many convenient helper functions for grouping aggregations, they are `eachCount(To)`, `eachSumBy(To)`, `eachMaxBy(To)`, `eachMinBy(To)`, `eachAverageBy(To)`. With these functions, we can write the code below to obtain average salaries for each department: 

```kotlin
val averageSalaries = database
    .sequenceOf(Employees)
    .groupingBy { it.departmentId }
    .eachAverageBy { it.salary }
```

Other familiar functions are also supported, such as `aggregate`, `fold`, `reduce`, etc. They have the same names as the extension functions of `kotlin.collections.Grouping`, and the usages are totally the same. The following code calculates the total salaries for each department using `fold`:

```kotlin
val totalSalaries = database
    .sequenceOf(Employees)
    .groupingBy { it.departmentId }
    .fold(0L) { acc, employee -> 
        acc + employee.salary 
    }
```

Detailed usages of entity sequence APIs can be found in the documentation of [entity sequence](./entity-sequence.html) and [sequence aggregation](./sequence-aggregation.html). 