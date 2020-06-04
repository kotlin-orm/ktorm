---
title: Ktorm 3.0 不兼容升级
lang: zh-cn
related_path: en/break-changes-in-ktorm-3.0.html
---

# Ktorm 3.0 不兼容更新

时隔几个月，我们终于迎来了 Ktorm 的第二次大版本更新（Ktorm 3.0），此次更新包含了多项优化，其中也有一些不兼容的变更，特此说明。

## ktorm-global

Ktorm 2.7 版本废弃了 `Database.global` 全局变量以及与之相关的一系列 API，从此以后，我们每次操作数据的时候，都需要显式地指定一个 `Database` 对象，而不是隐式地使用 `Database.global`。关于上个版本的更多信息，请参考[关于废弃 Database.global 对象的说明](./about-deprecating-database-global.html)。

使用全局变量是糟糕的设计模式， 这样写出来的代码会与全局的状态耦合，不方便扩展，这就是我们要废弃 `Database.global` 的原因。然而，全局变量也有它不可替代之处，它可以让某些 API 的设计更加简洁，帮助我们写出更简短的代码。比如 `Employees.findAll()`，在 Ktorm 2.7 以后，我们不得不写成 `database.sequenceOf(Employees).toList()`，看起来啰嗦好多。

Ktorm 3.0 已经完全删除了`Database.global` 相关的 API，但是，为了让大家有更多的选择，我们额外提供了一个 ktorm-global 模块，这个模块重新实现了原来的那套全局变量 API，大家可以按需使用。

要使用 ktorm-global，首先应该添加一个 Maven 依赖：

```xml
<dependency>
    <groupId>me.liuwj.ktorm</groupId>
    <artifactId>ktorm-global</artifactId>
    <version>${ktorm.version}</version>
</dependency>
```

或者 gradle： 

```groovy
compile "me.liuwj.ktorm:ktorm-global:${ktorm.version}"
```

然后，使用 `Database.connectGlobally` 函数连接到数据库：

```kotlin
Database.connectGlobally("jdbc:mysql://localhost:3306/ktorm?user=root&password=***")
```

这个方法会创建一个 `Database` 对象并返回，如果你需要的话，可以定义一个变量来保存这个返回值。但是通常来说，你没必要这么做，因为 ktorm-global 会自动记录最近创建的 `Database` 对象，在需要的使用，使用 `Database.global` 获取这个对象进行操作。

```kotlin
Database.global.useConnection { conn -> 
    // 使用连接进行操作...
}
```

有了全局对象，我们的很多代码都可以变得更简短，比如，直接使用 `Table.select` 扩展函数就可以创建一个查询：

```kotlin
for (row in Employees.select()) {
    println(row[Employees.name])
}
```

使用 `Table.findList` 扩展函数就可以获取表中的实体对象：

```kotlin
val employees = Employees.findList { it.departmentId eq 1 }
```

使用 `Table.sumBy` 就可以对表中的某个字段求和：

```kotlin
val total = Employees.sumBy { it.salary }
```

更多便捷用法请自己探索，也可以参照 [Ktorm 2.7 版本的修改点](./about-deprecating-database-global.html#修改点)，那些被废弃的函数，几乎全部都在 ktorm-global 中重新亮相。