---
title: About Deprecating Database.global
lang: en
related_path: zh-cn/about-deprecating-database-global.html
---

# About Deprecating Database.global

In Ktorm 2.7, we did a refactoring of the code. This refactoring deprecated `Database.global` and a series of functions implemented based on it, making Ktorm's API design more intuitive and easier to extend. 

## Why?

In previous versions, `Database.connect` function saved the latest created `Database` instance to a global variable automatically, then the framework would obtain it via `Database.global` when needed. 

```kotlin
Database.global.useConnection { conn -> 
    // Do something with the connection...
}
```

But sometimes, we have to operate many databases in one App, so it's needed to create many `Database` instances and choose one while performing our database specific operations. 

```kotlin
val mysql = Database.connect("jdbc:mysql://localhost:3306/ktorm")
val h2 = Database.connect("jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1")

mysql {
    // Obtain all employees in MySQL database.
    for (employee in Employees.asSequence()) {
        println(employee)
    }
}

h2 {
    // Obtain all employees in H2 database.
    for (employee in Employees.asSequence()) {
        println(employee)
    }
}
```

Here, we use the `db { }` syntax to switch between databases, but now it seems that this is not a good design for the following reasons: 

- `db { }` is implemented using `ThreadLocal`. Switching databases in this way is too implicit, which may lead to some misunderstandings and unexpected bugs, for example [#65](https://github.com/vincentlauvlwj/Ktorm/issues/65) and [#27](https://github.com/vincentlauvlwj/Ktorm/issues/27).
- Using global variables is a bad design pattern. Code written in this way will be coupled with some global states, which is difficult to be tested and extended. Related discussions are [#47](https://github.com/vincentlauvlwj/Ktorm/issues/47) and [#41](https://github.com/vincentlauvlwj/Ktorm/issues/41).

## Changes

Our main goal of this refactoring is to deprecate the global variable `Database.global` and a series of APIs implemented based on it, making users explicitly specify the `Database` instances to use while performing database operations, instead of implicitly use `Database.global`. 

In previous versions, although `Database.connect` returns a new created `Database` object, we usually ignore it because Ktorm automatically saves it to an internal global variable. But now, we have to define a variable by ourselves to hold the return value: 

```kotlin
val database = Database.connect("jdbc:mysql://localhost:3306/ktorm?user=root&password=***")
```

We used to create queries by the extension function `Table.select` before: 

```kotlin
// Old API
for (row in Employees.select()) {
    println(row[Employees.name])
}
```

This query uses `Database.global`, obtaining all records from `Employees` table, which is indeed very implicit as you can see. Now we have to specify the database instance explicitly and use the syntax of `database.from(..).select(..)` to create queries: 

```kotlin
for (row in database.from(Employees).select()) {
    println(row[Employees.name])
}
```

Here is another example: 

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

It can be seen that the changes of SQL DSL are very simple, we just need to change the syntax from `Table.select` to `database.from(..).select(..)`. As for sequence APIs, we used to create sequence objects via `asSequence` before, and now we just need to change it to `sequenceOf`. For example: 

```kotlin
val employees = database.sequenceOf(Employees).toList()
```

Another example using `sequenceOf`: 

```kotlin
val employees = database
    .sequenceOf(Employees)
    .filter { it.departmentId eq 1 }
    .filter { it.managerId.isNotNull() }
    .sortedBy { it.salary }
    .toList()
```

These are the two most significant changes in this refactoring. The documents on Ktorm's official website have now been updated for version 2.7. You can refer to the latest documents for what you are interested in. 

Attached below is a list of deprecated APIs. These APIs are still available in version 2.7, but they have been marked as `@Deprecated` and will be completely removed in the future. 

| Deprecated Usages                            | New Usages                                                   |
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

These deprecated APIs will be completely removed in a future Ktorm 3.0 release. However, they also have some advantages, as we can make some APIs more concise with the help of the global variable. In order to meet the needs of as many users as possible, we will add a module named ktorm-global in Ktorm 3.0. 

At that time, APIs deprecated in version 2.7 will be reimplemented in the ktorm-global module. This module  will serve as an extension of Ktorm and provide more concise APIs based on a global variable. In this way, Ktorm's core module can completely remove those deprecated APIs, and if you want to use them, just need to add an extra dependency of ktorm-global. Hope we can find the right balance by adding this module. Stay tuned!!

