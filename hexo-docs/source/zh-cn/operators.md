---
title: 操作符
lang: zh-cn
related_path: en/operators.html
---

# 操作符

在前面的章节中，我们已经对操作符有了一定的了解，现在，我们来对它进行详细的介绍。

## 内置操作符

Ktorm 的每个操作符实际上都是一个返回 `SqlExpression` 的 Kotlin 函数，下面是目前我们所支持的所有操作符的列表及使用示例：

| Kotlin 函数名 | SQL 关键字/符号 | 使用示例                                                     |
| ------------- | --------------- | ------------------------------------------------------------ |
| isNull        | is null         | Ktorm: Employees.name.isNull()<br />SQL: t_employee.name is null |
| isNotNull     | is not null     | Ktorm: Employees.name.isNotNull()<br />SQL: t_employee.name is not null |
| unaryMinus(-) | -               | Ktorm: -Employees.salary<br />SQL: -t_employee.salary        |
| unaryPlus(+)  | +               | Ktorm: +Employees.salary<br />SQL: +t_employee.salary        |
| not(!)        | not             | Ktorm: !Employees.name.isNull()<br />SQL: not (t_employee.name is null) |
| plus(+)       | +               | Ktorm: Employees.salary + Employees.salary<br />SQL: t_employee.salary + t_employee.salary |
| minus(-)      | -               | Ktorm: Employees.salary - Employees.salary<br />SQL: t_employee.salary - t_employee.salary |
| times(*)      | *               | Ktorm: Employees.salary \* 2<br />SQL: t_employee.salary \* 2 |
| div(/)        | /               | Ktorm: Employees.salary / 2<br />SQL: t_employee.salary / 2  |
| rem(%)        | %               | Ktorm: Employees.id % 2<br />SQL: t_employee.id % 2          |
| like          | like            | Ktorm: Employees.name like "vince"<br />SQL: t_employee.name like 'vince' |
| notLike       | not like        | Ktorm: Employees.name notLike "vince"<br />SQL: t_employee.name not like 'vince' |
| and           | and             | Ktorm: Employees.name.isNotNull() and (Employees.name like "vince")<br />SQL: t_employee.name is not null and t_employee.name like 'vince' |
| or            | or              | Ktorm: Employees.name.isNull() or (Employees.name notLike "vince")<br />SQL: t_employee.name is null or t_employee.name not like 'vince' |
| xor           | xor             | Ktorm: Employees.name.isNotNull() xor (Employees.name notLike "vince")<br />SQL: t_employee.name is not null xor t_employee.name not like 'vince' |
| less          | <               | Ktorm: Employees.salary less 1000<br />SQL: t_employee.salary < 1000 |
| lessEq        | <=              | Ktorm: Employees.salary lessEq 1000<br />SQL: t_employee.salary <= 1000 |
| greater       | >               | Ktorm: Employees.salary greater 1000<br />SQL: t_employee.salary > 1000 |
| greaterEq     | >=              | Ktorm: Employees.salary greaterEq 1000<br />SQL: t_employee.salary >= 1000 |
| eq            | =               | Ktorm: Employees.id eq 1<br />SQL: t_employee.id = 1         |
| notEq         | <>              | Ktorm: Employees.id notEq 1<br />SQL: t_employee.id <> 1     |
| between       | between         | Ktorm: Employees.id between 1..3<br />SQL: t_employee.id between 1 and 3 |
| notBetween    | not between     | Ktorm: Employees.id notBetween 1..3<br />SQL: t_employee.id not between 1 and 3 |
| inList        | in              | Ktorm: Employees.departmentId inList listOf(1, 2, 3)<br />SQL: t_employee.department_id in (1, 2, 3) |
| notInList     | not in          | Ktorm: Employees.departmentId notInList Departments.selectDistinct(Departments.id)<br />SQL: t_employee.department_id not in (select distinct t_department.id from t_department) |
| exists        | exists          | Ktorm: exists(Employees.select())<br />SQL: exists (select * from t_employee) |
| notExists     | not exists      | Ktorm: notExists(Employees.select())<br />SQL: not exists (select * from t_employee) |

这些操作符按照实现方式大概可以分为两类：

**使用 operator 关键字重载的 Kotlin 内置操作符：**这类操作符一般用于实现加减乘除等基本的运算，由于重载了 Kotlin 的内置操作符，它们使用起来就像是真的执行了运算一样，比如 `Employees.salary + 1000`。但实际上并没有，它们只是创建了一个 SQL 表达式，这个表达式会被 `SqlFormatter` 翻译为 SQL 中的对应符号。下面是加号操作符的代码实现，可以看到，它只是创建了一个 `BinaryExpression<T>` 而已：

```kotlin
infix operator fun <T : Number> ColumnDeclaring<T>.plus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.PLUS, asExpression(), expr.asExpression(), sqlType)
}
```

**自定义的操作符函数：**然而，Kotlin 重载操作符还有许多限制，比如 `equals` 方法要求必须返回 `Boolean`，然而 Ktorm  的操作符需要返回 SQL 表达式，因此，Ktorm 提供了另外一个 `eq` 函数用于相等比较。除此之外，还有许多 SQL 中的操作符在 Kotlin 中并不存在，比如 like，Ktorm 就提供了一个 `like` 函数用于字符串匹配。下面是 `like` 函数的实现，这类函数一般都具有 infix 关键字修饰：

```kotlin
infix fun ColumnDeclaring<*>.like(argument: String): BinaryExpression<Boolean> {
    return BinaryExpression(
        type = BinaryExpressionType.LIKE, 
        left = asExpression(), 
        right = ArgumentExpression(argument, VarcharSqlType), 
        sqlType = BooleanSqlType
    )
}
```

## 操作符优先级

