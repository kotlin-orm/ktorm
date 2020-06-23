---
title: 定义表结构
lang: zh-cn
related_path: en/schema-definition.html
---

# 定义表结构

在使用 SQL DSL 之前，我们首先要让 Ktorm 能够了解我们的表结构。假设我们有两个表，他们分别是部门表 `t_department` 和员工表 `t_employee`， 它们的建表 SQL 如下，我们要如何描述这两个表呢？

```sql
create table t_department(
  id int not null primary key auto_increment,
  name varchar(128) not null,
  location varchar(128) not null
);

create table t_employee(
  id int not null primary key auto_increment,
  name varchar(128) not null,
  job varchar(128) not null,
  manager_id int null,
  hire_date date not null,
  salary bigint not null,
  department_id int not null
);
```

## 表对象

一般来说，Ktorm 使用 Kotlin 中的 object 关键字定义一个继承 `Table` 类的对象来描述表结构，上面例子中的两个表可以像这样在 Ktorm 中定义：

```kotlin
object Departments : Table<Nothing>("t_department") {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val location = varchar("location")
}

object Employees : Table<Nothing>("t_employee") {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val job = varchar("job")
    val managerId = int("manager_id")
    val hireDate = date("hire_date")
    val salary = long("salary")
    val departmentId = int("department_id")
}
```

可以看到，`Departments` 和 `Employees` 都继承了 `Table`，并且在构造函数中指定了表名，`Table` 类还有一个泛型参数，它是此表绑定到的实体类的类型，在这里我们不需要绑定到任何实体类，因此指定为 `Nothing` 即可。

表中的列则使用 val 关键字定义为表对象中的成员属性，列的类型使用 int、long、varchar、date 等函数定义，它们分别对应了 SQL 中的相应类型，这些类型定义函数的普遍特征如下：

- 它们是 `Table` 类的扩展函数，只能在定义表对象时使用
- 它们的名称一般对应于其实际的 SQL 类型的名称
- 它们都接收一个字符串的参数，在这里我们需要把列的名称传入
- 它们的返回值都是 `Column<C>`，C 为该列的类型，我们可以链式调用 `primaryKey` 扩展函数，将当前列声明为主键

通常我们都会将表定义为 Kotlin 单例对象，但我们其实不必拘泥于此。例如，在某些情况下，我们有两个结构完全相同的表，只是表名不同（在数据备份的时候比较常见），难道我们一定要在每一个表对象中都写一遍完全相同的字段定义吗？当然不需要，这里我们可以使用继承重用代码：

```kotlin
sealed class Employees(tableName: String) : Table<Nothing>(tableName) {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val job = varchar("job")
    val managerId = int("manager_id")
    val hireDate = date("hire_date")
    val salary = long("salary")
    val departmentId = int("department_id")
}

object RegularEmployees : Employees("t_regular_employee")

object FormerEmployees : Employees("t_former_employee")
```

再比如，有时我们的某个表只需要使用一次，因此没有必要将其定义为全局对象，以免污染命名空间。这时，我们甚至可以在函数内部使用匿名对象定义一个表：

```kotlin
val t = object : Table<Nothing>("t_config") {
    val key = varchar("key").primaryKey()
    val value = varchar("value")
}

// Get all configs as a Map<String, String>
val configs = database.from(t).select().associate { row -> row[t.key] to row[t.value] }
```

灵活使用 Kotlin 的语法特性可以帮助我们减少重复代码、提高项目的可维护性。

## SqlType

`SqlType` 是一个抽象类，它为 SQL 中的数据类型提供了统一的抽象，基于 JDBC，它封装了从 `ResultSet` 中获取数据，往 `PreparedStatement` 设置参数等通用的操作。在前面定义表中的字段时，我们曾使用了表示不同类型的 int、varchar 等列定义函数，这些函数的背后其实都有一个特定的 `SqlType` 的子类。比如 int 函数的实现是这样的：

```kotlin
fun BaseTable<*>.int(name: String): Column<Int> {
    return registerColumn(name, IntSqlType)
}

object IntSqlType : SqlType<Int>(Types.INTEGER, typeName = "int") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Int) {
        ps.setInt(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Int? {
        return rs.getInt(index)
    }
}
```

`IntSqlType` 的实现特别简单，它只是使用了 `ResultSet.getInt` 函数获取来自结果集中的数据，使用 `PreparedStatement.setInt` 设置传递给数据库的参数而已。

Ktorm 默认支持的数据类型如下表：

| 函数名        | Kotlin 类型             | 底层 SQL 类型 | JDBC 类型码 (java.sql.Types) |
| ------------- | ----------------------- | ------------- | ---------------------------- |
| boolean       | kotlin.Boolean          | boolean       | Types.BOOLEAN                |
| int           | kotlin.Int              | int           | Types.INTEGER                |
| long          | kotlin.Long             | bigint        | Types.BIGINT                 |
| float         | kotlin.Float            | float         | Types.FLOAT                  |
| double        | kotlin.Double           | double        | Types.DOUBLE                 |
| decimal       | java.math.BigDecimal    | decimal       | Types.DECIMAL                |
| short         | kotlin.Short            | smallint      | Types.SMALLINT               |
| varchar       | kotlin.String           | varchar       | Types.VARCHAR                |
| text          | kotlin.String           | text          | Types.LONGVARCHAR            |
| blob          | kotlin.ByteArray        | blob          | Types.BLOB                   |
| bytes         | kotlin.ByteArray        | bytes         | Types.BINARY                 |
| jdbcTimestamp | java.sql.Timestamp      | timestamp     | Types.TIMESTAMP              |
| jdbcDate      | java.sql.Date           | date          | Types.DATE                   |
| jdbcTime      | java.sql.Time           | time          | Types.TIME                   |
| timestamp     | java.time.Instant       | timestamp     | Types.TIMESTAMP              |
| datetime      | java.time.LocalDateTime | datetime      | Types.TIMESTAMP              |
| date          | java.time.LocalDate     | date          | Types.DATE                   |
| time          | java.time.Time          | time          | Types.TIME                   |
| monthDay      | java.time.MonthDay      | varchar       | Types.VARCHAR                |
| yearMonth     | java.time.YearMonth     | varchar       | Types.VARCHAR                |
| year          | java.time.Year          | int           | Types.INTEGER                |
| enum          | kotlin.Enum             | enum          | Types.VARCHAR                |
| uuid          | java.util.UUID          | uuid          | Types.OTHER                  |

## 扩展更多的类型

有时候，Ktorm 内置的这些数据类型可能并不能完全满足你的需求，比如你希望在数据库中存储一个 json 字段，许多关系数据库都已经支持了 json 类型，但是原生 JDBC 并不支持，Ktorm 也并没有默认支持。这时你可以自己提供一个 `SqlType` 的实现：

```kotlin
class JsonSqlType<T : Any>(type: java.lang.reflect.Type, val objectMapper: ObjectMapper) 
    : SqlType<T>(Types.VARCHAR, typeName = "json") {
        
    private val javaType = objectMapper.constructType(type)

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T) {
        ps.setString(index, objectMapper.writeValueAsString(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): T? {
        val json = rs.getString(index)
        if (json.isNullOrBlank()) {
            return null
        } else {
            return objectMapper.readValue(json, javaType)
        }
    }
}
```

上面这个类使用 Jackson 框架进行 json 与对象之间的转换，提供了 json 数据类型的支持。有了 `JsonSqlType` 之后，怎样使用这个类型定义一个列呢？在前面 int 函数的实现中，我们注意到其中调用了 `registerColumn` 函数，这正是其中的秘诀，`registerColumn` 函数正是 Ktorm 提供的用来支持类型扩展的入口。我们可以写一个这样的扩展函数：

```kotlin
fun <C : Any> BaseTable<*>.json(
    name: String,
    typeReference: TypeReference<C>,
    objectMapper: ObjectMapper = sharedObjectMapper
): Column<C> {
    return registerColumn(name, JsonSqlType(typeReference.referencedType, objectMapper))
}
```

使用方式如下：

```kotlin
object Foo : Table<Nothing>("foo") {
    val bar = json("bar", typeRef<List<Int>>())
}
```

这样，Ktorm 就能无缝支持 json 字段的存取，事实上，这正是 ktorm-jackson 模块的功能之一。如果你真的需要使用 json 字段，请直接在项目中添加依赖即可，不必再写一遍上面的代码，这里仅作示范。

Maven 依赖： 

```xml
<dependency>
    <groupId>me.liuwj.ktorm</groupId>
    <artifactId>ktorm-jackson</artifactId>
    <version>${ktorm.version}</version>
</dependency>
```

或者 gradle： 

```groovy
compile "me.liuwj.ktorm:ktorm-jackson:${ktorm.version}"
```

最后，Ktorm 2.7 版本还新增了一个 `transform` 函数，使用这个函数，我们可以基于现有的数据类型进行扩展，增加一些自定义的转换行为，得到新的数据类型，而不必手动写一个 `SqlType` 的实现类。

例如下面的例子，我们定义了一个类型为 `Column<UserRole>` 的列，但是在数据库中保存的还是 `int` 值，只是在获取结果及设置参数到 `PreparedStatement` 时执行了一定的转换：

```kotlin
val role = int("role").transform({ UserRole.fromCode(it) }, { it.code })
```

需要注意的是，这个转换在获取每条结果时都会执行一次，所以在这里不要有太重的行为，以免对性能造成影响。