# What's KtOrm?

KtOrm is a lightweight and efficient ORM Framework for Kotlin directly based on pure JDBC. It provides strong typed and flexiable SQL DSL and many convenient extension functions to reduce our duplication of effort. All the SQLs are generated automaticlly, of course.

[![Build Status](https://www.travis-ci.org/vincentlauvlwj/KtOrm.svg?branch=master)](https://www.travis-ci.org/vincentlauvlwj/KtOrm)
[![Maven Central](https://img.shields.io/maven-central/v/me.liuwj.ktorm/ktorm-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22me.liuwj.ktorm%22)
[![Download](https://api.bintray.com/packages/vincentlauvlwj/maven/ktorm-core/images/download.svg)](https://bintray.com/vincentlauvlwj/maven)
[![Apache License 2](https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000)](LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/2cf0d4b81c3546809ad2f83a795c34c2)](https://app.codacy.com/app/vincentlauvlwj/KtOrm?utm_source=github.com&utm_medium=referral&utm_content=vincentlauvlwj/KtOrm&utm_campaign=Badge_Grade_Dashboard)
[![Author](https://img.shields.io/badge/author-vince-yellowgreen.svg)](https://www.liuwj.me)

# Features

 - No configuration files, no xml, lightweight, easy to use.
 - Strong typed SQL DSL, exposing low-level bugs at compile time.
 - Flexiable query, exactly control the generated SQLs as you wish.
 - Extensible design, write your own extensions to support more data types, SQL functions, etc.
 - Dialects supported, MySQL, Oracle, PostgreSQL, or you can write your own dialect by implementing the `SqlDialect` interface.

# Quick Start

KtOrm was deployed to maven central and jcenter, so you just need to a dependency to your `pom.xml` file if you are using maven: 

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

Then create a kotlin object to describe your table schema: 

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

When you run this program, KtOrm will generate a SQL `select * from t_employee`, selecting all employees in the table and printing their names. 

