---
title: 方言与原生 SQL
lang: zh-cn
related_path: en/dialects-and-native-sql.html
---

# 方言与原生 SQL

我们知道，SQL 语言虽然存在统一的标准，但是在标准之外，各种数据库都有着自己独特的特性。Ktorm 的核心模块（ktorm-core）仅对标准 SQL 提供了支持，如果希望使用某个数据库中特有的功能，我们就会用到方言模块。

## 启用方言

在 Ktorm 中，方言被抽象为一个接口，这个接口中目前只有一个 `createSqlFormatter` 函数，用来创建一个 `SqlFormatter` 的子类对象，使用自己特有的方言语法将 SQL 表达式格式化为 SQL 字符串。

```kotlin
interface SqlDialect {

    fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter
}
```

Ktorm 目前支持三种数据库方言，每种方言都作为一个独立于 ktorm-core 的模块发布，他们都会提供一个自己的 `SqlDialect` 实现类：

| 数据库类型 | 模块名                   | SqlDialect 实现类                                   |
| ---------- | ------------------------ | --------------------------------------------------- |
| MySQL      | ktorm-support-mysql      | me.liuwj.ktorm.support.mysql.MySqlDialect           |
| PostgreSQL | ktorm-support-postgresql | me.liuwj.ktorm.support.postgresql.PostgreSqlDialect |
| Oracle     | ktorm-support-oracle     | me.liuwj.ktorm.support.oracle.OracleDialect         |

现在我们以 MySQL 的 `on duplicate key update` 功能为例，介绍如何在 Ktorm 中启用方言。

MySQL 的 `on duplicate key update` 功能可以在插入记录时，判断是否存在键冲突，如果有冲突则自动执行更新操作，这是标准 SQL 中不支持的用法。要使用这个功能，我们首先需要在项目中添加 ktorm-support-mysql 模块的依赖，如果你使用 Maven：

```
<dependency>
    <groupId>me.liuwj.ktorm</groupId>
    <artifactId>ktorm-support-mysql</artifactId>
    <version>${ktorm.version}</version>
</dependency>
```

或者 gradle：

```groovy
compile "me.liuwj.ktorm:ktorm-support-mysql:${ktorm.version}"
```

添加完依赖后，我们需要修改 `Database.connect` 函数的调用处，这个函数用于创建一个 `Database` 对象，Ktorm 正是用这个对象来连接到数据库。我们需要指定 `dialect` 参数，告诉 Ktorm 需要使用哪个 `SqlDialect` 的实现类：

````kotlin
val db = Database.connect(
    url = "jdbc:mysql://localhost:3306/ktorm", 
    driver = "com.mysql.jdbc.Driver", 
    user = "root", 
    password = "***", 
    dialect = MySqlDialect
)
````

现在，我们就已经启用了 MySQL 的方言，可以使用它的功能了。尝试调用一下 `insertOrUpdate` 函数：

```kotlin
Employees.insertOrUpdate {
    it.id to 1
    it.name to "vince"
    it.job to "engineer"
    it.salary to 1000
    it.hireDate to LocalDate.now()
    it.departmentId to 1

    onDuplicateKey {
        it.salary to it.salary + 900
    }
}
```

生成 SQL：

````sql
insert into t_employee (id, name, job, salary, hire_date, department_id) values (?, ?, ?, ?, ?, ?) 
on duplicate key update t_employee.salary = t_employee.salary + ? 
````

完美！

## 方言功能列表

那么，除了前面出现过的那些，Ktorm 内置的方言还提供了什么功能呢？

下面是 ktorm-support-mysql 模块的功能列表：

- 支持使用 `limit` 函数进行分页，会自动翻译为 MySQL 的 `limit ?, ?` 语句
- 增加了 `bulkInsert` 函数，支持批量插入，与核心库的 `batchInsert` 函数不同，`bulkInsert` 使用 MySQL 的批量插入语法，具有更优的性能
- 增加了 `insertOrUpdate` 函数，支持插入或更新的功能，基于 `on duplicate key update` 语法
- 增加了 `naturalJoin` 函数，支持自然连接，基于 `natural join` 关键字
- 增加了 `jsonContains` 函数，判断 json 数组中是否存在指定元素，基于 `json_contains` 函数
- 增加了 `jsonExtract` 函数，支持从 json 中获取字段，即 MySQL 中的 -> 语法，基于 `json_extract` 函数
- 增加了 `rand`、`ifnull`、`greatest`、`least` 等函数，支持 MySQL 中的同名函数

ktorm-support-postgresql 提供的功能有：

- 支持使用 `limit` 函数进行分页，会自动翻译为 PostgreSQL 中的 `limit ? offset ?` 语句
- 增加了 `ilike` 操作符，用于忽略大小写的字符串匹配，基于 PostgreSQL 的 `ilike` 关键字

ktorm-support-oracle 提供的功能有：

- 支持使用 `limit` 函数进行分页，会自动翻译为 Oracle 中使用 `rownum` 筛选分页的写法

很遗憾地告诉大家，虽然 Ktorm 一直声称支持多种方言，但是实际上除了 MySQL 以外，我们对其他数据库的特殊语法的支持实在是十分有限。这是因为作者本人的精力有限，只能做到支持工作中常用的 MySQL，对于其他数据库纷繁复杂的特殊用法只能暂时把优先级降低。

好在核心库中支持的标准 SQL 已经能够实现我们的大部分需求，在那些方言支持完成之前，只使用标准 SQL 的功能子集也不会影响我们的业务功能。

Ktorm 的设计是开放的，为其增加功能十分容易，我们在前面的章节中就曾经示范过如何对 Ktorm 进行扩展。因此如果你需要的话，完全可以自己编写扩展，同时，欢迎 fork 我们的仓库，提交 PR，我们会将你编写的扩展合并到主分支，让更多的人受益。期待您的贡献！

## 原生 SQL

在极少数情况下，我们会遇到一些特殊的业务，Ktorm 可能暂时无法支持。比如 Ktorm 目前并不支持的复杂查询（如相关子查询），或者某些数据库中的特殊功能（如 SQL Server 中的 cross apply），再或者是对表结构进行操作的 DDL。

为了应对这种场景，Ktorm 提供了直接执行原生 SQL 的方式，这只需要我们写一点 JDBC 的代码。我们需要使用 `Database` 类中的 `useConnection` 函数获取数据库连接，获取到 `Connection` 实例之后，剩下的事情就和其他 JDBC 程序没有任何区别了。下面是一个例子：

```kotlin
val names = db.useConnection { conn ->
    val sql = """
        select name from t_employee
        where department_id = ?
        order by id
    """

    conn.prepareStatement(sql).use { statement ->
        statement.setInt(1, 1)
        statement.executeQuery().iterable().map { it.getString(1) }
    }
}

names.forEach { println(it) }
```

乍一看，上面的代码只是单纯的 JDBC 操作，但是它其实也受益于 Ktorm 提供的便利的支持：

- `useConnection` 函数用于获取或创建连接。如果当前线程已开启事务，在闭包中传入开启了事务的当前连接；否则，新建一个连接，在使用完毕后将其关闭。这正是 Ktorm 内部执行生成的 SQL 时用于获取数据库连接的函数，使用这个函数，可以与 Ktorm 内部执行的 SQL 共享连接池或者事务。
- `iterable` 函数用于将 `ResultSet` 对象包装成 `Iterable`，这样我们就能够使用 for-each 循环对其进行迭代，也可以使用 `map`、`filter` 等扩展函数对结果集进行二次处理。 

>  注意：尽管 Ktorm 对原生 SQL 也提供了方便的支持，但我们并不推荐你使用它，因为这严重违背了 Ktorm 的设计哲学。当你使用原生 SQL 时，Ktorm 原本提供的强类型 DSL 的优势都荡然无存。因此，在你开始考虑使用原生 SQL 解决问题的时候，不妨先思考一下是否真的有必要，一般来说，大部分复杂的 SQL 查询都可以转换为等价的简单多表连接或自连接查询，大部分数据库中特殊关键字或函数也可以通过前面章节中介绍的方法编写扩展来实现。