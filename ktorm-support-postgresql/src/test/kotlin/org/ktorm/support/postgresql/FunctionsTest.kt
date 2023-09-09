package org.ktorm.support.postgresql

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.entity.tupleOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import kotlin.test.assertEquals

class FunctionsTest : BasePostgreSqlTest() {

    object Arrays : Table<Nothing>("t_array") {
        val id = int("id").primaryKey()
        val shorts = shortArray("shorts")
        val ints = intArray("ints")
        val longs = longArray("longs")
        val floats = floatArray("floats")
        val doubles = doubleArray("doubles")
        val booleans = booleanArray("booleans")
        val texts = textArray("texts")
    }

    @Test
    fun testArrayPosition() {
        database.insert(Arrays) {
            set(it.shorts, shortArrayOf(1, 2, 3, 4))
            set(it.ints, intArrayOf(1, 2, 3, 4))
            set(it.longs, longArrayOf(1, 2, 3, 4))
            set(it.floats, floatArrayOf(1.0F, 2.0F, 3.0F, 4.0F))
            set(it.doubles, doubleArrayOf(1.0, 2.0, 3.0, 4.0))
            set(it.booleans, booleanArrayOf(false, true))
            set(it.texts, arrayOf("1", "2", "3", "4"))
        }

        val results = database
            .from(Arrays)
            .select(
                arrayPosition(Arrays.shorts, 2),
                arrayPosition(Arrays.ints, 2, 1),
                arrayPosition(Arrays.longs, 2),
                arrayPosition(Arrays.booleans, true),
                arrayPosition(Arrays.texts, "2")
            )
            .where(Arrays.id eq 1)
            .map { row ->
                tupleOf(row.getInt(1), row.getInt(2), row.getInt(3), row.getInt(4), row.getInt(5))
            }

        println(results)
        assert(results.size == 1)
        assert(results[0] == tupleOf(1, 1, 1, 1, 2)) // text[] is one-based, others are zero-based.
    }

    @Test
    fun testArrayPositionTextArray() {
        val namesSorted = database
            .from(Employees)
            .select()
            .orderBy(arrayPosition(arrayOf("tom", "vince", "marry"), Employees.name).asc())
            .map { row ->
                row[Employees.name]
            }

        assertEquals(listOf("tom", "vince", "marry", "penny"), namesSorted)
    }
}
