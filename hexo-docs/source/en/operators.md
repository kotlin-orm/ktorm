---
title: Operators
lang: en
related_path: zh-cn/operators.html
---

# Operators

In the former sections, we have had some knowledge about Ktorm's operators. Now let's introduce them more detailed. 

## Built-in Operators

Any operator in Ktorm is a Kotlin function that returns a `SqlExpression`. Here is a list of built-in operators that Ktorm supports now: 

| Kotlin Function Name | SQL Keyword/Operator | Usage                                                        |
| -------------------- | -------------------- | ------------------------------------------------------------ |
| isNull               | is null              | Ktorm: Employees.name.isNull()<br />SQL: t_employee.name is null |
| isNotNull            | is not null          | Ktorm: Employees.name.isNotNull()<br />SQL: t_employee.name is not null |
| unaryMinus(-)        | -                    | Ktorm: -Employees.salary<br />SQL: -t_employee.salary        |
| unaryPlus(+)         | +                    | Ktorm: +Employees.salary<br />SQL: +t_employee.salary        |
| not(!)               | not                  | Ktorm: !Employees.name.isNull()<br />SQL: not (t_employee.name is null) |
| plus(+)              | +                    | Ktorm: Employees.salary + Employees.salary<br />SQL: t_employee.salary + t_employee.salary |
| minus(-)             | -                    | Ktorm: Employees.salary - Employees.salary<br />SQL: t_employee.salary - t_employee.salary |
| times(*)             | *                    | Ktorm: Employees.salary \* 2<br />SQL: t_employee.salary \* 2 |
| div(/)               | /                    | Ktorm: Employees.salary / 2<br />SQL: t_employee.salary / 2  |
| rem(%)               | %                    | Ktorm: Employees.id % 2<br />SQL: t_employee.id % 2          |
| like                 | like                 | Ktorm: Employees.name like "vince"<br />SQL: t_employee.name like 'vince' |
| notLike              | not like             | Ktorm: Employees.name notLike "vince"<br />SQL: t_employee.name not like 'vince' |
| and                  | and                  | Ktorm: Employees.name.isNotNull() and (Employees.name like "vince")<br />SQL: t_employee.name is not null and t_employee.name like 'vince' |
| or                   | or                   | Ktorm: Employees.name.isNull() or (Employees.name notLike "vince")<br />SQL: t_employee.name is null or t_employee.name not like 'vince' |
| xor                  | xor                  | Ktorm: Employees.name.isNotNull() xor (Employees.name notLike "vince")<br />SQL: t_employee.name is not null xor t_employee.name not like 'vince' |
| less                 | <                    | Ktorm: Employees.salary less 1000<br />SQL: t_employee.salary < 1000 |
| lessEq               | <=                   | Ktorm: Employees.salary lessEq 1000<br />SQL: t_employee.salary <= 1000 |
| greater              | >                    | Ktorm: Employees.salary greater 1000<br />SQL: t_employee.salary > 1000 |
| greaterEq            | >=                   | Ktorm: Employees.salary greaterEq 1000<br />SQL: t_employee.salary >= 1000 |
| eq                   | =                    | Ktorm: Employees.id eq 1<br />SQL: t_employee.id = 1         |
| notEq                | <>                   | Ktorm: Employees.id notEq 1<br />SQL: t_employee.id <> 1     |
| between              | between              | Ktorm: Employees.id between 1..3<br />SQL: t_employee.id between 1 and 3 |
| notBetween           | not between          | Ktorm: Employees.id notBetween 1..3<br />SQL: t_employee.id not between 1 and 3 |
| inList               | in                   | Ktorm: Employees.departmentId inList listOf(1, 2, 3)<br />SQL: t_employee.department_id in (1, 2, 3) |
| notInList            | not in               | Ktorm: Employees.departmentId notInList Departments.selectDistinct(Departments.id)<br />SQL: t_employee.department_id not in (select distinct t_department.id from t_department) |
| exists               | exists               | Ktorm: exists(Employees.select())<br />SQL: exists (select * from t_employee) |
| notExists            | not exists           | Ktorm: notExists(Employees.select())<br />SQL: not exists (select * from t_employee) |

These operators can be divided into two groups by the implementation way: 

**Overloaded Kotlin built-in operators:** This group of operators are generally used to implement basic arithmetic operators (such as plus, minus, times, etc). Because of operator overloading, they are used just like real arithmetic performs, for example, `Employees.salary + 1000`. But actually, they just create SQL expressions instead, those expressions will be translated into the corresponding operators in SQL by `SqlFormatter`. Here is the implementation of the plus operator, we can see that it just creates a `BinaryExpression<T>`: 

```kotlin
infix operator fun <T : Number> ColumnDeclaring<T>.plus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.PLUS, asExpression(), expr.asExpression(), sqlType)
}
```

**Normal operator functions:** There are many limits overloading Kotlin's built-in operators. For example, the `equals` function is restricted to return `Boolean` values only, but Ktorm's operator functions need to return SQL expressions, so Ktorm provides another function `eq` for us to implement equality comparisons. Additionally, there are also many operators that don't exist in Kotlin, such as like, Ktorm provides a `like` function for string matching in SQL. Here is the implementation of the `like` function, and this kind of functions are generally marked with an infix keyword: 

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

## Operator Precedence

Operators can be used continuously, but if we use different operators together, we will meet the problem of their precedence. There can be many operators in an expression, different combination order of operators can lead to different results and even errors. Only if the operators are combined in a certain order, the expression's result can be correct and unique. 

For instance, in the expression 1 + 2 \* 3, the multiplication's precedence is higher than plus, so 2 \* 3 is combined first, the result is 7; If we ignore the precedence of operators, then 1 + 2 is combined first, the result will be 9, which is absolutely wrong. Normally, the precedence of multiplicative operators is higher than additive operators', the precedence of conjunctions are higher than disjunctions'. But there are a little different in Ktorm. 

For overloaded Kotlin built-in operators, their precedence follows the specification of Kotlin language. Such as the expression `Employees.salary + 1000 * 2`, the multiplication's precedence is higher, so the final translated SQL is `t_employee.salary + 2000`. 

**However, for normal operator functions, there is no such thing as precedence.** In the level of Kotlin language, they are all normal function callings, so they just need to be combined from head to end, although it's quite counterintuitive. For example, in the expression `a or b and c`, the `or` and `and` are both operator functions. Intuitively, the precedence of `and` is higher, so it should be combined first, but actually, they are both normal functions, so our intuition is wrong. If we don't have a clear understanding on this, some unexpected bugs may occur, to solve the problem, we can use brackets if needed, such as `a or (b and c)`. 

For detailed precedence in Kotlin language, please refer to [Kotlin Reference](https://kotlinlang.org/docs/reference/grammar.html#expressions). 

## Custom Operators

We've talked about the built-in operators provided by Ktorm's core module, those operators provided supports for operators in standard SQL, but what if we want to use some special operators provided by a special database? Let's take PostgreSQL's ilike operator as an example, learning how to extend our custom operators with Ktorm. 

ilike is a special operator in PostgreSQL. Similar to like, it also matches strings, but ignoring cases. Firstly, we create an expression type extending from `ScalarExpression<Boolean>`: 

```kotlin
data class ILikeExpression(
    val left: ScalarExpression<*>,
    val right: ScalarExpression<*>,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false
) : ScalarExpression<Boolean>()
```

Having the expression type, we also need an extension function to create expression instances conveniently, that's the operator function. We mark the function with an infix keyword, so it can be used just like a real operator in SQL: 

```kotlin
infix fun ColumnDeclaring<*>.ilike(argument: String): ILikeExpression {
    return ILikeExpression(asExpression(), ArgumentExpression(argument, VarcharSqlType)
}
```

Now we can use this operator function, just like other operators. But Ktorm cannot recognize our custom expression type `ILikeExpression` by default and are not able to generate SQLs correctly. Just like before, we need to extend the `SqlFormatter` class: 

```kotlin
class PostgreSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int)
    : SqlFormatter(database, beautifySql, indentSize) {

    override fun visitUnknown(expr: SqlExpression): SqlExpression {
        if (expr is ILikeExpression) {
            if (expr.left.removeBrackets) {
                visit(expr.left)
            } else {
                write("(")
                visit(expr.left)
                removeLastBlank()
                write(") ")
            }

            write("ilike ")

            if (expr.right.removeBrackets) {
                visit(expr.right)
            } else {
                write("(")
                visit(expr.right)
                removeLastBlank()
                write(") ")
            }

            return expr
        } else {
            super.visitUnknown(expr)
        }
    }
}
```

Now, the last thing we should do is to register this custom sql formatter into Ktorm by dialect support, you can read the later chapters for how to [enable dialects](./dialects-and-native-sql.html#Enable-Dialects).

