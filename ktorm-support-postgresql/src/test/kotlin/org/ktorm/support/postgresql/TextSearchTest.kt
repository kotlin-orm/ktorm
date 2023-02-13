package org.ktorm.support.postgresql

import org.junit.Test
import org.ktorm.database.use
import org.ktorm.dsl.*
import org.ktorm.entity.Entity
import org.ktorm.schema.*
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TextSearchTest : BasePostgreSqlTest() {

    object TestTable : Table<TestRecord>("t_textsearch") {
        val id = int("id").primaryKey().bindTo { it.id }
        val originalText = text("original_text").bindTo { it.originalText }
        val textsearch = tsvector("textsearch").bindTo { it.textsearch }
    }

    interface TestRecord : Entity<TestRecord> {
        companion object : Entity.Factory<TestRecord>()

        val id: Int
        val originalText: String
        val textsearch: TSVector
    }

    object TestQueryTable : Table<TestQueryRecord>("t_textsearch_query") {
        val id = int("id").primaryKey().bindTo { it.id }
        val query = tsquery("query").bindTo { it.query }
    }

    interface TestQueryRecord : Entity<TestQueryRecord> {
        companion object : Entity.Factory<TestQueryRecord>()

        val id: Int
        val query: String
    }

    private fun insertText(originalText: String, textSearch: ColumnDeclaring<TSVector>? = null): Int =
        database.insertReturning(TestTable, TestTable.id){
            set(TestTable.originalText, originalText)
            set(TestTable.textsearch, textSearch ?: toTSVector("simple", originalText))
        }!!

    // Test examples coming from https://www.postgresql.org/docs/current/datatype-textsearch.html#DATATYPE-TSVECTOR
    @Test
    fun testTSVectorWordsOnly() {
        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                statement.execute("select 'a fat cat sat on a mat and ate a fat rat'::tsvector")
                val resultSet = statement.resultSet
                resultSet.next()
                val tsVector = TSVectorSqlType.getResult(resultSet, 1)
                assertEquals(
                    listOf(
                        TSVectorLexeme("a"),
                        TSVectorLexeme("and"),
                        TSVectorLexeme("ate"),
                        TSVectorLexeme("cat"),
                        TSVectorLexeme("fat"),
                        TSVectorLexeme("mat"),
                        TSVectorLexeme("on"),
                        TSVectorLexeme("rat"),
                        TSVectorLexeme("sat")
                    ),
                    tsVector
                )
            }
        }
    }

    @Test
    fun testTSVectorSpaceAndQuotes() {
        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                statement.execute("select \$\$the lexeme '    ' & 'Joe''s' contains spaces and quotes\$\$::tsvector")
                val resultSet = statement.resultSet
                resultSet.next()
                val tsVector = TSVectorSqlType.getResult(resultSet, 1)
                assertEquals(
                    listOf(
                        TSVectorLexeme("    "),
                        TSVectorLexeme("&"),
                        TSVectorLexeme("Joe's"),
                        TSVectorLexeme("and"),
                        TSVectorLexeme("contains"),
                        TSVectorLexeme("lexeme"),
                        TSVectorLexeme("quotes"),
                        TSVectorLexeme("spaces"),
                        TSVectorLexeme("the"),
                    ),
                    tsVector
                )
            }
        }
    }

    @Test
    fun testTSVectorWithPositionsAndWeights() {
        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                statement.execute("select 'a:1A fat:2B,4C cat:5D'::tsvector")
                val resultSet = statement.resultSet
                resultSet.next()
                val tsVector = TSVectorSqlType.getResult(resultSet, 1)
                assertEquals(
                    listOf(
                        TSVectorLexeme("a", listOf(TSVectorLexemePosition(1, 'A'))),
                        TSVectorLexeme("cat", listOf(TSVectorLexemePosition(5))),
                        TSVectorLexeme("fat", listOf(TSVectorLexemePosition(2, 'B'), TSVectorLexemePosition(4, 'C')))
                    ),
                    tsVector
                )
            }
        }
    }

    @Test
    fun testTSVectorWithPositionsOnly() {
        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                statement.execute("select 'a:1 fat:2 cat:3 sat:4 on:5 a:6 mat:7 and:8 ate:9 a:10 fat:11 rat:12'::tsvector")
                val resultSet = statement.resultSet
                resultSet.next()
                val tsVector = TSVectorSqlType.getResult(resultSet, 1)
                assertEquals(
                    listOf(
                        TSVectorLexeme("a", listOf(TSVectorLexemePosition(1), TSVectorLexemePosition(6), TSVectorLexemePosition(10))),
                        TSVectorLexeme("and", listOf(TSVectorLexemePosition(8))),
                        TSVectorLexeme("ate", listOf(TSVectorLexemePosition(9))),
                        TSVectorLexeme("cat", listOf(TSVectorLexemePosition(3))),
                        TSVectorLexeme("fat", listOf(TSVectorLexemePosition(2), TSVectorLexemePosition(11))),
                        TSVectorLexeme("mat", listOf(TSVectorLexemePosition(7))),
                        TSVectorLexeme("on", listOf(TSVectorLexemePosition(5))),
                        TSVectorLexeme("rat", listOf(TSVectorLexemePosition(12))),
                        TSVectorLexeme("sat", listOf(TSVectorLexemePosition(4)))
                    ),
                    tsVector
                )
            }
        }
    }

    @Test
    fun testInsertTSVector() {
        val tsVector = listOf(
            TSVectorLexeme("a", listOf(TSVectorLexemePosition(1, 'A'))),
            TSVectorLexeme("cat", listOf(TSVectorLexemePosition(5))),
            TSVectorLexeme("fat", listOf(TSVectorLexemePosition(2, 'B'), TSVectorLexemePosition(4, 'C'))),
            TSVectorLexeme("sits", listOf(TSVectorLexemePosition(3), TSVectorLexemePosition(6)))
        )
        val id = database.insertReturning(TestTable, TestTable.id){
            set(TestTable.textsearch, tsVector)
        }!!
        val queried = database.from(TestTable).select().where { TestTable.id.eq(id) }.map { row ->
            row[TestTable.textsearch]
        }.first()
        assertEquals(tsVector, queried)
    }

    @Test
    fun testInsertTSVectorWordsOnly() {
        val tsVector = listOf(
            TSVectorLexeme("Joe's"),
            TSVectorLexeme("cat"),
            TSVectorLexeme("fat")
        )
        val id = database.insertReturning(TestTable, TestTable.id){
            set(TestTable.textsearch, tsVector)
        }!!
        val queried = database.from(TestTable).select().where { TestTable.id.eq(id) }.map { row ->
            row[TestTable.textsearch]
        }.first()
        assertEquals(tsVector, queried)
    }

    @Test
    fun testToTSVector() {
        val text = "Joe's contains a single quotation mark"
        val id = insertText(text, toTSVector("english", text))
        val expected = listOf(
            TSVectorLexeme("contain", listOf(TSVectorLexemePosition(3))),
            TSVectorLexeme("joe", listOf(TSVectorLexemePosition(1))),
            TSVectorLexeme("mark", listOf(TSVectorLexemePosition(7))),
            TSVectorLexeme("quotat", listOf(TSVectorLexemePosition(6))),
            TSVectorLexeme("singl", listOf(TSVectorLexemePosition(5)))
        )
        val queried = database.from(TestTable).select().where { TestTable.id.eq(id) }.map { row ->
            row[TestTable.textsearch]
        }.first()
        assertEquals(expected, queried)
    }

    @Test
    fun testTSVectorMatchTSQuery() {
        val text = "Hello tsVectorMatchTSQuery test"
        val id = insertText(text)
        val queried = database.from(TestTable).select().where { TestTable.textsearch match toTSQuery("simple", "tsVectorMatchTSQuery") }.map { row ->
            row[TestTable.id]
        }.toList()
        assertContains(queried, id)
    }

    @Test
    fun testTSQueryMatchTSVector() {
        val text = "Hello testTSQueryMatchTSVector test"
        val id = insertText(text)
        val queried = database.from(TestTable).select().where { toTSQuery("simple", "testTSQueryMatchTSVector") match TestTable.textsearch }.map { row ->
            row[TestTable.id]
        }.toList()
        assertContains(queried, id)
    }

    @Test
    fun testTextMatchTSQuery() {
        val text = "Hello testTSQueryMatchTSVector test"
        val id = insertText(text)
        val queried = database.from(TestTable).select().where { TestTable.originalText match toTSQuery("english", "testTSQueryMatchTSVector") }.map { row ->
            row[TestTable.id]
        }.toList()
        assertContains(queried, id)
    }

    @Test
    fun testTSVectorConcatTSVector() {
        val text1 = "Joe's contains a single quotation mark"
        val text2 = ", for some reason"
        val id = insertText(text1 + text2, toTSVector("simple", text1) concat toTSVector("simple", text2))
        val expected = listOf(
            TSVectorLexeme("a", listOf(TSVectorLexemePosition(4))),
            TSVectorLexeme("contains", listOf(TSVectorLexemePosition(3))),
            TSVectorLexeme("for", listOf(TSVectorLexemePosition(8))),
            TSVectorLexeme("joe", listOf(TSVectorLexemePosition(1))),
            TSVectorLexeme("mark", listOf(TSVectorLexemePosition(7))),
            TSVectorLexeme("quotation", listOf(TSVectorLexemePosition(6))),
            TSVectorLexeme("reason", listOf(TSVectorLexemePosition(10))),
            TSVectorLexeme("s", listOf(TSVectorLexemePosition(2))),
            TSVectorLexeme("single", listOf(TSVectorLexemePosition(5))),
            TSVectorLexeme("some", listOf(TSVectorLexemePosition(9)))
        )
        val queried = database.from(TestTable).select().where { TestTable.id.eq(id) }.map { row ->
            row[TestTable.textsearch]
        }.first()
        assertEquals(expected, queried)
    }

    @Test
    fun testTSQueryAndTSQuery() {
        val expectedText = "Hello expected text"
        val expectedId = insertText(expectedText)
        val notExpectedText = "Hello other text"
        val notExpectedId = insertText(notExpectedText)
        val queried = database.from(TestTable).select().where { TestTable.textsearch match (toTSQuery("simple", "text") and toTSQuery("simple", "expected")) }.map { row ->
            row[TestTable.id]
        }.toList()
        assertContains(queried, expectedId)
        assertFalse(queried.contains(notExpectedId))
    }

    @Test
    fun testTSQueryOrTSQuery() {
        val expectedText1 = "Hello text1"
        val expectedId1 = insertText(expectedText1)
        val expectedText2 = "Hello text2"
        val expectedId2 = insertText(expectedText2)
        val queried = database.from(TestTable).select().where { TestTable.textsearch match (toTSQuery("simple", "text1") or toTSQuery("simple", "text2")) }.map { row ->
            row[TestTable.id]
        }.toList()
        assertContains(queried, expectedId1)
        assertContains(queried, expectedId2)
    }

    @Test
    fun testNotTSQuery() {
        val expectedText = "Hello expected text"
        val expectedId = insertText(expectedText)
        val notExpectedText = "Hello not expected text"
        val notExpectedId = insertText(notExpectedText)
        val queried = database.from(TestTable).select().where { TestTable.textsearch match not(toTSQuery("simple", "not")) }.map { row ->
            row[TestTable.id]
        }.toList()
        assertContains(queried, expectedId)
        assertFalse(queried.contains(notExpectedId))
    }

    @Test
    fun testTSQueryFollowedByTSQuery() {
        val expectedText = "Hello expected text"
        val expectedId = insertText(expectedText)
        val notExpectedText = "Hello not expected text"
        val notExpectedId = insertText(notExpectedText)
        val queried = database.from(TestTable).select().where { TestTable.textsearch match (toTSQuery("simple", "hello") followedBy toTSQuery("simple", "expected")) }.map { row ->
            row[TestTable.id]
        }.toList()
        assertContains(queried, expectedId)
        assertFalse(queried.contains(notExpectedId))
    }

    @Test
    fun testTSQueryContainsTSQuery() {
        val id = database.insertReturning(TestQueryTable, TestQueryTable.id) {
            set(TestQueryTable.query, toTSQuery("simple", "test") and toTSQuery("simple", "query"))
        }
        val queried = database.from(TestQueryTable).select().where { TestQueryTable.query contains toTSQuery("simple", "test") }.map { row ->
            row[TestQueryTable.id]
        }.toList()
        assertContains(queried, id)
    }

    @Test
    fun testTSQueryContainedInTSQuery() {
        val id = database.insertReturning(TestQueryTable, TestQueryTable.id) {
            set(TestQueryTable.query, toTSQuery("simple", "test") and toTSQuery("simple", "query"))
        }
        val queried = database.from(TestQueryTable).select().where { toTSQuery("simple", "test") containedIn TestQueryTable.query }.map { row ->
            row[TestQueryTable.id]
        }.toList()
        assertContains(queried, id)
    }

    @Test
    fun testParentheses() {
        val text = "a, b"
        val id = insertText(text)
        // Query1: a & !b & c
        val queried1 = database.from(TestTable).select().where { TestTable.originalText match (toTSQuery("english", "a") and not(toTSQuery("english", "b")) and toTSQuery("english", "c")) }.map { row ->
            row[TestTable.id]
        }.toList()
        // Query2: a & !(b&c)
        val queried2 = database.from(TestTable).select().where { TestTable.originalText match (toTSQuery("english", "a") and not(toTSQuery("english", "b") and toTSQuery("english", "c"))) }.map { row ->
            row[TestTable.id]
        }.toList()
        assertFalse(queried1.contains(id))
        assertContains(queried2, id)
    }
}
