---
title: Query
lang: en
related_path: zh-cn/query.html
---

# Query

In former chapters, we have created a simple query, it selected all employees in the table and printed their names. Let's start from this query: 

```kotlin
for (row in Employees.select()) {
    println(row[Employees.name])
}
```

## Query Objects

In the example above, we get a `Query` from `select` function and iterates it with a for-each loop. Are there any other operations supported by `Query` besides iteration? Let's learn the `Query` class's definition first: 

```kotlin
data class Query(val expression: QueryExpression) : Iterable<QueryRowSet> {
    
    val sql: String by lazy { ... }

    val rowSet: QueryRowSet by lazy { ... }

    val totalRecords: Int by lazy { ... }

    override fun iterator(): Iterator<QueryRowSet> {
        return rowSet.iterator()
    }
}
```

`Query` is an abstraction of query operations and the core of Ktorm's query DSL. Its constructor accepts a parameter of type `QueryExpression`, which is the abstract representation of the executing SQL statement. Usually, we don't use the constructor to create `Query` objects but use the `Table.select` extension function instead. 

`Query` implements the `Iterable<QueryRowSet>` interface, that's why we can iterate the results by a for-each loop. Moreover, there are many extension functions for `Iterable` in Kotlin standard lib, so we can also process the results via functions such as map, filter, reduce, etc. Here is an example: 

```kotlin
data class Emp(val id: Int?, val name: String?, val salary: Long?)

Employees.select()
    .map { row -> Emp(row[Employees.id], row[Employees.name], row[Employees.salary]) }
    .filter { it.salary > 1000 }
    .sortedByDescending { it.salary }
    .forEach { println(it.name) }
```

Actually, in the example above, all the work Ktorm does is just to generate a simple SQL `select * from t_employee`. The following `.map { }.filter { }.sortedByDescending { }.forEach { }` are all extension functions in Kotlin standard lib. That's the advantages of implementing `Iterable` interface. 

There are some other useful properties in the `Query` class: 

- **sql:** Return the generated SQL string of this query, can be used to ensure whether the generated SQL is expected while debugging. 
- **rowSet:** Return the `ResultSet` object of this query, lazy initialized after first access, obtained from database by executing the generated SQL. 
- **totalRecords:** If the query doesn't limits the results via *offset* and *limit*, return the size of the result set. Or if it does, return the total record count of the query ignoring the *offset* and *limit* parameters. Ktorm provides this property to support pagination, we can calculate page count through dividing it by our page size.

## Obtain Query Results

Every JDBC user knows how to obtain query results from a `ResultSet`. We need a loop to iterate rows in it, calling the getter functions (such as `getInt`, `getString`, etc) to obtain the data of the specific column. A typical usage is based on a while loop: `while (rs.netxt())  { ... } `. Moreover, after finishing these works, we also have to call `close` function to release the resources. 

That's not so hard, but it's still easy to get bored with writing those duplicated codes. We have known that `Query` class implemented `Iterable` interface, that provided another possibility for us. We can iterate results sets by a for-each loop, or process them via extension functions like map, filter, etc, just like the previous example. 

You might have noticed that the return type of `Query.rowSet` was not a normal `ResultSet`, but a `QueryRowSet` instead. That's a special implementation provided by Ktorm, different from normal result sets, it provides additional features: 

- **Available offline:** It's connection independent, it remains available after the connection closed, and it's not necessary to be closed after being used. Ktorm creates `QueryRowSet` objects with all data being retrieved from the result set into memory, so we just need to wait for GC to collect them after they are not useful. 
- **Indexed access operator:** `QueryRowSet` overloads the [indexed access operator](https://kotlinlang.org/docs/reference/operator-overloading.html#indexed), so we can use square brackets `[]` to obtain the value by giving a specific `Column` instance. It's less error prone by the benefit of the compiler's static checking. Also, we can still use `getXxx` functions in the `ResultSet` to obtain our results by labels or column indices. 

Obtain results via indexed access operator: 

```kotlin
for (row in Employees.select()) {
    val id: Int? = row[Employees.id]
    val name: String? = row[Employees.name]
    val salary: Long? = row[Employees.salary]

    println("$id, $name, $salary")
}
```

We can see that if the column's type is `Column<Int>`, then the result's type is `Int?`, and if the column's type is `Column<String>`, the result type will be `String?`. The types are not limited to the return types of `getXxx` functions in `ResultSet`, they can be any types corresponding to the column instances instead. And additionally, there can be some necessary conversions on data, that depends on the column's implementation of [SqlType](./schema-definition.html#SqlType). 

## select

All queries in SQL start with a select keyword. Similarly, All queries in Ktorm start with a `select` function call. `select` is an extension function of `Table` class, so every query in Ktorm comes from a `Table` object. Here is the signature of this function: 

```kotlin
fun Table<*>.select(vararg columns: ColumnDeclaring<*>): Query
```

We can see it accepts any number of columns and returns a new-created `Query` object which selects specific columns from the current table. The example below obtains employees' ids and names via the `select` function: 

```kotlin
val query = Employees.select(Employees.id, Employees.name)
```

Now we have a `Query` object, but no SQL has been executed yet. We can chaining call `where` or other extension functions to modify it, or iterate it by a for-each loop or any other way. While the query object is iterated, Ktorm will execute a generated SQL, then we can obtain results in the way we discussed above. The generated SQL is given as follows: 

```sql
select t_employee.id as t_employee_id, t_employee.name as t_employee_name 
from t_employee 
```

Try to remove arguments passed to the `select` function: 

```kotlin
val query = Employees.select()
```

Then the generated SQL will be changed to `select *`: 

```sql
select * 
from t_employee 
```

You might have noticed that the parameter type of `select` function was `ColumnDeclaring` instead of `Column`. So we can not only select normal columns from a table, but complex expressions and aggregation functions are also supported. For instance, if we want to know the salary difference between the max and the min in a company, we can write a query like this: 

```kotlin
Employees
    .select(max(Employees.salary) - min(Employees.salary))
    .forEach { row -> println(row.getLong(1)) }
```

Here we use two aggregation functions, `max` and `min`, the return types of which are both `AggregateExpression`. Then subtracting the max by the min, we finally have a `BinaryExpression`, which is a subclass of `ColumnDeclaring`, so we can pass it to the `select` function. Generated SQL: 

```sql
select max(t_employee.salary) - min(t_employee.salary) 
from t_employee 
```

We can see that the generated SQL is highly corresponding to our Kotlin code. This benefits from Kotlin's excellent features. Ktorm provides many overloaded operators, that's why we can use the minus operator in the query above. Because of operator overloading, the minus operator here doesn't perform an actual subtraction but being translated to a minus operator in SQL and executed in our database. In the section of [Operators](./operators.html), we will learn more about Ktorm's operators. 

> Small regret: Although the `select` function supports complex expressions, the `QueryRowSet` doesn't. So while obtaining results from a `QueryRowSet`, we can not use index access operators `[]` here. The only thing we can use is `getXxx` functions extended from `ResultSet`, obtaining results by labels or column indices. 

## selectDistinct

`selectDistinct` is also an extension function of `Table` class. Just as its name implies, it will be translated to a `select distinct` statement in SQL, and its usage is totally the same with `select` function, so we won't repeat it. 

## where

`where` is also an extension function of `Table` class, let's learn its signature first: 

```kotlin
inline fun Query.where(block: () -> ColumnDeclaring<Boolean>): Query
```

It's an inline function that accepts a parameter of type `() -> ColumnDeclaring<Boolean>`, which is a closure function that returns a `ColumnDeclaring<Boolean>` as our filter condition. The `where` function creates a new `Query` object with all properties being copied from the current query, but applying a new filter condition, the return value of the closure. Typical usage: 

```kotlin
val query = Employees
    .select(Employees.salary)
    .where { (Employees.departmentId eq 1) and (Employees.name like "%vince%") }
```

Easy to know that the query obtains the salary of an employee named vince in department 1. The generated SQL is easy too: 

```sql
select t_employee.salary as t_employee_salary 
from t_employee 
where (t_employee.department_id = ?) and (t_employee.name like ?) 
```

We can return any filter conditions in `where` closure, here we constructed one by operators `eq`, `and` and  `like`. Kotlin provides an infix keyword, functions marked with it can be called using the [infix notation](https://kotlinlang.org/docs/reference/functions.html#infix-notation) (omitting the dot and the parentheses for the call), that's how these operators work.  

> Ktorm's built-in operators can be divided into two groups: those that is implemented by operator overloading, such as basic arithmetic operators; and those that is based on infix notations, such as `and`, `or`, `eq`, `like`, `greater`, `less`, etc. 

Sometimes, we need a variable number of filter conditions in our queries, those conditions are combined with `and` or `or` operator and each of them can be enabled or disabled depending on different conditions. To meet this requirement, many ORM frameworks provide features like *dynamic query*, such as the `<if>` tag of MyBatis. However, this is not a problem at all in Ktorm, because queries in Ktorm are pure Kotlin codes, which is natively *dynamic*. Let's learn the query below: 

```kotlin
val query = Employees
    .select(Employees.salary)
    .where {
        val conditions = ArrayList<ColumnDeclaring<Boolean>>()

        if (departmentId != null) {
            conditions += Employees.departmentId eq departmentId
        }
        if (managerId != null) {
            conditions += Employees.managerId eq managerId
        }
        if (name != null) {
            conditions += Employees.name like "%$name%"
        }

        conditions.reduce { a, b -> a and b }
    }
```

Here, we create an `ArrayList` to hold filter conditions first, then add different conditions to the list depending on whether the specific parameters are null or not, finally combine all of them with the  `and` operator. We don't need to do anything special with Ktorm, and the *dynamic query* is perfectly supported. 

Obviously, there is a bug in the query above, that the reduce operation may throw an exception if the list is empty, all conditions are not matched. To avoid this exception, we can replace the reduce operation with `conditions.combineConditions`. This is an extension function provided by Ktorm, it combines all conditions with `and` operator, otherwise, if the list is empty, true will be returned directly. 

```kotlin
fun Iterable<ColumnDeclaring<Boolean>>.combineConditions(): ColumnDeclaring<Boolean> {
    if (this.any()) {
        return this.reduce { a, b -> a and b }
    } else {
        return ArgumentExpression(true, BooleanSqlType)
    }
}
```

To be honest, it's easy to get bored with creating a new `ArrayList` and adding conditions to it every time. Ktorm provides a convenient function `whereWithConditions` which can reduce our duplicated codes. With this function, we can modify the query to: 

```kotlin
val query = Employees
    .select(Employees.salary)
    .whereWithConditions {
        if (departmentId != null) {
            it += Employees.departmentId eq departmentId
        }
        if (managerId != null) {
            it += Employees.managerId eq managerId
        }
        if (name != null) {
            it += Employees.name like "%$name%"
        }
    }
```

Using `whereWithConditins`, we just need to add conditions to `it` which is exactly a `MutableList`, not needed to create a list and combine the conditions by ourselves anymore. On the other hand, Ktorm also provides a `whereWithOrConditions` function, which does the same thing as the other, but finally combining conditions with `or` instead of `and`. 

## groupBy/having

Both `groupBy` and `having` are extension functions for `Query` class, they provide aggregation support for Ktorm, a usage example is shown below: 

```kotlin
val t = Employees
val query = t
    .select(t.departmentId, avg(t.salary))
    .groupBy(t.departmentId)
    .having { avg(t.salary) greater 100.0 }
```

This query selects departments whose average salary is greater than 100, then returns the average salaries along with their department's IDs. The usage is similar to other extension functions like `select` and  `where`, and the generated SQL is also simple and direct too: 

```sql
select t_employee.department_id as t_employee_department_id, avg(t_employee.salary) 
from t_employee 
group by t_employee.department_id 
having avg(t_employee.salary) > ?
```

What if we just add one column to the query above? Assuming if we want to select the employees' names additionally, what will happen?  

```kotlin
val query = t
    .select(t.departmentId, avg(t.salary), t.name)
    .groupBy(t.departmentId)
    .having { avg(t.salary) greater 100.0 }
```

The generated SQL will be changed to: 

```sql
select t_employee.department_id as t_employee_department_id, avg(t_employee.salary), t_employee.name as t_employee_name 
from t_employee 
group by t_employee.department_id 
having avg(t_employee.salary) > ? 
```

However, as any SQL users know, the generated SQL is wrong with syntax now, and it's impossible to be executed in a database. That's because the SQL's grammar restricts that if we are using group by, every select column either comes from the group by clause or appears in an aggregation function. So, that's not Ktorm's fault, we don't understand SQL enough, Ktorm just translates our Koltin code to SQL trustily. 

> Note: Ktorm generates SQLs, but our design goal is never to replace SQL in Kotlin. Ktorm doesn't mean to be an "automation" ORM framework that's "large and complete". Instead, one of our goals is to provide a set of flexible and convenient DSL for SQL by making full use of Kotlin's excellent features. This requires our users to have a certain understanding of SQL because Ktorm just translates our DSL to SQL trustily, we have to take the responsibility of our SQL's correctness and performance. 

## orderBy

`orderBy` is also an extension function for `Query` class, it's corresponding to SQL's order by keyword, here is its signature: 

```kotlin
fun Query.orderBy(vararg orders: OrderByExpression): Query
```

It can be seen that this function accepts a variable number of `OrderByExpression`s, that can be created by other two functions, `asc` and `desc`, naming by the keywords in SQL: 

```kotlin
fun ColumnDeclaring<*>.asc(): OrderByExpression
fun ColumnDeclaring<*>.desc(): OrderByExpression
```

Typical usage is shown below. The query obtains all employees' names, sorting them by their salaries descending: 

```kotlin
val query = Employees
    .select(Employees.name)
    .orderBy(Employees.salary.desc())
```

Similar to `select`, the `orderBy` function not only supports sorting by normal columns, but complex expressions are also OK. The query below obtains departments' IDs and their average salaries, and sorting them by their average salaries descending: 

```kotlin
val t = Employees
val query = t
    .select(t.departmentId, avg(t.salary))
    .groupBy(t.departmentId)
    .orderBy(avg(t.salary).desc())
```

Generated SQL：

```sql
select t_employee.department_id as t_employee_department_id, avg(t_employee.salary) 
from t_employee 
group by t_employee.department_id 
order by avg(t_employee.salary) desc 
```

## limit

The SQL standard doesn't say how to implement paging queries, so different databases provide different implementations on that. For example, MySQL uses `limit m, n` syntax for pagination; PostgreSQL uses `limit m offset n` syntax; Oracle doesn't even provide any keyword, we need to limit our pages in where clause by rownum. 

To hide the paging syntax's differences among databases, Ktorm provides a `limit` function to support pagination: 

```kotlin
fun Query.limit(offset: Int, limit: Int): Query
```

`limit` is also an extension function for `Query` class, it accepts two parameters of int: 

- offset: the offset to the first returned record, starts from 0. 
- limit: max record numbers returned by the query.

Here is an example, this query obtains the first employee in the table: 

```kotlin
val query = Employees.select().limit(0, 1)
```

When we are using the `limit` function, Ktorm will generate appropriate SQLs depending on the currently enabled dialect. If we don't use any dialects, an exception might be thrown: 

```
java.lang.UnsupportedOperationException: Pagination is not supported in Standard SQL.
```

This is OK, the SQL standard doesn't say how to implement paging queries, so Ktorm is not able to generate the SQL for us. To avoid this exception, do not use `limit`, or enable a dialect. Refer to later chapters for how to [enable dialects](./dialects-and-native-sql.html#Enable-Dialects).

## union/unionAll

Ktorm also supports to merge two or more query results, that's the `union` and `unionAll` functions. The `union` function is corresponding to the union keyword in SQL, removing duplicated rows; The `unionAll` function is corresponding to the union all keyword, not removing duplicated rows. Here is an example: 

```kotlin
val query = Employees
    .select(Employees.id)
    .union(
        Departments.select(Departments.id)
    )
    .unionAll(
        Departments.select(Departments.id)
    )
    .orderBy(Employees.id.desc())
```

Generated SQL：

```kotlin
(
  select t_employee.id as t_employee_id 
  from t_employee
) union (
  select t_department.id as t_department_id 
  from t_department
) union all (
  select t_department.id as t_department_id 
  from t_department
) 
order by t_employee_id desc 
```

