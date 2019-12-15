---
title: Data Manipulation
lang: en
related_path: zh-cn/dml.html
---

# Data Manipulation

Ktorm not only provides SQL DSL for query and joining, but it also supports data manipulation conveniently. Let's talk about its DML DSL now. 

## Insert

Ktorm uses an extension function `insert` of `Database` class to support data insertion, the signature of which is given as follows: 

```kotlin
fun <T : BaseTable<*>> Database.insert(table: T, block: AssignmentsBuilder.(T) -> Unit): Int
```

The function accepts a closure as its parameter in which we configure our insertion columns and values. After the insertion completes, an int number of affected records will be returned. Usage: 

```kotlin
database.insert(Employees) {
    it.name to "jerry"
    it.job to "trainee"
    it.managerId to 1
    it.hireDate to LocalDate.now()
    it.salary to 50
    it.departmentId to 1
}
```

Generated SQL: 

```sql
insert into t_employee (name, job, manager_id, hire_date, salary, department_id) values (?, ?, ?, ?, ?, ?) 
```

Here, we use `it.name to "jerry"` to set the name to jerry in the closure, do you know how it works? 

It can be seen that the type of the closure is `AssignmentsBuilder.(T) -> Unit`, which is a function that accepts a parameter `T`, the table object specified by the first parameter of `insert`, that's why we can use `it` to access the current table and its columns in the closure. Moreover, the closure is also an extension function of `AssignmentsBuilder`, so in the scope of the closure, `this` reference is changed to an `AssignmentsBuilder` instance, that's why we can call its member function `to` there. Yes, this `to` function is a member function of `AssignmentsBuilder`, but not the `to` function used to create `Pair` instances of Kotlin standard lib. 

Here is the source code of `AssignmentsBuilder`, we can see that the `to` function doesn't return any values, it just save the current column and its value into a `MutableList`.

```kotlin
@KtormDsl
open class AssignmentsBuilder(
    private val assignments: MutableList<ColumnAssignmentExpression<*>>
) {
    infix fun <C : Any> Column<C>.to(expr: ColumnDeclaring<C>) {
        assignments += ColumnAssignmentExpression(asExpression(), expr.asExpression())
    }

    infix fun <C : Any> Column<C>.to(argument: C?) {
        this to wrapArgument(argument)
    }
    
    @JvmName("toAny")
    infix fun Column<*>.to(argument: Any?) {
        checkAssignableFrom(argument)
        this to argument
    }
}
```

> Because the member function `to` doesn't return any values, we are not likely to mix it with the `kotlin.to`  of Kotlin standard lib. If you really want to use `kotlin.to` in the closure, but found it's resolved to `AssignmentsBuilder.to` and compiler error occurs. We recommend you to refactor your code and move the calling of `kotlin.to` outside the closure. 

Sometimes we may use auto-increment keys in our tables, we may need to obtain the auto-generated keys from databases after records are inserted. This time we can use `insertAndGenerateKey` function, the usage of which is similar to `insert`, but differently, it doesn't return the affected record numbers anymore, but returns the auto-generated keys instead. 

```kotlin
val id = database.insertAndGenerateKey(Employees) {
    it.name to "jerry"
    it.job to "trainee"
    it.managerId to 1
    it.hireDate to LocalDate.now()
    it.salary to 50
    it.departmentId to 1
}
```

Sometimes we may need to insert a large number of records in one time, and the performance of calling `insert` function in a loop may be intolerable. Ktorm provides a `batchInsert` function that can improve the performance of batch insertion, it's implemented based on `executeBatch` of JDBC. 

```kotlin
database.batchInsert(Employees) {
    item {
        it.name to "jerry"
        it.job to "trainee"
        it.managerId to 1
        it.hireDate to LocalDate.now()
        it.salary to 50
        it.departmentId to 1
    }
    item {
        it.name to "linda"
        it.job to "assistant"
        it.managerId to 3
        it.hireDate to LocalDate.now()
        it.salary to 100
        it.departmentId to 2
    }
}
```

The `batchInsert` function also accepts a closure as its parameter, the type of which is `BatchInsertStatementBluilder<T>.() -> Unit`, an extension function of `BatchInsertStatementBuilder`. The `item` is actually a member function in `BatchInsertStatementBuilder`, we use this function to configure every record of the batch insertion. After the batch insertion completes, an `IntArray` will be returned, that's the affected record numbers of each sub-operation. 

Sometimes, we may need to transfer data from a table to another one. Ktorm also provides an `insertTo` function, that's an extension function of `Query` class, used to insert the query's results into a specific table. Comparing to obtaining query results first and insert them via `batchInsert`, the `insertTo` function just execute one SQL, the performance is much better. 

```kotlin
database
    .from(Departments)
    .select(Departments.name, Departments.location)
    .where { Departments.id eq 1 }
    .insertTo(Departments, Departments.name, Departments.location)
```

Generated SQL: 

```sql
insert into t_department (name, location) 
select t_department.name as t_department_name, t_department.location as t_department_location 
from t_department 
where t_department.id = ? 
```

## Update

Ktorm uses an extension function `update` of `Database` class to support data update, the signature of which is given as follows: 

```kotlin
fun <T : BaseTable<*>> Database.update(table: T, block: UpdateStatementBuilder.(T) -> Unit): Int
```

Similar to the `insert` function, it also accepts a closure as its parameter and returns the affected record number after the update completes. The closure's type is `UpdateStatementBuilder.(T) -> Unit`, in which `UpdateStatementBuilder` is a subclass of `AssignmentsBuilder`, so we can still use `it.name to "jerry"` to set the name to jerry. Differently, `UpdateStatementBuilder` provides an additional function `where`, that's used to specify our update conditions. Usage: 

```kotlin
database.update(Employees) {
    it.job to "engineer"
    it.managerId to null
    it.salary to 100

    where {
        it.id eq 2
    }
}
```

Generated SQL: 

```sql
update t_employee set job = ?, manager_id = ?, salary = ? where id = ? 
```

It is worth mentioning that we can not only put a column value at the right side of the `to` function, but an expression is also OK. It means that a column can be updated to a specific value or a result of any complex expressions. We can use this feature to do something special, for instance, increasing someone's salary: 

```kotlin
database.update(Employees) {
    it.salary to it.salary + 100
    where { it.id eq 1 }
}
```

Generated SQL: 

```sql
update t_employee set salary = salary + ? where id = ? 
```

Sometimes we may need to execute a large number of updates in one time, and the performance of calling `update` function in a loop may be intolerable. This time, we can use `batchUpdate` function, that can improve the performance of batch updates. Similar to `batchInsert` function, it's also implemented based on `executeBatch` of JDBC. The operation below shows how to use `batchUpdate` to update specific departments' location to "Hong Kong". We can see that the usage is similar to `batchInsert`, the only difference is we need to specify the update conditions by `where` function. 

```kotlin
database.batchUpdate(Departments) {
    for (i in 1..2) {
        item {
            it.location to "Hong Kong"
            where {
                it.id eq i
            }
        }
    }
}
```

## Delete

Ktorm uses an extension function `delete` of `Database` class to support data deletion, the signature of which is given as follows: 

```kotlin
fun <T : BaseTable<*>> Database.delete(table: T, predicate: (T) -> ColumnDeclaring<Boolean>): Int
```

The `delete` function accepts a closure as its parameter, in which we need to specify our conditions. After the deletion completes, the affected record number will be returned. The closure accepts a parameter of type `T`, which is actually the current table object, so we can use `it` to access the current table in the closure. The usage is very simple: 

```kotlin
database.delete(Employees) { it.id eq 4 }
```

This line of code will delete the employee whose id is 4. 