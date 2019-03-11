---
title: Schema Definition
lang: en
related_path: zh-cn/schema-definition.html
---

# Schema Definition

To use SQL DSL, we need to let Ktorm know our schemas. Assuming we have two tables, `t_department` and `t_employee`, their schemas are given in SQL below, how do we descript these two tables with Ktorm?

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

## Table Objects

Generally, we can define a Kotlin object extending `Table` to descript our table schemas in Ktorm. The following code defines the two tables with Ktorm: 

```kotlin
object Departments : Table<Nothing>("t_department") {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val location by varchar("location")
}

object Employees : Table<Nothing>("t_employee") {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val job by varchar("job")
    val managerId by int("manager_id")
    val hireDate by date("hire_date")
    val salary by long("salary")
    val departmentId by int("department_id")
}
```

We can see that both `Departments` and `Employees` are extending from `Table` whose constructor accepts a table name as parameter. There is also a generic type parameter in `Table` class, that is the entity class's type that current table is binding to. Here we don't bind to any entity classes, so `Nothing` is OK. 

Columns are defined as properties in table objects by Kotlin's *val* and *by* keyword, their types are defined by type definition functions, such as int, long, varchar, date, etc. Commonly, these type definition functions follows the rules below:  

- They are all `Table` class's extension functions that are only allowed to used in table object definitions. 
- Their names are corresponding to the underlying SQL types' names. 
- They all accept a parameter of string type, that is the column's name.
- Their return types are `Table<E>.ColumnRegistration<C>`, in which E is the entity class, C is the type of current column. We can chaining call the `primaryKey` function on `ColumnRegistration` to declare current column as a primary key. 

> `ColumnRegistration` implements the `ReadOnlyProperty` interface, so we can use it as a [property delegate](https://kotlinlang.org/docs/reference/delegated-properties.html) via Kotlin's *by* keyword. Therefore, in the definition `val name by varchar("name")`, although the return type of `varchar` is `ColumnRegistration<String>`, the `val name` property's type is `Column<String>`. For the same reason, the `val managerId by int("manager_id")` property's type is `Column<Int>`.

In general, we define tables as Kotlin singleton objects, but we don't really have to stop there. For instance, assuming that we have two tables that are totally the same, they have the same columns, but their names are different. In this special case, do we have to copy the same column definitions to each table? No, we don't. We can reuse our codes by subclassing: 

```kotlin
sealed class Employees(tableName: String) : Table<Nothing>(tableName) {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val job by varchar("job")
    val managerId by int("manager_id")
    val hireDate by date("hire_date")
    val salary by long("salary")
    val departmentId by int("department_id")
}

object RegularEmployees : Employees("t_regular_employee")

object FormerEmployees : Employees("t_former_employee")
```

For another example, sometimes our table is on-off, we don't need to use it twice, so it's not necessary to define it as a global object, for fear that the naming space is polluted. This time, we can even define the table as an anonymous object inside a function: 

```kotlin
val t = object : Table<Nothing>("t_config") {
    val key by varchar("key").primaryKey()
    val value by varchar("value")
}

// Get all configs as a Map<String, String>
val configs = t.select().associate { row -> row[t.key] to row[t.value] }
```

Flexible usage of Kotlin's language feactures is helpful for us to reduce duplicated code and improve the maintainability of our projects. 

## SqlType

`SqlType` is an abstract class which provides a unified abstraction for data types in SQL. Based on JDBC, it encapsulates the common operations of obtaining data from a `ResultSet` and setting parameters to a `PreparedStatement`. In the section above, we defined columns by column definition functions, eg. int, varchar, etc. All these functions have a impelemtation of `SqlType` behind them. For example, here is the implementation of `int` function: 

```kotlin
fun <E : Entity<E>> Table<E>.int(name: String): Table<E>.ColumnRegistration<Int> {
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

`IntSqlType` is simple, it just obtaining int query results via `ResultSet.getInt` and setting parameters via `PreparedStatement.setInt`. 

Here is a list of SQL types supported in Ktorm by default: 

| Function Name | Kotlin Type             | Underlying SQL Type | JDBC Type Code (java.sql.Types) |
| ------------- | ----------------------- | ------------------- | ------------------------------- |
| boolean       | kotlin.Boolean          | boolean             | Types.BOOLEAN                   |
| int           | kotlin.Int              | int                 | Types.INTEGER                   |
| long          | kotlin.Long             | bigint              | Types.BIGINT                    |
| float         | kotlin.Float            | float               | Types.FLOAT                     |
| double        | kotlin.Double           | double              | Types.DOUBLE                    |
| decimal       | java.math.BigDecimal    | decimal             | Types.DECIMAL                   |
| varchar       | kotlin.String           | varchar             | Types.VARCHAR                   |
| text          | kotlin.String           | text                | Types.LONGVARCHAR               |
| blob          | kotlin.ByteArray        | blob                | Types.BLOB                      |
| datetime      | java.time.LocalDateTime | datetime            | Types.TIMESTAMP                 |
| date          | java.time.LocalDate     | date                | Types.DATE                      |
| time          | java.time.Time          | time                | Types.TIME                      |
| monthDay      | java.time.MonthDay      | varchar             | Types.VARCHAR                   |
| yearMonth     | java.time.YearMonth     | varchar             | Types.VARCHAR                   |
| year          | java.time.Year          | int                 | Types.INTEGER                   |
| timestamp     | java.time.Instant       | timestamp           | Types.TIMESTAMP                 |

## Extend More Data Types

Sometimes, Ktorm built-in data types may not satisfy your requirements. For example, you may want to save a json column to a table, many relational databases have supported json data type, but raw JDBC haven't yet, nor Ktorm doesn't support it by default. Now you can do it by yourself: 

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

The class above is a subclass of `SqlType`, it provides json data type support via Jackson framework. Now we have `JsonSqlType`, how can we use it to define a column? Looking back the `int` function's implementation above, we notice that the `registerColumn` function was called. This function is exactly the entry provided by Ktorm to support data type extensions. We can also write an extension function like this: 

```kotlin
fun <E : Entity<E>, C : Any> Table<E>.json(
    name: String,
    typeReference: TypeReference<C>,
    objectMapper: ObjectMapper = sharedObjectMapper
): Table<E>.ColumnRegistration<C> {
    return registerColumn(name, JsonSqlType(typeReference.referencedType, objectMapper))
}
```

The usage is as follows: 

```kotlin
object Foo : Table<Nothing>("foo") {
    val bar by json("bar", typeRef<List<Int>>())
}
```

In this way, Ktorm are able to reading and writing json columns now. Actually, this is one of the features of ktorm-jackson module, if you really need to use json columns, you don't have to repeat the code above, please add the dependency to your project: 

Maven： 

```xml
<dependency>
    <groupId>me.liuwj.ktorm</groupId>
    <artifactId>ktorm-jackson</artifactId>
    <version>${ktorm.version}</version>
</dependency>
```

Or Gradle： 

```groovy
compile "me.liuwj.ktorm:ktorm-jackson:${ktorm.version}"
```

