---
title: 实体类与列绑定
lang: zh-cn
related_path: en/entities-and-column-binding.html
---

# 实体类与列绑定

前面我们已经介绍了 SQL DSL，但是如果只有 DSL，Ktorm 还远不能称为一个 ORM 框架。接下来我们将介绍实体类的概念，了解如何将数据库中的表与实体类进行绑定，这正是 ORM 框架的核心：对象 - 关系映射。

## 定义实体类

我们仍然以前面的部门表 `t_department` 和员工表 `t_employee` 为例，创建两个 Ktorm 的实体类，分别用来表示部门和员工这两个业务概念：

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

可以看到，Ktorm 中的实体类都继承了 `Entity<E>` 接口，这个接口为实体类注入了一些通用的方法。实体类的属性则使用 var 或 val 关键字直接定义即可，根据需要确定属性的类型及是否为空。有一点可能会违背你的直觉，Ktorm 中的实体类并不是 data class，甚至也不是一个普通的 class，而是 interface。这是 Ktorm 的设计要求，通过将实体类定义为 interface，Ktorm 才能够实现一些特别的功能，以后你会了解到它的意义。

> 从 Ktorm 2.5 版本开始，我们也支持使用 data class 或其他任意的类定义实体类，参见[使用任意的类作为实体类](/zh-cn/define-entities-as-any-kind-of-classes.html)。

众所周知，接口并不能被实例化，既然实体类被定义为接口，我们要如何才能创建一个实体对象呢？Ktorm 提供了一个 `Entity.create` 函数，这个函数会使用 JDK 动态代理生成实体类接口的实现，并为我们创建一个实体对象。要创建一个部门对象，可以这样写：

````kotlin
val department = Entity.create<Department>()
````

如果你不喜欢这样创建实体对象，Ktorm 还提供了一个 `Entity.Factory` 抽象类，你可以在实体类中添加一个伴随对象，继承 `Entity.Factory`，就像这样：

```kotlin
interface Department : Entity<Department> {
    companion object : Entity.Factory<Department>()
    val id: Int
    var name: String
    var location: String
}
```

因为 `Entity.Factory` 类重载了 invoke 运算符，所以你可以把这个伴随对象当函数一样直接加上括号进行调用，创建一个部门对象的代码变成了这样：

````kotlin
val department = Department()
````

这就是 Kotlin 的魅力，`Department` 明明是一个接口，你却能创建一个它的对象，而且看起来就像在调用构造函数一样。你还可以在创建实体类的同时，为它的属性赋上初始值：

```kotlin
val department = Department {
    name = "tech"
    location = "Guangzhou"
}
```

## 列绑定

ORM 框架的一大功能就是将数据表与实体类进行绑定、将表中的列与实体类中的属性进行绑定，现在我们来了解 Ktorm 如何实现这个功能。

在前面介绍 SQL DSL 的时候，我们已经创建了两个表对象，他们分别是部门表 `Departments` 和员工表 `Employees`，在表对象中，使用 int、long、varchar 等函数声明表中的列。这些声明列的函数的返回值都是 `Table<E>.ColumnRegistration<C>`，在这里，`E` 代表实体类的类型，`C` 代表被声明的列的类型。

要将列绑定到实体类的属性十分简单，只需要链式调用 `ColumnRegistration` 类中的 `bindTo` 函数或 `references` 函数即可，下面的代码修改了前面的两个表对象，完成了 ORM 绑定：

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

> 命名规约：强烈建议使用单数名词命名实体类，使用名词的复数形式命名表对象，如：Employee/Employees、Department/Departments。

把两个表对象与修改前进行对比，我们可以发现两处不同：

1. `Table` 类的泛型参数，我们需要指定为实体类的类型，以便 Ktorm 将表对象与实体类进行绑定；在之前，我们设置为 `Nothing` 表示不绑定到任何实体类。
2. 在每个列声明函数的调用后，都链式调用了 `bindTo` 或 `references` 函数将该列与实体类的某个属性进行绑定；如果没有这个调用，则不会绑定到任何属性。

列绑定的意义在于，通过查询从数据库中获取实体对象的时候，Ktorm 会根据我们的绑定配置，将某个列的数据填充到它所绑定的属性中去；在将实体类中的修改更新回数据库中的时候（使用 `flushChanges` 函数），Ktorm 也会根据我们的绑定配置，将某个属性的变更，同步更新到绑定它的那个列。

Ktorm 提供以下几种不同的绑定类型：

1. **简单绑定：**使用 `bindTo` 函数将列绑定到一个简单的属性上，如 `c.bindTo { it.name }`。
2. **嵌套绑定：**使用 `bindTo` 函数将列绑定到多层嵌套的某个属性上，如 `c.bindTo { it.manager.department.id }`；这样，从数据库中获取该列时，它的值会被填充到 `employee.manager.department.id` 中；把修改更新到数据库时，只要嵌套的属性中的任何一级发生变化，都会将新的值同步更新到所绑定的这个列。简单绑定其实也是嵌套绑定的一种特例，只不过嵌套的属性只有一层。
3. **引用绑定：**使用 `references` 函数将列绑定到另一个表，如 `c.references(Departments) { it.department }`，相当于数据库中的外键引用。使用引用绑定的列，在通过实体查询函数从数据库中获取当前实体对象的时候，会自动递归地 left join 其关联表，并将关联的实体对象也一并获取。

另外，Ktorm 2.6 及以上版本还支持了多重绑定的功能，我们可以通过连续调用 `bindTo` 或 `references` 函数把一个列绑定到多个属性上。这样，当通过查询从数据库中获取实体对象的时候，这个列的值就会同时填充到它绑定的每一个属性上去。

```kotlin
interface Config : Entity<Config> {
    val key: String
    var value1: String
    var value2: String
}

object Configs : Table<Config>("t_config") {
    val key by varchar("key").primaryKey().bindTo { it.key }
    val value by varchar("value").bindTo { it.value1 }.bindTo { it.value2 }
}
```

在这个例子中，我们把 `value` 列同时绑定到了 `value1` 和 `value2` 上，因此在查询返回的实体对象中，这两个属性会包含同样的值。

> 请注意：多重绑定仅在查询时有效，在执行插入或更新实体的操作时，以第一个绑定的属性为准，其他的绑定都会被忽略。

## 关于 Entity 接口

前面提到，Ktorm 规定，所有的实体类都应该定义为 interface，并且继承 `Entity` 接口，而实体对象的创建，则是使用 JDK 动态代理完成的。如果你对 JDK 的动态代理有所了解，你应该知道，代理对象是通过 `Proxy.newProxyInstance` 方法创建的，提供一个 `InvocationHandler` 实例作为参数，所有对接口方法的调用，都会被 JDK 代理到这个 handler 中。在 Ktorm 内部，`EntityImplementation` 就是这个 handler 的实现，它被声明为 internal，因此你无法在 Ktorm 外部使用它，但是我们可以了解一下它的基本原理。

### 属性存取

当我们使用 Kotlin 在实体类中定义一个属性 `var name: String`，编译成 Java 字节码后，相当于定义了两个方法，分别是 `public String getName()` 和 `public void setName(String name)`，这两个方法的调用都会被代理到 `EntityImplementation` 中。

`EntityImplementation` 类中包含了一个 values 属性，它的类型是 `LinkedHashMap<String, Any?>`，用来保存实体中的所有属性的值。当我们使用 `e.name` 获取属性时，`EntityImplementation` 就会检测到 `getName()` 方法的调用，于是从 values 中使用“name”作为键获取属性值。当我们使用 `e.name = "foo"` 修改属性，同样会发生一个 `setName()` 方法的调用，于是，`EntityImplementation` 会使用“name”为键将传入的属性值保存到 values 中，同时还会记录一些额外的信息，以跟踪实体的状态变化。

也就是说，每个实体对象的背后，都有一个属性表保存了它的所有属性值，所有对实体类属性的获取或修改操作，实际上都是在操作底层的这个属性表。但是，如果在获取属性值时，属性表中不存在对应的键会怎么样呢？比如一个刚刚创建的实体对象，它底层的属性表就是空的，不存在任何键。Ktorm 针对这种情况定义了一套具体的规则：

- 当属性表中不含有指定属性时，如果该属性为可空类型，如 `var name: String?`，则返回 null
- 当属性表中不含有指定属性时，如果该属性为不可空类型，如 `var name: String`，此时返回 null 会导致意料之外的空指针异常，Ktorm 会返回对应类型的默认值

关于不同类型的默认值，也有一套规则：

- 对于 `Boolean` 类型，返回 false
- 对于 `Char` 类型，返回 \u0000
- 对于数值类型，如 `Int`、`Long`、`Double` 等，返回 0
- 对于 `String` 类型，返回空字符串
- 对于实体类型，返回一个新创建的空的实体对象
- 对于枚举类型，返回枚举的第一个值
- 对于数组类型，返回一个新创建的空数组
- 对于集合类型，如 `Set`、`List`、`Map` 等，返回一个对应类型的新创建的空的可变集合
- 其他未识别类型，调用其无参构造函数创建一个对象并返回，如果没有无参构造函数，则抛出异常

另外，`EntityImplementation` 内部对默认值存在一个缓存机制，即在没有修改属性值的情况下，多次调用 getter 获取到的默认值始终是同一个对象，以避免一些违反直觉的 bug。

### 非抽象成员

在领域驱动设计中，实体对象不仅仅是保存数据的属性值的集合，还可以具有一些业务逻辑，因此往往需要在实体类中定义一些业务函数。幸运的是，Kotlin 允许我们在接口中定义具有默认实现的函数，这使得 Ktorm 要求我们将实体类定义为接口并没有造成实际损失。我们可以在实体接口中增加非抽象的成员函数，像这样：

````kotlin
interface Foo : Entity<Foo> {
    companion object : Entity.Factory<Foo>()
    val name: String
    
    fun printName() {
        println(name)
    }
}
````

在上面的例子中，如果我们调用 `Foo().printName()`，就会输出 `name` 属性的值。

> 这看起来十分自然，但其背后的实现却没那么简单。我们知道，Ktorm 使用 JDK 动态代理创建实体对象，因此，`printName` 函数的调用实际上也会转发到 `EntityImplementation` 内部。这时， `EntityImplementation` 会检测到当前调用的函数并非抽象函数，然后自动查找到 `DefaultImpls` 类中的默认实现并调用之，然而在我们看起来，这跟直接调用该函数并没有任何区别。但是，如果你在方法上添加 `@JvmDefault` 注解，就可能导致 Ktorm 无法查找 `DefaultImpls` 类，具体原因有兴趣可以参考 [Kotlin 语言手册](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-default/index.html)，这对我们使用 Ktorm 并无太大影响。

除了非抽象函数，Kotlin 也允许我们在接口中添加具有自定义 getter 或 setter 的属性。例如下面的代码，当调用 `upperName` 时，就会返回全大写的名称，其原理与前面所说的完全一致：

````kotlin
interface Foo : Entity<Foo> {
    val name: String
    val upperName get() = name.toUpperCase()
}
````

### 序列化

`Entity` 接口继承了 `java.io.Serializable` 接口，所有的实体对象默认都是可序列化的，因此你可以将实体对象保存在磁盘中，或者通过网络在不同系统中传输，而不需要其他额外的工作。

唯一需要注意的时，在序列化时，Ktorm 将只会保存各个属性的值，其他用于追踪实体状态变化的数据都会丢失（被标记为 transient），因此你无法在一个系统中获取实体，然后在另一个系统中调用实体的 `flushChanges` 方法将属性变化更新到数据库。

> Java 使用 `ObjectOutputStream` 实现对象序列化，使用 `ObjectInputStream` 实现反序列化，具体可以参考这两个类的文档。

除了 JDK 序列化，ktorm-jackson 模块还为你提供了使用 JSON 格式进行序列化的功能。该模块为 Java 中著名的 JSON 框架 Jackson 提供了一个扩展，它支持将 Ktorm 中的实体对象格式化为 JSON，以及从 JSON 中解析实体对象。我们只需要将 `KtormModule` 注册到 `ObjectMapper` 中：

```kotlin
val objectMapper = ObjectMapper()
objectMapper.registerModule(KtormModule())
```

或者使用 `findAndRegisterModules` 方法自动扫描：

````kotlin
val objectMapper = ObjectMapper()
objectMapper.findAndRegisterModules()
````

现在，你就可以使用这个 `objectMapper` 对实体对象进行序列化与反序列化的操作了，具体可以参考 Jackson 框架的文档。

以上就是 Ktorm 实体类目前支持的两种序列化方式，如果希望得到更多支持，可以提出 issue，或向我们发送 PR，欢迎您的贡献！