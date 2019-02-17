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

**普通的操作符函数：**然而，Kotlin 重载操作符还有许多限制，比如 `equals` 方法要求必须返回 `Boolean`，然而 Ktorm  的操作符需要返回 SQL 表达式，因此，Ktorm 提供了另外一个 `eq` 函数用于相等比较。除此之外，还有许多 SQL 中的操作符在 Kotlin 中并不存在，比如 like，Ktorm 就提供了一个 `like` 函数用于字符串匹配。下面是 `like` 函数的实现，这类函数一般都具有 infix 关键字修饰：

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

操作符可以连续使用，但是，当我们一次使用多个操作符时，它们的优先级就成了一个问题。在一个表达式中可能包含多个操作符，不同的运算顺序可能得出不同的结果甚至出现运算错误，因为当表达式中含多种运算时，必须按一定顺序进行结合，才能保证运算的合理性和结果的正确性、唯一性。

例如 1 + 2 \* 3，乘号的优先级比较高，则 2 \* 3 优先结合，运算结果为 7；若不考虑操作符的优先级，从前往后结合，那么运算结果为 9，这是完全错误的。一般来说，乘除的优先级高于加减，与的优先级高于或，但是，在 Ktorm 中，情况却有些不同。

对于重载的 Kotlin 内置操作符，其优先级遵循 Kotlin 语言自己的规范。例如表达式 `Employees.salary + 1000 * 2`，由于乘号的优先级较高，最终翻译出来的 SQL 是 `t_employee.salary + 2000`。

**但是对于普通的操作符函数，却并没有优先级一说。**在 Kotlin 语言的层面，它们实际上都只是普通的函数调用，因此只需要遵循从前往后结合的原则，尽管这有时可能会违反我们的直觉。比如 `a or b and c`，这里的 `or` 和 `and` 都是操作符函数，直觉上，`and` 的优先级应该比 `or` 高，因此应该优先结合，但实际上，它们只是普通的 Kotlin 函数而已。如果对这一点没有清楚的认识，可能导致一些意料之外的 bug，为了解决这个问题，我们可以在需要的地方使用括号，比如 `a or (b and c)`。

## 自定义操作符

前面已经介绍过 Ktorm 核心模块的内置操作符，这些操作符为标准 SQL 中的操作符提供了支持，但如果我们想使用一些数据库方言中特有的操作符呢？下面我们以 PostgreSQL 中的 ilike 操作符为例，了解如何增加自己的操作符。

ilike 是 PostgreSQL 中特有的操作符，它的功能与 like 一样，也是进行字符串匹配，但是忽略大小写。我们首先创建一个表达式类型，它继承于 `ScalarExpression<Boolean>`，表示一个 ilike 操作：

```kotlin
data class ILikeExpression(
    val left: ScalarExpression<*>,
    val right: ScalarExpression<*>,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false
) : ScalarExpression<Boolean>()
```

有了表达式类型之后，我们只需要再增加一个扩展函数，这就是操作符函数，为了函数使用起来真的像一个操作符，我们需要添加 infix 关键字：

```kotlin
infix fun ColumnDeclaring<*>.ilike(argument: String): ILikeExpression {
    return ILikeExpression(asExpression(), ArgumentExpression(argument, VarcharSqlType)
}
```

这样我们就能使用这个操作符函数了，就像使用其他操作符一样。不过现在 Ktorm 还无法识别我们自己创建的 `IlikeExpression`，无法为我们生成正确的 SQL，跟之前一样，我们需要扩展 `SqlFormatter` 类：

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

接下来的事情就是使用方言（Dialect）支持将这个自定义的 SqlFormatter 注册到 Ktorm 中了，关于如何启用方言，可参考后面的章节。