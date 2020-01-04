---
title: Entity Sequence
lang: en
related_path: zh-cn/entity-sequence.html
---

# Entity Sequence

In the previous section, we briefly learned how to obtain entity objects via sequence APIs. Now we will introduce them in more detail. 

## Introduction

To create an entity sequence, we can use the extension function `sequenceOf`: 

```kotlin
val sequence = database.sequenceOf(Employees)
```

Now we got a default sequence, which can obtain all employees from the table. Please know that Ktorm doesn't execute the query right now. The sequence provides an iterator of type `Iterator<Employee>`, only when we iterate the sequence using the iterator, the query is executed. The following code prints all employees using a for-each loop: 

```kotlin
for (employee in sequence) {
    println(employee)
}
```

Generated SQL: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
```

> While calling `sequenceOf`, we can set the parameter `withReferences` to `false` to disable the auto-joining of reference tables, eg: `database.sequenceOf(Employees, withReferences = false)`

In addition to the for-each loop, we can also use the extension function  `toList` to save all the items from the sequence into a list: 

```kotlin
val employees = sequence.toList()
```

We can even add a filter condition by the `filter` function before calling `toList`: 

```kotlin
val employees = sequence.filter { it.departmentId eq 1 }.toList()
```

Now the generated SQL is: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where t_employee.department_id = ? 
```

Now let's learn the definition of the core class `EntitySequence`: 

```kotlin
data class EntitySequence<E : Any, T : BaseTable<E>>(
    val database: Database, 
    val sourceTable: T,
    val expression: SelectExpression,
    val entityExtractor: (row: QueryRowSet) -> E
) {
    val query = Query(database, expression)

    val sql get() = query.sql

    val rowSet get() = query.rowSet

    val totalRecords get() = query.totalRecords

    fun asKotlinSequence() = Sequence { iterator() }

    operator fun iterator() = object : Iterator<E> {
        private val queryIterator = query.iterator()

        override fun hasNext(): Boolean {
            return queryIterator.hasNext()
        }

        override fun next(): E {
            return entityExtractor(queryIterator.next())
        }
    }
}
```

We can see that any sequence object contains a query in it, and it's iterator exactly wraps the query's iterator. While an entity sequence is iterated, its internal query is executed, and the `entityExtractor` is applied to create an entity object for each row. As for other properties in sequences (such as `sql`, `rowSet`, `totalRecords`, etc), all of them delegates the callings to their internal query objects, and their usages are totally the same as the corresponding properties in `Query` class. 

Most of the entity sequence APIs are provided as extension functions, which can be divided into two groups: 

- **Intermediate operations:** these functions don't execute the internal queries but return new-created sequence objects applying some modifications. For example, the `filter` function creates a new sequence object with the filter condition given by its parameter. The return types of intermediate operations are usually `EntitySequence`, so we can chaining call other sequence functions continuously.  
- **Terminal operations:** the return types of these functions are usually a collection or a computed result, as they execute the queries right now, obtain their results and perform some calculations on them. Eg. `toList`, `reduce`, etc. 

## Intermediate Operations

Just like `kotlin.sequences`, the intermediate operations of `EntitySequence` doesn't iterate the sequences and execute the internal queries, they all return new-created sequence objects instead. These intermediate operations are listed below: 

### filter

```kotlin
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.filter(
    predicate: (T) -> ColumnDeclaring<Boolean>
): EntitySequence<E, T>
```

Similar to the `filter` function of `kotlin.sequences`, the `filter` function here also accepts a closure as its parameter, and the returned value from the closure will be used as a filter condition. Differently, our closure has a parameter of type `T`, the current table object, so what we get in the closure by `it` is the table object instead of an entity element. Besides, the closure's return type is `ColumnDeclaring<Boolean>` instead of `Boolean`. The following code obtains all the employees in department 1 by using `filter`: 

```kotlin
val employees = database.sequenceOf(Employees).filter { it.departmentId eq 1 }.toList()
```

We can see that the usage is almost the same as `kotlin.sequences`, the only difference is the `==` in the lambda is replaced by the `eq` function. The `filter` function can also be called continuously, as all the filter conditions are combined with the `and` operator. 

```kotlin
val employees = database
    .sequenceOf(Employees)
    .filter { it.departmentId eq 1 }
    .filter { it.managerId.isNotNull() }
    .toList()
```

Generated SQL: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where (t_employee.department_id = ?) and (t_employee.manager_id is not null) 
```

Actually, Ktorm provides a `filterNot` function, its usage is totally the same as `filter`, but negates the returned filter condition in the closure. For example, the second `filter` call in the code above can be replaced as `filterNot { it.managerId.isNull() }`. Additionally, Ktorm also provides `filterTo` and `filterNotTo`. But they are terminal operations, as they will iterate the sequence and collect the elements into a collection after applying the filter condition, that's equivalent to call `toCollection` immediately after calling `filter`. 

### filterColumns

```kotlin
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.filterColumns(
    selector: (T) -> List<Column<*>>
): EntitySequence<E, T>
```

By default, an entity sequence selects all the columns from the current table and referenced tables (if enabled), that may lead to unnecessary performance costs. If we are sensitive to the performance issue, we can use the `filterColumns` function, which supports us to custom the selected columns in the query. Assuming we want to get a list of departments, but their location data is not required, we can write codes like: 

```kotlin
val departments = database
    .sequenceOf(Departments)
    .filterColumns { it.columns - it.location }
    .toList()
```

Now, the location data is removed from the returned entity objects, generated SQL: 

```sql
select t_department.id as t_department_id, t_department.name as t_department_name 
from t_department 
```

### sortedBy

```kotlin
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.sortedBy(
    selector: (T) -> ColumnDeclaring<*>
): EntitySequence<E, T>
```

Ktorm provides a `sortedBy` function, which allows us to specify the *order by* clause for the sequence's internal query. The function accepts a closure as its parameter in which we need to return a column or expression. The following code obtains all the employees and sorts them by their salaries: 

```kotlin
val employees = database.sequenceOf(Employees).sortedBy { it.salary }.toList()
```

Generated SQL: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
order by t_employee.salary 
```

The `sortedBy` function sorts entities in ascending order, if we need descending order, we can use `sortedByDescending` instead. 

Sometimes, we need to sort entities by two or more columns, then we can use the `sorted` function, which accepts a closure of type `(T) -> List<OrderByExpression>` as its parameter. The example below sorts the employees firstly by salaries descending, then by hire dates ascending: 

```kotlin
val employees = database
    .sequenceOf(Employees)
    .sorted { listOf(it.salary.desc(), it.hireDate.asc()) }
    .toList()
```

Generated SQL: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
order by t_employee.salary desc, t_employee.hire_date 
```

### drop/take

```kotlin
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.drop(n: Int): EntitySequence<E, T>
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.take(n: Int): EntitySequence<E, T>
```

The `drop` and `take` functions are designed for pagination. The `drop` function returns a new sequence containing all elements except first n elements, while the `take` function returns a new sequence only containing first n elements. Usage example: 

```kotlin
val employees = database.sequenceOf(Employees).drop(1).take(1).toList()
```

If we are using MySQL, the generated SQL is: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
limit ?, ? 
```

Note that these two functions are implemented based on the pagination feature of the specific databases. However, the SQL standard doesn’t say how to implement paging queries, and different databases provide different implementations on that. So we have to enable a dialect if we need to use these two functions, more details can be found in the section [Query - limit](./query.html#limit).

## Terminal Operations

Terminal operations of entity sequences execute the queries right now, then obtain the query results and perform some calculations on them, the usage of which is almost the same as `kotlin.sequences`. 

### toCollection

```kotlin
fun <E : Any, C : MutableCollection<in E>> EntitySequence<E, *>.toCollection(destination: C): C
```

The `toCollection` function is used to collect all the elements in a sequence. It'll execute the internal query right now and iterate the results, adding them to the `destination`: 

```kotlin
val employees = database.sequenceOf(Employees).toCollection(ArrayList())
```

In addition, Ktorm also provides some convenient `toXxx` functions based on `toCollection` to convert sequences to particular type of collections, they are `toList`, `toMutableList`, `toSet`, `toMutableSet`, `toHashSet`, `toSortedSet`. 

### map

```kotlin
inline fun <E : Any, R> EntitySequence<E, *>.map(transform: (E) -> R): List<R>
```

According to our experience of functional programming, we might consider the `map` function as intermediate. However, it is terminal instead, which is a compromise of Ktorm's design. 

The `map` function will execute the internal query and iterate the query results right now, then perform the transformation specified by the `transform` closure for each element, finally collect the transforming results into a list. The following code obtains all the employees' names: 

```kotlin
val names = database.sequenceOf(Employees, withReferences = false).map { it.name }
```

Generated SQL: 

```sql
select * 
from t_employee 
```

Note that although we only need the names here, the generated SQL still selects all columns, that's because Ktorm doesn't know which columns are required. If we are sensitive to that performance issue, we can use the `filterColumns` function cooperatively, or we can also use the `mapColumns` function instead. 

In addition to the basic form of `map` function, Ktorm also provides `mapTo`, `mapIndexed` and `mapIndexedTo`, they have the same names as the extension functions of `kotlin.sequences` in Kotlin standard lib and their usages are totally the same. 

### mapColumns

```kotlin
inline fun <E : Any, T : BaseTable<E>, C : Any> EntitySequence<E, T>.mapColumns(
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): List<C?>
```

The `mapColumns` function is similar to `map`. Differently, its closure accepts the current table object `T` as the parameter, so what we get in the closure by `it` is the table object instead of an entity element. Besides, the closure's return type is `ColumnDeclaring<C>`, and we should return a column or expression needed to be selected from the database. Let's implement the same example as the previous one, the following code obtains all employees' names: 

```kotlin
val names = database.sequenceOf(Employees, withReferences = false).mapColumns { it.name }
```

Now we can see there is only the required column in the generated SQL: 

```sql
select t_employee.name 
from t_employee 
```

If we want to select two or more columns, we can change to `mapColumns2` or `mapColumns3`, then we need to wrap our selected columns by `Pair` or `Triple` in the closure, and the function's return type becomes `List<Pair<C1?, C2?>>`  or  `List<Triple<C1?, C2?, C3?>>`. The example below prints the IDs, names and hired days of the employees in department 1:  

```kotlin
// MySQL datediff function
fun dateDiff(left: LocalDate, right: ColumnDeclaring<LocalDate>) = FunctionExpression(
    functionName = "datediff",
    arguments = listOf(right.wrapArgument(left), right.asExpression()),
    sqlType = IntSqlType
)

database
    .sequenceOf(Employees, withReferences = false)
    .filter { it.departmentId eq 1 }
    .mapColumns3 { Triple(it.id, it.name, dateDiff(LocalDate.now(), it.hireDate)) }
    .forEach { (id, name, days) ->
        println("$id:$name:$days")
    }
```

The standard output: 

```plain
1:vince:473
2:marry:108
```

Generated SQL: 

```sql
select t_employee.id, t_employee.name, datediff(?, t_employee.hire_date) 
from t_employee 
where t_employee.department_id = ? 
```

> Ktorm provides many `mapColumnsN` functions and their variants (from `mapColumns2` to `mapColumns9`). That's to say, we are able to select a maximum of nine columns at once with these functions. But what if we want ten columns or more? I'm sorry to say no. Ktorm doesn't think it's a frequent-used feature. If you really need that, you can use the [query DSL](./query.html) instead. Moreover, to implement these functions, Ktorm also provides many tuple classes (from `Tuple2` to `Tuple9`), in which `Tuple2` and `Tuple3` are type aliases of `Pair` and `Triple`. 

In addition to the basic form of `mapColumns` function, Ktorm also provides `mapColumnsTo`, `mapColumnsNotNull`, `mapColumnsNotNullTo`, `mapColumnsNTo`. It's easy to know their usages by the names, so we won't repeat it. 

### associate

The `associate` function executes the internal query, then iterate the query results and collect them into a `Map`. Its usage is totally the same as the corresponding extension function of `kotlin.sequences`, more details can be found in Kotlin's documents. 

In addition to the basic form of `associate` function, Ktorm also provides `associateBy`, `associateWith`, `associateTo`, `associateByTo`, `associateWithTo`.

### elementAt/first/last/find/findLast/single

These functions are used to get the element at a specific position from the sequence. Their usages are also the same as the corresponding ones of `kotlin.sequences`. 

Especially, if a dialect is enabled, these functions will use the pagination feature to obtain the very record only. Assuming we are using MySQL and calling the `elementAt` with an index 10, a SQL containing `limit 10, 1` will be generated. But if there are no dialects enabled, then all records will be obtained to ensure the functions just works. 

In addition to the basic forms, there are also many variants for these functions, but it's not so necessary to list them here.  

### fold/reduce/forEach

This serial of functions provide features of iteration and folding, and their usages are also the same as the corresponding ones of `kotlin.sequences`. The following code calculates the total salary of all employees: 

```kotlin
val totalSalary = database.sequenceOf(Employees).fold(0L) { acc, employee -> acc + employee.salary }
```

Of course, if only the total salary is needed, we don't have to write codes in that way. Because the performance is really poor, as all employees are obtained from the database. Here we just show you the usage of the `fold` function. It's better to use `sumBy`: 

```kotlin
val totalSalary = database.sequenceOf(Employees).sumBy { it.salary }
```

### joinTo/joinToString

These two functions provide the feature of joining the sequence elements to strings, and their usages are also the same as the corresponding ones of `kotlin.sequences`. The following code joins all the employees' names to a string: 

```kotlin
val names = database.sequenceOf(Employees).joinToString(separator = ":") { it.name }
```

