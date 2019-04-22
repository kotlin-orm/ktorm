---
title: 序列聚合
lang: zh-cn
related_path: en/sequence-aggregation.html
---

# 序列聚合

实体序列 API 不仅可以让我们使用类似 `kotlin.Sequence` 的方式获取数据库中的实体对象，它还支持丰富的聚合功能，让我们可以方便地对指定字段进行计数、求和、求平均值等操作。

> 注意：实体序列 API 仅在 Ktorm 2.0 及以上版本中提供。

## 简单聚合

我们首先来看看 `aggregate` 函数的定义：

```kotlin
inline fun <E : Entity<E>, T : Table<E>, C : Any> EntitySequence<E, T>.aggregate(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): C?
```

这是一个终止操作，它接收一个闭包作为参数，在闭包中，我们需要返回一个聚合表达式。Ktorm 会使用我们返回的聚合表达式，根据当前序列的查询条件创建一个聚合查询， 然后执行这个查询，获取聚合的结果。下面的代码获取部门 1 中工资的最大值：

```kotlin
val max = Employees
    .asSequenceWithoutReferences()
    .filter { it.departmentId eq 1 }
    .aggregate { max(it.salary) }
```

如果你希望同时获取多个聚合结果，可以改用 `aggregate2` 或 `aggregate3` 函数，这时我们需要在闭包中使用 `Pair` 或 `Triple` 包装我们的这些聚合表达式，函数的返回值也相应变成了 `Pair<C1?, C2?>` 或 `Triple<C1?, C2?, C3?>`。下面的例子获取部门 1 中工资的平均值和极差：

```kotlin
val (avg, diff) = Employees
    .asSequenceWithoutReferences()
    .filter { it.departmentId eq 1 }
    .aggregate2 { Pair(avg(it.salary), max(it.salary) - min(it.salary)) }
```

生成 SQL：

````sql
select avg(t_employee.salary), max(t_employee.salary) - min(t_employee.salary) 
from t_employee 
where t_employee.department_id = ? 
````

> 那么有没有 `aggregate4` 或更多函数呢，很遗憾并没有，与上一节中的 `mapColumns` 一样，我们认为这并不是一个十分常用而且不可替代的功能。如果你确实需要的话，参考源码自己实现是十分简单的，或者你也可以给我们提 issue。

除了直接使用 `aggregate` 函数以外，Ktorm 还为序列提供了许多方便的辅助函数，他们都是基于 `aggregate` 函数实现的。比如 `maxBy { it.salary }` 即可获得工资的最大值，相当于 `aggregate { max(it.salary) }`。下面是这些函数的一个列表：

| 函数名    | 使用示例                     | 示例描述                           | 相当于                                              |
| --------- | ---------------------------- | ---------------------------------- | --------------------------------------------------- |
| count     | `count { it.salary > 1000 }` | 获取薪水大于 1000 的员工数量       | `filter { it.salary > 1000 }.aggregate { count() }` |
| any       | `any { it.salary > 1000 }`   | 判断是否存在薪水大于 1000 的员工   | `count { it.salary > 1000 } > 0`                    |
| none      | `none { it.salary > 1000 }`  | 判断是否不存在薪水大于 1000 的员工 | `count { it.salary > 1000 } == 0`                   |
| all       | `all { it.salary > 1000 }`   | 判断是否所有员工的薪水都大于 1000  | `count { it.salary <= 1000 } == 0`                  |
| sumBy     | `sumBy { it.salary }`        | 获得员工的薪水总和                 | `aggregate { sum(it.salary) }`                      |
| maxBy     | `maxBy { it.salary }`        | 获得员工薪水的最大值               | `aggregate { max(it.salary) }`                      |
| minBy     | `minBy { it.salary }`        | 获得员工薪水的最小值               | `aggregate { min(it.salary) }`                      |
| averageBy | `averageBy { it.salary }`    | 获得员工薪水的平均值               | `aggregate { avg(it.salary) }`                      |

## 分组聚合