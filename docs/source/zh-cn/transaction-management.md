---
title: 事务管理
lang: zh-cn
related_path: en/transaction-management.html
---

# 事务管理

事务是许多关系数据库都会提供的一个特性，它保证了一组复杂操作执行结果的强一致性，在一个事务中的操作，要么全部执行成功，要么全部失败。Ktorm 基于 JDBC 对事务提供了方便的支持。

## useTransaction 函数

`Database` 类提供了一个名为 `useTransaction` 的函数，这个函数以一个闭包 `(Transaction) -> T` 作为参数。它在事务中执行闭包中的代码，如果执行成功，返回闭包函数的返回值，如果执行失败，则回滚事务。你可以这样调用它：

```kotlin
database.useTransaction { 
    // 在事务中执行一组操作
}
```

下面是一个使用事务的例子：

```kotlin
class DummyException : Exception()

try {
    database.useTransaction {
        database.insert(Departments) {
            it.name to "administration"
            it.location to "Hong Kong"
        }

        assert(database.sequenceOf(Departments).count() == 3)

        throw DummyException()
    }

} catch (e: DummyException) {
    assert(database.sequenceOf(Departments).count() == 2)
}
```

在执行这段代码之前，`Departments` 表中已经有 2 条记录。这段代码开启了一个事务，在事务中插入了一条记录，插入成功后，我们断言现在表中的记录数是 3，然后抛出一个异常触发事务回滚，在事务回滚后，我们可以看到，表中的记录数又恢复为 2。这个例子可以清晰地看出事务的执行流程。

注意事项：

- 闭包中抛出的任何异常都会触发事务回滚，无论是 checked 还是 unchecked 异常（实际上，checked 异常是 Java 中才有的概念，Kotlin 中并不存在）。
- `useTransaction` 函数是可重入的，因此可以嵌套使用，但是内层并没有开启新的事务，而是与外层共享同一个事务。

## 事务管理器

有时，简单的 `useTransaction` 方法并不能满足你的需求。你可能希望对事务进行更精细的控制，比如指定事务的隔离级别，或者当符合特定条件的异常抛出时不需要回滚事务。这时，你可以使用 `database.transactionManager` 获取事务管理器完成你的操作，下面是一个例子：

```kotlin
val transactionManager = database.transactionManager
val transaction = transactionManager.newTransaction(isolation = TransactionIsolation.READ_COMMITTED)

try {
    // do something...
    transaction.commit()

} catch (e: Throwable) {
    if (someCondition) {
        transaction.commit()
    } else {
        transaction.rollback()
    }

} finally {
    transaction.close()
}
```

`TransactionManager` 是一个接口，它可以有多种不同的实现。一般来说，使用 `Database.connect` 方法创建的 `Database` 对象默认会使用 `JdbcTransactionManager` 实现，这是基于原生 JDBC 提供的功能实现的事务管理器。Ktorm 还提供了 `SpringManagedTransactionManager`，这个实现自身并没有任何事务管理的功能，而是将事务管理委托给了 Spring 框架，具体请参考 [Spring 支持](./spring-support.html)的相关文档。 

