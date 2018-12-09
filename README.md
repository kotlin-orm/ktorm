<p align="center">
    <a href="https://ktorm.liuwj.me">
        <img src="logo.png" alt="Ktorm" width="300" />
    </a>
</p>
<p align="center">
    <a href="https://www.travis-ci.org/vincentlauvlwj/KtOrm">
        <img src="https://www.travis-ci.org/vincentlauvlwj/KtOrm.svg?branch=master" alt="Build Status" />
    </a>
    <a href="https://search.maven.org/search?q=g:%22me.liuwj.ktorm%22">
        <img src="https://img.shields.io/maven-central/v/me.liuwj.ktorm/ktorm-core.svg?label=Maven%20Central" alt="Maven Central" />
    </a>
    <a href="https://bintray.com/vincentlauvlwj/maven">
        <img src="https://api.bintray.com/packages/vincentlauvlwj/maven/ktorm-core/images/download.svg" alt="Download" />
    </a>
    <a href="LICENSE">
        <img src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000" alt="Apache License 2" />
    </a>
    <a href="https://app.codacy.com/app/vincentlauvlwj/KtOrm?utm_source=github.com&utm_medium=referral&utm_content=vincentlauvlwj/KtOrm&utm_campaign=Badge_Grade_Dashboard">
        <img src="https://api.codacy.com/project/badge/Grade/2cf0d4b81c3546809ad2f83a795c34c2" alt="Codacy Badge" />
    </a>
    <a href="https://www.liuwj.me">
        <img src="https://img.shields.io/badge/author-vince-yellowgreen.svg" alt="Author" />
    </a>
</p>

# What's Ktorm?

Ktorm is a lightweight and efficient ORM Framework for Kotlin directly based on pure JDBC. It provides strong typed and flexible SQL DSL and many convenient extension functions to reduce our duplicated effort on database operations. All the SQLs, of course, are generated automatically. For more documentation, go to our site: [https://ktorm.liuwj.me](https://ktorm.liuwj.me).

:us: English | :cn: [简体中文](README_cn.md)

# Features

 - No configuration files, no xml, lightweight, easy to use.
 - Strong typed SQL DSL, exposing low-level bugs at compile time.
 - Flexiable query, exactly control the generated SQLs as you wish.
 - Extensible design, write your own extensions to support more data types, SQL functions, etc.
 - Dialects supports, MySQL, Oracle, PostgreSQL, or you can write your own dialect support by implementing the `SqlDialect` interface.

# Quick Start

Ktorm was deployed to maven central and jcenter, so you just need to add a dependency to your `pom.xml` file if you are using maven: 

````xml
<dependency>
    <groupId>me.liuwj.ktorm</groupId>
    <artifactId>ktorm-core</artifactId>
    <version>${ktorm.version}</version>
</dependency>
````

Or Gradle: 

````groovy
compile "me.liuwj.ktorm:ktorm-core:${ktorm.version}"
````

Then create a Kotlin object to describe your table schema: 

````kotlin
object Employees : Table<Nothing>("t_employee") {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val job by varchar("job")
    val managerId by int("manager_id")
    val hireDate by date("hire_date")
    val salary by long("salary")
    val departmentId by int("department_id")
}
````

Now connect to your database and write a simple query: 

````kotlin
fun main() {
    Database.connect("jdbc:mysql://localhost:3306/ktorm", driver = "com.mysql.jdbc.Driver")

    for (row in Employees.select()) {
        println(row[Employees.name])
    }
}
````

When you run this program, Ktorm will generate a SQL `select * from t_employee`, selecting all employees in the table and printing their names. 

