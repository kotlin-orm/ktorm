---
title: 实体增删改
lang: zh-cn
related_path: en/entity-dml.html
---

# 实体增删改

除了查询以外，序列 API 还支持实体对象的增删改操作。当然，我们首先要定义一些扩展属性，它们使用 `sequenceOf` 函数创建序列对象：

```kotlin
val Database.departments get() = this.sequenceOf(Departments)
val Database.employees get() = this.sequenceOf(Employees)
```

## 插入

序列 API 提供了一个扩展函数 `add`，用来将实体对象插入到数据库，插入成功后，返回受影响的记录数，这个函数的签名如下：

```kotlin
fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.add(entity: E): Int
```

要调用这个函数，我们首先需要创建一个实体对象。根据前面章节的介绍，实体对象的创建既可以调用 `Entity.create` 函数，也可以选择为实体类添加一个继承于 `Entity.Factory` 的伴随对象，这里我们选择第二种方式。下面的代码创建了一个员工对象，并将它插入到数据库中：

```kotlin
val employee = Employee {
    name = "jerry"
    job = "trainee"
    hireDate = LocalDate.now()
    salary = 50
    department = database.departments.find { it.name eq "tech" }
}

database.employees.add(employee)
```

在上面的例子中，我们创建了一个员工对象，并为它的各个属性设置了初始值。值得注意的是 `department` 这个属性，它是员工所属的部门，它的值是通过序列 API 从数据库中查询获得的实体对象，在调用 `add` 函数的时候，它的 ID 会被保存在 `Employees` 表中。生成的 SQL 如下：

````sql
insert into t_employee (name, job, hire_date, salary, department_id) 
values (?, ?, ?, ?, ?) 
````

可以看到，生成的 SQL 包含了 `Employee` 实体对象中的所有非空属性，如果我们将某个字段的赋值代码去掉或者修改为 null，那么生成的 insert SQL 中就不会出现这个字段。例如，创建实体对象时只设置员工名称 `Employee { name = "jerry" }`，那么生成的 SQL 也只会插入这一个字段 `insert into t_employee (name) values (?)`。

如果我们使用了数据库的自增主键功能，那么只要在表对象中使用 `primaryKey` 指定了主键列，`add` 函数在执行完插入之后，会自动从数据库中获取生成的主键，并填充到相应的属性中。但是这个功能要求我们不能事先设置了主键属性的值，如果你这样做了，所设置的值会被插入到数据库中，并且不会触发自增主键的生成。

还是以上面的代码为例，我们在创建实体对象时没有为其设置 `id` 属性，那么在执行完 `add` 方法之后，通过 `employee.id` 即可获取生成的主键。如果我们事先设置其 `id` 为某个值，那么生成的 SQL 就会包含该列，将它插入到数据库，插入后使用 `employee.id` 获取到的也是我们事先设置的这个值。

## 更新

我们知道，Ktorm 的实体类都定义为接口，并且继承 `Entity`。`Entity` 接口为实体对象注入了许多有用的函数，我们先来看一下它的定义，看看都有哪些函数。

```kotlin
interface Entity<E : Entity<E>> : Serializable {
    
    fun flushChanges(): Int

    fun discardChanges()

    fun delete(): Int

    operator fun get(name: String): Any?

    operator fun set(name: String, value: Any?)
}
```

可以看到里面有一个 `flushChanges` 函数，它的功能正是将实体对象的修改更新到数据库，执行后返回受影响的记录数。典型用法是先使用序列 API 从数据库中获取实体对象，然后按需修改它们的属性值，最后再调用 `flushChanges` 保存这些修改。

```kotlin
val employee = database.employees.find { it.id eq 5 } ?: return
employee.job = "engineer"
employee.salary = 100
employee.flushChanges()
```

上面的代码会生成两句 SQL，第一句是 `find` 前面已经介绍过，不必多说，`flushChanges` 生成的 SQL 如下：

````sql
update t_employee set job = ?, salary = ? where id = ? 
````

如果我们删除 `employee.salary = 100` 一行，只修改 `job` 属性，那么生成的 SQL 就会变成 `update t_employee set job = ? where id = ?`；如果我们不修改任何属性，直接调用 `flushChanges`，那么什么也不会发生，`flushChanges` 会直接返回 0。可见 Ktorm 会在内部跟踪实体对象的状态变化，这个跟踪是通过 JDK 动态代理来实现的，这正是 Ktorm 要求将实体类定义为接口的原因。

`discardChanges` 方法会清除 Ktorm 内部保存的该实体对象的状态变化信息，调用此函数之后，再调用 `flushChanges` 不会发生任何事情，因为 Ktorm 已经检测不到任何属性的变化。另外，如果连续对同一个实体对象调用两次 `flushChanges`，第一次调用之后，由于属性的变化已经保存到数据库，因此 Ktorm 会在内部清除它的状态数据，第二次 `flushChanges` 调用也不会发生任何事情。

另外，使用 `flushChanges` 函数还有以下两个注意事项：

1. 该函数要求在表对象中必须使用 `primaryKey` 函数指定主键列，否则 Ktorm 无法确定实体对象的唯一标识，在调用 `flushChanges` 的时候就会抛出异常。
2. 调用 `flushChanges` 函数的实体对象必须首先”与某个表关联“。在 Ktorm 的实现中，实体对象的内部持有一个表对象的引用 `fromTable`，表示该对象与某个表关联，或者来自某个表。使用序列 API 获取的实体对象，其内部的 `fromTable` 引用都指向其序列的来源表。使用 `Entity.create` 函数或 `Entity.Factory` 新创建的实体对象，其 `fromTable` 引用初始为空，因此不能对其调用 `flushChanges`，在使用 `add` 函数将其保存到数据库后，Ktorm 会修改 `fromTable` 为当前表对象，因此可以在后续调用它的 `flushChanges` 函数。

> 对于以上第二点，通俗来说，调用 `flushChanges` 函数的实体对象，必须来自序列 API 或者已被 `add` 函数保存到数据库。还有一点需要注意，在序列化时，Ktorm 只会保存各个属性的值，包括 `fromTable` 在内的用于追踪实体状态变化的数据都会丢失（被标记为 transient），因此你无法在一个系统中获取实体，然后在另一个系统中调用实体的 `flushChanges` 方法将属性变化更新到数据库。

## 删除

`Entity` 接口中还有一个 `delete` 函数，它的功能是从数据库中删除该实体对象，执行后返回受影响的记录数。典型用法是先使用序列 API 从数据库中获取实体对象，然后根据条件按需调用 `delete` 函数将其删除：

````kotlin
val employee = database.employees.find { it.id eq 5 } ?: return
employee.delete()
````

`delete` 函数生成的 SQL 如下：

````sql
delete from t_employee where id = ? 
````

与 `flushChanges` 相同， 使用 `delete` 函数也有两个注意事项：

1. 在表对象中必须使用 `primaryKey` 函数指定主键列，否则 Ktorm 无法确定实体对象的唯一标识。
2. 调用 `delete` 函数的实体对象必须首先”与某个表关联“。

最后，序列 API 还提供了 `removeIf` 和 `clear` 两个函数，`removeIf` 可以删除表中符合条件的记录，`clear` 可以删除表中的所有记录。下面使用 `removeIf` 删除部门 1 中的所有员工：

```kotlin
database.employees.removeIf { it.departmentId eq 1 }
```

生成 SQL：

```sql
delete from t_employee where department_id = ?
```

