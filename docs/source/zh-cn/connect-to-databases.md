---
title: 连接数据库
lang: zh-cn
related_path: en/connect-to-databases.html
---

# 连接数据库

要使用 Ktorm，首先你需要连接到你的数据库。Ktorm 提供了一个 `Database` 类用于管理你的数据库连接，一个 `Database` 实例代表了你的一个数据库。要创建 `Database` 对象，你可以调用其伴随对象上的 `connect` 方法，提供数据库连接参数或者一个现成的 `DataSource` 数据源对象。

## 使用 URL 连接到数据库

使用 URL、用户名和密码连接到 MySQL 数据库的代码如下：

````kotlin
val db = Database.connect(
    url = "jdbc:mysql://localhost:3306/ktorm", 
    driver = "com.mysql.jdbc.Driver", 
    user = "root", 
    password = "***"
)
````

很容易就可以猜到这个 `connect` 方法做了什么事情。就像所有的 JDBC 的样板代码一样，Ktorm 首先使用了 `Class.forName` 方法加载 MySQL 数据库驱动，然后再调用 `DriverManager.getConnection` 方法，使用你所提供的参数获取一个数据库连接。

> 当然，Ktorm 并没有在一开始就调用 `DriverManager.getConnection` 获取连接，而是在需要的时候（比如执行一条查询或操作数据的 SQL）才获取一个新连接，使用完之后再马上关闭。因此，使用此方法创建的 `Database` 对象没有任何复用连接的行为，频繁地创建连接会造成不小的性能消耗，在生产环境中，强烈建议使用数据库连接池。

## 使用连接池

Ktorm 并不限制你使用哪款连接池，你可以使用你喜欢的任何实现，比如 DBCP、C3P0 或者 Druid。`connect` 方法提供了一个以 `DataSource` 为参数的重载，你只需要把连接池对象传递给此方法即可：

````kotlin
val dataSource = SingleConnectionDataSource() // 任何 DataSource 的实现都可以
val db = Database.connect(dataSource)
````

这样，Ktorm 在需要数据库连接的时候，就会从连接池中获取一个连接，使用完后再归还到池中，避免了频繁创建连接的性能损耗。

> 使用连接池对大多数场景都是适用的而且高效的，我们强烈建议你使用这种方式来管理数据库连接。

## 手动管理连接

如果你没有使用任何连接池，但是又想自己管理连接的生命周期，该如何做呢？比如，在某些业务场景中，你的整个 App 的生命周期中只需要一个数据库连接，在 App 启动时，创建这个连接，在进程退出时，关闭连接。`connect` 方法提供了另一个灵活的重载，它接收一个 `() -> Connection` 作为参数，这是一个返回值为 `Connection` 的函数。下面这段代码就使用了 `connect` 方法的这个重载版本：

````kotlin
// App 启动时，建立连接
val conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ktorm")

Runtime.getRuntime().addShutdownHook(
    thread(start = false) {
        // 进程退出时，关闭连接
        conn.close()
    }
)

val db = Database.connect {
    object : Connection by conn {
        override fun close() {
            // 重写 close 方法，保持连接不关闭
        }
    }
}
````

在这里，我们给 `connect` 方法传递了一个闭包函数，一般来说，我们应该在这个闭包中创建一个连接。但是 `Connection` 是一个接口，我们可以传递一个代理对象而不是真正的 `Connection` 实例，这个代理对象将 `close` 方法重写为空操作。这样，当 Ktorm 需要连接的时候，调用这个闭包函数获取到的始终都是同一个连接对象，当使用完毕后，连接也仍然不会关闭。

## 全局对象与多数据库支持

`Database.connect` 方法会创建一个 `Database` 对象并返回，如果你需要的话，可以定义一个变量来保存这个返回值。但是通常来说，你没必要这么做，因为 Ktorm 会自己记录最后一次创建的 `Database` 对象，在需要的时候，使用 `Database.global` 获取这个对象进行操作。

````kotlin
Database.global.useConnection { conn -> 
    // 使用连接进行操作...
}
````

有时候，我们需要在一个 App 中操作多个数据库，这时就需要创建多个 `Database` 对象，在执行具体的操作时，指定你要使用哪个数据库。

```kotlin
val mysql = Database.connect("jdbc:mysql://localhost:3306/ktorm", driver = "com.mysql.jdbc.Driver")
val h2 = Database.connect("jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

mysql {
    assert(Database.global === mysql)
    // 操作 MySQL 数据库
}

h2 {
    assert(Database.global === h2)
    // 操作 H2 数据库
}
```

上面的代码先后使用 `connect` 方法连接上了两个数据库，并示范了如何在不同的数据库之间进行切换。因为 `Database` 重载了 `invoke` 操作符，接收一个闭包函数作为参数，在这个闭包函数中的作用域中，使用 `Database.global` 获取到的数据库对象会变成当前对象，因此达到了切换数据库的目的。

## 日志输出

Ktorm 在运行过程中，会在日志中输出其内部的一些有用的信息，比如生成的 SQL、SQL 执行参数、返回的结果等。如果你希望能够看到这些信息，监控 Ktorm 的运行过程，就需要对 Ktorm 的日志输出进行配置。

为了不依赖任何第三方的日志框架，Ktorm 自己对日志输出做了一个十分简单的抽象层，其中只有两个核心的类：

- `LogLevel`：这是一个枚举类，与大部分日志框架类似，Ktorm 定义了五种日志级别，它们分别是 `TRACE`、`DEBUG`、`INFO`、`WARN`、`ERROR`。
- `Logger`：这是一个接口，它里面定义了用于输出日志的各种方法。

其中，`Logger` 接口有如下实现：

| 类名                 | 功能                                        |
| -------------------- | ------------------------------------------- |
| ConsoleLogger        | 将日志输出到控制台                          |
| JdkLoggerAdapter     | 将日志委托给 java.util.logging 中的日志框架 |
| Slf4jLoggerAdapter   | 将日志委托给 slf4j 日志门面                 |
| CommonsLoggerAdapter | 将日志委托给 Apache Commons 日志门面        |
| AndroidLoggerAdapter | 将日志委托给 android.util.Log               |

默认情况下，在创建 `Database` 对象时，Ktorm 会从 classpath 中自动检测出我们正在使用的日志框架，并将 Ktorm 产生的日志委托给它。如果你想手动指定一个日志框架，则需要从上面的实现类中选择一个，用于指定 `logger` 参数。下面的代码使用最简单的 `ConsoleLogger`，输出级别大于等于 `INFO` 的日志到控制台。

```kotlin
val db = Database.connect(
    url = "jdbc:mysql://localhost:3306/ktorm", 
    driver = "com.mysql.jdbc.Driver", 
    user = "root", 
    password = "***",
    logger = ConsoleLogger(threshold = LogLevel.INFO)
)
```

Ktorm 使用不同的级别输出不同的日志，具体规则是：

- 生成的 SQL 和它们的执行参数使用 `DEBUG` 级别输出，所以如果你想看到 SQL，你应该配置你的日志框架，以启用 `DEBUG` 日志。
- 查询返回的每一个实体对象的数据使用 `TRACE` 级别输出，如果你想看到这些数据，你应该配置日志框架启用 `TRACE` 日志。
- 除此之外，启动信息使用 `INFO` 输出、警告使用 `WARN` 输出、异常使用 `ERROR` 输出，这些级别的日志最好都应该开启。

