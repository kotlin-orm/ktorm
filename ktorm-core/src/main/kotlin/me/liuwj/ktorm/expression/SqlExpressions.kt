package me.liuwj.ktorm.expression

import me.liuwj.ktorm.schema.BooleanSqlType
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.SqlType

/**
 * SQL 表达式，树形结构，可以视为 SQL 的抽象语法树（AST）.
 *
 * SQL 表达式的子类必须遵守如下约定：
 *
 * - 必须为 data 类，以提供解构、copy 等通用能力
 * - 必须为不可变对象（immutable），每次执行修改时都应返回一个新对象
 *
 * 如何方便地遍历 SQL 语法树，参见 [SqlExpressionVisitor]
 * 如何将 SQL 表达式格式化为可直接执行的 SQL 字符串，参见 [SqlFormatter]
 *
 * @see SqlExpressionVisitor
 * @see SqlFormatter
 */
@Suppress("UnnecessaryAbstractClass")
abstract class SqlExpression {

    /**
     * 是否叶子节点
     */
    abstract val isLeafNode: Boolean
}

/**
 * 标量 SQL 表达式，此类表达式具有一个计算结果，该结果的类型是 T，如：a + 1
 */
abstract class ScalarExpression<T : Any> : SqlExpression(), ColumnDeclaring<T> {

    abstract override val sqlType: SqlType<T>

    override fun asExpression(): ScalarExpression<T> {
        return this
    }

    override fun asDeclaringExpression(): ColumnDeclaringExpression {
        return ColumnDeclaringExpression(expression = this, declaredName = null)
    }

    override fun wrapArgument(argument: T?): ArgumentExpression<T> {
        return ArgumentExpression(argument, sqlType)
    }
}

/**
 * 包装一个 SQL 表达式，改变其返回类型
 */
data class CastingExpression<T : Any>(
    val expression: SqlExpression,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false
) : ScalarExpression<T>()

/**
 * Query source expression, used in the `from` clause of a [SelectExpression]
 */
abstract class QuerySourceExpression : SqlExpression()

/**
 * 查询表达式的基类
 *
 * @property orderBy 排序条件列表
 * @property offset 返回结果的起始记录数
 * @property limit 限制返回结果的条数
 * @property tableAlias 根据 SQL 语法，当 select 语句作为另一个 select 语句的查询源时（嵌套查询），必须设置表别名，即此字段
 */
sealed class QueryExpression : QuerySourceExpression() {
    abstract val orderBy: List<OrderByExpression>
    abstract val offset: Int?
    abstract val limit: Int?
    abstract val tableAlias: String?
    final override val isLeafNode: Boolean = false
}

tailrec fun QueryExpression.findDeclaringColumns(): List<ColumnDeclaringExpression> {
    return when (this) {
        is SelectExpression -> columns
        is UnionExpression -> left.findDeclaringColumns()
    }
}

/**
 * Select 表达式
 *
 * @property columns 查询的列声明列表，若为空，则 select *
 * @property from 查询的来源，一般为 [TableExpression] 或 [JoinExpression]，也可以是嵌套的 [QueryExpression]
 * @property where 查询的条件
 * @property groupBy 分组
 * @property having Having 子句
 * @property isDistinct 是否将返回结果去重
 */
data class SelectExpression(
    val columns: List<ColumnDeclaringExpression> = emptyList(),
    val from: QuerySourceExpression,
    val where: ScalarExpression<Boolean>? = null,
    val groupBy: List<ScalarExpression<*>> = emptyList(),
    val having: ScalarExpression<Boolean>? = null,
    val isDistinct: Boolean = false,
    override val orderBy: List<OrderByExpression> = emptyList(),
    override val offset: Int? = null,
    override val limit: Int? = null,
    override val tableAlias: String? = null
) : QueryExpression()

/**
 * Union 表达式
 *
 * @property left 左查询
 * @property right 右查询
 * @property isUnionAll 是否 union all，即保留所有记录，不去重
 */
data class UnionExpression(
    val left: QueryExpression,
    val right: QueryExpression,
    val isUnionAll: Boolean,
    override val orderBy: List<OrderByExpression> = emptyList(),
    override val offset: Int? = null,
    override val limit: Int? = null,
    override val tableAlias: String? = null
) : QueryExpression()

/**
 * 一元表达式类型
 */
enum class UnaryExpressionType(private val value: String) {
    IS_NULL("is null"),
    IS_NOT_NULL("is not null"),
    UNARY_MINUS("-"),
    UNARY_PLUS("+"),
    NOT("not");

    override fun toString(): String {
        return value
    }
}

/**
 * 一元表达式
 *
 * @property type 表达式类型
 * @property operand 操作数表达式
 */
data class UnaryExpression<T : Any>(
    val type: UnaryExpressionType,
    val operand: ScalarExpression<*>,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false
) : ScalarExpression<T>()

/**
 * 二元表达式类型
 */
enum class BinaryExpressionType(private val value: String) {
    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    DIV("/"),
    REM("%"),
    LIKE("like"),
    NOT_LIKE("not like"),
    AND("and"),
    OR("or"),
    XOR("xor"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    EQUAL("="),
    NOT_EQUAL("<>");

    override fun toString(): String {
        return value
    }
}

/**
 * 二元表达式
 *
 * @property type 表达式类型
 * @property left 左操作数
 * @property right 右操作数
 */
data class BinaryExpression<T : Any>(
    val type: BinaryExpressionType,
    val left: ScalarExpression<*>,
    val right: ScalarExpression<*>,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false
) : ScalarExpression<T>()

/**
 * Table 表达式
 *
 * @property name 表名
 * @property tableAlias 表别名
 */
data class TableExpression(
    val name: String,
    val tableAlias: String?,
    override val isLeafNode: Boolean = true
) : QuerySourceExpression()

/**
 * 列表达式
 *
 * @property tableAlias 该列所属的表的别名
 * @property name 列名
 */
data class ColumnExpression<T : Any>(
    val tableAlias: String?,
    val name: String,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = true
) : ScalarExpression<T>()

/**
 * 列声明表达式，select a.name as label from dual 语句中的 a.name as label 部分
 *
 * @property expression 该声明的来源表达式，一般是 [ColumnExpression] 也可以是其他表达式类型
 * @property declaredName 所声明的列别名，即 label
 */
data class ColumnDeclaringExpression(
    val expression: ScalarExpression<*>,
    val declaredName: String? = null,
    override val isLeafNode: Boolean = false
) : SqlExpression()

/**
 * Order by 排序方向
 */
enum class OrderType(private val value: String) {
    ASCENDING("asc"),
    DESCENDING("desc");

    override fun toString(): String {
        return value
    }
}

/**
 * Order by 表达式
 *
 * @property expression 排序来源表达式，一般是 [ColumnExpression] 也可以说其他表达式类型
 * @property orderType 排序方向
 */
data class OrderByExpression(
    val expression: ScalarExpression<*>,
    val orderType: OrderType,
    override val isLeafNode: Boolean = false
) : SqlExpression()

/**
 * 表联接类型
 */
enum class JoinType(private val value: String) {
    CROSS_JOIN("cross join"),
    INNER_JOIN("inner join"),
    LEFT_JOIN("left join"),
    RIGHT_JOIN("right join");

    override fun toString(): String {
        return value
    }
}

/**
 * 联表表达式
 *
 * @property type 联接类型
 * @property left 左表
 * @property right 右表
 * @property condition 联接条件
 */
data class JoinExpression(
    val type: JoinType,
    val left: QuerySourceExpression,
    val right: QuerySourceExpression,
    val condition: ScalarExpression<Boolean>? = null,
    override val isLeafNode: Boolean = false
) : QuerySourceExpression()

/**
 * SQL in 表达式，判断左操作数是否在右操作数集合中存在
 *
 * @property left 左操作数
 * @property query 右操作数可为查询，不可与 [values] 字段共存
 * @property values 右操作数可为集合，不可与 [query] 字段共存
 * @property notInList 是否取反
 */
data class InListExpression<T : Any>(
    val left: ScalarExpression<T>,
    val query: QueryExpression? = null,
    val values: List<ScalarExpression<T>>? = null,
    val notInList: Boolean = false,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false
) : ScalarExpression<Boolean>()

/**
 * SQL exists 表达式，判断指定查询是否至少返回一条结果
 *
 * @property query 查询表达式
 * @property notExists 是否取反
 */
data class ExistsExpression(
    val query: QueryExpression,
    val notExists: Boolean = false,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false
) : ScalarExpression<Boolean>()

/**
 * 字段聚合类型
 */
enum class AggregateType(private val value: String) {
    MIN("min"),
    MAX("max"),
    AVG("avg"),
    SUM("sum"),
    COUNT("count");

    override fun toString(): String {
        return value
    }
}

/**
 * 聚合表达式
 *
 * @property type 聚合类型
 * @property argument 所聚合的内容，一般为 [ColumnExpression] 也可为其他表达式类型
 * @property isDistinct 是否去重
 */
data class AggregateExpression<T : Any>(
    val type: AggregateType,
    val argument: ScalarExpression<*>?,
    val isDistinct: Boolean,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false
) : ScalarExpression<T>()

/**
 * SQL between 表达式，判断左操作数是否在给定范围中
 *
 * @property expression 左操作数
 * @property lower 下界
 * @property upper 上界
 * @property notBetween 是否取反
 */
data class BetweenExpression<T : Any>(
    val expression: ScalarExpression<T>,
    val lower: ScalarExpression<T>,
    val upper: ScalarExpression<T>,
    val notBetween: Boolean = false,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false
) : ScalarExpression<Boolean>()

/**
 * 参数表达式
 *
 * @property value 所包装的参数值
 * @property sqlType 该参数的 SQL 类型
 */
data class ArgumentExpression<T : Any>(
    val value: T?,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = true
) : ScalarExpression<T>()

/**
 * 函数调用表达式
 *
 * @property functionName 函数名
 * @property arguments 函数调用的参数列表
 */
data class FunctionExpression<T : Any>(
    val functionName: String,
    val arguments: List<ScalarExpression<*>>,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false
) : ScalarExpression<T>()

/**
 * 列赋值表达式
 *
 * @property column 要被赋值的列，左值
 * @property expression 被赋予的值，右值
 */
data class ColumnAssignmentExpression<T : Any>(
    val column: ColumnExpression<T>,
    val expression: ScalarExpression<T>,
    override val isLeafNode: Boolean = false
) : SqlExpression()

/**
 * 插入表达式
 *
 * @property table 要插入的表
 * @property assignments 赋值列表
 */
data class InsertExpression(
    val table: TableExpression,
    val assignments: List<ColumnAssignmentExpression<*>>,
    override val isLeafNode: Boolean = false
) : SqlExpression()

/**
 * 从查询中批量插入数据表达式，insert into tmp(num) select 1 from dual
 *
 * @property table 要插入的表
 * @property columns 要插入数据的字段
 * @property query 查询表达式
 */
data class InsertFromQueryExpression(
    val table: TableExpression,
    val columns: List<ColumnExpression<*>>,
    val query: QueryExpression,
    override val isLeafNode: Boolean = false
) : SqlExpression()

/**
 * 更新表达式
 *
 * @property table 要更新的表
 * @property assignments 赋值列表
 * @property where 更新条件
 */
data class UpdateExpression(
    val table: TableExpression,
    val assignments: List<ColumnAssignmentExpression<*>>,
    val where: ScalarExpression<Boolean>? = null,
    override val isLeafNode: Boolean = false
) : SqlExpression()

/**
 * 删除表达式
 *
 * @property table 要删除的记录所在的表
 * @property where 删除条件
 */
data class DeleteExpression(
    val table: TableExpression,
    val where: ScalarExpression<Boolean>?,
    override val isLeafNode: Boolean = false
) : SqlExpression()
