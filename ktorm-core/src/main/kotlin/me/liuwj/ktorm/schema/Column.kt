package me.liuwj.ktorm.schema

import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.ColumnDeclaringExpression
import me.liuwj.ktorm.expression.ColumnExpression
import me.liuwj.ktorm.expression.ScalarExpression
import kotlin.reflect.KProperty1

/**
 * 数据列绑定公共父类，可将数据列绑定到一个简单属性、层级嵌套的属性、也可绑定为另一个表的引用
 */
sealed class ColumnBinding

/**
 * 将列绑定到一个简单属性
 */
data class SimpleBinding(
    val property: KProperty1<*, *>
) : ColumnBinding()

/**
 * 将列绑定到层级嵌套的属性，仅支持两级嵌套
 */
data class NestedBinding(
    val property1: KProperty1<*, *>,
    val property2: KProperty1<*, *>
) : ColumnBinding()

/**
 * Binding the column to triple nested properties
 */
data class TripleNestedBinding(
    val property1: KProperty1<*, *>,
    val property2: KProperty1<*, *>,
    val property3: KProperty1<*, *>
): ColumnBinding()

/**
 * 将列绑定到一个引用表，对应 SQL 中的外键引用，在使用 find* 系列 Entity 扩展函数时，引用表会自动被 left join 联接
 *
 * @see me.liuwj.ktorm.entity.joinReferencesAndSelect
 * @see me.liuwj.ktorm.entity.createEntity
 */
data class ReferenceBinding(
    val referenceTable: Table<*>,
    val onProperty: KProperty1<*, *>
) : ColumnBinding()

/**
 * 列声明
 */
interface ColumnDeclaring<T : Any> {

    /**
     * 该列的 SQL 数据类型
     */
    val sqlType: SqlType<T>

    /**
     * 转换成 SQL 表达式
     */
    fun asExpression(): ScalarExpression<T>

    /**
     * 转换成列声明表达式 [ColumnDeclaringExpression]
     */
    fun asDeclaringExpression(): ColumnDeclaringExpression

    /**
     * 将给定参数包装为 SQL 表达式 [ArgumentExpression]
     */
    fun wrapArgument(argument: T?): ArgumentExpression<T>
}

/**
 * 数据列
 */
sealed class Column<T : Any> : ColumnDeclaring<T> {

    /**
     * 该列所属的表
     */
    abstract val table: Table<*>

    /**
     * 列名
     */
    abstract val name: String

    /**
     * 标签，即 SQL 中 select name as label dual 中的 label 部分，一般来说，从结果集中获取数据时，使用 label 而不是 name
     */
    abstract val label: String

    /**
     * 列绑定，可将数据列绑定到一个简单属性、层级嵌套的属性、也可绑定为另一个表的引用
     */
    abstract val binding: ColumnBinding?

    /**
     * 如果该列绑定了引用表，返回该表，否则返回空，Shortcut for (binding as? ReferenceBinding)?.referenceTable
     */
    val referenceTable: Table<*>? get() = (binding as? ReferenceBinding)?.referenceTable

    /**
     * 转换成 SQL 表达式
     */
    override fun asExpression(): ColumnExpression<T> {
        return ColumnExpression(table.alias ?: table.tableName, name, sqlType)
    }

    /**
     * 转换成列声明表达式 [ColumnDeclaringExpression]
     */
    override fun asDeclaringExpression(): ColumnDeclaringExpression {
        return ColumnDeclaringExpression(expression = asExpression(), declaredName = label)
    }

    /**
     * 将给定参数包装为 SQL 表达式 [ArgumentExpression]
     */
    override fun wrapArgument(argument: T?): ArgumentExpression<T> {
        return ArgumentExpression(argument, sqlType)
    }

    /**
     * 返回此列的字符串表示形式
     */
    override fun toString(): String {
        return "${table.alias ?: table.tableName}.$name"
    }

    /**
     * 重写 equals，并禁止子类重写，列对象只有引用相等时才视为相等
     */
    final override fun equals(other: Any?): Boolean {
        return this === other
    }

    /**
     * 重写 hashCode，并禁止子类重写
     */
    final override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}

/**
 * 简单数据列，[Column] 的默认实现
 */
data class SimpleColumn<T : Any>(
    override val table: Table<*>,
    override val name: String,
    override val sqlType: SqlType<T>,
    override val binding: ColumnBinding? = null
) : Column<T>() {

    override val label: String = "${table.alias ?: table.tableName}_$name"

    override fun toString(): String {
        return "${table.alias ?: table.tableName}.$name"
    }
}

/**
 * 别名列，包装了一个简单列，并额外增加了一个别名用于修改 label，
 * 查询的时候会将原数据列与别名列同时放入到 select 语句中，并赋予不同 label，以支持在同一个列上绑定多个字段，
 * 如 select name as label, name as label1 from dual
 */
data class AliasedColumn<T : Any>(
    val originColumn: SimpleColumn<T>,
    val alias: String,
    override val binding: ColumnBinding? = null
) : Column<T>() {

    override val table: Table<*> = originColumn.table

    override val name: String = originColumn.name

    override val label: String = "${table.alias ?: table.tableName}_$alias"

    override val sqlType: SqlType<T> = originColumn.sqlType

    override fun toString(): String {
        return "$originColumn as $alias"
    }
}