---
title: 查询
lang: zh-cn
related_path: en/query.html
---

# 查询

在前面的章节中，我们曾经创建过一个简单的查询，它查询表中所有的员工记录，然后打印出他们的名字，我们的介绍就从这里开始：

````kotlin
for (row in database.from(Employees).select()) {
    println(row[Employees.name])
}
````

## Query 对象

在上面的例子中，`select` 方法返回了一个类型为 `Query` 的对象，然后使用 for-each 循环对其进行迭代，那么除了迭代外，`Query` 类还支持什么操作呢？让我们先来看一下它的定义：

```kotlin
data class Query(val database: Database, val expression: QueryExpression) : Iterable<QueryRowSet> {
    
    val sql: String by lazy { ... }

    val rowSet: QueryRowSet by lazy { ... }

    val totalRecords: Int by lazy { ... }

    override fun iterator(): Iterator<QueryRowSet> {
        return rowSet.iterator()
    }
}
```

`Query` 表示一个查询操作，Ktorm 正是以这个类为核心支持所有的查询 DSL。

可以看到，`Query` 类的构造函数接收两个参数：`database` 是执行此查询的数据库对象；`expression` 是被执行的 SQL 语句的抽象表示。一般来说，我们不需要自己使用这个构造函数创建 `Query` 对象，而是使用 `database.from(..).select(..)` 的语法，由 Ktorm 为我们构造一个查询。

`Query` 类还实现了 `Iterable<QueryRowSet>` 接口，通过实现这个接口，我们才能够使用 for-each 循环的语法遍历查询返回的结果集。而且，Kotlin 标准库中也有许多针对 `Iterable` 接口的扩展函数，所以我们还可以使用 `map`、 `filter` 等函数对结果集进行各种各样的处理，就像这样：

```kotlin
data class Emp(val id: Int?, val name: String?, val salary: Long?)

val query = database.from(Employees).select()

query
    .map { row -> Emp(row[Employees.id], row[Employees.name], row[Employees.salary]) }
    .filter { it.salary > 1000 }
    .sortedBy { it.salary }
    .forEach { println(it.name) }
```

实际上，在这里 Ktorm 所完成的工作，也只是生成了一句简单的 SQL `select * from t_employee` 而已，后面的 `.map { }.filter { }.sortedBy { }.forEach { }` 全部都是 Kotlin 标准库中的函数，这就是实现 `Iterable` 接口给我们带来的好处。

`Query` 类中还有一些有用的属性：

- **sql：**返回该查询生成的 SQL 字符串，可以在调试程序的时候确认生成的 SQL 是否符合预期。
- **rowSet：**返回该查询的结果集对象，此字段懒初始化，在第一次获取时，执行 SQL 语句，从数据库中获取结果。
- **totalRecords：**如果该查询没有使用 offset, limit 进行分页，此字段返回结果集的总行数；如果使用了分页，返回去除 offset, limit 限制后的符合条件的总记录数。Ktorm 使用此字段来支持页码计算，你可以使用 totalRecords 除以你的每页大小来计算总页数。  

## 获取查询结果

如果你用过 JDBC，你应该知道如何从 `ResultSet` 中获取你的查询结果。你需要使用一个循环不断地遍历结果集中的行，在循环中调用 `getInt` 、`getString` 等方法获取指定列中的数据，典型的用法是一个 while 循环：`while (rs.next()) { ... }`。而且，使用完毕后，你还得调用 `close` 方法关闭结果集。

这种写法虽说并不复杂，但重复的代码写多了也难免让人厌烦。Ktorm 通过让 `Query` 类实现 `Iterable` 接口，为你提供了另一种可能。你可以使用 for-each 循环迭代 `Query` 的结果集，也可以使用 `map`、`filter` 等扩展函数对结果集进行二次处理，就像前面的例子一样。

你可能已经发现，`Query.rowSet` 返回的结果集并不是普通的 `ResultSet`，而是 `QueryRowSet`。这是 Ktorm 提供的特殊的 `ResultSet` 的实现，与普通的 `ResultSet` 不同，它增加了如下特性：

- **离线可用：**它不依赖于数据库连接，当连接关闭后，仍然可以正常使用，使用完毕也不需要调用 `close` 方法。`QueryRowSet` 在创建时，已经完整取出了结果集中的所有数据保存在内存中，因此只需要等待 GC 自动回收即可。
- **索引访问运算符：**`QueryRowSet` 重载了[索引访问运算符](https://kotlinlang.org/docs/reference/operator-overloading.html#indexed)，因此你可以使用方括号语法 `[]` ，通过传入指定的 `Column` 对象来获取这个列的数据，这种方法得益于编译器的静态检查，不易出错。不过，你仍然可以使用 `ResultSet` 中的 `getXxx` 方法，通过传入列的序号或名称字符串来获取。

使用索引访问运算符获取列的方法如下：

```kotlin
for (row in database.from(Employees).select()) {
    val id: Int? = row[Employees.id]
    val name: String? = row[Employees.name]
    val salary: Long? = row[Employees.salary]

    println("$id, $name, $salary")
}
```

可以看到，如果列的类型是 `Column<Int>`，返回的结果的类型就是 `Int?`，如果列的类型是 `Column<String>`，返回的结果的类型就是 `String?`。而且，列的类型并不局限于 `ResultSet` 中的 `getXxx` 方法返回的那些类型，它可以是任意类型，结果也始终是对应的类型，其中还可以包含一些对结果的必要的转换行为，具体取决于定义该列时所使用的 [SqlType](./schema-definition.html#SqlType)。

## from

`from` 是 `Database` 的扩展函数，它的功能是把一个表对象包装成 `QuerySource` 对象：

```kotlin
fun Database.from(table: BaseTable<*>): QuerySource
```

正如函数名 `from` 所示，`QuerySource` 表示 SQL 查询中的 from 子句。当得到一个 `QuerySource` 对象后，我们可以调用 `select` 函数创建一个查询，也可以继续调用 `innerJoin`、`leftJoin` 等函数进行联表操作。

在本文中我们将使用 `from` 函数引出所有的查询 DSL，至于[联表](./joining.html)，请参考后面章节的内容。

## select

SQL 中的查询语句都开始于一个 select 关键字，类似地，Ktorm 中的查询也始于 `select` 函数的调用。`select` 是 `QuerySource` 的扩展函数，它的签名如下：

````kotlin
fun QuerySource.select(vararg columns: ColumnDeclaring<*>): Query
````

可以看到，它接受任意数量的列，返回一个 `Query` 对象，这个查询对象从当前 `QuerySource` 中查询指定的列。下面使用 `select` 函数查询员工的 id 和姓名：

````kotlin
val query = database.from(Employees).select(Employees.id, Employees.name)
````

得到 `Query` 对象之后，SQL 实际上还没有运行，你可以继续链式调用 `where` 或其他扩展函数修改这个 `Query` 对象，也可以使用 `for-each` 循环或其他方式迭代它，这时，Ktorm 会执行一条 SQL，然后我们就能按照上文所述的方法获取查询结果。Ktorm 生成的 SQL 如下：

````sql
select t_employee.id as t_employee_id, t_employee.name as t_employee_name 
from t_employee 
````

可以尝试删除传递给 `select` 方法的参数，即：

````kotlin
val query = database.from(Employees).select()
````

然后，生成的 SQL 就会变成：

````sql
select * 
from t_employee 
````

可能你已经注意到，`select` 函数的参数类型是 `ColumnDeclaring`，而不是 `Column`，这使它不仅可以从表中查询普通的列，还支持使用复杂的表达式和聚合函数。如果我们想知道公司里最高薪员工和最低薪员工的薪水之差，查询可以这样写：

````kotlin
database
    .from(Employees)
    .select(max(Employees.salary) - min(Employees.salary))
    .forEach { row -> println(row.getLong(1)) }
````

这里我们使用了 `max` 和 `min` 两个聚合函数，他们的返回值都是 `AggregateExpression`，然后将他们相减，最终得到一个 `BinaryExpression`，它是 `ColumnDeclaring` 的子类，因此可以直接传入 `select` 方法中。最终生成的 SQL 如下：

````sql
select max(t_employee.salary) - min(t_employee.salary) 
from t_employee 
````

可以看到，生成的 SQL 和我们写出来的 Kotlin 代码高度一致，这得益于 Kotlin 优秀的语法特性。Ktorm 提供了许多重载的运算符，这就是我们能够在上面的查询中使用减号的原因。由于运算符的重载，这里的减号并没有执行实际的减法，而是被翻译为 SQL 中的减号送到数据库中去执行。在[运算符](./operators.html)一节中我们会介绍 Ktorm 提供的其他运算符。

> 有个小遗憾：虽然 `select` 方法支持使用复杂的表达式，但是将查询的结果从 `QueryRowSet` 中取出来时，我们却不能使用前面介绍的索引访问运算符 []，只能使用继承自 `ResultSet` 中的 `getXxx` 方法，使用列的序号获取该列的值。

## selectDistinct

`selectDistinct` 也是 `QuerySource` 的扩展函数，顾名思义，它对应于 SQL 中的 `select distinct` 操作，会将查询的结果进行去重。除此之外，它的使用方法与 `select` 完全一致，在此不再赘述。

## where

`where` 是 `Query` 类的扩展函数，我们先来看看它的签名：

````kotlin
inline fun Query.where(block: () -> ColumnDeclaring<Boolean>): Query
````

它是一个内联函数，接受一个闭包作为参数，我们在这个闭包中指定查询的 where 子句，闭包的返回值是 `ColumnDeclaring<Boolean>`。`where` 函数会创建一个新的 `Query` 对象，它的所有属性都复制自当前 `Query`，并使用闭包的返回值作为其筛选条件。典型的用法如下：

```kotlin
val query = Employees
    .select(Employees.salary)
    .where { (Employees.departmentId eq 1) and (Employees.name like "%vince%") }
```

一眼明了，这个查询的目的是获得部门 1 中名字为 vince 的员工的薪水，生成的 SQL 你应该也能猜到：

````sql
select t_employee.salary as t_employee_salary 
from t_employee 
where (t_employee.department_id = ?) and (t_employee.name like ?) 
````

在 `where` 闭包中，我们可以返回任何查询条件，这里我们使用 `eq`、`and` 和 `like` 运算符构造了一个。infix 是 Kotlin 提供的关键字，使用此关键字修饰的函数，在调用时可以省略点和括号，这样，代码写起来就像说话一样自然，我们这里使用的运算符正是 Ktorm 提供的 infix 函数。

> Ktorm 提供的内置运算符分为两类，一类是通过运算符重载实现的，比如加减乘除取反取余等常用运算，还有一类就是基于 infix 函数实现的，如 `and`、`or`、`eq`、`like`、`greater`、`less` 等 Kotlin 中无法重载的运算符。

有时候，我们的查询需要许多个筛选条件，这些条件使用 and 或 or 运算符连接，他们的数量不定，而且还会根据不同的情况启用不同的条件。为满足这种需求，许多 ORM 框架都提供了名为“动态查询”的特性，比如 MyBatis 的 `<if>` 标签。然而，在 Ktorm 中，这种需求根本就不是问题，因为 Ktorm 的查询都是纯 Kotlin 代码，因此天然具有这种“动态性”。我们看看下面这个查询：

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

这里我们使用一个 `ArrayList` 保存所有查询条件，然后使用 if 语句根据不同的参数是否为空将查询条件添加到 list 中，最后使用一个 reduce 操作将所有条件用 and 连接起来。使用 Ktorm 不需要特别的操作就能够完美支持所谓的“动态查询”。

当然，上面的写法还是有一点漏洞，当所有情况都不满足，list 为空时，reduce 操作会抛出一个异常。为了避免这个异常，可以使用 `conditions.combineConditions()` 代替 reduce 操作。`combineConditions` 是 Ktorm 提供的函数，它的功能就是使用 and 将所有条件连接起来，当 list 为空时，直接返回 true，这是它的实现：

```kotlin
fun Iterable<ColumnDeclaring<Boolean>>.combineConditions(): ColumnDeclaring<Boolean> {
    if (this.any()) {
        return this.reduce { a, b -> a and b }
    } else {
        return ArgumentExpression(true, BooleanSqlType)
    }
}
```

其实，每次都创建一个 `ArrayList`，然后往里面添加条件，最后使用 reduce 连接的操作也挺烦的。Ktorm 提供了一个方便的函数 `whereWithConditions`，可以减少我们的这两行重复代码，使用这个函数，上面的查询可以改写成：

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

使用 `whereWithConditions`，我们只需要在闭包中往 `it` 中添加条件就好了，这个 `it` 就是一个 `MutableList`，创建 list 和合并条件的操作就不需要重复做了。对应地，Ktorm 还提供了一个 `whereWithOrConditions` 函数，这个函数的功能其实是一样的，只不过最后是使用 or 将所有条件连接起来，而不是 and。

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

````sql
select t_employee.department_id as t_employee_department_id, avg(t_employee.salary) 
from t_employee 
group by t_employee.department_id 
having avg(t_employee.salary) > ?
````

值得一提的是，如果我们在这个查询的 `select` 方法中再加一列会怎么样呢，比如我们希望再返回一下员工的名字：

```kotlin
val query = t
    .select(t.departmentId, avg(t.salary), t.name)
    .groupBy(t.departmentId)
    .having { avg(t.salary) greater 100.0 }
```

现在生成的 SQL 是这样的：

````sql
select t_employee.department_id as t_employee_department_id, avg(t_employee.salary), t_employee.name as t_employee_name 
from t_employee 
group by t_employee.department_id 
having avg(t_employee.salary) > ? 
````

然而，了解 SQL 语法的人都知道，这条生成的 SQL 的语法是错误的，完全无法在数据库中执行。这是因为 SQL 语法规定，在使用 group by 时，select 子句中出现的字段，要么是 group by 中的列，要么被包含在聚合函数中。然而，这能怪 Ktorm 吗？这只能怪你对 SQL 的不了解，Ktorm 只是忠实地将你的代码翻译成了 SQL 而已。

> 注意：Ktorm 虽然有 SQL 生成，但是我们的设计目标，从来都不是为了取代 SQL，我们不希望做成一个大而全的“自动化” ORM 框架，相反，我们的目标是充分使用 Kotlin 优越的语法特性，为 SQL 提供方便灵活的 DSL。这要求使用者对 SQL 有一定的了解，因为 Ktorm 的工作只是将 DSL 忠实地翻译成 SQL 而已，SQL 的正确性和性能都需要使用者自己负起责任。

## orderBy

`orderBy` 也是 `Query` 的扩展函数，它对应于 SQL 中的 order by 关键字，下面是它的签名：

```kotlin
fun Query.orderBy(vararg orders: OrderByExpression): Query
```

可以看到，这个函数接受一个或多个 `OrderByExpression`，这就涉及到另外两个函数，它们分别是 `asc` 和 `desc`，和 SQL 中的关键字名称一样：

````kotlin
fun ColumnDeclaring<*>.asc(): OrderByExpression
fun ColumnDeclaring<*>.desc(): OrderByExpression
````

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

````sql
select t_employee.department_id as t_employee_department_id, avg(t_employee.salary) 
from t_employee 
group by t_employee.department_id 
order by avg(t_employee.salary) desc 
````

## limit

SQL 标准中并没有规定如何进行分页查询的语法，因此，每种数据库提供商对其都有不同的实现。例如，在 MySQL 中，分页是通过 `limit m, n` 语法完成的，在 PostgreSQL 中，则是 `limit m offset n`，而 Oracle 则没有提供任何关键字，我们需要在 where 子句使用 rownum 限定自己需要的数据页。

为了抹平不同数据库分页语法的差异，Ktorm 提供了一个 `limit` 函数，我们使用这个函数对查询进行分页：

````kotlin
fun Query.limit(offset: Int, limit: Int): Query
````

`limit` 也是 `Query` 类的扩展函数，它接收两个整形参数，分别是：

- offset: 需要返回的第一条记录相对于整个查询结果的位移，从 0 开始
- limit: 需要返回的记录的数量

使用示例如下，这个查询获取员工表的第一条记录：

````kotlin
val query = Employees.select().limit(0, 1)
````

使用 `limit` 函数时，Ktorm 会根据当前使用的不同数据库（Dialect）生成合适的分页 SQL。但是如果你没有启用任何方言，你可能会得到这样一个异常：

````
me.liuwj.ktorm.database.DialectFeatureNotSupportedException: Pagination is not supported in Standard SQL.
````

这个是正常的，因为标准 SQL 中的确没有规定分页的语法，因此 Ktorm 无法为你生成这种 SQL，要避免这个异常，要么放弃使用 `limit` 函数，要么启用一个数据库方言。关于如何[启用方言](./dialects-and-native-sql.html#启用方言)，可参考后面的章节。

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

````kotlin
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
````

## aliased

在 Ktorm 2.6 版本中，我们支持了列别名的功能，这个功能允许我们为查询的列指定别名，并在后续的 `group by` 和 `having` 等子句中使用它们，就像 SQL 中的 `as` 关键字一样。下面是一个例子，这个查询获取平均工资大于 100 的部门，返回他们的部门 id 以及平均工资：

```kotlin
val deptId = Employees.departmentId.aliased("dept_id")
val salaryAvg = avg(Employees.salary).aliased("salary_avg")

Employees
    .select(deptId, salaryAvg)
    .groupBy(deptId)
    .having { salaryAvg greater 100.0 }
    .forEach { row ->
        println("${row[deptId]}:${row[salaryAvg]}")
    }
```

生成 SQL：

```sql
select t_employee.department_id as dept_id, avg(t_employee.salary) as salary_avg 
from t_employee 
group by dept_id 
having salary_avg > ? 
```

