---
title: 增删改
lang: zh-cn
related_path: en/dml.html
---

# 增删改

Ktorm 不仅提供了查询和联表的 DSL，而且还能方便地对数据进行增删改操作，下面我们开始介绍 Ktorm 的 DML DSL。

## 插入

Ktorm 使用 `insert` 函数来实现数据插入，它是 `Table` 类的扩展函数，签名如下：

```kotlin
fun <T : Table<*>> T.insert(block: AssignmentsBuilder.(T) -> Unit): Int
```

`insert` 函数接受一个闭包作为参数，我们需要在这个闭包中指定插入的字段和它们的值，插入成功后，返回受影响的记录数，例如：

```kotlin
Employees.insert {
    it.name to "jerry"
    it.job to "trainee"
    it.managerId to 1
    it.hireDate to LocalDate.now()
    it.salary to 50
    it.departmentId to 1
}
```

生成 SQL：

````sql
insert into t_employee (name, job, manager_id, hire_date, salary, department_id) values (?, ?, ?, ?, ?, ?) 
````

从上面的例子可以看出，在闭包函数中，我们可以使用 `it.name to "jerry"` 为 name 字段赋值为 jerry，这是如何实现的呢？

这是因为闭包函数的类型是 `AssignmentsBuilder.(T) -> Unit`，它接受一个 `T` 作为参数，而 `T` 正是当前表对象，因此我们可以在闭包中使用 `it` 获取表对象，进而获取到它的字段。我们还发现，这个闭包函数同时也是 `AsssignmentsBuilder` 类的扩展函数，因此，在闭包的范围内，`this` 引用指向的是一个 `AssignmentsBuilder` 对象，因此我们可以调用到它的 `to` 函数。没错，**这里的 `to` 是 `AssignmentsBuilder` 里面的成员函数，而不是 Kotlin 标准库中创建 `Pair` 的 `to` 函数。**

下面是 `AssignmentsBuilder` 类的源码，可以看到，`to` 函数没有任何返回值，它的作用仅仅是把当前列和它的值保存到一个 `MutableList` 中，以便在插入数据时使用。

```kotlin
@KtormDsl
open class AssignmentsBuilder(private val assignments: MutableList<ColumnAssignmentExpression<*>>) {

    infix fun <C : Any> Column<C>.to(expr: ColumnDeclaring<C>) {
        assignments += ColumnAssignmentExpression(asExpression(), expr.asExpression())
    }

    infix fun <C : Any> Column<C>.to(argument: C?) {
        this to wrapArgument(argument)
    }

    @Suppress("UNCHECKED_CAST")
    @JvmName("toAny")
    infix fun Column<*>.to(argument: Any?) {
        if (argument == null) {
            (this as Column<Any>) to (null as Any?)
        } else {
            throw IllegalArgumentException("Argument type ${argument.javaClass.name} cannot assign to ${sqlType.typeName}")
        }
    }
}
```

> 由于 `AssignmentsBuilder` 里面的 `to` 函数并没有返回值，因此你不太可能会将它和 `kotlin.to` 函数搞混。如果你确实希望在闭包中使用 `kotlin.to` 函数，却发现被编译器解析为 `AssignmentsBuilder.to`，这时会产生一个编译错误，我们推荐你重构一下自己的代码，将 `kotlin.to` 函数的调用移到闭包外面。

有时我们的表会使用自增主键，我们可能希望在插入一条数据后，能够获取到数据库自动生成的主键，这时我们可以使用 `insertAndGenerateKey` 函数。与 `insert` 函数不同，它不再返回受影响的记录数，而是返回自动生成的主键，除此之外，其他用法完全一致。

```kotlin
val id = Employees.insertAndGenerateKey {
    it.name to "jerry"
    it.job to "trainee"
    it.managerId to 1
    it.hireDate to LocalDate.now()
    it.salary to 50
    it.departmentId to 1
}
```

有时候，我们需要一次性插入许多条数据，而循环调用 `insert` 方法的性能可能无法忍受。Ktorm 提供了一个 `batchInsert` 函数，它基于原生 JDBC 提供的 `executeBatch` 函数实现，可以提高批量操作的性能。

```kotlin
Employees.batchInsert {
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

`batchInsert` 函数也接受一个闭包函数作为参数，这个闭包函数的类型是 `BatchInsertStatementBuilder<T>.() -> Unit`，而 `item` 正是 `BatchInsertStatementBuilder` 的成员函数。我们使用 `item` 函数指定批量插入的每一条数据，通常，`item` 函数应该会在一个循环中调用。批量插入成功后，返回一个 `IntArray`，它包含每个子操作所影响的记录数。

有时候，我们会遇到一些转移数据的需求，需要将一个表的数据转移到另一个表。Ktorm 提供了一个 `insertTo` 函数，它是 `Query` 类的扩展函数，用于将一个查询的结果插入到指定的表中。相比于先获取查询的结果，然后调用 `batchInsert` 批量插入数据的方式，`insertTo` 只执行了一条 SQL，性能大大提升。

```kotlin
Departments
    .select(Departments.name, Departments.location)
    .where { Departments.id eq 1 }
    .insertTo(Departments, Departments.name, Departments.location)
```

生成 SQL：

````sql
insert into t_department (name, location) 
select t_department.name as t_department_name, t_department.location as t_department_location 
from t_department 
where t_department.id = ? 
````

## 更新

Ktorm 使用 `update` 函数实现数据更新，它也是 `Table` 类的扩展函数，签名如下：

```kotlin
fun <T : Table<*>> T.update(block: UpdateStatementBuilder.(T) -> Unit): Int
```

与 `insert` 函数类似，它也接受一个闭包作为参数，更新成功后，返回受影响的记录数。闭包函数的类型是 `UpdateStatementBuilder.(T) -> Unit`，其中，`UpdateStatementBuilder` 正是 `AssignmentsBuilder` 的子类，所以在这里我们仍然可以使用 `it.name to "jerry"` 的写法为 name 字段赋值为 jerry。不同的是，`UpdateStatementBuilder` 增加了一个 `where` 函数，用于指定更新的条件。使用方法如下：

```kotlin
Employees.update {
    it.job to "engineer"
    it.managerId to null
    it.salary to 100

    where {
        it.id eq 2
    }
}
```

生成 SQL：

````sql
update t_employee set job = ?, manager_id = ?, salary = ? where id = ? 
````

值得注意的是，`to` 函数的右侧不仅可以是一个参数值，也可以是一个表达式，即一个字段不仅可以更新为一个固定值，也可更新为指定表达式的计算结果。我们可以利用此特性实现一些特殊的功能，比如为某个员工增加 100 薪水：

```kotlin
Employees.update {
    it.salary to it.salary + 100
    where { it.id eq 1 }
}
```

生成 SQL：

````sql
update t_employee set salary = salary + ? where id = ? 
````

有时候，我们需要一次性更新多条数据，而循环调用 `update` 方法的性能可能难以忍受。这时，我们可以使用 `batchUpdate` 函数，与 `batchInsert` 类似，它基于原生 JDBC 提供的 `executeBatch` 函数实现，可以提高批量操作的性能。下面的操作将 id 为 1 和 2 的部门的 location 字段更新为 Hong Kong，当然我们也可以不用 `batchUpdate` 而是把更新条件指定为 `it.id between 1..2`，这里只是为了示范。可以看到，它的用法与 `batchInsert` 函数十分类似，只是多了一个 `where` 函数用于指定更新条件。

```kotlin
Departments.batchUpdate {
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

## 删除

Ktorm 使用 `delete` 函数实现数据删除，它也是 `Table` 类的扩展函数，签名如下：

```kotlin
fun <T : Table<*>> T.delete(predicate: (T) -> ColumnDeclaring<Boolean>): Int
```

`delete` 接受一个闭包函数作为参数，我们需要在闭包函数中指定删除的数据的条件，删除完成后，返回受影响的记录数。闭包函数接受一个 `T` 作为参数，而 `T` 正是当前表对象，因此我们可以在闭包中使用 `it` 获取表对象。使用方法非常简单：

```kotlin
Employees.delete { it.id eq 4 }
```

这个操作将 id 为 4 的员工从数据库中删除。