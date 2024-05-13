package org.ktorm.support.postgresql

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.entity.tupleOf
import org.ktorm.schema.Table
import org.ktorm.schema.int

/**
 * Created by vince at Sep 09, 2023.
 */
class ArraysTest : BasePostgreSqlTest() {

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
    fun testArrays() {
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
            .select(Arrays.columns)
            .where(Arrays.id eq 1)
            .map { row ->
                tupleOf(
                    row[Arrays.shorts],
                    row[Arrays.ints],
                    row[Arrays.longs],
                    row[Arrays.floats],
                    row[Arrays.doubles],
                    row[Arrays.booleans],
                    row[Arrays.texts]
                )
            }

        val (shorts, ints, longs, floats, doubles, booleans, texts) = results[0]
        assert(shorts.contentEquals(shortArrayOf(1, 2, 3, 4)))
        assert(ints.contentEquals(intArrayOf(1, 2, 3, 4)))
        assert(longs.contentEquals(longArrayOf(1, 2, 3, 4)))
        assert(floats.contentEquals(floatArrayOf(1.0F, 2.0F, 3.0F, 4.0F)))
        assert(doubles.contentEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0)))
        assert(booleans.contentEquals(booleanArrayOf(false, true)))
        assert(texts.contentEquals(arrayOf("1", "2", "3", "4")))
    }
}