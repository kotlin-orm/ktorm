---
title: 实体序列
lang: zh-cn
related_path: en/entity-sequence.html
---

# 实体序列

除了 `find*` 函数以外，Ktorm 还提供了一套名为"实体序列"的 API，用来从数据库中获取实体对象。正如其名字所示，它的风格和使用方式与 Kotlin 标准库中的序列 API 及其类似，它提供了许多同名的扩展函数，比如 `filter`、`map`、`reduce` 等。

> 注意：实体序列 API 仅在 Ktorm 2.0 及以上版本中提供。

## 序列简介

要获取一个实体序列，我们可以在表对象上调用 `asSequence` 扩展函数：

````kotlin
val sequence = Employees.asSequence()
````

这样我们就得到了一个默认的序列，它可以获得表中的所有员工。但是请放心，Ktorm 并不会马上执行这个查询，序列对象提供了一个迭代器 `Iterator<Employee>`，当我们使用它迭代序列中的数据时，查询才会执行。下面我们使用 for-each 循环打印出序列中所有的员工：

````kotlin
for (employee in Employees.asSequence()) {
    println(employee)
}
````

生成的 SQL 如下：

````sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
````

除了使用 for-each 循环外，我们还能用 `toList` 扩展函数将序列中的元素保存为一个列表：

````kotlin
val employees = Employees.asSequence().toList()
````

我们还能在 `toList` 之前，使用 `filter` 扩展函数添加一个筛选条件：

```kotlin
val employees = Employees.asSequence().filter { it.departmentId eq 1 }.toList()
```

此时生成的 SQL 会变成：

````sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where t_employee.department_id = ? 
````

上面的两个例子，其实相当于我们在上一节中介绍的 `findAll` 和 `findList` 函数。事实上，这两个函数正是基于实体序列 API 实现的，它们提供了简短的调用链，而序列 API 提供的，则是更强大灵活的使用方式。

我们再来看看最核心的 `EntitySequence` 类的定义：

```kotlin
data class EntitySequence<E : Entity<E>, T : Table<E>>(val sourceTable: T, val expression: SelectExpression) {

    val query = Query(expression)

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
            return sourceTable.createEntity(queryIterator.next())
        }
    }
}
```

可以看出，每个实体序列中都包含了一个查询，实体序列的迭代器正是包装了它内部的查询的迭代器。当序列被迭代时，会执行内部的查询，然后使用 `createEntity` 为每行创建一个实体对象。至于序列中的其他属性，比如 `sql`、`rowSet`、`totalRecords` 等，也都是直接来自它内部的查询对象，其功能与 `Query` 类中的同名属性完全相同。

Ktorm 的实体序列 API，大部分都是以扩展函数的方式提供的，这些扩展函数大致可以分为两类：

- 中间操作：这类函数并不会执行序列中的查询，而是修改并创建一个新的序列对象，比如 `filter` 函数会创建一个新的序列对象，应用了指定的筛选条件。中间函数的返回值类型通常都是 `EntitySequence`，以便我们继续链式调用其他序列函数。
- 终止操作：这类函数的返回值通常是一个集合或者是某个计算的结果，他们会马上执行一个查询，并获取它的执行结果，比如 `toList`、`reduce` 等。

## 中间操作

就像 `kotlin.Sequence` 一样，`EntitySequence` 的中间操作并不会迭代序列执行查询，它们都返回一个新的序列对象。`EntitySequence` 的中间操作主要有如下几个。

### filter

````kotlin
inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.filter(
    predicate: (T) -> ColumnDeclaring<Boolean>
): EntitySequence<E, T>
````

与 `kotlin.Sequence` 的 `filter` 函数类似，`EntitySequence` 的 `filter` 函数也接受一个闭包作为参数，使用闭包中指定的筛选条件对序列进行过滤。不同的是，我们的闭包接受当前表对象 `T` 作为参数，因此我们在闭包中使用 `it` 访问到的并不是实体对象，而是表对象，另外，闭包的返回值也是 `ColumnDeclaring<Boolean>`，而不是 `Boolean`。下面使用 `filter` 获取部门 1 中的所有员工：

```kotlin
val employees = Employees.asSequence().filter { it.departmentId eq 1 }.toList()
```

可以看到，用法几乎与 `kotlin.Sequence` 完全一样，不同的仅仅是在 lambda 表达式中的等号 `==` 被这里的 `eq` 函数代替了而已。`filter` 函数还可以连续使用，此时所有的筛选条件将使用 `and` 操作符进行连接，比如：

```kotlin
val employees = Employees
    .asSequence()
    .filter { it.departmentId eq 1 }
    .filter { it.managerId.isNotNull() }
    .toList()
```

生成 SQL：

````sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where (t_employee.department_id = ?) and (t_employee.manager_id is not null) 
````

其实，Ktorm 还提供了一个 `filterNot` 函数，它的用法与 `filter` 一样，但是会将闭包中的筛选条件取反。比如上面例子中的第二个 `filter` 调用就可以改写为 `filterNot { it.managerId.isNull() }`。除此之外，Ktorm 还提供了 `filterTo` 和 `filterNotTo`，但这两个函数其实是终止操作，它们会在获取到新的序列对象后马上迭代它，将里面的元素添加到给定的集合中，其效果相当于连续调用 `filter` 和 `toCollection` 两个函数。

### filterColumns

### sortedBy

### drop/take

## 终止操作

### toCollection

### map

### associate

### elementAt

### first/last

### fold/reduce/forEach

### joinTo

## 分组与聚合