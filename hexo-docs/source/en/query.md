---
title: Query
lang: en
related_path: zh-cn/query.html
---

# Query

In former chapters, we have created a simple query, it selected all employees in the table table and printed their names. Let's start from this query: 

```kotlin
for (row in Employees.select()) {
    println(row[Employees.name])
}
```

## Query Objects

In the example above, we get a `Query` from `select` function and iterates it with a for-each loop. Is there any other operations supported by `Query` besides iteration? Let's learn the `Query` class's definition first: 

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

Actually, in the example above, all the work Ktorm dose is just to generate a simple SQL `select * from t_employee`. The following `.map { }.filter { }.sortedByDescending { }.forEach { }` are all extension functions in Kotlin standard lib. That's the advantages of implementing `Iterable` interface. 

There are some other useful properties in `Query` class: 

- **sql:** Return the generated SQL string of this query, can be used to ensure whether the generated SQL is expected while debugging. 
- **rowSet:** Return `ResultSet` object of this query, lazy initialized after first access, obtained from database by executing the generated SQL. 
- **totalRecords:** If the query dosen't limits the results via *offset* and *limit*, return the size of the result set. Or if it dose, return the total record count of the query ignoring the *offset* and *limit* parameters. Ktorm provides this property to support pagination, you can calculate page count through dividing it by your page size.

## Obtain Query Results

Every JDBC user knows how to obtain query results from a `ResultSet`. We need a loop to iterate rows in the `ResultSet`, calling the getter functions (such as `getInt`, `getString`, etc) to obtain the data of the specific column. A typical usage is based on a while loop: `while (rs.netxt())  { ... } `. Moreover, after finishing these works, we also have to call `close` function to release the resources. 

That's not so complex, but it's still easy to get bored to write duplicated codes. We have know that `Query` class implemented `Iterable` interaface, that provided another posibility for us. We can iterate results sets by a for-each loop, or process them via extension functions like map, filter, etc, just like the previous example. 

You might have noticed that the return type of `Query.rowSet` was not a normal `ResultSet`, but a `QueryRowSet` instead. That's a special implementation provided by Ktorm, different from normal result sets, it provides additional features: 

- **Available offline:** It's connection independent, it remains available after the connection closed, and it's not necessary to be closed after being used. Ktorm creates `QueryRowSet` objects with all data being retrieved from the result set into memory, so we just need to wait GC to collect them after they are not useful. 
- **Indexed access operator:** `QueryRowSet` oveloads the [indexed access operator](https://kotlinlang.org/docs/reference/operator-overloading.html#indexed), so we can use square brackets `[]` to obtain the value by giving a specific `Column` instance. It's not easy to get wrong by the benefit of the compiler's static checking, but you can still use `getXxx` functions in the `ResultSet` to obtain your results by labels or column indices. 

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

All queries in SQL start with a select keyword. Similarly, All queries in Ktorm start with a `select` function calling. `select` is an extension function of `Table` class, so every query in Ktorm comes from a `Table` object. Here is the signature of this function: 

```kotlin
fun Table<*>.select(vararg columns: ColumnDeclaring<*>): Query
```

We can see it accepts any number of columns and returns a new created `Query` object which selects specific columns from current table. The example below queries employees' ids and names via `select` function: 

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

Then the generated SQL will change to `select *`: 

```sql
select * 
from t_employee 
```

You might have noticed that the paramter type of `select` function was `ColumnDeclaring` instead of `Column`. So we can not only select normal columns from a table, but complex expressions and aggregation functions are also supported. For insance, if we want to know the salary difference between the max and the min in a company, we can write a query like this: 

```kotlin
Employees
    .select(max(Employees.salary) - min(Employees.salary))
    .forEach { row -> println(row.getLong(1)) }
```

Here we use two aggregation functions, `max` and `min`, the return types of which are both `AggregateExpression`. Then substracting the max by the min, we finally have a `BinaryExpression`, which is a subclass of `ColumnDeclaring`, so we can pass it to the `select` function. Generated SQL: 

```sql
select max(t_employee.salary) - min(t_employee.salary) 
from t_employee 
```

We can see that the generated SQL is highly corresponding to our Kotlin code. This benefits from Kotlin's excellent features. Ktorm provides many overloaded operators, thats why we can use minus operator in the query above. Because of the operator overloading, the minus operator here dosen't perform an actual substraction, but being translated to a minus operator in SQL and executed in our database. In the section of [Operators](./operators.html), we will learn more about Ktorm's operators. 

> Small regret: Although the `select` function supports complex expressions, the `QueryRowSet` dosen't. So while obtaining results from a `QueryRowSet`, we can not use index access operators `[]` here. The only thing we can use is `getXxx` functions extended from `ResultSet`, obtaining results by lables or column indices. 

## selectDistinct

`selectDistinct` is also an extension function of `Table` class. Just as its name implies, it will be transalated to a `select distinct` statement in SQL. Bisides of this, it's usage is totally the same with `select` function, so we won't repeat it. 

## where

`where` is also an extension function of `Table` class, let's learn its signature first: 

```kotlin
inline fun Query.where(block: () -> ScalarExpression<Boolean>): Query
```

It's an inline function that accepts a parameter of type `() -> ScalarExpression<Boolean>`, which is a closure function that returns a `ScalarExpression<Boolean>` as our filter condition. The `where` function creates a new `Query` object with all properties being copied from current query, but applying a new filter condition, the return value of the closure. Typical usage: 

```kotlin
val query = Employees
    .select(Employees.salary)
    .where { (Employees.departmentId eq 1) and (Employees.name like "%vince%") }
```

Easy to know that the query obtains the salary of an employee named vince in department 1. Generated SQL is easy too: 

```sql
select t_employee.salary as t_employee_salary 
from t_employee 
where (t_employee.department_id = ?) and (t_employee.name like ?) 
```

We can return any filter conditions in `where` closure, here we constructed one by operators `eq`, `and` and  `like`. Ktolin provides an infix keyword, functions marked with it can be called using the [infix notation](https://kotlinlang.org/docs/reference/functions.html#infix-notation) (omitting the dot and the parentheses for the call), that's how these operators works.  

> Ktorm's built-in operators can be divided into two groups: those that is implemented by operator overloading, such as basic arithmetic operators; and those that is based on infix notations, such as `and`, `or`, `eq`, `like`, `greater`, `less`, etc. 

Sometimes, we need a variable number of filter conditions in our queries, those conditions are combined with `and` or `or` operator, and each of them can be enabled or disabled depending on different conditions. To meet this requirement, many ORM frameworks provide features like *dynamic query*, such as the `<if>` tag of MyBatis. However, this is not a problem at all in Ktorm, because queries in Ktorm are pure Kotlin codes, which is natively *dynamic*. Let's learn the query below: 

```kotlin
val query = Employees
    .select(Employees.salary)
    .where {
        val conditions = ArrayList<ScalarExpression<Boolean>>()

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

Obviously, there is a bug in the query above, that the reduce operation may throw an exception if the list is empty, all conditions are not matched. To avoid this exception, we can replace the reduce operation with `conditions.combineConditions`. This is a extension function provided by Ktorm, it combines all conditions with `and` operator, otherwise if the list is empty, true will be returned directly. 

```kotlin
fun Iterable<ScalarExpression<Boolean>>.combineConditions(): ScalarExpression<Boolean> {
    if (this.any()) {
        return this.reduce { a, b -> a and b }
    } else {
        return ArgumentExpression(true, BooleanSqlType)
    }
}
```

To be honest, it's easy to get bored with creating a new `ArrayList` and adding conditions into it every time. Ktorm provides a convenient function `whereWithConditions` which can reduce our duplicated codes. With this function, we can modify the query to: 

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

`groupBy` 和 `having` 也都是 `Query` 类的扩展函数，他们为 SQL 中的聚合功能提供了支持，下面是一个使用的例子：

```kotlin
val t = Employees
val query = t
    .select(t.departmentId, avg(t.salary))
    .groupBy(t.departmentId)
    .having { avg(t.salary) greater 100.0 }

```

这个查询获取平均工资大于 100 的部门，返回他们的部门 id 以及平均工资。用法与前面介绍的 `select`、`where` 等函数相似，生成的 SQL 也是十分简单直接：

```sql
select t_employee.department_id as t_employee_department_id, avg(t_employee.salary) 
from t_employee 
group by t_employee.department_id 
having avg(t_employee.salary) > ?

```

值得一提的是，如果我们再这个查询的 `select` 方法中再加一列会怎么样呢，比如我们希望再返回一下员工的名字：

```kotlin
val query = t
    .select(t.departmentId, avg(t.salary), t.name)
    .groupBy(t.departmentId)
    .having { avg(t.salary) greater 100.0 }

```

现在生成的 SQL 是这样的：

```sql
select t_employee.department_id as t_employee_department_id, avg(t_employee.salary), t_employee.name as t_employee_name 
from t_employee 
group by t_employee.department_id 
having avg(t_employee.salary) > ? 

```

然而，了解 SQL 语法的人都知道，这条生成的 SQL 的语法是错误的，完全无法在数据库中执行。这是因为 SQL 语法规定，在使用 group by 时，select 子句中出现的字段，要么是 group by 中的列，要么被包含在聚合函数中。然而，这能怪 Ktorm 吗？这只能怪你对 SQL 的不了解，Ktorm 只是忠实地将你的代码翻译成了 SQL 而已。

> 注意：Ktorm 虽然有 SQL 生成，但是我们的设计目标，从来都不是为了取代 SQL，我们不希望做成一个大而全的“自动化” ORM 框架，相反，我们的目标是充分使用 Kotlin 优越的语法特性，为 SQL 提供方便灵活的 DSL。这要求使用者对 SQL 有一定的了解，因为 Ktorm 的工作只是将 DSL 忠实地翻译成 SQL 而已，SQL 的正确性和性能都需要使用者自己负起责任。

## orderBy

`orderBy` 也是 `Query` 的扩展函数，它对应于 SQL 中的 order by 关键字，下面是它的签名：

```kotlin
fun Query.orderBy(vararg orders: OrderByExpression): Query

```

可以看到，这个函数接受一个或多个 `OrderByExpression`，这就涉及到另外两个函数，它们分别是 `asc` 和 `desc`，和 SQL 中的关键字名称一样：

```kotlin
fun ColumnDeclaring<*>.asc(): OrderByExpression
fun ColumnDeclaring<*>.desc(): OrderByExpression

```

`orderBy` 的典型用法如下，这个查询获取所有员工的名字，按工资从高到低排序：

```kotlin
val query = Employees
    .select(Employees.name)
    .orderBy(Employees.salary.desc())

```

与 `select` 函数一样，`orderBy` 不仅支持按普通的列排序，还支持复杂的表达式，下面的查询获取每个部门的 ID 和部门内员工的平均工资，并按平均工资从高到低排序：

```kotlin
val t = Employees
val query = t
    .select(t.departmentId, avg(t.salary))
    .groupBy(t.departmentId)
    .orderBy(avg(t.salary).desc())

```

生成 SQL：

```sql
select t_employee.department_id as t_employee_department_id, avg(t_employee.salary) 
from t_employee 
group by t_employee.department_id 
order by avg(t_employee.salary) desc 

```

## limit

SQL 标准中并没有规定如何进行分页查询的语法，因此，每种数据库提供商对其都有不同的实现。例如，在 MySQL 中，分页是通过 `limit m, n` 语法完成的，在 PostgreSQL 中，则是 `limit m offset n`，而 Oracle 则没有提供任何关键字，我们需要在 where 子句使用 rownum 限定自己需要的数据页。

为了抹平不同数据库分页语法的差异，Ktorm 提供了一个 `limit` 函数，我们使用这个函数对查询进行分页：

```kotlin
fun Query.limit(offset: Int, limit: Int): Query

```

`limit` 也是 `Query` 类的扩展函数，它接收两个整形参数，分别是：

- offset: 需要返回的第一条记录相对于整个查询结果的位移，从 0 开始
- limit: 需要返回的记录的数量

使用示例如下，这个查询获取员工表的第一条记录：

```kotlin
val query = Employees.select().limit(0, 1)

```

使用 `limit` 函数时，Ktorm 会根据当前使用的不同数据库（Dialect）生成合适的分页 SQL。但是如果你没有启用任何方言，你可能会得到这样一个异常：

```
java.lang.UnsupportedOperationException: Pagination is not supported in Standard SQL.

```

这个是正常的，因为标准 SQL 中的确没有规定分页的语法，因此 Ktorm 无法为你生成这种 SQL，要避免这个异常，要么放弃使用 `limit` 函数，要么启用一个数据库方言。关于如何[启用方言](./dialects-and-raw-sql.html#启用方言)，可参考后面的章节。

## union/unionAll

Ktorm 也支持将两个或多个查询的结果进行合并，这时我们使用 `union` 或 `unionAll` 函数。其中，`union` 对应 SQL 中的 union 关键字，会对合并的结果进行去重；`unionAll` 对应 SQL 中的 union all 关键字，保留重复的结果。下面是一个例子：

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

生成 SQL：

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

