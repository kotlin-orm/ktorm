---
title: Break Changes in Ktorm 3.0
lang: en
related_path: zh-cn/break-changes-in-ktorm-3.0.html

---

# Break Changes in Ktorm 3.0

After a few months, we finally ushered in another major version update of Ktorm (Ktorm 3.0). This update contains many optimizations, and there are also some incompatible changes, which is hereby explained.

> If these incompatible updates have an impact on your project, we are very sorry, but this is a trade-off that must be made in order to ensure the long-term iteration of the framework. Please simply modify your code according to this document, which will only cost you a few minutes time.

## ktorm-global

Ktorm 2.7 deprecated the global variable `Database.global` and a series of APIs implemented based on it, making users explicitly specify the `Database` instances to use while performing database operations, instead of implicitly use `Database.global`. For more information about the previous version, please refer to [About Deprecating Database.global](./about-deprecating-database-global.html).

Using global variables is a bad design pattern. Code written in this way will be coupled with some global states, which is difficult to be tested and extended, and this is why we have to deprecate `Database.global`. However, there are also some advantages, as we can make some APIs more concise with the help of the global variable. For example, `Employees.findAll()`, after Ktorm 2.7, we have to write `database.sequenceOf(Employees).toList()`, which looks a lot more verbose.

Ktorm 3.0 has completely removed `Database.global` and its related APIs. But in order to give users more choices, we provide an additional module ktorm-global, which reimplements those deprecated APIs in version 2.7. You can use it as needed. 

To use ktorm-global, you should add a Maven dependency first: 

```xml
<dependency>
    <groupId>me.liuwj.ktorm</groupId>
    <artifactId>ktorm-global</artifactId>
    <version>${ktorm.version}</version>
</dependency>
```

Or Gradle： 

```groovy
compile "me.liuwj.ktorm:ktorm-global:${ktorm.version}"
```

Then connect to the database via function `Database.connectGlobally`: 

```kotlin
Database.connectGlobally("jdbc:mysql://localhost:3306/ktorm?user=root&password=***")
```

This function returns a new-created `Database` object, you can define a variable to save the returned value if needed. But generally, it's not necessary to do that, because ktorm-global will save the latest created `Database` instance automatically, then obtain it via `Database.global` when needed.  

```kotlin
Database.global.useConnection { conn -> 
    // Do something with the connection...
}
```

With the help of the global object, our code can be more shorter, for example, to create a query by directly using the extension function `Table.select`:  

```kotlin
for (row in Employees.select()) {
    println(row[Employees.name])
}
```

Use `Table.findList` to obtain entity objects in the table that matches the given condition: 

```kotlin
val employees = Employees.findList { it.departmentId eq 1 }
```

Use `Table.sumBy` to sum a column in the table: 

```kotlin
val total = Employees.sumBy { it.salary }
```

For more convenient usages, please explore by yourself. You can also refer to [Changes in Ktorm 2.7](./about-deprecating-database-global.html#Changes). Almost all those deprecated functions are reimplemented in ktorm-global. 

## Use = Instead of Property Delegation to Define Columns

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

## Query doesn't Implement Iterable anymore

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

## Support Compound Primary Keys

在 Ktorm 3.0 中，我们还支持为一个表设置复合主键，复合主键由多个字段组成，这些字段共同决定主键的唯一性。使用方法很简单，只需要在定义表对象时，为每个主键字段调用 `primaryKey` 函数即可：

```kotlin
object Departments : Table<Nothing>("t_department") {
    val id = int("id").primaryKey()
    val name = varchar("name").primaryKey()
    val location = varchar("location")
}
```

这看起来只是一个简单的功能增强，但这里也存在与之前版本不兼容的地方。`BaseTable` 类中的 `val primaryKey: Column<*>` 属性被删除，改为了 `val primaryKeys: List<Column<*>>`，用于获取组成主键的所有字段。

## Others

除了上述的不兼容变更，Ktorm 3.0 中还有不少来自开源社区热心人的更新，感谢他们的贡献：

- MySQL `bulkInsert` 函数支持 `on duplcate key update`，感谢 [@hangingman](https://github.com/hangingman)
- PostgreSQL `hstore` 数据类型及其一系列操作符，感谢 [@arustleund](https://github.com/arustleund)
- ktorm-jackson 模块支持简单的 Jackson 注解，如 `@JsonProperty`、`@JsonAlias`、`@JsonIgnore`，感谢 [@onXoot](