---
title: 序列聚合
lang: zh-cn
related_path: en/sequence-aggregation.html
---

# 序列聚合

实体序列 API 不仅可以让我们使用类似 `kotlin.Sequence` 的方式获取数据库中的实体对象，它还支持丰富的聚合功能，让我们可以方便地对指定字段进行计数、求和、求平均值等操作。

> 注意：实体序列 API 仅在 Ktorm 2.0 及以上版本中提供。

## 简单聚合

我们首先来看看 `aggregateColumns` 函数的定义：

```kotlin
inline fun <E : Entity<E>, T : Table<E>, C : Any> EntitySequence<E, T>.aggregateColumns(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): C?
```

这是一个终止操作，它接收一个闭包作为参数，在闭包中，我们需要返回一个聚合表达式。Ktorm 会使用我们返回的聚合表达式，根据当前序列的查询条件创建一个聚合查询， 然后执行这个查询，获取聚合的结果。下面的代码获取部门 1 中工资的最大值：

```kotlin
val max = Employees
    .asSequenceWithoutReferences()
    .filter { it.departmentId eq 1 }
    .aggregateColumns { max(it.salary) }
```

如果你希望同时获取多个聚合结果，可以改用 `aggregateColumns2` 或 `aggregateColumns3` 函数，这时我们需要在闭包中使用 `Pair` 或 `Triple` 包装我们的这些聚合表达式，函数的返回值也相应变成了 `Pair<C1?, C2?>` 或 `Triple<C1?, C2?, C3?>`。下面的例子获取部门 1 中工资的平均值和极差：

```kotlin
val (avg, diff) = Employees
    .asSequenceWithoutReferences()
    .filter { it.departmentId eq 1 }
    .aggregateColumns2 { Pair(avg(it.salary), max(it.salary) - min(it.salary)) }
```

生成 SQL：

````sql
select avg(t_employee.salary), max(t_employee.salary) - min(t_employee.salary) 
from t_employee 
where t_employee.department_id = ? 
````

> 与 `mapColumnsN` 类似，Ktorm 提供了从 `aggregateColumns2` 到 `aggregateColumns9` 等多个函数，也就是说，我们最多可以使用 `aggregateColumnsN` 系列函数一次获得九个聚合结果。

除了直接使用 `aggregateColumns` 函数以外，Ktorm 还为序列提供了许多方便的辅助函数，他们都是基于 `aggregateColumns` 函数实现的。比如 `maxBy { it.salary }` 即可获得工资的最大值，相当于 `aggregateColumns { max(it.salary) }`。下面是这些函数的一个列表：

| 函数名    | 使用示例                           | 示例描述                           | 相当于                                                       |
| --------- | ---------------------------------- | ---------------------------------- | ------------------------------------------------------------ |
| count     | `count { it.salary greater 1000 }` | 获取薪水超 1000 的员工数           | `filter { it.salary greater 1000 }`<br/>`.aggregateColumns { count() }` |
| any       | `any { it.salary greater 1000 }`   | 判断是否存在薪水大于 1000 的员工   | `count { it.salary greater 1000 } > 0`                       |
| none      | `none { it.salary greater 1000 }`  | 判断是否不存在薪水大于 1000 的员工 | `count { it.salary greater 1000 } == 0`                      |
| all       | `all { it.salary greater 1000 }`   | 判断是否所有员工的薪水都大于 1000  | `count { it.salary lessEq 1000 } == 0`                       |
| sumBy     | `sumBy { it.salary }`              | 获得员工的薪水总和                 | `aggregateColumns { sum(it.salary) }`                        |
| maxBy     | `maxBy { it.salary }`              | 获得员工薪水的最大值               | `aggregateColumns { max(it.salary) }`                        |
| minBy     | `minBy { it.salary }`              | 获得员工薪水的最小值               | `aggregateColumns { min(it.salary) }`                        |
| averageBy | `averageBy { it.salary }`          | 获得员工薪水的平均值               | `aggregateColumns { avg(it.salary) }`                        |

## 分组聚合

要使用分组聚合，我们首先要学习如何对序列中的元素进行分组。Ktorm 为实体序列提供了两个不同的分组函数，它们是 `groupBy` 和 `groupingBy`。

### groupBy

```kotlin
inline fun <E : Entity<E>, K> EntitySequence<E, *>.groupBy(
    keySelector: (E) -> K
): Map<K, List<E>>
```

很明显，这是一个终止操作，它会马上执行查询，迭代所有返回的实体对象，通过闭包传入的 `keySelector` 获取实体对象的分组 key，按照这个 key 对它们进行分组，将每个元素添加到所属组的集合中。下面的代码获取所有员工对象，并按部门进行分组：

```kotlin
val employees = Employees.asSequence().groupBy { it.department.id }
```

在这里，`employees` 的类型是 `Map<Int, List<Employee>>`，其中，key 是部门 ID，value 是在这个部门下的所有员工的列表。现在我们已经有了所有部门下的员工列表，然后就可以使用这些数据进行一些聚合计算。比如下面的代码可以计算出所有部门的平均工资：

```kotlin
val averageSalaries = Employees
    .asSequence()
    .groupBy { it.department.id }
    .mapValues { (_, employees) -> employees.map { it.salary }.average() }
```

但可惜的是，我们这里的聚合计算是在 JVM 完成的，所生成的 SQL 依然获取了所有的员工数据，尽管我们并不需要他们：

````sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
````

如果仅仅需要计算平均工资，却不得不获取数据库中的所有员工数据，这个性能开销在大多数时候都是不可忍受的。那么我们能不能利用 SQL 中自带的 group by 和聚合功能，生成恰当的 SQL，让数据库来帮我们进行聚合计算呢？这时我们应该使用下面将要介绍的 `groupingBy` 函数。

> 请注意 `groupBy` 和 `groupingBy` 函数的区别，它们设计的使用场景是完全不同的。`groupBy` 是终止操作，它会获取当前序列中的所有实体对象，在 JVM 内存中对它们进行分组；`groupingBy` 是中间操作，它会为最终生成的 SQL 添加一个 group by 子句，具体执行的聚合操作需要在后续使用 `EntityGrouping` 的扩展函数来指定。

### groupingBy

```kotlin
fun <E : Entity<E>, T : Table<E>, K : Any> EntitySequence<E, T>.groupingBy(
    keySelector: (T) -> ColumnDeclaring<K>
): EntityGrouping<E, T, K> {
    return EntityGrouping(this, keySelector)
}
```

`groupingBy` 是一个中间操作，它接收一个闭包作为参数，我们需要在闭包中返回一个 `ColumnDeclaring<K>` 作为 SQL group by 子句中的列。实际上，`groupingBy` 函数什么也没做，它只是使用我们传入的 `keySelector` 创建了一个 `EntityGrouping` 对象而已。`EntityGrouping` 的定义也十分简单：

```kotlin
data class EntityGrouping<E : Entity<E>, T : Table<E>, K : Any>(
    val sequence: EntitySequence<E, T>,
    val keySelector: (T) -> ColumnDeclaring<K>
) {
    fun asKotlinGrouping(): kotlin.collections.Grouping<E, K?> { ... }
}
```

大部分 `EntityGrouping` 的 API，都是以扩展函数的方式来提供的，我们首先来看看最基本的 `aggregateColumns` 函数：

```kotlin
inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Any> EntityGrouping<E, T, K>.aggregateColumns(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?>
```

与 `EntitySequence` 的 `aggregateColumns` 函数类似，这是一个终止操作，它接收一个闭包作为参数，在闭包中，我们需要返回一个聚合表达式。Ktorm 会使用我们返回的聚合表达式，根据当前序列的查询条件和分组条件创建一个聚合查询，然后执行这个查询，获取聚合的结果。它的返回值是 `Map<K?, C?>`，其中，key 是我们的分组列的值，value 是该组中的聚合结果。下面的代码可以获取所有部门的平均工资：

```kotlin
val averageSalaries = Employees
    .asSequenceWithoutReferences()
    .groupingBy { it.departmentId }
    .aggregateColumns { avg(it.salary) }
```

可以看到，这时生成的 SQL 就使用了 group by 子句，把聚合计算放到了数据库中执行：

````sql
select t_employee.department_id, avg(t_employee.salary) 
from t_employee 
group by t_employee.department_id 
````

如果你希望同时获取多个聚合结果，可以改用 `aggregateColumns2` 或 `aggregateColumns3` 函数，这时我们需要在闭包中使用 `Pair` 或 `Triple` 包装我们的这些聚合表达式，函数的返回值也相应变成了 `Map<K?, Pair<C1?, C2?>>` 或 `Map<K?, Triple<C1?, C2?, C3?>>`。下面的例子会打印出所有部门工资的平均值和极差：

```kotlin
Employees
    .asSequenceWithoutReferences()
    .groupingBy { it.departmentId }
    .aggregateColumns2 { Pair(avg(it.salary), max(it.salary) - min(it.salary)) }
    .forEach { departmentId, (avg, diff) ->
        println("$departmentId:$avg:$diff")
    }
```

生成 SQL：

````sql
select t_employee.department_id, avg(t_employee.salary), max(t_employee.salary) - min(t_employee.salary) 
from t_employee 
group by t_employee.department_id 
````

除了直接使用 `aggregateColumns` 函数以外，Ktorm 还提供了许多方便的辅助函数，它们都是基于 `aggregateColumns` 函数实现的，下面是这些函数的列表：

| 函数名            | 使用示例                      | 示例描述               | 相当于                                |
| ----------------- | ----------------------------- | ---------------------- | ------------------------------------- |
| eachCount(To)     | `eachCount()`                 | 获取每个分组的记录数量 | `aggregateColumns { count() }`        |
| eachSumBy(To)     | `eachSumBy { it.salary }`     | 获取每个分组的工资总和 | `aggregateColumns { sum(it.salary) }` |
| eachMaxBy(To)     | `eachMaxBy { it.salary }`     | 获取每个分组的最高工资 | `aggregateColumns { max(it.salary) }` |
| eachMinBy(To)     | `eachMinBy { it.salary }`     | 获取每个分组的最低工资 | `aggregateColumns { min(it.salary) }` |
| eachAverageBy(To) | `eachAverageBy { it.salary }` | 获取每个分组的平均工资 | `aggregateColumns { avg(it.salary) }` |

有了这些辅助函数，上面获取所有部门平均工资的代码就可以改写成：

```kotlin
val averageSalaries = Employees
    .asSequenceWithoutReferences()
    .groupingBy { it.departmentId }
    .eachAverageBy { it.salary }
```

除此之外，Ktorm 还提供了 `aggregate`、`fold`、`reduce` 等函数，它们与 `kotlin.collections.Grouping` 的相应函数同名，功能也完全一样。下面的代码使用 `fold` 函数计算每个部门工资的总和：

```kotlin
val totalSalaries = Employees
    .asSequenceWithoutReferences()
    .groupingBy { it.departmentId }
    .fold(0L) { acc, employee -> 
        acc + employee.salary 
    }
```

当然，如果仅仅为了获得工资总和，我们没必要这样做。这是性能低下的写法，它会查询出所有员工的数据，然后对它们进行迭代，这里仅用作示范，更好的写法是使用 `eachSumBy` 函数：

```kotlin
val totalSalaries = Employees
    .asSequenceWithoutReferences()
    .groupingBy { it.departmentId }
    .eachSumBy { it.salary }
```