<p align="center">
    <a href="https://ktorm.liuwj.me">
        <img src="hexo-docs/source/images/logo-full.png" alt="Ktorm" width="300" />
    </a>
</p>
<p align="center">
    <a href="https://www.travis-ci.org/vincentlauvlwj/Ktorm">
        <img src="https://www.travis-ci.org/vincentlauvlwj/Ktorm.svg?branch=master" alt="Build Status" />
    </a>
    <a href="https://search.maven.org/search?q=g:%22me.liuwj.ktorm%22">
        <img src="https://img.shields.io/maven-central/v/me.liuwj.ktorm/ktorm-core.svg?label=Maven%20Central" alt="Maven Central" />
    </a>
    <a href="LICENSE">
        <img src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000" alt="Apache License 2" />
    </a>
    <a href="https://app.codacy.com/app/vincentlauvlwj/Ktorm?utm_source=github.com&utm_medium=referral&utm_content=vincentlauvlwj/Ktorm&utm_campaign=Badge_Grade_Dashboard">
        <img src="https://api.codacy.com/project/badge/Grade/65d4931bfbe14fe986e1267b572bed53" alt="Codacy Badge" />
    </a>
    <a href="https://www.liuwj.me">
        <img src="https://img.shields.io/badge/author-vince-yellowgreen.svg" alt="Author" />
    </a>
</p>

# Ktorm 是什么？

Ktorm 是一个直接基于纯 JDBC 编写的高效简洁的轻量级 Kotlin ORM 框架，它提供了强类型而且灵活的 SQL DSL 和许多方便的扩展函数，以减少我们操作数据库的重复劳动。当然，所有的 SQL 都是自动生成的。查看更多详细文档，请前往官网：[https://ktorm.liuwj.me](https://ktorm.liuwj.me)。

:cn: 简体中文 | :us: [English](README.md)

# 特性

 - 没有配置文件、没有 xml、轻量级、简洁易用
 - 强类型 SQL DSL，将低级 bug 暴露在编译期
 - 灵活的查询，随心所欲地精确控制所生成的 SQL
 - 易扩展的设计，可以灵活编写扩展，支持更多数据类型和 SQL 函数等
 - 方言支持，MySQL、Oracle、PostgreSQL，你也可以自己编写方言支持，只需要实现 `SqlDialect` 接口即可

# 快速开始

Ktorm 已经发布到 maven 中央仓库和 jcenter，因此，如果你使用 maven 的话，只需要在 `pom.xml` 文件里面添加一个依赖： 

````xml
<dependency>
    <groupId>me.liuwj.ktorm</groupId>
    <artifactId>ktorm-core</artifactId>
    <version>${ktorm.version}</version>
</dependency>
````

或者 gradle： 

````groovy
compile "me.liuwj.ktorm:ktorm-core:${ktorm.version}"
````

首先，创建 Kotlin object，描述你的表结构： 

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

然后，连接到数据库，执行一个简单的查询：

````kotlin
fun main() {
    Database.connect("jdbc:mysql://localhost:3306/ktorm", driver = "com.mysql.jdbc.Driver")

    for (row in Employees.select()) {
        println(row[Employees.name])
    }
}
````

现在，你可以执行这个程序了，Ktorm 会生成一条 SQL `select * from t_employee`，查询表中所有的员工记录，然后打印出他们的名字。 因为 `select` 函数返回的查询对象实现了 `Iterable<T>` 接口，所以你可以在这里使用 for-each 循环语法。当然，任何针对 `Iteralble<T>` 的扩展函数也都可用，比如 Kotlin 标准库提供的 map/filter/reduce 系列函数。

## SQL DSL

让我们在上面的查询里再增加一点筛选条件： 

```kotlin
val names = Employees
    .select(Employees.name)
    .where { (Employees.departmentId eq 1) and (Employees.name like "%vince%") }
    .map { row -> row[Employees.name] }
println(names)
```

生成的 SQL 如下: 

```sql
select t_employee.name as t_employee_name 
from t_employee 
where (t_employee.department_id = ?) and (t_employee.name like ?) 
```

这就是 Kotlin 的魔法，使用 Ktorm 写查询十分地简单和自然，所生成的 SQL 几乎和 Kotlin 代码一一对应。并且，Ktorm 是强类型的，编译器会在你的代码运行之前对它进行检查，IDE 也能对你的代码进行智能提示和自动补全。

基于条件的动态查询：

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

聚合查询：

```kotlin
val t = Employees
val salaries = t
    .select(t.departmentId, avg(t.salary))
    .groupBy(t.departmentId)
    .having { avg(t.salary) greater 100.0 }
    .associate { it.getInt(1) to it.getDouble(2) }
```

一些方便的聚合函数：

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

Union：

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

多表连接查询：

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

插入：

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

更新：

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

删除：

```kotlin
Employees.delete { it.id eq 4 }
```

更多 SQL DSL 的用法，参考[具体文档](https://ktorm.liuwj.me)。

## Entity

跟其他 ORM 框架一样，Ktorm 也支持实体对象。在 Ktorm 里面，我们使用接口定义实体类，继承 `Entity<E>` 接口即可：

```kotlin
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
```

修改前面的表对象，把数据库中的列绑定到实体类的属性上：

```kotlin
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
```

根据名字获取 Employee 对象： 

```kotlin
val vince = Employees.findOne { it.name eq "vince" }
println(vince)
```

`findOne` 函数接受一个 lambda 表达式作为参数，使用该 lambda 的返回值作为条件，生成一条查询 SQL，自动 left jion 了关联表 `t_department`。生成的 SQL 如下：

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where t_employee.name = ?
```

> 命名规约：强烈建议使用单数名词命名实体类，使用名词的复数形式命名表对象，如：Employee/Employees、Department/Departments。

其他 `find*` 系列函数：

```kotlin
Employees.findAll()
Employees.findById(1)
Employees.findListByIds(listOf(1))
Employees.findMapByIds(listOf(1))
Employees.findList { it.departmentId eq 1 }
Employees.findOne { it.name eq "vince" }
```

从查询 DSL 中返回实体对象：

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

将实体对象保存到数据库：

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

将内存中实体对象的变化更新到数据库：

```kotlin
val employee = Employees.findById(2) ?: throw AssertionError()
employee.job = "engineer"
employee.salary = 100
employee.flushChanges()
```

从数据库中删除实体对象：

```kotlin
val employee = Employees.findById(2) ?: throw AssertionError()
employee.delete()
```

更多实体 API 的用法，参考[具体文档](https://ktorm.liuwj.me)。

