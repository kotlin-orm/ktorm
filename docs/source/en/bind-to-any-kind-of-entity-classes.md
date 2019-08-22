---
title: Define Entities as Any Kind of Classes
lang: en
related_path: zh-cn/bind-to-any-kind-of-entity-classes.html
---

# Define Entities as Any Kind of Classes

In Ktorm 2.5, we did a refactoring of the code. This refactoring allowed us defining entities as any kind of classes, such as data class, POJO, and so on. From then on, the entity classes in Ktorm do not have to be defined as interfaces extending from `Entity` anymore. This reduces the invasion of user code to some extent, which is very important for a common-used library. 

> About how to define entities as interfaces, see the documentation of [Entities & Column Binding](/en/entities-and-column-binding.html).

## Table & BaseTable

Before the refactoring, `Table` was the common base class of all table objects in Ktorm, providing basic abilities of table definition, column definition, and binding support to `Entity` interfaces. But now, there is a more fundamental base class `BaseTable` on top of `Table`. 

`BaseTable` is an abstract class. It is the common base class of all table objects after Ktorm 2.5. It provides the basic ability of table and column definition but doesn't support any binding mechanisms. There is an abstract function `doCreateEntity` in `BaseTable`. Subclasses should implement this function, creating an entity object from the result set returned by a query, using the binding rules defined by themselves. Here, the type of the entity object could be an interface extending from `Entity`, or a data class, POJO, or any kind of classes. 

Just like before, `Table` limits our entity classes with an upper bound `Entity` on the type parameter. It provides the basic ability of table and column definition as it's a subclass of `BaseTable`, and it also supports a binding mechanism with `Entity` interfaces based on functions such as `bindTo`, `references`. Additionally, `Table` implements the `doCreateEntity` function from the parent class. This function automatically creates an entity object using the binding configuration specified by `bindTo` and `references`, reading columns' values from the result set and filling them into corresponding entity properties. 

## Define an Entity as Data Class

To use data classes, we should define our table objects as subclasses of `BaseTable` instead of `Table`. Also, it's not needed to call `bindTo` and `references` anymore because `BaseTable` doesn't support any binding mechanisms. Instead, we implement the `doCreateEntity` function, creating an entity object from the result set manually by ourselves. 

Here is an example: 

```kotlin
data class Staff(
    val id: Int,
    val name: String,
    val job: String,
    val managerId: Int,
    val hireDate: LocalDate,
    val salary: Long,
    val sectionId: Int
)

object Staffs : BaseTable<Staff>("t_employee") {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val job by varchar("job")
    val managerId by int("manager_id")
    val hireDate by date("hire_date")
    val salary by long("salary")
    val sectionId by int("department_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Staff(
        id = row[id] ?: 0,
        name = row[name].orEmpty(),
        job = row[job].orEmpty(),
        managerId = row[managerId] ?: 0,
        hireDate = row[hireDate] ?: LocalDate.now(),
        salary = row[salary] ?: 0,
        sectionId = row[sectionId] ?: 0
    )
}
```

As you can see, the `Staff` here is just a simple data class. Ktorm doesn't have any special requirements for this class. It is no longer necessary to define it as an interface, which minimizes the intrusion of the framework to user code. The table object `Staffs` is also defined as a subclass of `BaseTable` and implements the `doCreateEntity` function, in which we get columns' values via square brackets `[]` and fill them into the data object. 

Technically, it is OK for us to end this article here, because the usages (such as SQL DSL, Sequence APIs, etc) are totally the same as before. Here are some simple examples. 

Query data via SQL DSL: 

```kotlin
val staffs = Staffs
    .select(Staffs.id, Staffs.name)
    .where { Staffs.id eq 1 }
    .map { Staffs.createEntity(it) }
```

Obtain entity objects via `find*` functions: 

```kotlin
val staffs = Staffs.findList { it.sectionId eq 1 }
```

Obtain entity objects via Sequence APIs, and sorting them by the specific column: 

```kotlin
val staffs = Staffs.asSequence()
    .filter { it.sectionId eq 1 }
    .sortedBy { it.id }
    .toList()
```

Get the number of staffs with a salary of less than 100 thousand in each department: 

```kotlin
val counts = Staffs.asSequence()
    .filter { it.salary less 100000L }
    .groupingBy { it.sectionId }
    .eachCount()
```

For more usages, see the documentation of [SQL DSL](/en/query.html) and [Entity Sequence](/en/entity-sequence.html). 

## Limitation

However, data classes are not perfect, and that's why Ktorm decided to use `Entity` interfaces when it was originally designed. In fact, even after Ktorm 2.5 released, defining entities as interfaces is still our first choice because there are currently two limitations to using data classes: 

- **Column bindings are not available:** Since `BaseTable` is directly used as the parent class, we cannot configure the bindings between database columns and entity properties via `bindTo` and `references` while defining our table objects. Therefore, each table object must implement the `doCreateEntity` function, in which we should create our entity objects manually. 
- **Entity manipulation APIs are not available:** Since we define entities as data classes, Ktorm cannot proxy them and cannot detect the status changes of entity objects, which makes it impossible for us to use entity manipulation APIs such as `Table.add`, `Entity.flushChanges`, etc. But SQL DSL is not affected. We can still use DSL function such as `BaseTable.insert` and `BaseTable.update` to perform our data modifications. 

Because of these limitations, you should think carefully before you decide to define your entities as data classes. You might be benefited from using data classes and you would lose other things at the same time. Remember: **Defining entities as interfaces is still our first choice.** 
