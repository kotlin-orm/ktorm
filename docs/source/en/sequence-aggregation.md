---
title: Sequence Aggregation
lang: en
related_path: zh-cn/sequence-aggregation.html
---

# Sequence Aggregation

The entity sequence APIs not only allow us to obtain entities from databases just like using `kotlin.sequences`, but they also provide rich support for aggregations, so we can conveniently count the columns, sum them, or calculate their averages, etc. 

## Simple Aggregation

Let's learn the definition of the extension function `aggregateColumns` first: 

```kotlin
inline fun <E : Any, T : BaseTable<E>, C : Any> EntitySequence<E, T>.aggregateColumns(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): C?
```

It's a terminal operation, and it accepts a closure as its parameter, in which we need to return an aggregate expression. Ktorm will create an aggregate query, using the current filter condition and selecting the aggregate expression specified by us, then execute the query and obtain the aggregate result. The following code obtains the max salary in department 1: 

```kotlin
val max = database.employees
    .filter { it.departmentId eq 1 }
    .aggregateColumns { max(it.salary) }
```

If we want to aggregate two or more columns, we can change to `aggregateColumns2` or `aggregateColumns3`, then we need to wrap our aggregate expressions by `Pair` or `Triple` in the closure, and the function's return type becomes `Pair<C1?, C2?>` or `Triple<C1?, C2?, C3?>`. The example below obtains the average and the range of salaries in department 1: 

```kotlin
val (avg, diff) = database.employees
    .filter { it.departmentId eq 1 }
    .aggregateColumns2 { Pair(avg(it.salary), max(it.salary) - min(it.salary)) }
```

Generated SQL: 

```sql
select avg(t_employee.salary), max(t_employee.salary) - min(t_employee.salary) 
from t_employee 
where t_employee.department_id = ? 
```

> Just like `mapColumnsN`, Ktorm provides many `aggregateColumnsN` functions (from `aggregateColumns2` to `aggregateColumns9`). That's to say, we are able to aggregate a maximum of nine columns at once with these functions. 

Additionally, Ktorm also provides many convenient helper functions, they are all implemented based on `aggregateColumns`. For example, we can use `maxBy { it.salary }` to obtain the max salary, that's equivalent to `aggregateColumns { max(it.salary) }`. Here is a list of these functions: 

| Name      | Usage Example                      | Description                                 | Quivalent                                                    |
| --------- | ---------------------------------- | ------------------------------------------- | ------------------------------------------------------------ |
| count     | `count { it.salary greater 1000 }` | Count those whose salary greater than 1000  | `filter { it.salary greater 1000 }`<br/>`.aggregateColumns { count() }` |
| any       | `any { it.salary greater 1000 }`   | True if any one's salary greater than 1000  | `count { it.salary greater 1000 } > 0`                       |
| none      | `none { it.salary greater 1000 }`  | True if no one's salary greater than 1000   | `count { it.salary greater 1000 } == 0`                      |
| all       | `all { it.salary greater 1000 }`   | True if everyone's salary greater than 1000 | `count { it.salary lessEq 1000 } == 0`                       |
| sumBy     | `sumBy { it.salary }`              | Obtain the salaries' sum                    | `aggregateColumns { sum(it.salary) }`                        |
| maxBy     | `maxBy { it.salary }`              | Obtain the salaries' max value              | `aggregateColumns { max(it.salary) }`                        |
| minBy     | `minBy { it.salary }`              | Obtain the salaries' min value              | `aggregateColumns { min(it.salary) }`                        |
| averageBy | `averageBy { it.salary }`          | Obtain the average salary                   | `aggregateColumns { avg(it.salary) }`                        |

## Grouping Aggregation

To use grouping aggregations, we need to learn how to group elements in an entity sequence first. Ktorm provides two different grouping functions, they are `groupBy` and `groupingBy`. 

### groupBy

```kotlin
inline fun <E : Any, K> EntitySequence<E, *>.groupBy(
    keySelector: (E) -> K
): Map<K, List<E>>
```

Obviously, `groupBy` is a terminal operation, it will execute the internal query and iterate the query results right now, then extract a grouping key by the `keySelector` closure for each element, finally collect them into the groups they are belonging to. The following code obtains all the employees and groups them by their departments: 

```kotlin
val employees = database.employees.groupBy { it.department.id }
```

Here, the type of `employees` is `Map<Int, List<Employee>>`, in which the keys are departments' IDs, and the values are the lists of employees belonging to the departments. Now we have the employees' data for every department, we are able to do some aggregate calculations over the data. The following code calculates the average salaries for each department: 

```kotlin
val averageSalaries = database.employees
    .groupBy { it.department.id }
    .mapValues { (_, employees) -> employees.map { it.salary }.average() }
```

But, unfortunately, the aggregate calculation here is performed inside the JVM, and the generated SQL still obtains all the employees, although we don't really need them: 

```sql
select * 
from t_employee 
```

Here, the only thing we need is the average salaries, but we still have to obtain all the employees' data from the database. The performance issue may be intolerable in most cases. It'll be better for us to generate proper SQLs using *group by* clauses and aggregate functions, and move the aggregate calculations back to the database. To solve this problem, we need to use the `groupingBy` function. 

> Note that these two functions are design for very different purposes. The `groupBy` is a terminal operation, as it'll obtain all the entity objects and divide them into groups inside the JVM memory; However, the `groupingBy` is an intermediate operation, it'll add a *group by* clause to the final generated SQL, and particular aggregations should be specified using the following extension functions of `EntityGrouping`. 

### groupingBy

```kotlin
fun <E : Any, T : BaseTable<E>, K : Any> EntitySequence<E, T>.groupingBy(
    keySelector: (T) -> ColumnDeclaring<K>
): EntityGrouping<E, T, K> {
    return EntityGrouping(this, keySelector)
}
```

The `groupingBy` function is an intermediate operation, and it accepts a closure as its parameter, in which we should return a `ColumnDeclaring<K>` as the grouping key. The grouping key can be a column or expression, and it'll be used in the SQL's *group by* clause. Actually, the `groupingBy` function doesn't do anything, it just returns a new-created  `EntityGrouping` with the `keySelector` given by us. The definition of `EntityGrouping` is simple: 

```kotlin
data class EntityGrouping<E : Any, T : BaseTable<E>, K : Any>(
    val sequence: EntitySequence<E, T>,
    val keySelector: (T) -> ColumnDeclaring<K>
) {
    fun asKotlinGrouping(): kotlin.collections.Grouping<E, K?> { ... }
}
```

Most of the `EntityGrouping`'s APIs are provided as extension functions. Let's learn the `aggregateColumns` first: 

```kotlin
inline fun <E : Any, T : BaseTable<E>, K : Any, C : Any> EntityGrouping<E, T, K>.aggregateColumns(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?>
```

Similar to the `aggregateColumns` of `EntitySequence`, it's a terminal operation, and it accepts a closure as its parameter, in which we should return an aggregate expression. Ktorm will create an aggregate query, using the current filter condition and the grouping key, selecting the aggregate expression specified by us, then execute the query and obtain the aggregate results. Its return type is `Map<K?, C?>`, in which the keys are our grouping keys, and the values are the aggregate results for the groups. The following code obtains the average salaries for each department: 

```kotlin
val averageSalaries = database.employees
    .groupingBy { it.departmentId }
    .aggregateColumns { avg(it.salary) }
```

Now we can see that the generated SQL uses a *group by* clause and do the aggregation inside the database: 

```sql
select t_employee.department_id, avg(t_employee.salary) 
from t_employee 
group by t_employee.department_id 
```

If we want to aggregate two or more columns, we can change to `aggregateColumns2` or `aggregateColumns3`, then we need to wrap our aggregate expressions by `Pair` or `Triple` in the closure, and the function’s return type becomes `Map<K?, Pair<C1?, C2?>>` or `Map<K?, Triple<C1?, C2?, C3?>>`. The following code prints the averages and the ranges of salaries for each department: 

```kotlin
database.employees
    .groupingBy { it.departmentId }
    .aggregateColumns2 { Pair(avg(it.salary), max(it.salary) - min(it.salary)) }
    .forEach { departmentId, (avg, diff) ->
        println("$departmentId:$avg:$diff")
    }
```

Generated SQL: 

```sql
select t_employee.department_id, avg(t_employee.salary), max(t_employee.salary) - min(t_employee.salary) 
from t_employee 
group by t_employee.department_id 
```

Additionally, Ktorm also provides many convenient helper functions, they are all implemented based on `aggregateColumns`. Here is a list of them: 

| Name              | Usage Example                 | Description                                | Equivalent                            |
| ----------------- | ----------------------------- | ------------------------------------------ | ------------------------------------- |
| eachCount(To)     | `eachCount()`                 | Obtain record counts for each group        | `aggregateColumns { count() }`        |
| eachSumBy(To)     | `eachSumBy { it.salary }`     | Obtain salaries's sums for each group      | `aggregateColumns { sum(it.salary) }` |
| eachMaxBy(To)     | `eachMaxBy { it.salary }`     | Obtain salaries' max values for each group | `aggregateColumns { max(it.salary) }` |
| eachMinBy(To)     | `eachMinBy { it.salary }`     | Obtain salaries' min values for each group | `aggregateColumns { min(it.salary) }` |
| eachAverageBy(To) | `eachAverageBy { it.salary }` | Obtain salaries' averages for each group   | `aggregateColumns { avg(it.salary) }` |

With these functions, we can write the code below to obtain average salaries for each department: 

```kotlin
val averageSalaries = database.employees
    .groupingBy { it.departmentId }
    .eachAverageBy { it.salary }
```

Besides, Ktorm also provides `aggregate`, `fold`, `reduce`, they have the same names as the extension functions of `kotlin.collections.Grouping`, and the usages are totally the same. The following code calculates the total salaries for each department: 

```kotlin
val totalSalaries = database.employees
    .groupingBy { it.departmentId }
    .fold(0L) { acc, employee -> 
        acc + employee.salary 
    }
```

Of course, if only the total salaries are needed, we don’t have to write codes in that way. Because the performance is really poor, as all employees are obtained from the database. Here we just show you the usage of the `fold` function. It’s better to use `eachSumBy`:

```kotlin
val totalSalaries = database.employees
    .groupingBy { it.departmentId }
    .eachSumBy { it.salary }
```