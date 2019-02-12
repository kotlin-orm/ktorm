---
title: 联表
lang: zh-cn
related_path: en/joining.html
---

# 联表

在上一节中，我们介绍了查询的 SQL DSL，这足以应付许多的场景。不过前面的查询都只限于单表，在大部分情况下，我们的业务都需要多个表来完成。连接查询的支持，对于一个 ORM 框架而言必不可少。

## 联表扩展函数

Ktorm 使用扩展函数对连接查询提供支持，内置的标准连接类型有四种：

| 连接类型 | 扩展函数名 | 对应的 SQL 关键字 |
| -------- | ---------- | ----------------- |
| 内连接   | innerJoin  | inner join        |
| 左连接   | leftJoin   | left join         |
| 右连接   | rightJoin  | right join        |
| 交叉连接 | crossJoin  | cross join        |

以上函数都是 `Table` 和 `JoinExpression` 的扩展函数，最简单的使用方式如下：

````kotlin
val joining = Employees.crossJoin(Departments)
````

上面这行代码把员工表和部门表进行交叉连接，`crossJoin` 函数的返回值是一个 `JoinExpression`。然而，大部分时候，我们持有一个 `JoinExpression` 并没有任何用处，我们需要将它变成一个 `Query` 对象，以便进行多表查询，并取得查询的结果。

在上一节中，我们使用 `select` 函数从一个表对象中创建一个查询，这里的 `select` 是 `Table` 类的扩展函数。其实，`select` 函数也对 `JoinExpression` 提供了一个重载，所以我们可以使用类似的用法创建一个 `Query` 对象。

````kotlin
val query = Employees.crossJoin(Departments).select()
````

上面的查询把员工表和部门表进行交叉连接，并返回所有记录（笛卡尔积），生成的 SQL 如下：

````sql
select * 
from t_employee 
cross join t_department 
````

上面的查询比较简单，在实际使用中，如此简单的联表查询通常都用处有限。接下来是一个比较实际的例子，这个查询获取所有薪水大于 100 的员工的名字和他所属的部门的名字。在这里，我们指定了 `leftJoin` 函数的第二个参数，它就是连接条件，至于 `select` 和 `where` 函数的用法，都已经在上一节中有详细介绍。

```kotlin
val query = Employees
    .leftJoin(Departments, on = Employees.departmentId eq Departments.id)
    .select(Employees.name, Departments.name)
    .where { Employees.salary greater 100L }
```

生成的 SQL 如下：

````sql
select t_employee.name as t_employee_name, t_department.name as t_department_name 
from t_employee 
left join t_department on t_employee.department_id = t_department.id 
where t_employee.salary > ? 
````

## 自连接查询与表别名

自连接是连接查询的一种特殊用法，它支持把一个表与它自身进行连接，比如下面这句 SQL 就使用了自连接，它查询每个员工的名字、他直接负责的管理者的名字以及他所属部门的名称：

````sql
select emp.name as emp_name, mgr.name as mgr_name, dept.name as dept_name 
from t_employee emp 
left join t_employee mgr on emp.manager_id = mgr.id 
left join t_department dept on emp.department_id = dept.id 
order by emp.id 
````

可以看到，在这句 SQL 中，`t_employee` 表出现了两次，但是它们拥有不同的别名，分别是 `emp` 和 `mgr`，正是这两个别名区分开了连接查询中的两个相同的表。那么在 Ktorm 中，我们如何实现这样的查询呢？

如果你有心的话，可能已经发现，`Table` 类中正好提供了一个 `aliased` 函数，它返回一个新的表对象，该对象复制自当前对象，具有完全相同的数据和结构，但是赋予了新的 `alias` 属性，这个函数正是在现在这个场景中使用的。使用 `aliased` 函数，尝试完成上面的自连接查询，你可能会写出这样的代码：

```kotlin
data class Names(val name: String, val managerName: String?, val departmentName: String)

val emp = Employees.aliased("emp") // 第三行，对 Employees 表对象赋予别名
val mgr = Employees.aliased("mgr") // 第四行，对 Employees 表对象赋予另一个不同的别名
val dept = Departments.aliased("dept")

val results = emp
    .leftJoin(mgr, on = emp.managerId eq mgr.id) // 第八行，连接两个不同的 Employees 表
    .leftJoin(dept, on = emp.departmentId eq dept.id)
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

上面的代码很符合直觉，也正是 Ktorm 的 SQL DSL 所推荐的书写风格，但遗憾的是，它很有可能无法通过编译。为了帮助我们分析这个错误，在这里先贴出 `Employees` 表对象的定义，这个定义复制自[定义表结构 - 表对象](/zh-cn/schema-definition.html#%E8%A1%A8%E5%AF%B9%E8%B1%A1)一节：

````kotlin
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

而父类 `Table` 中 `aliased` 方法的签名则是这样的：

```kotlin
open fun aliased(alias: String): Table<E> { ... }
```

很显然，根据 `aliased` 方法的签名，上面第三行中的 `Employees.aliased("emp")` 得到的返回值的类型应该是 `Table<E>`，第四行中的 `mgr` 变量的类型也是如此。那么，第八行中的 `emp.managerId eq mrg.id` 明显就是错误的了，因为 `id` 和 `managerId` 两个属性只在 `Employees` 对象中存在，而这里的两个具有别名的表对象的类型都是 `Table<E>`，而不是 `Employees`。

受限于 Kotlin 语言的限制，`Table.aliased` 函数虽然能够完成复制表结构并赋予别名的功能，但它的返回值只能是 `Table<E>`，而无法与它的调用者具有完全相同的类型。例如在这里我们使用 object 关键字将 `Employees` 定义为单例的表对象，由于 Kotlin 的单例限制，`aliased` 方法创建的新的表对象不可能也是 `Employees`。

为了正常实现自连接查询，我们推荐，**如果需要使用到表别名功能，请勿将表对象定义为 object，而应该使用 class 代替，并重写 `aliased` 方法使其返回完全相同的类型**：

```kotlin
class Employees(alias: String?) : Table<Nothing>("t_employee", alias) {
    override fun aliased(alias: String) = Employees(alias)
    // 此处省略无关的列定义
}
```

但是，单纯把 object 改成 class 也会遇到问题，比如无法再使用 `Employees.name` 的写法快速获取一个列，而必须要先调用构造方法创建一个表对象。因此，我们还推荐**在将表定义为 class 的同时，提供一个伴随对象，作为未赋予别名的默认表对象，这样既支持了原来的写法，又能使用表别名的功能**。最终的 `Employees` 定义如下：

```kotlin
open class Employees(alias: String?) : Table<Nothing>("t_employee", alias) {
    companion object : Employees(null)
    override fun aliased(alias: String) = Employees(alias)

    val id by int("id").primaryKey()
    val name by varchar("name")
    val job by varchar("job")
    val managerId by int("manager_id")
    val hireDate by date("hire_date")
    val salary by long("salary")
    val departmentId by int("department_id")
}
```

以上就是 Ktorm 提供的表别名的支持。现在你可以再尝试执行一下前面的自连接查询，如无意外，它现在应该已经可以完美生成 SQL，返回结果了。