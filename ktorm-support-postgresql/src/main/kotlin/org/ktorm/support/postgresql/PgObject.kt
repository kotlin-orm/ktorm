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

package org.ktorm.support.postgresql

import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 *  Custom PGObject class that can be used instead of the actual implementation. The actual implementation is not a
 *  JDK 9 module, thus we are not able to require it in module-info.java.
 *
 *  The implementation accesses the postgresql API by reflection.
 */
@JvmInline
internal value class PgObject private constructor(internal val pgObject: Any) {

    internal val type: String
        get() = getTypeMethod.invoke(pgObject) as String

    internal val value: String?
        get() = getValueMethod.invoke(pgObject) as String?

    internal companion object {
        private val pgObjectClass = Class.forName("org.postgresql.util.PGobject")
        private val pgObjectConstructor = pgObjectClass.getDeclaredConstructor()
        private val setTypeMethod = pgObjectClass.getMethod("setType", String::class.java)
        private val setValueMethod = pgObjectClass.getMethod("setValue", String::class.java)
        private val getTypeMethod = pgObjectClass.getMethod("getType")
        private val getValueMethod = pgObjectClass.getMethod("getValue")

        internal fun fromPGobject(obj: Any): PgObject = PgObject(pgObjectClass.cast(obj))

        internal operator fun invoke(type: String, value: String?): PgObject {
            val pgObject = pgObjectConstructor.newInstance()
            setTypeMethod.invoke(pgObject, type)
            setValueMethod.invoke(pgObject, value)
            return PgObject(pgObject)
        }
    }
}

internal fun PreparedStatement.setPgObject(parameterIndex: Int, ktormPgObject: PgObject) {
    this.setObject(parameterIndex, ktormPgObject.pgObject)
}

internal fun ResultSet.getPgObject(columnIndex: Int): PgObject? {
    val obj = this.getObject(columnIndex)
    if (obj == null) {
        return null
    } else {
        return PgObject.fromPGobject(obj)
    }
}
