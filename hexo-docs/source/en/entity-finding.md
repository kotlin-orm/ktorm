---
title: Entity Finding
lang: en
related_path: zh-cn/entity-finding.html
---

# Entity Finding

Ktorm provides many extension functions to obtain entity objects from databases, their names commonly start with `find`. 

## find\* Functions

Let's discuss `findList` function fist, it's an extension function of `Table` class, and its signature is given as follows: 

```kotlin
inline fun <E : Entity<E>, T : Table<E>> T.findList(predicate: (T) -> ColumnDeclaring<Boolean>): List<E>
```

This function accepts a closure as its parameter, executes a query with the filter condition returned by the closure, then returns a list of entity objects obtained from the result set. The closure function also accepts a parameter of type `T`, which is the current table object, so we can use `it` to access the table in the closure. The code obtaining all employees in department 1: 

```kotlin
val employees = Employees.findList { it.departmentId eq 1 }
```

That's natural, just like filtering a collection via Kotlin's built-in extension functions, the only difference is the  `==` in the lambda is replace by a `eq` function. 

Generated SQL: 

```sql
select t_employee.id as t_employee_id, t_employee.name as t_employee_name, t_employee.job as t_employee_job, t_employee.manager_id as t_employee_manager_id, t_employee.hire_date as t_employee_hire_date, t_employee.salary as t_employee_salary, t_employee.department_id as t_employee_department_id, _ref0.id as _ref0_id, _ref0.name as _ref0_name, _ref0.location as _ref0_location 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
where t_employee.department_id = ? 
```

> The generated SQL contains a very long field list, that's necessary, Ktorm tries its best to avoid using `select *`. But for the sake of presentation, in later SQLs, we will still replace them with `select *`. 

Reading the generated SQL, we can find that Ktorm auto left joins `t_employee`'s reference table `t_department` using a foreign key. That's because we bind the `departmentId` column to `Departments` table by a reference binding in the table object. By using the reference binding, when we obtain employees via `find*` functions, Ktorm will auto left joins the referenced table, obtaining the departments at the same time, and filling them into property `Employee.department`. 

> Note: please avoid circular references while using reference bindings. For instance, now that `Employees` references `Departments`, then `Departments` can not reference `Elmployees` directly or indirectly, otherwise a stack overflow will occur when Ktorm tries to left join `Departments`. 

Including `findList`, Ktorm provides a list of `find*` functions, they are all extension functions of `Table` class and their behaviors are similar: 

- **findList:** obtain a list of entity objects using the specific condition. 
- **findAll:** obtain all the entity objects in the table. 
- **findOne:** obtain one entity object using the specific condition. If it doesn't exist, return null, otherwise if there are more than one entities matching the condition, an exception will be thrown. 
- **findById:** obtian an entity object by primary key. If it doesn't exist, return null, otherwise if there many, throw an exception. 
- **findListByIds:** obtain a list of entity objects by primary key.
- **findMapByIds:** obtain a map of entity objects by primary key, in which the keys are primary key, and the values are the entities. 

## Get Entities from Queries

`find*` functions will auto left join reference tables, that may be unnecessary in some casts. Besides, `find*` functions also can not control the selected columns, ordering, pagination, etc. If you want more fine-grained control over the queries, you can use the query DSL introduced in the former sections. Ktorm provides a way to create entity objects from a query DSL. 

The example below uses `createEntity` function to obtain a list of entities from a query DSL: 

```kotlin
val employees = Employees
    .select()
    .orderBy(Employees.id.asc())
    .map { Employees.createEntity(it) }

employees.forEach { println(it) }
```

`Query` implements the `Iterable<QueryRowSet>` interface, so we can use the Kotlin built-in `map` function to iterate it and create an entity object from the result set via `createEntity` for each row. `createEntity` is an extension function of `Table` class, it will create an entity object from the result set, using the binding configurations in the table object, filling columns' values into corresponding entities' properties. And if there are any reference bindings to other tables, it will also create the referenced entity objects recursively. 

The selected columns in query DSL are customizable, and there may be no columns from referenced tables. In this case, Ktorm provides a `createEntityWithoutReferences` function since version 2.0, which do the same thing as `createEntity`. But it doesn't obtain referenced entities' data automatically. It treats all reference bindings as nested bindings to the referenced entities' primary keys. For example the binding `c.references(Departments) { it.department }`, it is equivalent to `c.bindTo { it.department.id }` for it, that avoids unnecessary object creations and some exceptions raised by conflict column names. 

Get back the example above, no matter we use `createEntity` or `createEntityWithoutReferences`, it will generate a simple SQL `select * from t_employee order by t_employee.id` and print the same results:  

```plain
Employee{id=1, name=vince, job=engineer, hireDate=2018-01-01, salary=100, department=Department{id=1}}
Employee{id=2, name=marry, job=trainee, manager=Employee{id=1}, hireDate=2019-01-01, salary=50, department=Department{id=1}}
Employee{id=3, name=tom, job=director, hireDate=2018-01-01, salary=200, department=Department{id=2}}
Employee{id=4, name=penny, job=assistant, manager=Employee{id=3}, hireDate=2019-01-01, salary=100, department=Department{id=2}}
```

## joinReferencesAndSelect

`joinReferencesAndSelect` is also an extension function of `Table` class, it returns a new created `Query` object, left joining all the reference tables recursively, and selecting all columns of them. We can not only use the returned `Query` object to obtain all entity objects, but also call any other extension functions of `Query` to modify it. Actually, `find*` functions are implemented based on `joinReferencesAndSelect`. 

The example below queries all the employees along with their departments, sorting them by their IDs ascending: 

```kotlin
val employees = Employees
    .joinReferencesAndSelect()
    .orderBy(Employees.id.asc())
    .map { Employees.createEntity(it) }
```

Generated SQL: 

```sql
select * 
from t_employee 
left join t_department _ref0 on t_employee.department_id = _ref0.id 
order by t_employee.id 
```

We can see in the SQL that the query above is equivalent to calling the `leftJoin` function manually, the following query is completely equal to the example above. Using `joinReferencesAndSelect` can help us to reduce some boilerplate codes. 

```kotlin
val emp = Employees
val dept = emp.departmentId.referenceTable as Departments

val employees = emp
    .leftJoin(dept, on = emp.departmentId eq dept.id)
    .select(emp.columns + dept.columns)
    .orderBy(emp.id.asc())
    .map { emp.createEntity(it) }
```