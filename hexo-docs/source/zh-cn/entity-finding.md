---
title: 实体查询
lang: zh-cn
related_path: en/entity-finding.html
---

# 实体查询

Ktorm 提供了许多扩展函数，用来从数据库中获取实体对象，它们的共同特征是名称以 `find` 开头。

## find\* 函数

我们首先来看看 `findList` 函数，它是 `Table` 类的扩展函数，签名如下：

````kotlin
inline fun <E : Entity<E>, T : Table<E>> T.findList(predicate: (T) -> ColumnDeclaring<Boolean>): List<E>
````

这个函数接受一个闭包作为参数，使用闭包中返回的 `ColumnDeclaring<Boolean>` 作为查询的筛选条件，SQL 执行完毕后，从结果集中创建一个实体对象的列表并返回。作为参数的闭包函数本身也接收一个参数 `T`，这正是当前表对象，因此我们能在闭包中使用 it 引用它。获取部门 1 中所有员工的代码如下：

````kotlin
val employees = Employees.findList { it.departmentId eq 1 }
````

这种写法十分自然，就像使用 Kotlin 标准库中的函数从一个集合中筛选符合条件的元素一样，不同的仅仅是在 lambda 表达式中的等号 `==` 被这里的 `eq` 函数代替了而已。

上述代码生成的 SQL 如下：

````sql
select t_employee.id as t_employee_id, t_employee.name as t_employee_name, t_employee.job as t_employee_job, t_employee.manager_id as t_employee_manager_id, t_employee.hire_date as t_employee_hire_date, t_employee.salary as t_employee_salary, t_employee.department_id as t_employee_department_id, _ref0.id as _ref0_id, _ref0.name as _ref0_name, _ref0.location as _ref0_location 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where t_employee.department_id = ? 
````

> 生成 SQL 中包含了十分长的字段列表，这是有必要的，Ktorm 会尽量避免使用 select \*，但是为了展示的方便，以后在文档中出现的 SQL，太长的字段列表会使用 select \* 代替。

观察生成的 SQL，我们发现 Ktorm 自动使用外键 left join 了 `t_employee` 的关联表 `t_department`。这是因为我们在表对象中声明 `departmentId` 这一列时使用 `references` 函数将此列绑定到了 `Departments` 表。在调用 `find*` 系列的函数时，Ktorm 会自动递归地 left join 所有关联的表，将部门表的数据一并查询出来，填充到 `Employee.department` 属性中。

> 在使用 `references` 绑定时请注意避免循环的引用，比如 `Employees` 表引用了 `Departments`，则 `Departments` 不能再直接或间接地引用 `Employees`，否则会导致 Ktorm 在自动 left join 的过程中出现栈溢出。

包含 `findList` 在内，Ktorm 提供的 `find*` 系列函数的列表如下，它们都是 `Table` 类的扩展函数，都具有类似的行为：

- **findList：**获取符合条件的实体对象的列表
- **findAll：**获取该表中所有实体对象的列表
- **findOne：**获取符合条件的一个实体对象的列表，如果没有找到，返回 null，如果符合条件的实体数量多于一个，抛出异常
- **findById：**根据主键获取指定的实体对象，如果没有找到，返回 null，如果符合条件的实体数量多于一个，抛出异常
- **findListByIds：**根据主键批量获取实体对象的列表
- **findMapByIds：**根据主键批量获取实体对象，返回一个 `Map`，key 是主键，value 是实体对象

## 从查询 DSL 获取实体对象

`find*` 函数会自动 left join 引用表，有时这可能会造成一点浪费，另外，`find*` 函数还无法控制查询需要返回的具体字段、无法控制返回记录的排序、分页等。如果你希望对查询进行细粒度的控制，你可以使用前面章节中介绍的查询 DSL，Ktorm 提供了从查询 DSL 中获取实体对象的方法。

下面的例子使用 `createEntity` 函数从查询 DSL 中获取了实体对象的列表：

```kotlin
val employees = Employees
    .select()
    .orderBy(Employees.id.asc())
    .map { Employees.createEntity(it) }

employees.forEach { println(it) }
```

`Query` 对象实现了 `Iterable<QueryRowSet>` 接口，在这里，我们使用 `map` 函数对查询进行迭代，在迭代中使用 `createEntity` 为每一行返回的记录创建一个实体对象。`createEntity` 是 `Table` 类的扩展函数，它会根据表对象中的列绑定配置，自动创建实体对象，从结果集中读取数据填充到实体对象的各个属性中。如果该表使用 `references` 引用绑定了其它表，也会递归地对所引用的表调用 `createEntity` 创建关联的实体对象。

上述例子会生成一句简单的 SQL `select * from t_employee order by t_employee.id`，打印出如下结果：

````plain
Employee{id=1, name=vince, job=engineer, hireDate=2018-01-01, salary=100, department=Department{id=1}}
Employee{id=2, name=marry, job=trainee, manager=Employee{id=1}, hireDate=2019-01-01, salary=50, department=Department{id=1}}
Employee{id=3, name=tom, job=director, hireDate=2018-01-01, salary=200, department=Department{id=2}}
Employee{id=4, name=penny, job=assistant, manager=Employee{id=3}, hireDate=2019-01-01, salary=100, department=Department{id=2}}
````

## joinReferencesAndSelect

`joinReferencesAndSelect` 也是 `Table` 类的扩展函数，它创建一个 `Query` 对象，这个查询递归地 left join 当前表对象的所有关联表，并且 select 出它们的所有列。你可以直接从这个返回的 `Query` 对象中获取所有的记录，也可以紧接着调用 `Query` 类的其他扩展方法修改这个查询。实际上，`find*` 系列函数都是基于这个函数实现的。

下面是一个使用示例，这个查询获取所有的员工及其所在的部门的信息，并按员工的 ID 进行排序：

````kotlin
val employees = Employees
    .joinReferencesAndSelect()
    .orderBy(Employees.id.asc())
    .map { Employees.createEntity(it) }
````

生成的 SQL 如下：

````sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
order by t_employee.id 
````

从生成的 SQL 中可以看出，上面的查询实际上相当于手动调用 `leftJoin` 函数，例如下面的代码与上面例子中的查询就是完全等价的，但是使用 `joinReferencesAndSelect` 可以为我们减少一些样板代码。

```kotlin
val emp = Employees
val dept = emp.departmentId.referenceTable as Departments

val employees = emp
    .leftJoin(dept, on = emp.departmentId eq dept.id)
    .select(emp.columns + dept.columns)
    .orderBy(emp.id.asc())
    .map { emp.createEntity(it) }
```