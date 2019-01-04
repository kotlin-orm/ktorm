---
title: Spring 支持
lang: zh-cn
related_path: en/spring-support.html
---

# Spring 支持

在 Java 世界里，Spring 是一款著名的框架，对 JavaEE 的发展影响巨大。除了提供 IoC、AOP 等核心功能外，Spring JDBC 模块还提供了对 JDBC 的简易支持，其中包含 JdbcTemplate、事务管理等功能。Ktorm 对 Spring 的支持就基于 Spring JDBC 模块，因此你首先需要确保项目中有它的依赖：

 ````xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>${spring.version}</version>
</dependency>
 ````

或者 Gradle：

````groovy
compile "org.springframework:spring-jdbc:${spring.version}"
````

## 创建 Database 对象

跟其他 Ktorm 程序一样，你需要一个 `Database` 对象才能对数据库进行操作。但这次我们并不使用 `Database.connect` 方法，而是改用 `Database.connectWithSpringSupport`，这个方法需要我们传入一个 `DataSource` 数据源对象：

````kotlin
Database.connectWithSpringSupport(dataSource)
````

一般来说，这就已经足够，使用 `Database.connectWithSpringSupport` 创建的 `Database` 对象就已经支持了 Spring 的各种特性，接下来我们只需要愉快地使用 Ktorm 的 SQL DSL 和 Entity API 就好了。但是，你可能希望将 `Database` 注册为容器中的 bean，以享受 Spring 容器提供的其他好处：

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

是的，最多就只有这么点代码，Ktorm 集成 Spring 简直不能再简单，这只要求你的容器有一个 `DataSource` 的 bean。然而，这个 `DataSource` 怎么创建呢？这已经不是 Ktorm 的职责，相信每个使用 Spring 的读者都能够自己完成，我们这里不再赘述。

## 事务代理

与普通的 `Database` 对象不一样，使用 `Database.connectWithSpringSupport` 方法创建的对象使用了 `SpringManagedTransactionManager` 作为事务管理器。这个事务管理器实际上并没有任何事务管理功能，它把事务的操作都委托给了 Spring 框架：

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

可以看到，它的 `currentTransaction` 属性永远返回 null，它的 `newTransaction` 方法会抛出异常。因此我们无法使用它来创建事务，在需要连接的时候只能使用 `newConnection` 方法从 [TransactionAwareDataSourceProxy](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/datasource/TransactionAwareDataSourceProxy.html) 获取一个代理，通过这个代理，我们的事务将完全被 Spring 接管。

> 注意：开启了 Spring 支持之后，`useTransaction` 方法将不能使用，请改用 Spring 框架提供的 `@Transactional` 注解，否则会抛出异常：java.lang.UnsupportedOperationException: Transaction is managed by Spring, please use Spring's @Transactional annotation instead.

## 异常转换

除了事务管理，Spring JDBC 还提供了异常转换的功能，它能将 JDBC 中抛出的 `SQLException` 统一转换为 [DataAccessException](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/dao/DataAccessException.html) 重新抛出，这个功能有两条好处：

- 使用 unchecked 异常：JDBC 抛出的 `SQLException` 是 checked 异常，对于 Java 用户，这意味着我们需要在许多地方被迫地捕获一些没用的异常。Spring JDBC 统一将他们转换为 `RuntimeException`，有利于代码的整洁。不过 Kotlin 中不存在此问题，因此从这个角度看，此功能意义不大。
- 统一数据访问层的异常体系：在 JDBC 中，使用不同的驱动，其底层抛出的异常类型都不同（虽然它们都是 `SQLException` 的子类），而且 JDBC 中定义的异常体系语义模糊。Spring JDBC 定义了一套成体系的清晰简洁的异常类型，能帮助我们更好地选择感兴趣的异常进行处理，并且屏蔽了不同数据库之间的异常差异。

使用 `Database.connectWithSpringSupport` 方法创建的 `Database` 对象默认启用了 Spring JDBC 的异常转换功能，因此我们可以写出这样的代码：

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

这段代码首先尝试插入一条 ID 为 1 的 `Department`，当捕获到主键冲突的异常时，再改为更新 location 字段 ，从而实现了 `upsert` 的功能，这个例子体现了异常转换功能的好处。