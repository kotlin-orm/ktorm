---
title: Connect to Databases
lang: en
related_path: zh-cn/connect-to-databases.html
---

# Connect to Databases

To use Ktorm, you need to connect to your databases first. Ktorm provides a `Database` class to manage your database connections. The `Database` class is an abstraction of real databases. To create an instance of it, you can call the `connect` function on its companion object, providing your connection arguments or an existing `DataSource` object. 

## Connect with a URL

The code connecting to a MySQL database with a URL, user name and password: 

````kotlin
val db = Database.connect(
    url = "jdbc:mysql://localhost:3306/ktorm", 
    driver = "com.mysql.jdbc.Driver", 
    user = "root", 
    password = "***"
)
````

Easy to know what we do in the `connect` function. Just like any JDBC boilerplate code, Ktorm loads the MySQL database driver by `Class.forName` method first, then calls `DriverManager.getConnection` with your arguments to obtain a connection. 

> Of course, Ktorm doesn't call `DriverManager.getConnection` in the beginning. Instead, we obtain connections only when it's really needed (such as executing a SQL), then close them after they are not useful anymore. Therefore, `Database` objects created by this way won't reuse any connections, creating connections frequently can lead to huge performance costs. It's highly recommended to use connection pools in your production environment. 

## Connect with a Pool

Ktorm doesn't limit you, you can use any connection pool you like, such as DBCP, C3P0 or Druid. The `connect` function provides an overloaded edition which accepts a `DataSource` parameter, you just need to create a `DataSource` object and call that function with it: 

````kotlin
val dataSource = SingleConnectionDataSource() // Any DataSource implementation is OK. 
val db = Database.connect(dataSource)
````

Now, Ktorm will obtain connections from the `DataSource` when necessary, then return them to the pool after they are not useful. This avoids the performance costs of frequent connection creation. 

> Connection pools are applicative and effective in most cases, we highly recommend you manage your connections in this way. 

## Connect Manually

If you want to manage connections' lifecycle manually by yourself without using any connection pools, how to do that with Ktorm? For example, in some special business cases, there is only one connection needed in our whole App's lifecycle. The connection is created when the App starts and closed when the process exits. The `connect` function provides another flexible overloaded edition which accepts a parameter of type `() -> Connection`, a function that returns a `Connection`. The code below shows how to use it: 

````kotlin
// Create a connection when the App starts. 
val conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ktorm")

Runtime.getRuntime().addShutdownHook(
    thread(start = false) {
        // Close the connection when the process exits. 
        conn.close()
    }
)

val db = Database.connect {
    object : Connection by conn {
        override fun close() {
            // Override the close function and do nothing, keep the connection open. 
        }
    }
}
````

Here, we call the `connect` function with a closure in which we should generally create a connection. However, the `Connection` is an interface, this allows us to return a proxy object to Ktorm instead of a real connection. The proxy overrides the `close` function as a no-op. In this way, Ktorm will always get the same connection object by calling the closure, and the connection is never closed in the whole App's lifecycle. 

## Global Object & Multi Databases

The `Database.connect` function returns a new-created `Database` object, you can define a variable to save the returned value if needed. But generally, it's not necessary to do that, because Ktorm will save the latest created `Database` instance automatically, then obtain it via `Database.global` when needed. 

````kotlin
Database.global.useConnection { conn -> 
    // Do something with the connection...
}
````

Sometimes, we have to operate many databases in one App, so it's needed to create many `Database` instances and choose one while performing our database specific operations. 

```kotlin
val mysql = Database.connect("jdbc:mysql://localhost:3306/ktorm", driver = "com.mysql.jdbc.Driver")
val h2 = Database.connect("jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

mysql {
    assert(Database.global === mysql)
    // Use MySQL database
}

h2 {
    assert(Database.global === h2)
    // Use H2 database
}
```

The code above connects to two different databases using `Database.connect` and shows how to switch between them. The `Database` class overloads `invoke` operator with a closure function as the parameter. In the scope of the closure, `Database.global` will always return the current database which is exactly the one calling the `invoke` operator. This is the way Ktorm supports multi-databases. 

## Logging

During the running process, Ktorm outputs some useful internal information to the logs, such as generated SQLs, execution parameters, returned results, etc. If you want to see this information and monitor the running process, you may need to configure the logging output. 

In order not to depend on any third-party logging frameworks, Ktorm adds a simple abstraction layer for logging, in which there are only two core classes: 

- `LogLevel`: similar to most of the logging frameworks, it's an enum class that contains five logging levels: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`.
- `Logger`: an interface that provides some methods used to output logs.

Ktorm provides many implementations for the `Logger` interface: 

| Class Name           | Description                                  |
| -------------------- | -------------------------------------------- |
| ConsoleLogger        | Output logs to the console                   |
| JdkLoggerAdapter     | Delegate logs to java.util.logging           |
| Slf4jLoggerAdapter   | Delegate logs to slf4j framework             |
| CommonsLoggerAdapter | Delegate logs to  Apache commons logging lib |
| AndroidLoggerAdapter | Delegate logs to android.util.Log            |

By default, Ktorm auto detects the logging framework we are using from the classpath while creating `Database` instances, and delegates the logs to it. If you want to output logs using a specific logging framework, you can choose an adapter implementation above and set it to the `logger` parameter. The code below uses a `ConsoleLogger`, and print logs whose level is greater or equal `INFO` to the console. 

```kotlin
val db = Database.connect(
    url = "jdbc:mysql://localhost:3306/ktorm", 
    driver = "com.mysql.jdbc.Driver", 
    user = "root", 
    password = "***",
    logger = ConsoleLogger(threshold = LogLevel.INFO)
)
```

Ktorm prints different logs at different levels: 

 - Generated SQLs and their execution arguments are printed at `DEBUG` level, so if you want to see the SQLs, you should configure your logging framework to enable the `DEBUG` level.
 - Detailed data of every returned entity object are printed at `TRACE` level, if you want to see them, you should configure your framework to enable `TRACE`.
 - Besides, start-up messages are printed at `INFO` level, warnings are printed at `WARN` level, and exceptions are printed at `ERROR` level. These levels should be enabled by default.


