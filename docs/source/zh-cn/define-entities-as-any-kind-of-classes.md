---
title: 使用任意的类作为实体类
lang: zh-cn
related_path: en/define-entities-as-any-kind-of-classes.html
---

# 使用任意的类作为实体类

在 Ktorm 2.5 版本中，我们对代码进行了一次重构，这次重构让我们可以使用 data class、POJO、或者任意的类作为实体类。从此，Ktorm 中的实体类，不一定非要定义为 interface 并继承 `Entity` 接口，在一定程度上降低了对用户代码的侵入性，这对于一个通用的框架而言是很重要的。

> 关于如何使用 interface 定义实体类，可参考[实体类与列绑定](./entities-and-column-binding.html)相关的文档。

## Table & BaseTable

在此之前，`Table` 作为 Ktorm 中表对象的公共父类，提供了基础的表定义、列定义等功能以及把表绑定到 `Entity` interface 的支持。在此次重构中，我们在 `Table` 之上增加了一个更抽象的 `BaseTable`。

`BaseTable` 是一个抽象类，是 Ktorm 2.5 版本以后所有表对象的公共父类，它提供了基础的表定义、列定义等功能，但是不负责实体类绑定相关的任何逻辑。`BaseTable` 中有一个抽象函数 `doCreateEntity`，子类需要实现这个函数，根据自己定义的绑定规则，从查询返回的结果集中创建出一个实体对象。在这里，我们使用的实体对象的类型并没有任何限制，它可以是继承于 `Entity` 的 interface，也可以是 data class、POJO、或者任意的类。

而 `Table` 则和以前一样，它限制了我们的实体类必须定义为 `Entity` interface。它是 `BaseTable` 的子类，除了基本的表定义、列定义等功能外，它还额外提供了 `bindTo`、`references` 等函数，以支持实体类的绑定功能。`Table` 实现了父类中的 `doCreateEntity` 函数，这个函数会使用 `bindTo`、`references` 等函数指定的列绑定配置，自动创建实体对象，从结果集中读取数据填充到实体对象的各个属性中。

## 以 data class 作为实体类

要使用 data class 作为实体类，我们在定义表对象时，应该改为继承 `BaseTable`，而不是 `Table`。另外，也不再需要调用 `bindTo`、`references` 等函数指定数据库列与实体类属性的绑定关系，而是实现 `doCreateEntity` 函数，自行完成从查询结果 `QueryRowSet` 中创建一个实体对象的过程。

下面是一个例子：

```kotlin
data class Staff(
    val id: Int,
    val name: String,
    val job: String,
    val managerId: Int,
    val hireDate: LocalDate,
    val salary: Long,
    val sectionId: Int
)

object Staffs : BaseTable<Staff>("t_employee") {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val job = varchar("job")
    val managerId = int("manager_id")
    val hireDate = date("hire_date")
    val salary = long("salary")
    val sectionId = int("department_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Staff(
        id = row[id] ?: 0,
        name = row[name].orEmpty(),
        job = row[job].orEmpty(),
        managerId = row[managerId] ?: 0,
        hireDate = row[hireDate] ?: LocalDate.now(),
        salary = row[salary] ?: 0,
        sectionId = row[sectionId] ?: 0
    )
}

val Database.staffs get() = this.sequenceOf(Staffs)
```

可以看到，这里的 `Staff` 只是一个单纯的 data class，Ktorm 对这个类没有任何特殊的要求，不再需要把它定义为 interface，把框架对用户代码的侵入性做到了最低。`Staffs` 表对象在定义的时候也改为继承 `BaseTable` 类，并且实现了 `doCreateEntity` 函数，在里面使用方括号语法 `[]` 获取每个列的值填充到数据对象中。

理论上，到此为止这篇文章基本就可以结束了，因为其他的用法（如 SQL DSL、Sequence API 等）与之前并没有任何的区别，甚至他们都是共用一套代码实现的，下面是几个简单的例子。

使用 SQL DSL 查询数据：

```kotlin
val staffs = database
    .from(Staffs)
    .select(Staffs.id, Staffs.name)
    .where { Staffs.id eq 1 }
    .map { Staffs.createEntity(it) }
```

使用序列 API 获取实体对象，并按指定字段排序：

```kotlin
val staffs = database.staffs
    .filter { it.sectionId eq 1 }
    .sortedBy { it.id }
    .toList()
```

获取每个部门中薪资小于十万的员工的数量：

```kotlin
val counts = database.staffs
    .filter { it.salary less 100000L }
    .groupingBy { it.sectionId }
    .eachCount()
```

更多用法，请参见 [SQL DSL](./query.html) 和[实体序列](./entity-sequence.html)等相关文档。

## 相关限制

如果 data class 真的那么完美的话，Ktorm 在最初设计的时候就不会决定使用 `Entity` interface 了。事实上，即使在 2.5 版本发布以后，使用 interface 定义实体类仍然是我们的第一选择。与使用 interface 定义实体类相比，使用 data class 目前还存在如下两个限制：

- **无法使用列绑定功能：**由于直接以 `BaseTable` 作为父类，我们无法在定义表对象时使用 `bindTo`、`references` 等函数指定数据库列与实体类属性的绑定关系，因此每个表对象都必须实现 `doCreateEntity` 函数，在此函数中手动创建实体对象，并一一对各个属性赋值。
- **无法使用实体对象的增删改 API：**由于使用 data class 作为实体类，Ktorm 无法对其进行代理，因此无法检测到实体对象的状态变化，这导致 `sequence.add(..)`、`entity.flushChanges()` 等针对实体对象的增删改 API 将无法使用。但是 SQL DSL 并没有影响，我们仍然可以使用 `database.insert(..) {..}`、`database.update(..) {..}` 等 DSL 函数进行增删改操作。

由于以上限制的存在，在你决定使用 data class 作为实体类之前，应该慎重考虑，你获得了使用 data class 的好处，同时也会失去其他的东西。请记住这句话：**使用 interface 定义实体类仍然是我们的第一选择。**

