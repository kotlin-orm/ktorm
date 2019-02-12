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

以上函数都是 `Table` 类的扩展函数，最简单的使用方式如下：

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

