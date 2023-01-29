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

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.FunctionExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.DoubleSqlType
import org.ktorm.schema.SqlType

/**
 * Enum for 'cube' and 'earthdistance' binary operators.
 */
public enum class CubeExpressionType(private val value: String) {

    /**
     * Cube overlaps operator, translated to the && operator in PostgreSQL.
     */
    OVERLAP("&&"),

    /**
     * Cube contains operator, translated to the @> operator in PostgreSQL.
     */
    CONTAINS("@>"),

    /**
     * Cube contained in operator, translated to the <@ operator in PostgreSQL.
     */
    CONTAINED_IN("<@");

    override fun toString(): String {
        return value
    }
}

/**
 * Expression class represents PostgreSQL `Cube` operations.
 *
 * @property type the expression's type.
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
public data class CubeExpression<T : Any>(
    val type: CubeExpressionType,
    val left: ScalarExpression<*>,
    val right: ScalarExpression<*>,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * Cube contains operator, translated to the @> operator in PostgreSQL.
 */
public fun ColumnDeclaring<Cube>.contains(expr: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.CONTAINS, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Cube contains operator, translated to the @> operator in PostgreSQL.
 */
public fun ColumnDeclaring<Cube>.contains(argument: Cube): CubeExpression<Boolean> {
    return this.contains(wrapArgument(argument))
}

/**
 * Cube contains operator, translated to the @> operator in PostgreSQL.
 */
@JvmName("containsEarth")
public fun ColumnDeclaring<Cube>.contains(expr: ColumnDeclaring<Earth>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.CONTAINS, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Cube contains operator, translated to the @> operator in PostgreSQL.
 */
@JvmName("containsEarth")
public fun ColumnDeclaring<Cube>.contains(argument: Earth): CubeExpression<Boolean> {
    return this.contains(ArgumentExpression(argument, EarthSqlType))
}

/**
 * Cube contained in operator, translated to the <@ operator in PostgreSQL.
 */
public fun ColumnDeclaring<Cube>.containedIn(expr: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.CONTAINED_IN, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Cube contained in operator, translated to the <@ operator in PostgreSQL.
 */
public fun ColumnDeclaring<Cube>.containedIn(argument: Cube): CubeExpression<Boolean> {
    return this.containedIn(wrapArgument(argument))
}

/**
 * Cube contained in operator, translated to the <@ operator in PostgreSQL.
 */
@JvmName("earthContainedIn")
public fun ColumnDeclaring<Earth>.containedIn(expr: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.CONTAINED_IN, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Cube contained in operator, translated to the <@ operator in PostgreSQL.
 */
@JvmName("earthContainedIn")
public fun ColumnDeclaring<Earth>.containedIn(argument: Cube): CubeExpression<Boolean> {
    return this.containedIn(ArgumentExpression(argument, CubeSqlType))
}

/**
 * Cube overlap operator, translated to the && operator in PostgreSQL.
 */
public fun ColumnDeclaring<Cube>.overlaps(expr: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.OVERLAP, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Cube overlap operator, translated to the && operator in PostgreSQL.
 */
public fun ColumnDeclaring<Cube>.overlaps(argument: Cube): CubeExpression<Boolean> {
    return this.overlaps(wrapArgument(argument))
}

/**
 * Returns the location of a point on the surface of the Earth
 * given its latitude (argument 1) and longitude (argument 2) in degrees.
 *
 * Function from earthdistance extension
 */
public fun llToEarth(lat: ColumnDeclaring<Double>, lng: ColumnDeclaring<Double>): FunctionExpression<Earth> {
    // ll_to_earth(lat, lng)
    return FunctionExpression(
        functionName = "ll_to_earth",
        arguments = listOf(lat.asExpression(), lng.asExpression()),
        sqlType = EarthSqlType
    )
}

/**
 * Returns the location of a point on the surface of the Earth
 * given its latitude (argument 1) and longitude (argument 2) in degrees.
 *
 * Function from earthdistance extension
 */
public fun llToEarth(lat: ColumnDeclaring<Double>, lng: Double): FunctionExpression<Earth> {
    return llToEarth(lat, ArgumentExpression(lng, DoubleSqlType))
}

/**
 * Returns the location of a point on the surface of the Earth
 * given its latitude (argument 1) and longitude (argument 2) in degrees.
 *
 * Function from earthdistance extension
 */
public fun llToEarth(lat: Double, lng: ColumnDeclaring<Double>): FunctionExpression<Earth> {
    return llToEarth(ArgumentExpression(lat, DoubleSqlType), lng)
}

/**
 * Returns the location of a point on the surface of the Earth
 * given its latitude (argument 1) and longitude (argument 2) in degrees.
 *
 * Function from earthdistance extension
 */
public fun llToEarth(lat: Double, lng: Double): FunctionExpression<Earth> {
    return llToEarth(ArgumentExpression(lat, DoubleSqlType), ArgumentExpression(lng, DoubleSqlType))
}

/**
 * Returns the latitude in degrees of a point on the surface of the Earth.
 *
 * Function from earthdistance extension, `latitude(earth)` in SQL.
 */
public fun latitude(earth: ColumnDeclaring<Earth>): FunctionExpression<Double> {
    // latitude(earth)
    return FunctionExpression(
        functionName = "latitude",
        arguments = listOf(earth.asExpression()),
        sqlType = DoubleSqlType
    )
}

/**
 * Returns the latitude in degrees of a point on the surface of the Earth.
 *
 * Function from earthdistance extension, `latitude(earth)` in SQL.
 */
public fun latitude(earth: Earth): FunctionExpression<Double> {
    return latitude(ArgumentExpression(earth, EarthSqlType))
}

/**
 * Returns the longitude in degrees of a point on the surface of the Earth.
 *
 * Function from earthdistance extension, `longitude(earth)` in SQL.
 */
public fun longitude(earth: ColumnDeclaring<Earth>): FunctionExpression<Double> {
    // longitude(earth)
    return FunctionExpression(
        functionName = "longitude",
        arguments = listOf(earth.asExpression()),
        sqlType = DoubleSqlType
    )
}

/**
 * Returns the longitude in degrees of a point on the surface of the Earth.
 *
 * Function from earthdistance extension, `longitude(earth)` in SQL.
 */
public fun longitude(earth: Earth): FunctionExpression<Double> {
    return longitude(ArgumentExpression(earth, EarthSqlType))
}

/**
 * Returns the great circle distance between two points on the surface of the Earth.
 *
 * Function from earthdistance extension, `earth_distance(p1, p2)` in SQL.
 */
public fun earthDistance(p1: ColumnDeclaring<Earth>, p2: ColumnDeclaring<Earth>): FunctionExpression<Double> {
    // earth_distance(p1, p2)
    return FunctionExpression(
        functionName = "earth_distance",
        arguments = listOf(p1.asExpression(), p2.asExpression()),
        sqlType = DoubleSqlType
    )
}

/**
 * Returns the great circle distance between two points on the surface of the Earth.
 *
 * Function from earthdistance extension, `earth_distance(p1, p2)` in SQL.
 */
public fun earthDistance(p1: ColumnDeclaring<Earth>, p2: Earth): FunctionExpression<Double> {
    return earthDistance(p1, ArgumentExpression(p2, EarthSqlType))
}

/**
 * Returns the great circle distance between two points on the surface of the Earth.
 *
 * Function from earthdistance extension, `earth_distance(p1, p2)` in SQL.
 */
public fun earthDistance(p1: Earth, p2: ColumnDeclaring<Earth>): FunctionExpression<Double> {
    return earthDistance(ArgumentExpression(p1, EarthSqlType), p2)
}

/**
 * Returns the great circle distance between two points on the surface of the Earth.
 *
 * Function from earthdistance extension, `earth_distance(p1, p2)` in SQL.
 */
public fun earthDistance(p1: Earth, p2: Earth): FunctionExpression<Double> {
    return earthDistance(ArgumentExpression(p1, EarthSqlType), ArgumentExpression(p2, EarthSqlType))
}

/**
 * Returns a box suitable for an indexed search using the cube @> operator for points within a given great circle
 * distance of a location. Some points in this box are further than the specified great circle distance from the
 * location, so a second check using earth_distance should be included in the query.
 *
 * Function from earthdistance extension, `earth_box(point, radius)` in SQL.
 */
public fun earthBox(point: ColumnDeclaring<Earth>, radius: ColumnDeclaring<Double>): FunctionExpression<Cube> {
    // earth_box(point, radius)
    return FunctionExpression(
        functionName = "earth_box",
        arguments = listOf(point.asExpression(), radius.asExpression()),
        sqlType = CubeSqlType
    )
}

/**
 * Returns a box suitable for an indexed search using the cube @> operator for points within a given great circle
 * distance of a location. Some points in this box are further than the specified great circle distance from the
 * location, so a second check using earth_distance should be included in the query.
 *
 * Function from earthdistance extension, `earth_box(point, radius)` in SQL.
 */
public fun earthBox(point: ColumnDeclaring<Earth>, radius: Double): FunctionExpression<Cube> {
    return earthBox(point, ArgumentExpression(radius, DoubleSqlType))
}

/**
 * Returns a box suitable for an indexed search using the cube @> operator for points within a given great circle
 * distance of a location. Some points in this box are further than the specified great circle distance from the
 * location, so a second check using earth_distance should be included in the query.
 *
 * Function from earthdistance extension, `earth_box(point, radius)` in SQL.
 */
public fun earthBox(point: Earth, radius: ColumnDeclaring<Double>): FunctionExpression<Cube> {
    return earthBox(ArgumentExpression(point, EarthSqlType), radius)
}

/**
 * Returns a box suitable for an indexed search using the cube @> operator for points within a given great circle
 * distance of a location. Some points in this box are further than the specified great circle distance from the
 * location, so a second check using earth_distance should be included in the query.
 *
 * Function from earthdistance extension, `earth_box(point, radius)` in SQL.
 */
public fun earthBox(point: Earth, radius: Double): FunctionExpression<Cube> {
    return earthBox(ArgumentExpression(point, EarthSqlType), ArgumentExpression(radius, DoubleSqlType))
}
