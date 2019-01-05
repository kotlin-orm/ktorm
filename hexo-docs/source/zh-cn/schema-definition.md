---
title: 定义表结构
lang: zh-cn
related_path: en/schema-definition.html
---

# 定义表结构

在使用 SQL DSL 之前，我们首先要让 Ktorm 能够了解我们的表结构。假设我们有两个表，他们分别是部门表 `t_department` 和员工表 `t_employee`， 它们的建表 SQL 如下，我们要如何描述这两个表呢？

```sql
create table t_department(
  id int not null primary key auto_increment,
  name varchar(128) not null,
  location varchar(128) not null
);

create table t_employee(
  id int not null primary key auto_increment,
  name varchar(128) not null,
  job varchar(128) not null,
  manager_id int null,
  hire_date date not null,
  salary bigint not null,
  department_id int not null
);
```

## 表对象

一般来说，Ktorm 使用 Kotlin 中的 object 关键字定义一个继承 `Table` 类的对象来描述表结构，上面例子中的两个表可以像这样在 Ktorm 中定义：

```kotlin
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
```

可以看到，`Departments` 和 `Employees` 都继承了 `Table`，并且在构造函数中指定了表名，`Table` 类还有一个泛型参数，它是此表绑定到的实体类的类型，在这里我们不需要绑定到任何实体类，因此指定为 `Nothing` 即可。

表中的列则使用 val 和 by 关键字定义为表对象中的成员属性，列的类型使用 int、long、varchar、date 等函数定义，它们分别对应了 SQL 中的相应类型，这些类型定义函数的普遍特征如下：

- 它们是 `Table` 类的扩展函数，只能在定义表对象时使用
- 它们的名称一般对应于其实际的 SQL 类型的名称
- 它们都接收一个字符串的参数，在这里我们需要把列的名称传入
- 它们的返回值都是 `Table<E>.ColumnRegistration<C>`，E 为实体类的类型，C 为该列的类型，我们可以链式调用 `ConlumnRegistration` 上的 `primaryKey` 函数，将当前列声明为主键

> `ColumnRegistration` 实现了 `ReadOnlyProperty` 接口，所以可以结合 by 关键字用作属性委托。因此，在 `val name by varchar("name")` 中，虽然 `varchar` 函数的返回值类型为 `ColumnRegistration<String>`，但 `val name` 的类型却是 `Column<String>`。以此类推， `val managerId by int("manager_id")` 定义的属性的类型应该是 `Column<Int>`。

通常我们都会将表定义为 Kotlin 单例对象，但我们其实不必拘泥于此。例如，在某些情况下，我们有两个结构完全相同的表，只是表名不同（在数据备份的时候比较常见），难道我们一定要在每一个表对象中都写一遍完全相同的字段定义吗？当然不需要，这里我们可以使用继承重用代码：

```kotlin
sealed class Employees(tableName: String) : Table<Nothing>(tableName) {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val job by varchar("job")
    val managerId by int("manager_id")
    val hireDate by date("hire_date")
    val salary by long("salary")
    val departmentId by int("department_id")
}

object RegularEmployees : Employees("t_regular_employee")

object FormerEmployees : Employees("t_former_employee")
```

再比如，有时我们的某个表只需要使用一次，因此没有必要将其定义为全局对象，以免污染命名空间。这时，我们甚至可以在函数内部使用匿名对象定义一个表：

```kotlin
val t = object : Table<Nothing>("t_config") {
    val key by varchar("key").primaryKey()
    val value by varchar("value")
}

// Get all configs as a Map<String, String>
val configs = t.select().associate { row -> row[t.key] to row[t.value] }
```

灵活使用 Kotlin 的语法特性可以帮助我们减少重复代码、提高项目的可维护性。