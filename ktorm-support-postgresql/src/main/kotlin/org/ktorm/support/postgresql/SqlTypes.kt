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

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Represent values of PostgreSQL `text[]` SQL type.
 */
public typealias TextArray = Array<String?>

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
 * Represent values of PostgreSQL `hstore` SQL type.
 */
public typealias HStore = Map<String, String?>

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
 * Represents a box suitable for an indexed search using the cube @> operator.
 * Part of PostgreSQL `cube` SQL extension.
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
 * Part of PostgreSQL `cube` SQL extension.
 * https://www.postgresql.org/docs/9.5/cube.html
 */
public object CubeSqlType : SqlType<Cube>(Types.OTHER, "cube") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Cube) {
        ps.setObject(index, parameter, Types.OTHER)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Cube? =
        rs.getPgObject(index)?.value?.let {
            val numbers = it.replace("(", "").replace(")", "").split(",").map { it.trim().toDouble() }
            val (x, y) = numbers.chunked(numbers.size / 2).map { it.toDoubleArray() }
            Cube(x, y)
        }
}

/**
 * Cube-based earth abstraction, using 3 coordinates representing the x, y, and z distance from the center of the Earth.
 * Part of PostgreSQL `earthdistance` extension.
 * https://www.postgresql.org/docs/12/earthdistance.html
 */
public typealias Earth = Triple<Double, Double, Double>

/**
 * Define a column typed [EarthSqlType].
 */
public fun BaseTable<*>.earth(name: String): Column<Earth> {
    return registerColumn(name, EarthSqlType)
}

/**
 * Cube-based earth abstraction, using 3 coordinates representing the x, y, and z distance from the center of the Earth.
 * Part of PostgreSQL `earthdistance` SQL extension.
 */
public object EarthSqlType : SqlType<Earth>(Types.OTHER, "earth") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Earth) {
        ps.setObject(index, parameter, Types.OTHER)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Earth? =
        rs.getPgObject(index)?.value?.let {
            val (x, y, z) = it.removeSurrounding("(", ")").split(",").map { it.trim().toDouble() }
            return Earth(x, y, z)
        }
}

/**
 * Sorted list of lexemes, which are words that have been normalized to merge different variants of the same word and
 * are used for text search.
 * See: https://www.postgresql.org/docs/current/datatype-textsearch.html
 */
public typealias TSVector = List<TSVectorLexeme>

/**
 * A lexeme of a tsvector which is the actual word with its weighted positions. The positions are optional and empty by
 * default.
 * See: https://www.postgresql.org/docs/current/datatype-textsearch.html
 */
public data class TSVectorLexeme(val word: String, val positions: List<TSVectorLexemePosition> = emptyList())

/**
 * The position of a lexeme contains its numeric position and a weight represented as a character ranging A-D. The
 * weight is optional and the default value is 'D'. If the weight does not exist, then the weight is 'D'
 * See: https://www.postgresql.org/docs/current/datatype-textsearch.html
 */
public data class TSVectorLexemePosition(val position: Int, val weight: Char? = null)

/**
 * Defines a column typed [TSVectorSqlType].
 */
public fun BaseTable<*>.tsvector(name: String): Column<TSVector> = registerColumn(name, TSVectorSqlType)

/**
 * Defines a column typed [TSQuerySqlType].
 */
public fun BaseTable<*>.tsquery(name: String): Column<TSQuery> = registerColumn(name, TSQuerySqlType)

/**
 * [SqlType] Implementation represents PostgreSQL `tsvector` type.
 */
public object TSVectorSqlType : SqlType<TSVector>(Types.OTHER, "tsvector") {
    // Examples of a tsvector:
    //      - '   ' 'Joe''s' 'a' 'and' 'contains' 'lexeme' 'quote' 'spaces' 'the'
    //      - 'a':1A 'cat':6 'fat':2B,5C 'nice: 3,7' 'slow: 4B,8'
    //
    // A single word always starts and ends with a "'". If the word contains a "'", then this character can be escaped
    // by using it twice ("''").
    // Regex explained: Start with "'", then match as long as there is no "'", except when it is "''" and finish on "'".
    private const val wordRegex = """'((?:[^']|'')+)'"""
    // The positions list starts with ":" as the separator to the word and then continues as a list of the position
    // number and optionally its weight. The position entries are seperated by ","
    // Regex explained: Start with ":", then match for any word character (includes numbers and letters) followed by an
    // optional "," (the last entry doe not contain ",")
    private const val positionsListRegex = """:((?:\w+,?)+)"""
    // A lexeme contains of a word and optionally its positions list.
    private val tsVectorLexemeRegex = Regex("""$wordRegex(?:$positionsListRegex)?""")

    override fun doGetResult(rs: ResultSet, index: Int): TSVector? =
        rs.getPgObject(index)?.value?.let { value ->
            tsVectorLexemeRegex.findAll(value).map { res ->
                val unescapedWord = res.groupValues[1].replace("''", "'")
                val positions = res.groupValues[2].split(",").mapNotNull { pos ->
                    if (pos.isNotEmpty()) {
                        val position = pos.takeWhile { it.isDigit() }.toInt()
                        val weight = pos.lastOrNull()?.takeUnless { it.isDigit() }
                        TSVectorLexemePosition(position, weight)
                    } else {
                        null
                    }
                }
                TSVectorLexeme(unescapedWord, positions)
            }.toList()
        }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: TSVector) {
        val lexemes = parameter.joinToString(" ") { lexeme ->
            val escapedWord = lexeme.word.replace("'", "''")
            val positions = lexeme.positions.map {
                "${it.position}${it.weight ?: ""}"
            }.ifEmpty { null }?.joinToString(",", ":")
            "'$escapedWord'${positions ?: ""}"
        }
        ps.setPgObject(index, PgObject(this.typeName, lexemes))
    }
}

/**
 * The query to do text search
 * See: https://www.postgresql.org/docs/current/datatype-textsearch.html
 */
public typealias TSQuery = String

/**
 * [SqlType] Implementation represents PostgreSQL `tsquery` type.
 */
public object TSQuerySqlType : SqlType<TSQuery>(Types.OTHER, "tsquery") {
    override fun doGetResult(rs: ResultSet, index: Int): TSQuery? = rs.getString(index)

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: TSQuery) {
        ps.setPgObject(index, PgObject(this.typeName, parameter))
    }
}

/**
 * Configures the text search. E.g.: 'english', 'simple'. Defaults to 'english' if not used.
 * See: https://www.postgresql.org/docs/current/textsearch-controls.html
 */
public typealias RegConfig = String

/**
 * [SqlType] Implementation represents PostgreSQL `regconfig` type.
 */
public object RegConfigSqlType : SqlType<RegConfig>(Types.OTHER, "regconfig") {
    override fun doGetResult(rs: ResultSet, index: Int): RegConfig? = rs.getString(index)

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: RegConfig) {
        ps.setPgObject(index, PgObject(this.typeName, parameter))
    }
}
