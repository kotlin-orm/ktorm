---
title: 查询
lang: zh-cn
related_path: en/query.html
---

# 查询

在前面的章节中，我们曾经创建过一个简单的查询，它查询表中所有的员工记录，然后打印出他们的名字，我们的介绍就从这里开始：

````kotlin
for (row in Employees.select()) {
    println(row[Employees.name])
}
````

## Query 对象

在上面的例子中，`select` 方法返回了一个类型为 `Query` 的对象，然后使用 for-each 循环对其进行迭代，那么除了迭代外，`Query` 类还支持什么操作呢？让我们先来看一下它的定义：

```kotlin
data class Query(val expression: QueryExpression) : Iterable<QueryRowSet> {
    
    val sql: String by lazy { ... }

    val rowSet: QueryRowSet by lazy { ... }

    val totalRecords: Int by lazy { ... }

    override fun iterator(): Iterator<QueryRowSet> {
        return rowSet.iterator()
    }
}
```

`Query` 表示一个查询操作，Ktorm 正是以这个类为核心支持所有的查询 DSL。

可以看到，`Query` 的主构造函数接受一个 `QueryExpression` 作为参数，这正是此次查询需要执行的 SQL 语句的抽象表示，一般来说，我们不需要自己使用这个构造函数创建 `Query` 对象，而是使用 `Table.select` 扩展函数，由 Ktorm 为我们构造一个查询。

`Query` 类还实现了 `Iterable<QueryRowSet>` 接口，通过实现这个接口，我们才能够使用 for-each 循环的语法遍历查询返回的结果集。而且，Kotlin 标准库中也有许多针对 `Iterable` 接口的扩展函数，所以我们还可以使用 `map`、 `filter` 等函数对结果集进行各种各样的处理，就像这样：

```kotlin
data class Emp(val id: Int?, val name: String?, val salary: Long?)

Employees.select()
    .map { row -> Emp(row[Employees.id], row[Employees.name], row[Employees.salary]) }
    .filter { it.salary > 1000 }
    .sortedByDescending { it.salary }
    .forEach { println(it.name) }
```

实际上，在这里 Ktorm 所完成的工作，也只是生成了一句简单的 SQL `select * from t_employee` 而已，后面的 `.map { }.filter { }.sortedByDescending { }.forEach { }` 全部都是 Kotlin 标准库中的函数，这就是实现 `Iterable` 接口给我们带来的好处。

`Query` 类中还有一些有用的属性：

- sql：返回该查询生成的 SQL 字符串，可以在调试程序的时候确认生成的 SQL 是否符合预期。
- rowSet：返回该查询的结果集对象，此字段懒初始化，在第一次获取时，执行 SQL 语句，从数据库中获取结果。
- totalRecords：如果该查询没有使用 offset, limit 进行分页，此字段返回结果集的总行数；如果使用了分页，返回去除 offset, limit 限制后的符合条件的总记录数。Ktorm 使用此字段来支持页码计算，你可以使用 `totalRecords / limit` 计算总页数。  

## 获取查询结果

如果你用过 JDBC，你应该知道如何从 `ResultSet` 中获取你的查询结果。你需要使用一个循环不断地遍历结果集中的行，在循环中调用 `getInt` 、`getString` 等方法获取指定列中的数据，典型的用法是一个 while 循环：`while (rs.next()) { ... }`。而且，使用完毕后，你还得调用 `close` 方法关闭结果集。

这种写法虽说并不复杂，但重复的代码写多了也难免让人厌烦。Ktorm 通过让 `Query` 类实现 `Iterable` 接口，为你提供了另一种可能。你可以使用 for-each 循环迭代 `Query` 的结果集，也可以使用 `map`、`filter` 等扩展函数对结果集进行二次处理，就像前面的例子一样。

你可能已经发现，`Query.rowSet` 返回的结果集并不是普通的 `ResultSet`，而是 `QueryRowSet`。这是 Ktorm 提供的特殊的 `ResultSet` 的实现，与普通的 `ResultSet` 不同，它有如下特性：

- 离线可用：它不依赖于数据库连接，当连接关闭后，仍然可以正常使用，使用完毕也不需要调用 `close` 方法。`QueryRowSet` 在创建时，已经完整取出了结果集中的所有数据保存在内存中，因此只需要等待 GC 自动回收即可。
- 索引访问操作符：`QueryRowSet` 重载了[索引访问操作符](https://kotlinlang.org/docs/reference/operator-overloading.html#indexed)，因此你可以使用方括号语法 `[]` ，通过传入指定 `Column` 对象来获取这个列的数据，这种方法得益于编译器的静态检查，不易出错。不过，你仍然可以使用 `ResultSet` 中的 `getXxx` 方法，通过传入列的序号或名称字符串来获取。

使用索引访问操作符获取数据的方法如下：

```kotlin
for (row in Employees.select()) {
    val id: Int? = row[Employees.id]
    val name: String? = row[Employees.name]
    val salary: Long? = row[Employees.salary]

    println("$id, $name, $salary")
}
```

可以看到，如果列的类型是 `Column<Int>`，返回的结果的类型就是 `Int?`，如果列的类型是 `Column<String>`，返回的结果的类型就是 `String?`。而且，列的类型并不局限于 `ResultSet` 中的 `getXxx` 方法返回的那些类型，它可以是任意类型，结果也始终是对应的类型，其中还可以包含一些对结果的必要的转换行为，具体取决于定义该列时所使用的 [SqlType](/zh-cn/schema-definition.html#SqlType)。

## select