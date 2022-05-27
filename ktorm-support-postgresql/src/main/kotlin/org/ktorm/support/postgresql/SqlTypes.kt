/*
 * Copyright 2018-2022 the original author or authors.
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

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import org.postgresql.util.PGobject
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Represent values of PostgreSQL `hstore` SQL type.
 */
public typealias HStore = Map<String, String?>

/**
 * Represent values of PostgreSQL `text[]` SQL type.
 */
public typealias TextArray = Array<String?>

/**
 * Define a column typed [HStoreSqlType].
 */
public fun BaseTable<*>.hstore(name: String): Column<HStore> {
    return registerColumn(name, HStoreSqlType)
}

/**
 * [SqlType] implementation represents PostgreSQL `hstore` type.
 */
public object HStoreSqlType : SqlType<HStore>(Types.OTHER, "hstore") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: HStore) {
        ps.setObject(index, parameter)
    }

    @Suppress("UNCHECKED_CAST")
    override fun doGetResult(rs: ResultSet, index: Int): HStore? {
        return rs.getObject(index) as HStore?
    }
}

/**
 * Define a column typed [TextArraySqlType].
 */
public fun BaseTable<*>.textArray(name: String): Column<TextArray> {
    return registerColumn(name, TextArraySqlType)
}

/**
 * [SqlType] implementation represents PostgreSQL `text[]` type.
 */
public object TextArraySqlType : SqlType<TextArray>(Types.ARRAY, "text[]") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: TextArray) {
        ps.setObject(index, parameter)
    }

    @Suppress("UNCHECKED_CAST")
    override fun doGetResult(rs: ResultSet, index: Int): TextArray? {
        val sqlArray = rs.getArray(index) ?: return null
        try {
            val objectArray = sqlArray.array as Array<Any?>?
            return objectArray?.map { it as String? }?.toTypedArray()
        } finally {
            sqlArray.free()
        }
    }
}

/**
 * Represents location of a point on the surface of the Earth.
 * Part of PostgreSQL's `earthdistance` extension.
 * https://www.postgresql.org/docs/12/earthdistance.html
 */
public typealias Earth = Triple<Double, Double, Double>

/**
 * Represents a point on Earth's surface
 * Part of PostgreSQL's `earthdistance` SQL extension.
 */
public object PGEarthType : SqlType<Earth>(Types.OTHER, "earth") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Earth) {
        ps.setObject(index, parameter, Types.OTHER)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Earth? {
        return rs.getObject(index)?.let {
            (it as PGobject).value
                .substring(1, it.value.length - 1)
                .split(",")
                .let { rawNumbers ->
                    Earth(rawNumbers[0].toDouble(), rawNumbers[1].toDouble(), rawNumbers[2].toDouble())
                }
        }
    }
}

/**
 * Define a column typed [PGEarthType].
 */
public fun BaseTable<*>.earth(name: String): Column<Earth> = registerColumn(name, PGEarthType)

/**
 * Represents a box suitable for an indexed search using the cube @> operator.
 * Part of PostgreSQL's `cube` SQL extension.
 * https://www.postgresql.org/docs/9.5/cube.html
 */
public data class Cube(val x: DoubleArray, val y: DoubleArray) {
    init {
        if (x.size != y.size) {
            throw IllegalArgumentException("x and y should have same dimensions.")
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is Cube && x.contentEquals(other.x) && y.contentEquals(other.y)
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + x.contentHashCode()
        result = 31 * result + y.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "(${x.joinToString(", ")}), (${y.joinToString(", ")})"
    }
}

/**
 * Define a column typed [CubeSqlType].
 */
public fun BaseTable<*>.cube(name: String): Column<Cube> {
    return registerColumn(name, CubeSqlType)
}

/**
 * Represents a Cube by storing 2 n-dimensional points
 * Part of PostgreSQL's `cube` SQL extension.
 * https://www.postgresql.org/docs/9.5/cube.html
 */
public object CubeSqlType : SqlType<Cube>(Types.OTHER, "cube") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Cube) {
        ps.setObject(index, parameter, Types.OTHER)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Cube? {
        val obj = rs.getObject(index) as PGobject?
        if (obj == null) {
            return null
        } else {
            // (1, 2, 3), (4, 5, 6)
            val numbers = obj.value.replace("(", "").replace(")", "").split(",").map { it.trim().toDouble() }
            val (x, y) = numbers.chunked(numbers.size / 2).map { it.toDoubleArray() }
            return Cube(x, y)
        }
    }
}
