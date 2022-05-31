package org.ktorm.support.postgresql

import org.junit.Assert.*
import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.entity.Entity
import org.ktorm.entity.add
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table

/**
 * Created by Kacper on 14/01/2022
 */
class EarthdistanceTest : BasePostgreSqlTest() {

    object TestTable : Table<TestRecord>("t_earthdistance") {
        val e = earth("earth_field").bindTo { it.e }
        val c = cube("cube_field").bindTo { it.c }
    }

    interface TestRecord : Entity<TestRecord> {
        companion object : Entity.Factory<TestRecord>()
        val e: Earth
        var c: Cube
    }

    @Test
    fun testEarthType() {
        val earthValue = Earth(-849959.4557142439, -5441944.3654614175, 3216183.683045588)
        val inserted = database.insert(TestTable) {
            set(it.e, earthValue)
        }

        val queried = database
            .from(TestTable)
            .select()
            .where(TestTable.e eq earthValue)
            .map { it[TestTable.e] }
            .firstOrNull()

        assertEquals(1, inserted)
        assertEquals(earthValue, queried)
    }

    @Test
    fun testCubeType() {
        assertThrows(IllegalArgumentException::class.java) {
            Cube(doubleArrayOf(1.0), doubleArrayOf(1.0, 2.0))
        }

        val cubeValue = Cube(doubleArrayOf(-1.1, 2.2, 3.0), doubleArrayOf(1.1, -2.2, 0.3))
        val inserted = database.insert(TestTable) {
            set(it.c, cubeValue)
        }

        val queried = database
            .from(TestTable)
            .select()
            .where(TestTable.c eq cubeValue)
            .map { it[TestTable.c] }
            .firstOrNull()

        assertEquals(1, inserted)
        assertEquals(cubeValue, queried)
    }

    @Test
    fun testCubeExpression() {
        val cube1 = Cube(doubleArrayOf(1.0, 1.0, 1.0), doubleArrayOf(-1.0, -1.0, -1.0))
        val cube2 = Cube(doubleArrayOf(0.0, 0.0, 0.0), doubleArrayOf(2.0, 2.0, 2.0))
        val cube3 = Cube(doubleArrayOf(0.5, 0.5, 0.5), doubleArrayOf(-0.5, -0.5, -0.5))
        val record = TestRecord()
        record.c = cube1

        database.sequenceOf(TestTable).add(record)

        val t1 = TestTable.c.contains(cube3).aliased("t1") // true
        val t2 = TestTable.c.containedIn(cube2).aliased("t2") // false
        val t3 = TestTable.c.overlaps(cube2).aliased("t3") // true
        val t4 = TestTable.c.eq(cube1).aliased("t4") // true
        database
            .from(TestTable)
            .select(t1, t2, t3, t4)
            .where(TestTable.c eq cube1)
            .map { row ->
                assertEquals(true, row[t1])
                assertEquals(false, row[t2])
                assertEquals(true, row[t3])
                assertEquals(true, row[t4])
                return
            }
        assertTrue(false) // Throws if above statement didn't return anything
    }

    @Test
    fun testEarthDistanceFunctions() {
        val distance = earthDistance(llToEarth(0.0, 0.0), llToEarth(1.0, 0.0)).aliased("distance")

        val record = TestRecord()
        record.c = Cube(doubleArrayOf(0.0), doubleArrayOf(0.0))
        database.sequenceOf(TestTable).add(record)

        database
            .from(TestTable)
            .select(distance)
            .map { row ->
                assertTrue(row[distance]!! > 100000)
            }

        val box = earthBox(llToEarth(0.0, 0.0), 10000.0)
        val pointInBox = llToEarth(0.01, 0.01)
        val pointOutsideBox = llToEarth(10.0, 10.0)
        val check1 = box.contains(pointInBox).aliased("c1")
        val check1r = pointInBox.containedIn(box).aliased("c1r")
        val check2 = box.contains(pointOutsideBox).aliased("c2")
        val check2r = pointOutsideBox.containedIn(box).aliased("c2r")
        database
            .from(TestTable)
            .select(check1, check2, check1r, check2r)
            .map { row ->
                assertTrue(row[check1]!!)
                assertTrue(row[check1r]!!)
                assertFalse(row[check2]!!)
                assertFalse(row[check2r]!!)
                return
            }
        assertTrue(false)
    }

    @Test
    fun testLatLng() {
        database.insert(TestTable) {
            set(it.e, llToEarth(22.5, 113.0))
        }

        val results = database
            .from(TestTable)
            .select(latitude(TestTable.e), longitude(TestTable.e))
            .map { row ->
                Pair(row.getDouble(1), row.getDouble(2))
            }

        assert(results.size == 1)
        assertEquals(results[0].first, 22.5, 0.1)
        assertEquals(results[0].second, 113.0, 0.1)
    }
}