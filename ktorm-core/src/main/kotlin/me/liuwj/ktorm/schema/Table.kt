package me.liuwj.ktorm.schema

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.expression.TableExpression
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
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
@Suppress("UNCHECKED_CAST")
open class Table<E : Entity<E>>(
    val tableName: String,
    val alias: String? = null,
    entityClass: KClass<E>? = null
) : TypeReference<E>() {

    private val _refCounter = AtomicInteger()
    private val _columns = LinkedHashMap<String, Column<*>>()
    private var _primaryKeyName: String? = null

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
                    val col = column as AliasedColumn<Any>
                    val originColumn = col.originColumn.copy(table = this)
                    _columns[col.alias] = col.copy(originColumn = originColumn, binding = newBinding)
                }
            }
        }
    }

    private fun copyReference(table: Table<*>): Table<*> {
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

        val originColumn = this as SimpleColumn<C>
        _columns[alias] = AliasedColumn(originColumn.copy(binding = null), alias)
        return ColumnRegistration(alias)
    }

    /**
     * 封装了对新注册的列添加更多修改的操作
     */
    inner class ColumnRegistration<C : Any>(private val key: String) : ReadOnlyProperty<Table<E>, Column<C>> {

        /**
         * Current table object.
         */
        val table = this@Table

        /**
         * 获取该列，实现从 [ReadOnlyProperty] 来的 getValue 方法，以支持 by 语法
         */
        override operator fun getValue(thisRef: Table<E>, property: KProperty<*>): Column<C> {
            assert(thisRef === table)
            return getColumn()
        }

        /**
         * 获取该列
         */
        fun getColumn(): Column<C> {
            val column = _columns[key] ?: throw NoSuchElementException(key)
            return column as Column<C>
        }

        /**
         * 将当前列设置为主键
         */
        fun primaryKey(): ColumnRegistration<C> {
            if (getColumn() is AliasedColumn) {
                throw UnsupportedOperationException("Cannot set aliased column $key as a primary key.")
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
        inline fun <R : Entity<R>> references(referenceTable: Table<R>, selector: (E) -> R?): ColumnRegistration<C> {
            val entityClass = table.entityClass ?: error("No entity class configured for table: ${table.tableName}")
            val properties = ArrayList<KProperty1<*, *>>()

            val proxy = ColumnBindingHandler.createProxy(entityClass, properties)
            selector(proxy as E)

            if (properties.isEmpty()) {
                throw IllegalArgumentException("No binding properties found.")
            }
            if (properties.size > 1) {
                throw IllegalArgumentException("Reference binding doesn't support nested properties.")
            }

            return bindTo(ReferenceBinding(referenceTable, properties[0]))
        }

        inline fun bindTo(selector: (E) -> C?): ColumnRegistration<C> {
            val entityClass = table.entityClass ?: error("No entity class configured for table: ${table.tableName}")
            val properties = ArrayList<KProperty1<*, *>>()

            val proxy = ColumnBindingHandler.createProxy(entityClass, properties)
            selector(proxy as E)

            if (properties.isEmpty()) {
                throw IllegalArgumentException("No binding properties found.")
            }

            return bindTo(NestedBinding(properties))
        }

        fun bindTo(binding: ColumnBinding): ColumnRegistration<C> {
            val checkedBinding = when (binding) {
                is NestedBinding -> binding
                is ReferenceBinding -> {
                    checkCircularReference(binding.referenceTable)
                    ReferenceBinding(copyReference(binding.referenceTable), binding.onProperty)
                }
            }

            val column = _columns[key] ?: throw NoSuchElementException(key)

            _columns[key] = when (column) {
                is SimpleColumn -> column.copy(binding = checkedBinding)
                is AliasedColumn -> column.copy(binding = checkedBinding)
            }

            return this
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

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Deprecated method, please use references(table) { it.prop } instead", level = DeprecationLevel.ERROR)
        fun <R : Entity<R>> references(referenceTable: Table<R>, onProperty: KProperty1<E, R?>): ColumnRegistration<C> {
            throw UnsupportedOperationException("Deprecated method, please use references(table) { it.prop } instead")
        }

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Deprecated method, please use bindTo { it.prop } instead", level = DeprecationLevel.ERROR)
        fun bindTo(property: KProperty1<E, C?>): ColumnRegistration<C> {
            throw UnsupportedOperationException("Deprecated method, please use bindTo { it.prop } instead")
        }

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Deprecated method, please use bindTo { it.prop1.prop2 } instead", level = DeprecationLevel.ERROR)
        fun <R : Entity<R>> bindTo(property1: KProperty1<E, R?>, property2: KProperty1<R, C?>): ColumnRegistration<C> {
            throw UnsupportedOperationException("Deprecated method, please use bindTo { it.prop1.prop2 } instead")
        }

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Deprecated method, please use bindTo { it.prop1.prop2.prop3 } instead", level = DeprecationLevel.ERROR)
        fun <R : Entity<R>, S : Entity<S>> bindTo(
            property1: KProperty1<E, R?>,
            property2: KProperty1<R, S?>,
            property3: KProperty1<S, C?>
        ): ColumnRegistration<C> {
            throw UnsupportedOperationException("Deprecated method, please use bindTo { it.prop1.prop2.prop3 } instead")
        }

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Deprecated method, please use bindTo { it.prop1.prop2.prop3.prop4 } instead", level = DeprecationLevel.ERROR)
        fun <R : Entity<R>, S : Entity<S>, T : Entity<T>> bindTo(
            property1: KProperty1<E, R?>,
            property2: KProperty1<R, S?>,
            property3: KProperty1<S, T?>,
            property4: KProperty1<T, C?>
        ): ColumnRegistration<C> {
            throw UnsupportedOperationException("Deprecated method, please use bindTo { it.prop1.prop2.prop3.prop4 } instead")
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
