/*
 * Copyright 2018-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")

package org.ktorm.global

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.ColumnDeclaring

/**
 * Join the right table and return a new [QuerySource], translated to `cross join` in SQL.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun BaseTable<*>.crossJoin(right: BaseTable<*>, on: ColumnDeclaring<Boolean>? = null): QuerySource {
    return Database.global.from(this).crossJoin(right, on)
}

/**
 * Join the right table and return a new [QuerySource], translated to `inner join` in SQL.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun BaseTable<*>.innerJoin(right: BaseTable<*>, on: ColumnDeclaring<Boolean>? = null): QuerySource {
    return Database.global.from(this).innerJoin(right, on)
}

/**
 * Join the right table and return a new [QuerySource], translated to `left join` in SQL.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun BaseTable<*>.leftJoin(right: BaseTable<*>, on: ColumnDeclaring<Boolean>? = null): QuerySource {
    return Database.global.from(this).leftJoin(right, on)
}

/**
 * Join the right table and return a new [QuerySource], translated to `right join` in SQL.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun BaseTable<*>.rightJoin(right: BaseTable<*>, on: ColumnDeclaring<Boolean>? = null): QuerySource {
    return Database.global.from(this).rightJoin(right, on)
}

/**
 * Return a new-created [Query] object, left joining all the reference tables, and selecting all columns of them.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun BaseTable<*>.joinReferencesAndSelect(): Query {
    return Database.global.from(this).joinReferencesAndSelect()
}

/**
 * Create a query object, selecting the specific columns or expressions from this table.
 *
 * Note that the specific columns can be empty, that means `select *` in SQL.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun BaseTable<*>.select(columns: Collection<ColumnDeclaring<*>>): Query {
    return Database.global.from(this).select(columns)
}

/**
 * Create a query object, selecting the specific columns or expressions from this table.
 *
 * Note that the specific columns can be empty, that means `select *` in SQL.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun BaseTable<*>.select(vararg columns: ColumnDeclaring<*>): Query {
    return Database.global.from(this).select(*columns)
}

/**
 * Create a query object, selecting the specific columns or expressions from this table distinctly.
 *
 * Note that the specific columns can be empty, that means `select distinct *` in SQL.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun BaseTable<*>.selectDistinct(columns: Collection<ColumnDeclaring<*>>): Query {
    return Database.global.from(this).selectDistinct(columns)
}

/**
 * Create a query object, selecting the specific columns or expressions from this table distinctly.
 *
 * Note that the specific columns can be empty, that means `select distinct *` in SQL.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun BaseTable<*>.selectDistinct(vararg columns: ColumnDeclaring<*>): Query {
    return Database.global.from(this).selectDistinct(*columns)
}
