---
title: Spring Support
lang: en
related_path: zh-cn/spring-support.html
---

# Spring Support

Spring is a famous framework that deeply influences the development of JavaEE. In addition to the core functions of Ioc and AOP, the Spring JDBC module also provides convenient support for JDBC, such as JdbcTemplate, transaction management, etc. Ktorm's Spring support is exactly based on this module, so you need to ensure your project contains it's dependency first: 

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>${spring.version}</version>
</dependency>
```

Or Gradle：

```groovy
compile "org.springframework:spring-jdbc:${spring.version}"
```

## Create Database Objects

Just like any other Ktorm programs, we need to create `Database` objects first. But this time, we use the `Database.connectWithSpringSupport` function instead of `Database.connect`, both of them accept a `DataSourcce` parameter: 

```kotlin
Database.connectWithSpringSupport(dataSource)
```

That's enough in general, `Database` objects created by `Database.connectWithSpringSupport` have supported many features of Spring framework, all that's left is to enjoy our SQL DSL and Entity APIs now. 

Maybe you want to register the created object as a Spring bean: 

```kotlin
@Configuration
class KtOrmConfiguration {
    @Autowired
    lateinit var dataSource: DataSource

    @Bean
    fun database(): Database {
        return Database.connectWithSpringSupport(dataSource)
    }
}
```

Yes, that's all. Ktorm's Spring support is easy, the only thing required is a `DataSource` bean in your container. But how can we create the `DataSource` bean? That is not Ktorm's duty anymore, we believe every Spring user can do this by him/her self. 

> If you need a simple example project integrating Ktorm with Spring Boot, click here: [vincentlauvlwj/ktorm-example-spring-boot](https://github.com/vincentlauvlwj/ktorm-example-spring-boot)

## Transaction Delegation

Differently, instances created by `Database.connectWithSpringSupport` function use `SpringManagedTransactionManager` as their transaction manager implementation. This implementation delegates all transaction operations to Spring framework: 

```kotlin
class SpringManagedTransactionManager(val dataSource: DataSource) : TransactionManager {

    val dataSourceProxy = dataSource as? TransactionAwareDataSourceProxy ?: TransactionAwareDataSourceProxy(dataSource)

    override val defaultIsolation get() = TransactionIsolation.REPEATABLE_READ

    override val currentTransaction: Transaction? = null

    override fun newTransaction(isolation: TransactionIsolation): Nothing {
        val msg = "Transaction is managed by Spring, please use Spring's @Transactional annotation instead."
        throw UnsupportedOperationException(msg)
    }

    override fun newConnection(): Connection {
        return dataSourceProxy.connection
    }
}
```

We can see it's `currentTransaction` property always returns null, and it's `newTransaction` function always throws an exception. So we cannot open transactions with it, the only thing we can do is to obtain a connection via `newConnection` function when needed. This function creates a proxied `Connection` instance via [TransactionAwareDataSourceProxy](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/datasource/TransactionAwareDataSourceProxy.html), that's how the transaction delegation works. 

> Note: we cannot use [useTransaction](./transaction-management.html#useTransaction-function) function anymore if the Spring support is enabled, please use Spring's `@Transactional` annotation instead, Otherwise, an exception will be thrown: java.lang.UnsupportedOperationException: Transaction is managed by Spring, please use Spring's @Transactional annotation instead. 

## Exception Translation

Besides of transaction management, Spring JDBC also provides a feature of exception translation, which can convert any `SQLException` thrown by JDBC to [DataAccessException](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/dao/DataAccessException.html) and rethrow it. There are two benefits: 

- **Unchecked exceptions:** `SQLException` is checked, it forces Java users to catch and rethrow them anywhere, even if it's useless. Spring JDBC converts them to unchecked `RuntimeException` to solve this problem, which is helpful to Java users to make their code clean. However, for Kotlin users, this is not so significant. 
- **Unified exception system of data access layer:** in JDBC, different drivers throw different types of exceptions, they are all subclasses of `SQLException`, but the exception system is too complex and ambiguity. Spring JDBC defines a system of clear and simple exception types, which can help us to exactly handle our exceptions interested and hide the deferences among many database drivers.

`Database` instances created by `Database.connectWithSpringSupport` enable the feature of exception translation by default, so we can write code like this: 

```kotlin
try {
    Departments.insert { 
        it.id to 1
        it.name to "tech"
        it.location to "Guangzhou"
    }
} catch (e: DuplicateKeyException) {
    Departments.update { 
        it.location to "Guangzhou"
        where { 
            it.id eq 1
        }
    }
}
```

The code above tries to insert a `Department` with ID 1 first. If a [DuplicateKeyException](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/dao/DuplicateKeyException.html) is thrown, it means that the ID is already existing in the table, so we change to update the location column of the record. This example shows the advantage of exception translation. 

