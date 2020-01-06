---
title: Entity Finding
lang: en
related_path: zh-cn/entity-finding.html
---

# Entity Query

Ktorm provides a set of APIs named *Entity Sequence*, which can be used to obtain entity objects from databases. As the name implies, its style and use pattern are highly similar to the sequence APIs in Kotlin standard lib, as it provides many extension functions with the same names, such as `filter`, `map`, `reduce`, etc.

## Get Entities by Sequences

To use entity sequence, we need to create a sequence object via `sequenceOf` firstly: 

```kotlin
val sequence = database.sequenceOf(Employees)
```

The returned object can be thought of as a sequence holding all records in the `Employees` table. To get an entity object from this sequence, you can use the `find` function: 

```kotlin
val employee = sequence.find { it.id eq 1 }
```

This is natural, just like finding from a collection via Kotlin’s built-in extension functions, the only difference is the `==` in the lambda is replace by the `eq` function.

The `find` function accepts a closure typed of `(T) -> ColumnDeclaring<Boolean>`, executes a query with the filter condition returned by the closure, then returns the first entity object obtained from the result set. The closure function itself as the parameter also accepts a parameter typed of `T`, which is the current table object, so we can use `it` to access the table in the closure.

Generated SQL: 

```sql
select t_employee.id as t_employee_id, t_employee.name as t_employee_name, t_employee.job as t_employee_job, t_employee.manager_id as t_employee_manager_id, t_employee.hire_date as t_employee_hire_date, t_employee.salary as t_employee_salary, t_employee.department_id as t_employee_department_id, _ref0.id as _ref0_id, _ref0.name as _ref0_name, _ref0.location as _ref0_location 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where t_employee.id = ? 
```

> The generated SQL contains a very long field list, that’s necessary, Ktorm tries its best to avoid using `select *`. But for the sake of presentation, in later documents, we will still replace those field lists with `select *`.

Reading the generated SQL, we can find that Ktorm auto left joins `t_employee`'s reference table `t_department` using a foreign key. That’s because we bind the `departmentId` column to `Departments` table by a reference binding in the table object. By using the reference binding, when we obtain employees via sequence APIs, Ktorm will auto left join the referenced table, obtaining the departments at the same time, and filling them into property `Employee.department`.

> Note: please avoid circular references while using reference bindings. For instance, now that `Employees` references `Departments`, then `Departments` cannot reference `Employees` directly or indirectly, otherwise a stack overflow will occur when Ktorm tries to left join `Departments`. 

Now that referenced tables are auto left joined, we can also use their columns in our filter conditions. The code below uses `Column.referenceTable` to access `departmentId`'s referenced table and obtains an employee who works at Guangzhou:

```kotlin
val employee = sequence.find {
    val dept = it.departmentId.referenceTable as Departments
    dept.location eq "Guangzhou"
}
```

To make it more elegant, we can add a get property to `Employees` table. The following code is completely equivalent to the above’s, but it reads more natural: 

```kotlin
open class Employees(alias: String?) : Table<Employee>("t_employee", alias) {
    // Omit columns definitions here...
    val department get() = departmentId.referenceTable as Departments
}

val employee = sequence.find { it.department.location eq "Guangzhou" }
```

Generated SQL: 

````sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where _ref0.location = ? 
````

> Note: here we get the referenced table object via `it.departmentId.referenceTable` and cast it as `Departments`, which requires us to define tables as classes instead of singleton objects and to override the `aliased` function. More details can be found in the documentation of [table aliases](./joining.html#Self-Joining-amp-Table-Aliases).

Besides the `find` function, *Entity Sequence* also provides many convenient functions for us. For example, using `filter` to find elements that matches the given condition, using `groupingBy` to group elements and do some aggregation, etc.. Comparing with SQL DSL, sequence APIs are more functional, and can be used just like operating a collection in memory, so we recommend it as your first choice. For more documents, see [Entity Sequence](./entity-sequence.html) and [Sequence Aggregation](./sequence-aggregation.html).

## Get Entities by Query DSL

Sequence APIs will auto left join reference tables, that may be unnecessary in some cases. If you want more fine-grained control over the queries, you can use the query DSL introduced in the former sections. Ktorm provides a way to create entity objects from query DSL. 

The example below uses `createEntity` function to obtain a list of entities from a query: 

```kotlin
val employees = database
    .from(Employees)
    .select()
    .orderBy(Employees.id.asc())
    .map { row -> Employees.createEntity(row) }

employees.forEach { println(it) }
```

`Query` implements the `Iterable<QueryRowSet>` interface, so we can use the Kotlin built-in `map` function to iterate it and create an entity object from the result set via `createEntity` for each row. `createEntity` is a function of `Table` class, it will create an entity object from the result set, using the binding configurations of the table, filling columns' values into corresponding entities' properties. And if there are any reference bindings to other tables, it will also create the referenced entity objects recursively. 

However, the selected columns in query DSL are customizable, and there may be no columns from referenced tables. In this case, the function provides a parameter named `withReferences`, which is defaultly `true`. But if we set it to `false`, it will not obtain referenced entities' data anymore, it will treat all reference bindings as nested bindings to the referenced entities' primary keys. For example the binding `c.references(Departments) { it.department }`, it is equivalent to `c.bindTo { it.department.id }` for it, that avoids some unnecessary object creations. 

```kotlin
Employees.createEntity(row, withReferences = false)
```

Get back the example above, because we didn't join any tables, so no matter we set the parameter to `true` or `false`, Ktorm will generate a simple SQL `select * from t_employee order by t_employee.id` and print the same results:  

```plain
Employee{id=1, name=vince, job=engineer, hireDate=2018-01-01, salary=100, department=Department{id=1}}
Employee{id=2, name=marry, job=trainee, manager=Employee{id=1}, hireDate=2019-01-01, salary=50, department=Department{id=1}}
Employee{id=3, name=tom, job=director, hireDate=2018-01-01, salary=200, department=Department{id=2}}
Employee{id=4, name=penny, job=assistant, manager=Employee{id=3}, hireDate=2019-01-01, salary=100, department=Department{id=2}}
```

## joinReferencesAndSelect

`joinReferencesAndSelect` is an extension function of `QuerySource`, it returns a new-created `Query` object, left joining all the reference tables recursively, and selecting all columns of them. Not only we can use the returned query to obtain all entity objects, but also we can call any other extension functions of `Query` to modify it. Actually, sequence APIs are based on this function to implement the auto joining of reference tables. 

The example below queries all the employees along with their departments, sorting them by their IDs ascending: 

```kotlin
val employees = database
    .from(Employees)
    .joinReferencesAndSelect()
    .orderBy(Employees.id.asc())
    .map { row -> Employees.createEntity(row) }
```

Generated SQL: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
order by t_employee.id 
```

We can see in the SQL that the query above is equivalent to calling the `leftJoin` function manually, the following query is completely equal to the example above. Using `joinReferencesAndSelect` can help us to reduce some boilerplate code. 

```kotlin
val emp = Employees
val dept = emp.departmentId.referenceTable as Departments

val employees = database
    .from(emp)
    .leftJoin(dept, on = emp.departmentId eq dept.id)
    .select(emp.columns + dept.columns)
    .orderBy(emp.id.asc())
    .map { row -> emp.createEntity(row) }
```