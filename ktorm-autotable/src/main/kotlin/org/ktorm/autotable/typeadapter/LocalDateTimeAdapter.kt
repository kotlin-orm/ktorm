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

package org.ktorm.autotable.typeadapter

import org.ktorm.autotable.TypeAdapter
import org.ktorm.autotable.datetime
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import java.time.LocalDateTime
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmErasure

/**
 * TypeAdapter to register LocalDateTime Column.
 */
public object LocalDateTimeAdapter : TypeAdapter<LocalDateTime> {
    override fun register(table: BaseTable<Any>, field: KProperty1<Any, LocalDateTime>): Column<LocalDateTime>? {
        return if (field.returnType.jvmErasure == LocalDateTime::class) {
            table.datetime(field)
        } else {
            null
        }
    }

    override fun toString(): String = "LocalDateTimeAdapter"
}
