---
title: Transaction Management
lang: en
related_path: zh-cn/transaction-management.html
---

# Transaction Management

Database transactions allow units of work to recover correctly from failures and keep a database consistent even in cases of system failure, when execution stops and many operations upon a database remain uncompleted, with unclear status. Ktorm provides convenient support for transactions based on JDBC.

## useTransaction function

The `Database` class provides a `useTransaction` function which accepts a parameter of type `(Transaction) -> T`, a function that accepts a `Transaction` and returns a result `T`. `useTransaction` runs the provided code in a transaction and returns its result if the execution succeeds, otherwise, if the execution fails, the transaction will be rollback. You can use the function like this: 

```kotlin
database.useTransaction { 
    // Do something in the transaction. 
}
```

Here is an example: 

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

There have been 2 records in the `Departments` table before. The code above opens a transaction and inserts a record in the transaction. After the insertion, we assert that there are 3 records now, then throws an exception to trigger a rollback. Finally, after the rollback, we can see there are still 2 records there. This shows the execution process clearly. 

Note: 

- Any exceptions thrown in the closure can trigger a rollback, no matter the exception is checked or unchecked. Actually, *checked exception* is a Java-only concept, there is no such thing in Kotlin. 
- `useTransaction` is reentrant, so it can be called nested. However, the inner calls don't open new transactions but share the same ones with outers. 

## Transaction Manager

Sometimes, the simple `useTransaction` function may not satisfy you requirements. You may want to control your transactions more precisely, like setting the isolation level of them, or rollinkg back them only when some special exceptions thrown in some conditions. At this time, you can obtain a `TransactionManager` via `database.transactionManager`, here is an example: 

```kotlin
val transactionManager = database.transactionManager
val transaction = transactionManager.newTransaction(isolation = TransactionIsolation.READ_COMMITTED)
var throwable: Throwable? = null

try {
    // do something...
} catch (e: Throwable) {
    throwable = e
    throw e
} finally {
    try {
        if (shouldRollback(throwable)) transaction.rollback() else transaction.commit()
    } finally {
        transaction.close()
    }
}
```

`TransactionManager` is an interface that has several implementations. In general, `Database` objects created by `Database.connect` function uses the `JdbcTransactionManager` implementation by default, this implementation supports transaction management directly based on raw JDBC. Ktorm also provides a `SpringManagedTransactionManager` implementation which doesn't support transaction management by itself but delegates it to Spring framework, refer to [Spring Support](./spring-support.html) for more details. 

