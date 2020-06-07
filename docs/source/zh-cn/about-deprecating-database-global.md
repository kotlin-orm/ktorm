---
title: 关于废弃 Database.global 对象的说明
lang: zh-cn
related_path: en/about-deprecating-database-global.html
---

# 关于废弃 Database.global 对象的说明

在 Ktorm 2.7 版本中，我们对代码进行了一次重构，这次重构废弃掉了 `Database.global` 以及基于它实现的一系列函数，使 Ktorm 的 API 设计更加直观、更易扩展。

## 原因

在之前的版本中，`Database.connect` 函数会自动把最近一次创建的 `Database` 对象保存到一个全局变量中，在需要的时候，Ktorm 会通过 `Database.global` 获取到这个对象进行操作。

```kotlin
Database.global.useConnection { conn -> 
    // 使用连接进行操作...
}
```

但是有时候，我们需要在一个 App 中操作多个数据库，这时就需要创建多个 `Database` 对象，在执行具体的操作时，指定你要使用哪个数据库。

```kotlin
val mysql = Database.connect("jdbc:mysql://localhost:3306/ktorm")
val h2 = Database.connect("jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1")

mysql {
    // 获取 MySQL 数据库中的员工列表
    for (employee in Employees.asSequence()) {
        println(employee)
    }
}

h2 {
    // 获取 H2 数据库中的员工列表
    for (employee in Employees.asSequence()) {
        println(employee)
    }
}
```

在这里，我们使用 `db { }` 的语法实现了多数据源的切换，但是现在看来，这并不是一个很好的设计，理由如下：

- `db { }` 使用 `ThreadLocal` 实现，这种切换数据源的方式过于隐蔽，可能会导致一些误解，产生一些意料之外的 bug，比如 [#65](https://github.com/vincentlauvlwj/Ktorm/issues/65), [#27](https://github.com/vincentlauvlwj/Ktorm/issues/27)
- 使用全局变量是糟糕的设计模式，这样写出来的代码会与全局的状态耦合，不方便进行单元测试，也不方便以后的扩展，相关的讨论有 [#47](https://github.com/vincentlauvlwj/Ktorm/issues/47), [#41](https://github.com/vincentlauvlwj/Ktorm/issues/41)

## 修改点

这次重构，我们的主要目标就是废弃掉 `Database.global` 全局变量以及与之相关的一系列 API，让用户在操作数据库的时候，显式地指定要使用的 `Database` 对象，而不是隐式地使用 `Database.global`。

在之前，虽然 `Database.connect` 函数会返回一个 `Database` 对象，但是我们通常都会忽略它，因为 Ktorm 会自动把它保存到内部的全局变量中。但是现在，我们必须自己定义一个变量去接收它的返回值：

```kotlin
val database = Database.connect("jdbc:mysql://localhost:3306/ktorm?user=root&password=***")
```

在之前，我们直接使用 `Table.select` 扩展函数就可以创建一个查询：

```kotlin
// 旧 API
for (row in Employees.select()) {
    println(row[Employees.name])
}
```

这个查询使用 `Database.global` 对象，从 `Employees` 表中获取所有的记录，可以看到，这确实十分隐蔽。现在，我们必须要显式指定数据源对象，改用 `database.from(..).select(..)` 的语法创建查询：

```kotlin
for (row in database.from(Employees).select()) {
    println(row[Employees.name])
}
```

一个稍微复杂的例子：

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

可以看出，SQL DSL 的修改十分简单，我们只需要把原来 `Table.select` 的语法改成 `database.from(..).select(..)` 即可。至于序列 API，我们之前是通过 `asSequence` 扩展函数获取序列对象，现在也只需要改成 `sequenceOf` 函数，例如：

```kotlin
val employees = database.sequenceOf(Employees).toList()
```

再举一个稍微复杂的例子：

```kotlin
val employees = database
    .sequenceOf(Employees)
    .filter { it.departmentId eq 1 }
    .filter { it.managerId.isNotNull() }
    .sortedBy { it.salary }
    .toList()
```

以上就是本次重构中最明显的两个变化，Ktorm 官网中的文档现在都已经针对 2.7 版本做了更新，您可以查阅最新的文档获取你感兴趣的内容。

下面附上本次重构废弃的所有 API 的列表，这些 API 在 2.7 版本中仍然可用，但是已经被标记为 `@Deprecated`，并且将会在未来的版本中彻底移除。

| 废弃用法                                     | 新的用法                                                     |
| -------------------------------------------- | ------------------------------------------------------------ |
| Database.global                              | -                                                            |
| Employees.select()                           | database.from(Employees).select()                            |
| Employees.xxxJoin(Departments)               | database.from(Employees).xxxJoin(Departments)                |
| Employees.joinReferencesAndSelect()          | database.from(Employees).joinReferencesAndSelect()           |
| Employees.createEntityWithoutReferences(row) | Employees.createEntity(row, withReferences = false)          |
| Employees.asSequence()                       | database.sequenceOf(Employees)                               |
| Employees.asSequenceWithoutReferences()      | database.sequenceOf(Employees, withReferences = false)       |
| Employees.findList { .. }                    | database.sequenceOf(Employees).filter { .. }.toList()        |
| Employees.findAll()                          | database.sequenceOf(Employees).toList()                      |
| Employees.findOne { .. }                     | database.sequenceOf(Employees).find { .. }                   |
| Employees.findById(id)                       | database.sequenceOf(Employees).find { it.id eq id }          |
| Employees.findListByIds(ids)                 | database.sequenceOf(Employees).filter { it.id inList ids }.toList() |
| Employees.findMapByIds(ids)                  | database.sequenceOf(Employees).filter { it.id inList ids }.associateBy { it.id } |
| Employees.update { .. }                      | database.update(Employees) { .. }                            |
| Employees.batchUpdate { .. }                 | database.batchUpdate(Employees) { .. }                       |
| Employees.insert { .. }                      | database.insert(Employees) { .. }                            |
| Employees.batchInsert { .. }                 | database.batchInsert(Employees) { .. }                       |
| Employees.insertAndGenerateKey { .. }        | database.insertAndGenerateKey(Employees) { .. }              |
| Employees.delete { .. }                      | database.delete(Employees) { .. }                            |
| Employees.deleteAll()                        | database.deleteAll(Employees)                                |
| Employees.add(entity)                        | database.sequenceOf(Employees).add(entity)                   |
| Employees.all { .. }                         | database.sequenceOf(Employees).all { .. }                    |
| Employees.any { .. }                         | database.sequenceOf(Employees).any { .. }                    |
| Employees.none { .. }                        | database.sequenceOf(Employees).none { .. }                   |
| Employees.count { .. }                       | database.sequenceOf(Employees).count { .. }                  |
| Employees.sumBy { .. }                       | database.sequenceOf(Employees).sumBy { .. }                  |
| Employees.maxBy { .. }                       | database.sequenceOf(Employees).maxBy { .. }                  |
| Employees.minBy { .. }                       | database.sequenceOf(Employees).minBy { .. }                  |
| Employees.averageBy { .. }                   | database.sequenceOf(Employees).averageBy { .. }              |

## ktorm-global

在未来的 Ktorm 3.0 版本中，这些废弃的 API 将会彻底移除。但是，它们其实也有一些可取之处，比如使用全局对象之后，某些 API 的设计可以变得更简洁。为了尽可能满足更多用户的需求，在 Ktorm 3.0 版本中，我们将增加一个 ktorm-global 模块。

届时，在 2.7 版本中废弃掉的 API 都会放到 ktorm-global 模块中重新实现。这个模块会作为 Ktorm 的扩展，提供基于全局对象设计的更简洁的 API，这样，在 Ktorm 的核心模块中就可以彻底移除全局变量相关的 API，如果要使用全局变量，额外添加 ktorm-global 的依赖即可。通过这种方式，我们希望能够找到一个微妙的平衡。敬请期待！

> ktorm-global 模块现已发布，请参见 [Ktorm 3.0 不兼容更新](./break-changes-in-ktorm-3.0.html)。