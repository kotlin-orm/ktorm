---
title: Ktorm 3.0 不兼容更新
lang: zh-cn
related_path: en/break-changes-in-ktorm-3.0.html
---

# Ktorm 3.0 不兼容更新

时隔几个月，我们终于迎来了 Ktorm 的第二次大版本更新（Ktorm 3.0），此次更新包含了多项优化，其中也有一些不兼容的变更，特此说明。

> 如果这些不兼容的更新对您的项目产生了影响，我们表示十分抱歉，但这是为了确保框架长期迭代必须作出的取舍，请对照更新文档进行简单修改即可，这只会花费您几分钟的时间。

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

或者 Gradle： 

```groovy
compile "me.liuwj.ktorm:ktorm-global:${ktorm.version}"
```

然后，使用 `Database.connectGlobally` 函数连接到数据库：

```kotlin
Database.connectGlobally("jdbc:mysql://localhost:3306/ktorm?user=root&password=***")
```

这个方法会创建一个 `Database` 对象并返回，如果你需要的话，可以定义一个变量来保存这个返回值。但是通常来说，你没必要这么做，因为 ktorm-global 会自动记录最近创建的 `Database` 对象，在需要的时候，使用 `Database.global` 获取这个对象进行操作。

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

使用 `Table.findList` 扩展函数就可以获取表中符合条件的实体对象：

```kotlin
val employees = Employees.findList { it.departmentId eq 1 }
```

使用 `Table.sumBy` 就可以对表中的某个字段求和：

```kotlin
val total = Employees.sumBy { it.salary }
```

更多便捷用法请自己探索，也可以参照 [Ktorm 2.7 版本的修改点](./about-deprecating-database-global.html#修改点)，那些被废弃的函数，几乎全部都在 ktorm-global 中重新亮相。

## 使用 = 定义列，不再使用属性代理 by

在之前，我们定义一个表对象的时候，需要使用 `by` 关键字，利用属性代理来定义它的列，就像这样：

```kotlin
// Ktorm 3.0 之前
object Departments : Table<Nothing>("t_department") {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val location by varchar("location")
}
```

现在，我们不再需要属性代理，直接使用等号 `=` 即可：

```kotlin
// Ktorm 3.0
object Departments : Table<Nothing>("t_department") {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val location = varchar("location")
}
```

使用等号 `=` 更加简单直接，也避免了编译器为属性代理生成的额外字段。但是，这个改动会导致你的项目在升级到新版本之后产生许多编译错误，不用担心，你只需要找到你的所有表对象，把里面的 `by` 关键字批量替换成等号 `=` 即可。

## Query 类不再实现 Iterable 接口

在之前，为了能方便地获取查询结果，我们决定让 `Query` 类直接实现 `Iterable` 接口，这样我们就能直接使用 for-each 循环对查询结果进行遍历，也可以使用 `map`、`flatMap` 等函数对结果集进行各种各样的处理，比如：

```kotlin
data class Emp(val id: Int?, val name: String?, val salary: Long?)

val query = database.from(Employees).select()

query
    .map { row -> Emp(row[Employees.id], row[Employees.name], row[Employees.salary]) }
    .filter { it.salary > 1000 }
    .sortedBy { it.salary }
    .forEach { println(it.name) }
```

但是这也给我们带来了许多问题，因为 `Iterable` 的许多扩展函数的名称与 `Query` 的函数类似，甚至还可能存在名称冲突，这会让用户产生许多误解，比如 [#124](https://github.com/vincentlauvlwj/Ktorm/issues/124)、[#125](https://github.com/vincentlauvlwj/Ktorm/issues/125)。

因此，我们决定在 Ktorm 3.0 中，`Query` 类不再实现 `Iterable` 接口，为了确保原来的 DSL 代码不变，我们还提供了与 `Iterable` 相同的扩展函数。升级之后你会发现，尽管可能会产生一些编译错误，但是你的代码几乎是不用改的，唯一需要的可能是增加一行 `import` 语句，把原来对 `Iterable.map` 函数的调用，改成 `Query.map`：

```kotlin
import me.liuwj.ktorm.dsl.*
```

## 支持复合主键

在 Ktorm 3.0 中，我们还支持为一个表设置复合主键，复合主键由多个字段组成，这些字段共同决定主键的唯一性。使用方法很简单，只需要在定义表对象时，为每个主键字段调用 `primaryKey` 函数即可：

```kotlin
object Departments : Table<Nothing>("t_department") {
    val id = int("id").primaryKey()
    val name = varchar("name").primaryKey()
    val location = varchar("location")
}
```

这看起来只是一个简单的功能增强，但这里也存在与之前版本不兼容的地方。`BaseTable` 类中的 `val primaryKey: Column<*>` 属性被删除，改为了 `val primaryKeys: List<Column<*>>`，用于获取组成主键的所有字段。

## 其他更新

除了上述的不兼容变更，Ktorm 3.0 中还有不少来自开源社区热心人的更新，感谢他们的贡献：

- MySQL `bulkInsert` 函数支持 `on duplcate key update`，感谢 [@hangingman](https://github.com/hangingman)
- PostgreSQL `hstore` 数据类型及其一系列运算符，感谢 [@arustleund](https://github.com/arustleund)
- ktorm-jackson 模块支持简单的 Jackson 注解，如 `@JsonProperty`、`@JsonAlias`、`@JsonIgnore`，感谢 [@onXoot](https://github.com/onXoot)