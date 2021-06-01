/*
 * Copyright 2018-2021 the original author or authors.
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

package org.ktorm.autotable

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import kotlin.reflect.KProperty1

/**
 * 将属性转化为表列的适配器.
 */
public interface TypeAdapter<T : Any> {
    /**
     * 该适配器的优先级，优先级越高则越优先匹配.
     * 同等优先级先注册者先匹配.
     */
    public val priority: Int get() = 0

    /**
     * 注册列.
     * 如果失败则返回null.
     */
    public fun register(
        table: BaseTable<Any>,
        field: KProperty1<Any, T>,
    ): Column<T>?
}
