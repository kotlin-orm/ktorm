package org.ktorm.support.postgresql

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.FunctionExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.DoubleSqlType
import org.ktorm.schema.SqlType

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
 * Expression class represents PostgreSQL's `Cube` operations.
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
public infix fun ColumnDeclaring<Cube>.contains(argument: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.CONTAINS, asExpression(), argument.asExpression(), BooleanSqlType)
}

/**
 * Cube contains operator, translated to the @> operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.contains(argument: Cube): CubeExpression<Boolean> {
    return this.contains(wrapArgument(argument))
}

/**
 * Cube contains operator, translated to the @> operator in PostgreSQL.
 */
@JvmName("containsEarth")
public infix fun ColumnDeclaring<Cube>.contains(argument: ColumnDeclaring<Earth>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.CONTAINS, asExpression(), argument.asExpression(), BooleanSqlType)
}

/**
 * Cube contains operator, translated to the @> operator in PostgreSQL.
 */
@JvmName("containsEarth")
public infix fun ColumnDeclaring<Cube>.contains(argument: Earth): CubeExpression<Boolean> {
    return this.contains(ArgumentExpression(argument, PGEarthType))
}

/**
 * Cube contained in operator, translated to the <@ operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.containedIn(argument: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.CONTAINED_IN, asExpression(), argument.asExpression(), BooleanSqlType)
}

/**
 * Cube contained in operator, translated to the <@ operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.containedIn(argument: Cube): CubeExpression<Boolean> {
    return this.containedIn(wrapArgument(argument))
}

/**
 * Cube contained in operator, translated to the <@ operator in PostgreSQL.
 */
@JvmName("earthContainedInCube")
public infix fun ColumnDeclaring<Earth>.containedIn(argument: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.CONTAINED_IN, asExpression(), argument.asExpression(), BooleanSqlType)
}

/**
 * Cube contained in operator, translated to the <@ operator in PostgreSQL.
 */
@JvmName("earthContainedInCube")
public infix fun ColumnDeclaring<Earth>.containedIn(argument: Cube): CubeExpression<Boolean> {
    return this.containedIn(ArgumentExpression(argument, PGCubeType))
}

/**
 * Cube overlap operator, translated to the && operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.overlaps(argument: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.OVERLAP, asExpression(), argument.asExpression(), BooleanSqlType)
}

/**
 * Cube overlap operator, translated to the && operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.overlaps(argument: Cube): CubeExpression<Boolean> {
    return this.overlaps(wrapArgument(argument))
}

/**
 * Returns the location of a point on the surface of the Earth
 * given its latitude (argument 1) and longitude (argument 2) in degrees.
 *
 * Function from earthdistance extension
 */
public fun llToEarth(
    lat: ColumnDeclaring<Double>,
    lng: ColumnDeclaring<Double>
): FunctionExpression<Earth> =
    FunctionExpression(
        functionName = "ll_to_earth",
        arguments = listOf(lat.asExpression(), lng.asExpression()),
        sqlType = PGEarthType
    )

/**
 * Returns the location of a point on the surface of the Earth
 * given its latitude (argument 1) and longitude (argument 2) in degrees.
 */
public fun llToEarth(
    lat: ColumnDeclaring<Double>,
    lng: Double
): FunctionExpression<Earth> =
    llToEarth(lat, ArgumentExpression(lng, DoubleSqlType))

/**
 * Returns the location of a point on the surface of the Earth
 * given its latitude (argument 1) and longitude (argument 2) in degrees.
 */
public fun llToEarth(
    lat: Double,
    lng: ColumnDeclaring<Double>
): FunctionExpression<Earth> =
    llToEarth(ArgumentExpression(lat, DoubleSqlType), lng)

/**
 * Returns the location of a point on the surface of the Earth
 * given its latitude (argument 1) and longitude (argument 2) in degrees.
 */
public fun llToEarth(
    lat: Double,
    lng: Double
): FunctionExpression<Earth> =
    llToEarth(ArgumentExpression(lat, DoubleSqlType), ArgumentExpression(lng, DoubleSqlType))

/**
 * Get distance between 2 points on earth.
 *
 * Function from earthdistance extension, `earth_distance(point1, point2)` in SQL.
 */
public fun earthDistance(
    point1: ColumnDeclaring<Earth>,
    point2: ColumnDeclaring<Earth>
): FunctionExpression<Double> =
    FunctionExpression(
        functionName = "earth_distance",
        arguments = listOf(point1.asExpression(), point2.asExpression()),
        sqlType = DoubleSqlType
    )

/**
 * Get distance between 2 points on earth.
**/
public fun earthDistance(
    point1: ColumnDeclaring<Earth>,
    point2: Earth
): FunctionExpression<Double> =
    earthDistance(point1, ArgumentExpression(point2, PGEarthType))

/**
 * Get distance between 2 points on earth.
 **/
public fun earthDistance(
    point1: Earth,
    point2: ColumnDeclaring<Earth>
): FunctionExpression<Double> =
    earthDistance(ArgumentExpression(point1, PGEarthType), point2)

/**
 * Get distance between 2 points on earth.
 **/
public fun earthDistance(
    point1: Earth,
    point2: Earth
): FunctionExpression<Double> =
    earthDistance(ArgumentExpression(point1, PGEarthType), ArgumentExpression(point2, PGEarthType))

/**
 * Creates a bounding cube, sized to contain all the points that are not farther than radius meters from a given point.
 *
 * Function from earthdistance extension, `earth_box(point, radius)` in SQL.
 */
public fun earthBox(
    point: ColumnDeclaring<Earth>,
    radius: ColumnDeclaring<Double>
): FunctionExpression<Cube> =
    FunctionExpression(
        functionName = "earth_box",
        arguments = listOf(point.asExpression(), radius.asExpression()),
        sqlType = PGCubeType
    )

/**
 * Creates a bounding cube, sized to contain all the points that are not farther than radius meters from a given point.
**/
public fun earthBox(
    point: ColumnDeclaring<Earth>,
    radius: Double
): FunctionExpression<Cube> =
    earthBox(point, ArgumentExpression(radius, DoubleSqlType))

/**
 * Creates a bounding cube, sized to contain all the points that are not farther than radius meters from a given point.
 **/
public fun earthBox(
    point: Earth,
    radius: ColumnDeclaring<Double>
): FunctionExpression<Cube> =
    earthBox(ArgumentExpression(point, PGEarthType), radius)

/**
 * Creates a bounding cube, sized to contain all the points that are not farther than radius meters from a given point.
 **/
public fun earthBox(
    point: Earth,
    radius: Double
): FunctionExpression<Cube> =
    earthBox(ArgumentExpression(point, PGEarthType), ArgumentExpression(radius, DoubleSqlType))
