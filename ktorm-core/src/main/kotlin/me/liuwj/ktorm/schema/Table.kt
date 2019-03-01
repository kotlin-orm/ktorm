package me.liuwj.ktorm.schema

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.expression.TableExpression
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

/**
 * SQL 数据表
 *
 * @property tableName 表名
 * @property alias 别名
 * @property entityClass 实体类类型
 * @property columns 获取该表中的所有列
 * @property primaryKey 获取该表的主键列
 */
open class Table<E : Entity<E>>(
    val tableName: String,
    val alias: String? = null,
    entityClass: KClass<E>? = null
) : TypeReference<E>() {

    private val _refCounter = AtomicInteger()
    private val _columns = LinkedHashMap<String, Column<*>>()
    private var _primaryKeyName: String? = null

    @Suppress("UNCHECKED_CAST")
    val entityClass: KClass<E>? = entityClass ?: (referencedKotlinType.classifier as? KClass<E>)?.takeIf { it != Nothing::class }

    val columns: List<Column<*>> get() = _columns.values.toList()

    val primaryKey: Column<*>? get() = _primaryKeyName?.let { this[it] }

    /**
     * 使用列名获取表中的某列
     */
    operator fun get(columnName: String): Column<*> {
        return _columns[columnName] ?: throw NoSuchElementException(columnName)
    }

    /**
     * 返回一个新的表对象，该对象与原表具有完全相同的数据和结构，但是赋予了新的 [alias] 属性
     */
    open fun aliased(alias: String): Table<E> {
        val result = Table(tableName, alias, entityClass)
        result.rewriteDefinitions(columns, _primaryKeyName, copyReferences = true)
        return result
    }

    private fun rewriteDefinitions(columns: List<Column<*>>, primaryKeyName: String?, copyReferences: Boolean) {
        _primaryKeyName = primaryKeyName
        _columns.clear()

        if (copyReferences) {
            _refCounter.set(0)
        }

        for (column in columns) {
            val binding = column.binding

            val newBinding = if (copyReferences && binding is ReferenceBinding) {
                binding.copy(referenceTable = copyReference(binding.referenceTable))
            } else {
                binding
            }

            when (column) {
                is SimpleColumn -> {
                    _columns[column.name] = column.copy(table = this, binding = newBinding)
                }
                is AliasedColumn -> {
                    @Suppress("UNCHECKED_CAST")
                    val col = column as AliasedColumn<Any>
                    _columns[col.alias] = col.copy(originColumn = col.originColumn.copy(table = this), binding = newBinding)
                }
            }
        }
    }

    private fun copyReference(table: Table<*>): Table<*> {
        // val copy = table.aliased("${alias ?: tableName}_ref${_refCounter.getAndIncrement()}")
        val copy = table.aliased("_ref${_refCounter.getAndIncrement()}")

        val columns = copy.columns.map {
            val binding = it.binding
            if (binding !is ReferenceBinding) {
                it
            } else {
                val newBinding = binding.copy(referenceTable = copyReference(binding.referenceTable))
                when (it) {
                    is SimpleColumn -> it.copy(binding = newBinding)
                    is AliasedColumn -> it.copy(binding = newBinding)
                }
            }
        }

        copy.rewriteDefinitions(columns, copy._primaryKeyName, copyReferences = false)
        return copy
    }

    /**
     * 提供列名和 SQL 数据类型，往表中注册一个新列；在该方法返回的 [ColumnRegistration] 对象中可以对新注册的列添加更多的修改
     */
    fun <C : Any> registerColumn(name: String, sqlType: SqlType<C>): ColumnRegistration<C> {
        if (name in _columns) {
            throw IllegalArgumentException("Duplicate column name: $name")
        }

        _columns[name] = SimpleColumn(this, name, sqlType)
        return ColumnRegistration(name)
    }

    /**
     * 对指定的列给定一个别名，额外注册一个别名列；在该方法返回的 [ColumnRegistration] 对象中可以对新注册的列添加更多的修改
     *
     * @see AliasedColumn
     */
    fun <C : Any> Column<C>.aliased(alias: String): ColumnRegistration<C> {
        if (alias in _columns) {
            throw IllegalArgumentException("Duplicate column name: $alias")
        }

        _columns[alias] = AliasedColumn(this as SimpleColumn<C>, alias)
        return ColumnRegistration(alias)
    }

    /**
     * 封装了对新注册的列添加更多修改的操作
     */
    inner class ColumnRegistration<C : Any>(private val key: String) : ReadOnlyProperty<Table<E>, Column<C>> {

        /**
         * 获取该列，实现从 [ReadOnlyProperty] 来的 getValue 方法，以支持 by 语法
         */
        override operator fun getValue(thisRef: Table<E>, property: KProperty<*>): Column<C> {
            assert(thisRef === this@Table)
            return getColumn()
        }

        /**
         * 获取该列
         */
        fun getColumn(): Column<C> {
            val column = _columns[key] ?: throw NoSuchElementException(key)
            @Suppress("UNCHECKED_CAST")
            return column as Column<C>
        }

        /**
         * 将当前列设置为主键
         */
        fun primaryKey(): ColumnRegistration<C> {
            val column = getColumn()
            if (column is AliasedColumn) {
                throw UnsupportedOperationException("Cannot set aliased column $key as a primary key.")
            }
            if (column.binding is ReferenceBinding) {
                throw UnsupportedOperationException("Reference binding is not supported on primary key: $column")
            }

            _primaryKeyName = key
            return this
        }

        /**
         * 将列绑定到一个引用表，对应 SQL 中的外键引用，在使用 find* 系列 Entity 扩展函数时，引用表会自动被 left join 联接
         *
         * @see me.liuwj.ktorm.entity.joinReferencesAndSelect
         * @see me.liuwj.ktorm.entity.createEntity
         */
        fun <R : Entity<R>> references(referenceTable: Table<R>, onProperty: KProperty1<E, R?>): ColumnRegistration<C> {
            checkAbstractProperties(onProperty)
            checkCircularReference(referenceTable)
            return doBinding(ReferenceBinding(copyReference(referenceTable), onProperty))
        }

        /**
         * Check if the [root] table has the reference to current table.
         */
        private fun checkCircularReference(root: Table<*>, stack: LinkedList<String> = LinkedList()) {
            stack.push(root.tableName)

            if (tableName == root.tableName) {
                throw IllegalArgumentException("Circular reference detected, current table: $tableName, reference route: ${stack.asReversed()}")
            }

            for (column in root.columns) {
                val ref = column.referenceTable
                if (ref != null) {
                    checkCircularReference(ref, stack)
                }
            }

            stack.pop()
        }

        /**
         * Bind the column to a simple property.
         */
        fun bindTo(property: KProperty1<E, C?>): ColumnRegistration<C> {
            checkAbstractProperties(property)
            return doBinding(NestedBinding1(property))
        }

        /**
         * Bind the column to double nested properties.
         */
        fun <R : Entity<R>> bindTo(property1: KProperty1<E, R?>, property2: KProperty1<R, C?>): ColumnRegistration<C> {
            checkAbstractProperties(property1, property2)
            return doBinding(NestedBinding2(property1, property2))
        }

        /**
         * Binding the column to triple nested properties.
         */
        fun <R : Entity<R>, S : Entity<S>> bindTo(
            property1: KProperty1<E, R?>,
            property2: KProperty1<R, S?>,
            property3: KProperty1<S, C?>
        ): ColumnRegistration<C> {

            checkAbstractProperties(property1, property2, property3)
            return doBinding(NestedBinding3(property1, property2, property3))
        }

        /**
         * Binding the column to 4 levels of nested properties.
         */
        fun <R : Entity<R>, S : Entity<S>, T : Entity<T>> bindTo(
            property1: KProperty1<E, R?>,
            property2: KProperty1<R, S?>,
            property3: KProperty1<S, T?>,
            property4: KProperty1<T, C?>
        ): ColumnRegistration<C> {

            checkAbstractProperties(property1, property2, property3, property4)
            return doBinding(NestedBinding4(property1, property2, property3, property4))
        }

        private fun doBinding(binding: ColumnBinding): ColumnRegistration<C> {
            val column = _columns[key] ?: throw NoSuchElementException(key)

            if (column == primaryKey && binding is ReferenceBinding) {
                throw UnsupportedOperationException("Reference binding is not supported on primary key: $column")
            }

            _columns[key] = when (column) {
                is SimpleColumn -> column.copy(binding = binding)
                is AliasedColumn -> column.copy(binding = binding)
            }

            return this
        }

        private fun checkAbstractProperties(vararg properties: KProperty1<*, *>) {
            for (prop in properties) {
                if (!prop.isAbstract) {
                    throw IllegalArgumentException("Cannot bind a column to a non-abstract property: $prop")
                }
            }
        }
    }

    /**
     * 获取该表对应的 SQL 表达式
     */
    fun asExpression(): TableExpression {
        return TableExpression(tableName, alias)
    }

    /**
     * 返回此表的字符串表示形式
     */
    override fun toString(): String {
        if (alias == null) {
            return "table $tableName"
        } else {
            return "table $tableName $alias"
        }
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
