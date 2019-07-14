---
title: Dialects and Native SQL
lang: en
related_path: zh-cn/dialects-and-native-sql.html
---

# Dialects & Native SQL

It's known that there is a uniform standard for SQL language, but beyond the standard, many databases still have their special features. The core module of Ktorm (ktorm-core) only provides support for standard SQL, if we want to use some special features of a database, we need to support dialects. 

## Enable Dialects

In Ktorm, `SqlDialect` interface is the abstraction of dialects, there is only one function `createSqlFormatter` in the interface now. This function is used to create `SqlFormatter` instances, formatting SQL expressions to strings using its own dialect grammars. 

```kotlin
interface SqlDialect {

    fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter
}
```

Ktorm supports four dialects now, each of them is published as a separated module independent of ktorm-core, and they all provide their own implementation of `SqlDialect`. 

| Database Name | Module Name              | SqlDialect Implementation                           |
| ------------- | ------------------------ | --------------------------------------------------- |
| MySQL         | ktorm-support-mysql      | me.liuwj.ktorm.support.mysql.MySqlDialect           |
| PostgreSQL    | ktorm-support-postgresql | me.liuwj.ktorm.support.postgresql.PostgreSqlDialect |
| Oracle        | ktorm-support-oracle     | me.liuwj.ktorm.support.oracle.OracleDialect         |
| SqlServer     | ktorm-support-sqlserver  | me.liuwj.ktorm.support.sqlserver.SqlServerDialect   |

Now let's take MySQL's `on duplicate key update` feature as an example, learning how to enable dialects in Ktorm. 

This feature can determine if there is a conflict while records are being inserted into databases, and automatically performs updates if any conflict exists, which is not supported by standard SQL. To use this feature, we need to add the dependency of ktorm-support-mysql to our projects. If we are using Maven: 

```
<dependency>
    <groupId>me.liuwj.ktorm</groupId>
    <artifactId>ktorm-support-mysql</artifactId>
    <version>${ktorm.version}</version>
</dependency>
```

Or Gradle: 

```groovy
compile "me.liuwj.ktorm:ktorm-support-mysql:${ktorm.version}"
```

Having the dependency, we also need to modify the calling of the `Database.connect` function, this function is used to create `Database` objects. We need to specify its `dialect` parameter, telling Ktorm which `SqlDialect` implementation should be used. 

```kotlin
val db = Database.connect(
    url = "jdbc:mysql://localhost:3306/ktorm", 
    driver = "com.mysql.jdbc.Driver", 
    user = "root", 
    password = "***", 
    dialect = MySqlDialect()
)
```
> Since version 2.4, Ktorm's dialect modules start following the convention of JDK `ServiceLoader` SPI, so we don't need to specify the `dialect` parameter explicitly anymore while creating `Database` instances. Ktorm auto detects one for us from the classpath. We just need to insure the dialect module exists in the dependencies. 

Now we have enabled MySQL's dialect implementation and all of its features are available. Try to call the `insertOrUpdate` function: 

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

Generated SQL: 

```sql
insert into t_employee (id, name, job, salary, hire_date, department_id) values (?, ?, ?, ?, ?, ?) 
on duplicate key update salary = salary + ? 
```

Perfect！

## Built-in Dialects' Features

Now, let's talk about Ktorm's built-in dialects' features. 

Here is a list of features provided by module ktorm-support-mysql: 

- Support paginations via `limit` function, translating paging expressions into MySQL's `limit ?, ?` statement. 
- Add `bulkInsert` function for bulk insertion, different from `batchInsert` in the core module, it uses MySQL's bulk insertion syntax and the performance is much better. 
- Add `insertOrUpdate` function for data "upsert", based on MySQL's feature of `on duplicate key update`. 
- Add `naturalJoin` function for natural joining, based on `natural join` keyword. 
- Add `jsonContains` function to determine if the specific item exists in a json array, based on the `json_contains` function in MySQL. 
- Add `jsonExtract` function to obtain fields in a json, that's the `->` grammar in MySQL, based on `json_extract` function. 
- Add `match` and `against` functions for fulltext search, based on MySQL's `match ... against ...` syntax. 
- Add other functions such as `rand`, `ifnull`, `greatest`, `least`, `dateDiff`, `replace`, etc, supporting the corresponding functions in MySQL. 

The features of ktorm-support-postgresql are listed below: 

- Support paginations via `limit` function, translating paging expressions into PostgreSQL's `limit ? offset ?` statement. 
- Add `insertOrUpdate` function for data "upsert", based on PostgreSQL's `on conflict (key) do update set` syntax.
- Add `ilike` operator for string matchings ignoring cases, based on PostgreSQL's `ilike` keyword. 

ktorm-support-oracle provides: 

- Support paginations via `limit` function, translating paging expressions into Oracle's paging SQL using `rownum`. 

ktorm-support-sqlserver provides: 

- Support paginations via `limit` function, translating paging expressions into SqlServer's paging SQL using `top` and `row_number() over(...)`. 

Ktorm always claims that we are supporting many dialects, but actually, the support for databases other than MySQL is really not enough. I'm so sorry about that, my time and energy are really limited, so I have to lower the precedence of supporting other databases. 

Fortunately, the standard SQL supported by the core module is enough for most requirements, so there is little influence on our business before the dialects are completed. 

Ktorm's design is open, it's easy to add features to it, and we have learned how to write our own extensions in the former sections. So we can also implement dialects by ourselves if it's really needed. Welcome to fork the repository and send your pull requests to me, I'm glad to check and merge your codes. Looking forward to your contributions!

## Native SQL

In some rare situations, we have to face some special businesses that Ktorm may not be able to support now, such as some complex queries (eg. correlated subqueries), special features of a dialect (eg. SQL Server's cross apply), or DDL that operates the table schemas. 

To solve the problem, Ktorm provides a way for us to execute native SQLs directly. We need to obtain a database connection via `useConnection` function of the `Database` class first, then perform our operations by writing some JDBC codes. Here is an example: 

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

At first glance, there are only boilerplate JDBC codes in the example, but actually, it's also benefited from some convenient functions of Ktorm: 

- `useConnection` function is used to obtain or create connections. If the current thread has opened a transaction, then this transaction's connection will be passed to the closure. Otherwise, Ktorm will pass a new-created connection to the closure and auto close it after it's not useful anymore. Ktorm also uses this function to obtain connections to execute generated SQLs. So, by calling `useConnection`, we can share the transactions or connection pools with Ktorm's internal SQLs. 
- `iterable` function is used to wrap `ResultSet` instances as `Iterable`, then we can iterate the result sets by for-each loops, or process them via extension functions of Kotlin standard lib, such as `map`, `filter`, etc. 

> Note: Although Ktorm provides supports for native SQLs, we don't recommend you to use it, because it violates the design philosophy of Ktorm. Once native SQL is used, we will lose the benefits of the strong typed DSL, so please ensure whether it's really necessary to do that. In general, most complex SQLs can be converted to equivalent simple joining queries, and most special keywords and SQL functions can also be implemented by writing some extensions with Ktorm. 

