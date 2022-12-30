package org.ktorm.support.postgresql

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table
import org.ktorm.schema.int

class HStoreTest : BasePostgreSqlTest() {

    object Metadatas : Table<Nothing>("t_metadata") {
        val id = int("id").primaryKey()
        val attributes = hstore("attrs")
        val numbers = textArray("numbers")
    }

    @Test
    fun testHStore() {
        val attributes = get { it.attributes } ?: error("Cannot get the attributes!")
        MatcherAssert.assertThat(attributes.size, CoreMatchers.equalTo(3))
        MatcherAssert.assertThat(attributes["a"], CoreMatchers.equalTo("1"))
        MatcherAssert.assertThat(attributes["b"], CoreMatchers.equalTo("2"))
        MatcherAssert.assertThat(attributes["c"], CoreMatchers.nullValue())
    }

    @Test
    fun testHStoreIsNull() {
        database.update(Metadatas) {
            set(it.attributes, null)
            where { it.id eq 1 }
        }

        val attributes = get { it.attributes }
        MatcherAssert.assertThat(attributes, CoreMatchers.nullValue())
    }

    @Test
    fun testHStoreGetValue() {
        assert(get { it.attributes["a"] } == "1")
        assert(get { it.attributes["b"] } == "2")
        assert(get { it.attributes["c"] } == null)
    }

    private inline fun <T : Any> get(op: (Metadatas) -> ColumnDeclaring<T>): T? {
        val column = op(Metadatas)
        return database.from(Metadatas).select(column).map { row -> column.sqlType.getResult(row, 1) }.first()
    }

    @Test
    fun testHStoreGetValues() {
        val arrayOfAC: TextArray = arrayOf("a", "c")
        MatcherAssert.assertThat(get { it.attributes[arrayOfAC] }, CoreMatchers.equalTo(arrayOf("1", null)))

        val arrayOfBD: TextArray = arrayOf("b", "d")
        MatcherAssert.assertThat(get { it.attributes[arrayOfBD] }, CoreMatchers.equalTo(arrayOf("2", null)))
    }

    @Test
    fun testHStoreConcat() {
        database.update(Metadatas) {
            set(it.attributes, it.attributes + mapOf("d" to "4", "e" to null))
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        MatcherAssert.assertThat(updatedAttributes.size, CoreMatchers.equalTo(5))
        MatcherAssert.assertThat(updatedAttributes["a"], CoreMatchers.equalTo("1"))
        MatcherAssert.assertThat(updatedAttributes["b"], CoreMatchers.equalTo("2"))
        MatcherAssert.assertThat(updatedAttributes["c"], CoreMatchers.nullValue())
        MatcherAssert.assertThat(updatedAttributes["d"], CoreMatchers.equalTo("4"))
        MatcherAssert.assertThat(updatedAttributes["e"], CoreMatchers.nullValue())
    }

    @Test
    fun testHStoreContainsKey() {
        assert(get { it.attributes.containsKey("a") } == true)
        assert(get { it.attributes.containsKey("d") } == false)
    }

    @Test
    fun testHStoreContainsAll() {
        val arrayOfAC: TextArray = arrayOf("a", "c")
        assert(get { it.attributes.containsAll(arrayOfAC) } == true)

        val arrayOfBD: TextArray = arrayOf("b", "d")
        assert(get { it.attributes.containsAll(arrayOfBD) } == false)
    }

    @Test
    fun testHStoreContainsAny() {
        val arrayOfAC: TextArray = arrayOf("a", "c")
        assert(get { it.attributes.containsAny(arrayOfAC) } == true)

        val arrayOfBD: TextArray = arrayOf("b", "d")
        assert(get { it.attributes.containsAny(arrayOfBD) } == true)

        val arrayOfEF: TextArray = arrayOf("e", "f")
        assert(get { it.attributes.containsAny(arrayOfEF) } == false)
    }

    @Test
    fun testHStoreContains() {
        assert(get { it.attributes.contains(mapOf("a" to "1")) } == true)
        assert(get { it.attributes.contains(mapOf("a" to "1", "c" to null)) } == true)
        assert(get { it.attributes.contains(mapOf("a" to "1", "c" to "3")) } == false)
        assert(get { it.attributes.contains(mapOf("a" to "1", "d" to "4")) } == false)
    }

    @Test
    fun testHStoreContainedIn() {
        assert(get { it.attributes.containedIn(mapOf("a" to "1", "b" to "2", "c" to null)) } == true)
        assert(get { it.attributes.containedIn(mapOf("a" to "1", "b" to "2", "c" to null, "d" to "4")) } == true)
        assert(get { it.attributes.containedIn(mapOf("a" to "1")) } == false)
        assert(get { it.attributes.containedIn(mapOf("a" to "1", "b" to "2", "c" to "c")) } == false)
    }

    @Test
    fun testHStoreDeleteKey() {
        database.update(Metadatas) {
            set(it.attributes, it.attributes - "b")
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        MatcherAssert.assertThat(updatedAttributes, CoreMatchers.equalTo(mapOf("a" to "1", "c" to null)))
    }

    @Test
    fun testHStoreDeleteKeys() {
        database.update(Metadatas) {
            set(it.attributes, it.attributes - arrayOf("b", "c"))
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        MatcherAssert.assertThat(updatedAttributes, CoreMatchers.equalTo(mapOf<String, String?>("a" to "1")))
    }

    @Test
    fun testHStoreDeleteMatching() {
        database.update(Metadatas) {
            set(it.attributes, it.attributes - mapOf("a" to "1", "b" to "2", "c" to null))
            where { it.id eq 1 }
        }

        val updatedAttributes = get { it.attributes } ?: error("Cannot get the attributes!")
        MatcherAssert.assertThat(updatedAttributes, CoreMatchers.equalTo(emptyMap()))
    }

    @Test
    fun testTextArray() {
        database.update(Metadatas) {
            set(it.numbers, arrayOf("a", "b"))
            where { it.id eq 1 }
        }

        val numbers = get { it.numbers } ?: error("Cannot get the numbers!")
        MatcherAssert.assertThat(numbers, CoreMatchers.equalTo(arrayOf<String?>("a", "b")))
    }

    @Test
    fun testTextArrayIsNull() {
        database.update(Metadatas) {
            set(it.numbers, null)
            where { it.id eq 1 }
        }

        val numbers = get { it.numbers }
        MatcherAssert.assertThat(numbers, CoreMatchers.nullValue())
    }
}