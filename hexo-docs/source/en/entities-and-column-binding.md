---
title: Entities and Column Binding
lang: en
related_path: zh-cn/entities-and-column-binding.html
---

# Entities and Column Binding

We've learned Ktorm's SQL DSL in former sections, but Ktorm is still far from being an ORM framework if it only provides the DSL. Now, we will introduce entities, and learn how to bind relational tables to them. That's exactly the core of an ORM framework: object-relational mapping. 

## Define Entities

We still take the two tables `t_department` and `t_employee` as an exmaple, creating two entity classes with Ktorm to present our departments and employees: 

```kotlin
interface Department : Entity<Department> {
    val id: Int
    var name: String
    var location: String
}

interface Employee : Entity<Employee> {
    val id: Int?
    var name: String
    var job: String
    var manager: Employee?
    var hireDate: LocalDate
    var salary: Long
    var department: Department
}
```

We can see classes above both extends from `Entity<E>` interface, which injects some useful functions into entities. Their properties are defined by keyword var or val, you can mark the types as nullable or not depending on your business requirements. 

It may be counterintuitive that entities in Ktorm are not data classes, even not normal classes, but interfaces instead, that's a design requiment of Ktorm. By defining enties as interfaces, Ktorm can implement some special features, you will see the significance later. 

As everyone knows, interfaces can not be instantiated, now that all entities are interfaces, how can we create their instances? Ktorm provides an `Entity.create` function, which generates implementations for entity interfaces via JDK dynamic proxy, and create their instances for us. To create a department object, we can do this: 

```kotlin
val department = Entity.create<Department>()
```

If you don't like creating objects in that way, Ktorm also provides an abstract class `Entity.Factory`. We can add a companion object to our entity class extending from `Entity.Factory`: 

```kotlin
interface Department : Entity<Department> {
    companion object : Entity.Factory<Department>()
    val id: Int
    var name: String
    var location: String
}
```

The `Entity.Factory` class overloads invoke operator, so we can use brackets to call the companion object as it's a function. The code creating a department object: 

```kotlin
val department = Department()
```

That's the charm of Kotlin, `Department` is an interface, but we can still create its instances, just like calling a constructor function. Moreover, we can also init some properties when creating entity objects: 

```kotlin
val department = Department {
    name = "tech"
    location = "Guangzhou"
}
```

## Column Binding

The core feature of an ORM framework is to bind database tables to entites, bind tables' columns to entities' properties. Now let's learn how to do that with Ktorm. 

In former sections learning SQL DSL, we created two table objects, they are `Departments` and `Employees`. In the table objects, we defined columns by calling column definition functions such as `int`, `long`, `varchar`, etc. The return type of these functions is `Table<E>.ColumnRegistration<C>`, in which `E` is the entity's type, `C` is the declaring column's type.

It's easy to bind a column to an entity's property, we just need to chaining call the `bindTo` or `references` function in `ColumnRegistration`. The code below modifies those two table objects and completes the O-R bindings: 

```kotlin
object Departments : Table<Department>("t_department") {
    val id by int("id").primaryKey().bindTo { it.id }
    val name by varchar("name").bindTo { it.name }
    val location by varchar("location").bindTo { it.location }
}

object Employees : Table<Employee>("t_employee") {
    val id by int("id").primaryKey().bindTo { it.id }
    val name by varchar("name").bindTo { it.name }
    val job by varchar("job").bindTo { it.job }
    val managerId by int("manager_id").bindTo { it.manager.id }
    val hireDate by date("hire_date").bindTo { it.hireDate }
    val salary by long("salary").bindTo { it.salary }
    val departmentId by int("department_id").references(Departments) { it.department }
}
```

> Naming Strategy: It's highly recommended to name your entity classes by singular nouns, name table objects by plurals (eg. Employee/Employees, Department/Departments).

Comparing the table objects with before, we can find two differences: 

1. The type parameter of `Table` is specified to the entity's type now, that's the way we bind table objects to entity classes. We set this parameter to `Nothing` before, that meant the table object was not bound to any entity class. 
2. After the calling of column definition functions, we chaining call `bindTo` or `references` functions to bind the current column to a property in the entity class. If we don't do that, the column won't be bound to any property. 

The significance of column bindings is that, while obtaining entities from databases (eg. `findList` function), Ktorm will use our binding configurations to fill columns' values to their corresponding properties, and while updating entities' changes into databases (eg. `flushChanges` function), Ktorm will also use the configurations to find corresponding columns of properties.  

Ktorm provides the following different binding types: 

1. **Simple Binding:** Use `bindTo` function to bind a column to a simple property, eg. `c.bindTo { it.name }`. 
2. **Reference Binding:** Use `references` function to bind a column to another table, eg. `c.references(Departments) { it.department }`, equivalent to the foreign key in databases. Using reference binding, while obtaining entities from databases (eg. `findList`, `findOne`, etc), Ktorm will auto left join all its reference tables, obtaining the referenced entity objects at the same time. 
3. **Nested Binding:** Use `bindTo` function to bind a column to nested properties, for example `c.bindTo { it.manager.department.id }`. While obtaining entities from databases, the value of this column will be filled to `employee.manager.department.id`. With only a single level of properties, simple binding is a special case of nested binding. 
4. **Aliased Binding:** At times we need to bind a column to multiple properties, but it's a pity that we can only call `bindTo` or `references` once on a `ColumnRegistration`. Ktorm provides an `aliased` function to create a copy of current column with a specific alias, then we can bind this new created column to any property we want, that's equivalent to the label syntax `select name as label` in SQL. For example, assuming that we have a `t_foo` table, in which there is only one column `bar`, and its binding configurations (with an aliased binding in it) is given as follows. In this example, while obtaining entities from databases, the column's values will be filled to both `bar` and `barCopy` properties. Please note that aliased bindings are only available for query operations, they will be ignored when inserting or updating entities. 

```kotlin
interface Foo : Entity<Foo> {
    val bar: String
    val barCopy: String
}

object Foos : Table<Foo>("t_foo") {
    val bar by varchar("bar").bindTo { it.bar }
    val barCopy by bar.aliased("bar_copy").bindTo { it.barCopy }
}
```

## More About Entities

We know that Ktorm's entity classes should be defined as interfaces extending from `Entity`, and we create entity objects via JDK dynamic proxy. If you have used dynamic proxy before, you may know proxy objects are created by `Proxy.newProxyInstance` method, providing an instance of `InvocationHandler`. When a method is invoked on a proxy instance, the method invocation is encoded and dispatched to the invocation handler. In Ktorm, `EntityImpl` is the implementation of entities' invocation handler. It's marked as internal, so we can not use it outside Ktorm, but we can learn its basic principles. 

### Getting and Setting Properties

When we define a property `var name: String` in Kotlin, we actually define two methods in Java byte code, they are `public String getName()` and `public void setName(String name)`. The invocations on these two methods will also be dispatched to `EntityImpl`. 

There is a `values` property in `EntityImpl`, its type is `LinkedHashMap<String, Any?>`, and it holds all property values of the entity object. When we use `e.name` to obtain the property's value, `EntityImpl` receives an invocation on `getName()` method, then it will get the value from the `values` using the key "name". When we use `e.name = "foo"` to modify the property, `EntityImpl` also receives an invocation on `setName()` method, then it will put the given value to `values` and save some additional infomation to track the entity's status changes. 

That is to say, behind every entity object, there is a value table that holds all the values of its properties. Any operation of getting or setting a property is actually operating the underlying value table. However, what if the value doesn't exist while we are getting a property? It's possible because any new created entity object has an empty underlying value table. Ktorm defines a set of rules for this situation: 

- If the value doesn't exist and the property's type is marked nullable, eg `var name: String?`, then we'll return null. 
- If the value doesn't exist and the property's type is not nullable, eg `var name: String`, then we can not return null anymore, because the null value here can cause an unexpected null pointer exception, we'll return the type's default value instead. 

The default values of different types are well-defined: 

- For `Boolean` type, the default value is false.
- For `Char` type, the default value is \u0000. 
- For number types (such as `Int`, `Long`, `Double`, etc), the default value is zero. 
- For `String` type, the default value is the empty string. 
- For entity types, the default value is a new created entity object which is empty. 
- For enum types, the default value is the first value of the enum, whose ordinal is 0. 
- For array types, the default value is a new created empty array. 
- For collection types (such as `Set`, `List`, `Map`, etc), the default value is a new created mutable collection of the concrete type. 
- For any other types, the default value is an instance created by its no-args constructor. If the constructor doesn't exist, an exception is thrown. 

Moreover, there is a cache mechanism for default values in `EntityImpl`, that ensures a property always reutrns the same default value instance even if it's called twice or more. This can avoid some counterintuitive bugs. 

### Non-abstract Members

If we are using domain driven design, then entities are not only data containers that hold property values, there are also some behaviors, so we need to add some business functions to our entities. Fortunately, Kotlin allows us to define non-abstract functions in interfaces, that's why we don't loss anything even if Ktorm's entity classes are all interfaces. Here is an example: 

```kotlin
interface Foo : Entity<Foo> {
    companion object : Entity.Factory<Foo>()
    val name: String
    
    fun printName() {
        println(name)
    }
}
```

Then if we call `Foo().printName()`, the value of the property `name` will be printed. 

> That looks natural, but the underlying implementation is not that simple. We know that Ktorm creates entity objects via JDK dynamic proxy, and the invocation on `printName` function will also be delegated into `EntityImpl`. When `EntityImpl` receives the invocation, it finds that the calling function is not abstract, then it will search the default implementation in the generated `DefaultImpls` class and call it. That's transparent to us, and it looks not different from calling the function directly for us. Moreover, if we add a `@JvmDefault` annotation to the function, Ktorm may not be able to find the `DefaultImpls` class anymore, but that has little influence for us to use Ktorm, so just let it go. If you are realy interested, please refer to [Kotlin Reference](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-default/index.html).

Besides of non-abstract functions, Kotlin also allows us to define properties with custom getters or setters in interfaces. For example, in the following code, if we call the `upperName` property, then the value of the `name` property will be returned in upper case. The principle is the same as we discussed above. 

```kotlin
interface Foo : Entity<Foo> {
    val name: String
    val upperName get() = name.toUpperCase()
}
```

### Serialization

The `Entity` interface extends from `java.io.Serializable`, so all entity objects are serializable by default. We can save them to our disks, or transfer them between systems through network. 

Note that Ktorm only saves entities' property values when serialization, any other data that used to track entity status are lost (marked as transient). So we can not obtain an entity object from one system, then flush its changes into the database in another system. 

> Java uses `ObjectOutputStream` to serialize objects, and uses `ObjectInputStream` to deserialize them, you can refer to their documentations for more details. 

Besides of JDK serializatiion, the ktorm-jackson module also supports serializing entities in JSON format. This module provides an extension for Jackson, the famous JSON framework in Java word. It supports serializing entity objects into JSON format, and parsing JSONs as entity objects. We just need to register the `KtormModule` into an `ObjectMapper`: 

```kotlin
val objectMapper = ObjectMapper()
objectMapper.registerModule(KtormModule())
```

Or use `findAndRegisterModules` method to auto detect and register it: 

```kotlin
val objectMapper = ObjectMapper()
objectMapper.findAndRegisterModules()
```

Now, we can use this `objectMapper` to do the serialization and deserialization for entities, please refer to Jackson's documentations for more details. 

That's the two serialization formats supported by Ktorm, if you need more serialization formats, please raise your issue, or you can do it by yourself and send a pull request to me. Welcome for you contributions!