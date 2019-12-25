---
title: 实体查询
lang: zh-cn
related_path: en/entity-finding.html
---

# 实体查询

Ktorm 提供了一套名为”实体序列”的 API，用来从数据库中获取实体对象。正如其名字所示，它的风格和使用方式与 Kotlin 标准库中的序列 API 极其类似，它提供了许多同名的扩展函数，比如 `filter`、`map`、`reduce` 等。

## 使用序列 API 获取实体

要使用序列 API，我们首先要通过 `sequenceOf` 函数得到一个实体序列：

```kotlin
val sequence = database.sequenceOf(Employees)
```

返回的 `sequence` 对象可以视为保存了 `Employees` 表中所有记录的一个序列。要从这个序列中获取实体对象，可以使用 `find` 函数：

```kotlin
val employee = sequence.find { it.id eq 1 }
```

这种写法十分自然，就像使用 Kotlin 标准库中的函数从一个集合中筛选符合条件的元素一样，不同的仅仅是在 lambda 表达式中的等号 `==` 被这里的 `eq` 函数代替了而已。

`find` 函数接收一个类型为 `(T) -> ColumnDeclaring<Boolean>` 的参数，这是一个闭包函数，其中的返回值会作为查询的筛选条件，SQL 执行完毕后，返回结果集中的第一条记录。作为参数的闭包函数本身也接收一个参数 `T`，这正是当前表对象，因此我们能在闭包中使用 it 引用它。

上述代码生成的 SQL 如下：

```sql
select t_employee.id as t_employee_id, t_employee.name as t_employee_name, t_employee.job as t_employee_job, t_employee.manager_id as t_employee_manager_id, t_employee.hire_date as t_employee_hire_date, t_employee.salary as t_employee_salary, t_employee.department_id as t_employee_department_id, _ref0.id as _ref0_id, _ref0.name as _ref0_name, _ref0.location as _ref0_location 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where t_employee.id = ? 
```

> 生成 SQL 中包含了十分长的字段列表，这是有必要的，Ktorm 会尽量避免使用 select \*，但是为了展示的方便，以后在文档中出现的 SQL，太长的字段列表会使用 select \* 代替。

观察生成的 SQL，我们发现 Ktorm 自动使用外键 left join 了 `t_employee` 的关联表 `t_department`。这是因为我们在表对象中声明 `departmentId` 这一列时使用 `references` 函数将此列绑定到了 `Departments` 表。在使用序列 API 的时候，Ktorm 会自动递归地 left join 所有关联的表，将部门表的数据一并查询出来，填充到 `Employee.department` 属性中。

> 在使用 `references` 绑定时请注意避免循环的引用，比如 `Employees` 表引用了 `Departments`，则 `Departments` 不能再直接或间接地引用 `Employees`，否则会导致 Ktorm 在自动 left join 的过程中出现栈溢出。

既然 Ktorm 会自动 left join 关联表，我们当然也能在筛选条件中使用关联表中的列。下面的代码可以获取一个在广州工作的员工对象，这里我们通过列的 `referenceTable` 属性获取 `departmentId` 所引用的表对象：

```kotlin
val employee = sequence.find {
    val dept = it.departmentId.referenceTable as Departments
    dept.location eq "Guangzhou"
}
```

为了让代码看起来优雅一点，我们可以在 `Employees` 表对象中添加一个 get 属性。下面的代码和上面是完全等价的，但是阅读起来却更加自然：

```kotlin
open class Employees(alias: String?) : Table<Employee>("t_employee", alias) {
    // 此处省略无关的列定义...
    val department get() = departmentId.referenceTable as Departments
}

val employee = sequence.find { it.department.location eq "Guangzhou" }
```

生成的 SQL 如下：

````sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where _ref0.location = ? 
````

> 注意：我们在通过 `it.departmentId.referenceTable` 获取到引用的表对象后，把它转型成了 `Departments`，因此这要求我们必须使用 class 而不是 object 定义表对象，并且重写 `aliased` 函数，具体参见[表别名](./joining.html#自连接查询与表别名)的相关介绍。

除了 `find` 函数外，实体序列 API 还给我们提供了许多方便的函数，如使用 `filter` 对元素进行筛选、使用 `groupingBy` 进行分组聚合等。与 SQL DSL 相比，实体序列 API 更具有函数式风格，其使用方式就像在操作内存中的集合一样，因此我们建议大家优先使用。更多用法请参见[实体序列](./entity-sequence.html)一节。

## 使用查询 DSL 获取实体

序列 API 会自动 left join 引用表，有时这可能会造成一点浪费，另外，我们还无法控制查询需要返回的具体字段、无法控制返回记录的排序、分页等。如果你希望对查询进行细粒度的控制，你可以使用前面章节中介绍的查询 DSL，Ktorm 提供了从查询 DSL 中获取实体对象的方法。

下面的例子使用 `createEntity` 函数从查询 DSL 中获取了实体对象的列表：

```kotlin
val employees = database
    .from(Employees)
    .select()
    .orderBy(Employees.id.asc())
    .map { Employees.createEntity(it) }

employees.forEach { println(it) }
```

`Query` 对象实现了 `Iterable<QueryRowSet>` 接口，在这里，我们使用 `map` 函数对查询进行迭代，在迭代中使用 `createEntity` 为每一行返回的记录创建一个实体对象。`createEntity` 是 `Table` 类的函数，它会根据表对象中的列绑定配置，自动创建实体对象，从结果集中读取数据填充到实体对象的各个属性中。如果该表使用 `references` 引用绑定了其它表，也会递归地对所引用的表调用 `createEntity` 创建关联的实体对象。

但是查询 DSL 返回的列是可自定义的，里面不一定包含引用表中的列。针对这种情况，Ktorm 提供了 `createEntityWithoutReferences` 函数，它的功能是一样的，但是不会自动获取引用表关联的实体对象的数据。它把引用绑定视为到其所引用的实体对象的主键的嵌套绑定，例如 `c.references(Departments) { it.department }`，在它眼里相当于 `c.bindTo { it.department.id }`，因此避免了不必要的对象创建和一些因列名冲突导致的异常。

在上面的例子中，不管我们使用 `createEntity` 还是 `createEntityWithoutReferences`，都会生成一句简单的 SQL `select * from t_employee order by t_employee.id`，打印出同样的结果：

````plain
Employee{id=1, name=vince, job=engineer, hireDate=2018-01-01, salary=100, department=Department{id=1}}
Employee{id=2, name=marry, job=trainee, manager=Employee{id=1}, hireDate=2019-01-01, salary=50, department=Department{id=1}}
Employee{id=3, name=tom, job=director, hireDate=2018-01-01, salary=200, department=Department{id=2}}
Employee{id=4, name=penny, job=assistant, manager=Employee{id=3}, hireDate=2019-01-01, salary=100, department=Department{id=2}}
````

## joinReferencesAndSelect

`joinReferencesAndSelect` 也是 `Table` 类的扩展函数，它创建一个 `Query` 对象，这个查询递归地 left join 当前表对象的所有关联表，并且 select 出它们的所有列。你可以直接从这个返回的 `Query` 对象中获取所有的记录，也可以紧接着调用 `Query` 类的其他扩展方法修改这个查询。实际上，实体序列 API 就是基于这个函数实现自动联表的。

下面是一个使用示例，这个查询获取所有的员工及其所在的部门的信息，并按员工的 ID 进行排序：

````kotlin
val employees = database
    .from(Employees)
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

val employees = database
    .from(emp)
    .leftJoin(dept, on = emp.departmentId eq dept.id)
    .select(emp.columns + dept.columns)
    .orderBy(emp.id.asc())
    .map { emp.createEntity(it) }
```