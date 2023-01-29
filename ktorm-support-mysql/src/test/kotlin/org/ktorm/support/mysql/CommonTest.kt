package org.ktorm.support.mysql

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.ktorm.database.use
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.jackson.json
import org.ktorm.schema.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals

/**
 * Created by vince on Dec 12, 2018.
 */
class CommonTest : BaseMySqlTest() {

    @Test
    fun testKeywordWrapping() {
        val configs = object : Table<Nothing>("t_config") {
            val key = varchar("key").primaryKey()
            val value = varchar("value")
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table t_config(`key` varchar(128) primary key, `value` varchar(128))"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(configs) {
            set(it.key, "test")
            set(it.value, "test value")
        }

        assert(database.from(configs).select(count()).where(configs.key eq "test").map { it.getInt(1) }[0] == 1)

        database.delete(configs) { it.key eq "test" }
    }

    @Test
    fun testLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(0, 2)
        assert(query.totalRecordsInAllPages == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids.size == 2)
        assert(ids[0] == 4)
        assert(ids[1] == 3)
    }

    /**
     * Verifies that invalid pagination parameters are ignored.
     */
    @Test
    fun testBothLimitAndOffsetAreNotPositive() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(0, -1)
        assert(query.totalRecordsInAllPages == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(4, 3, 2, 1))
    }

    /**
     * Verifies that limit parameter works as expected.
     */
    @Test
    fun testLimitWithoutOffset() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(2)
        assert(query.totalRecordsInAllPages == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(4, 3))
    }

    /**
     * Verifies that offset parameter works as expected.
     */
    @Test
    fun testOffsetWithoutLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).offset(2)
        assert(query.totalRecordsInAllPages == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(2, 1))
    }

    /**
     * Verifies that limit and offset parameters work together as expected.
     */
    @Test
    fun testOffsetWithLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).offset(2).limit(1)
        assert(query.totalRecordsInAllPages == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(2))
    }

    @Test
    fun testInsertOrUpdate() {
        database.insertOrUpdate(Employees) {
            set(it.id, 1)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onDuplicateKey {
                set(it.salary, it.salary + 1000)
            }
        }
        database.insertOrUpdate(Employees.aliased("t")) {
            set(it.id, 5)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onDuplicateKey {
                set(it.salary, it.salary + 1000)
            }
        }

        assert(database.employees.find { it.id eq 1 }!!.salary == 1100L)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1000L)
    }

    @Test
    fun testBulkInsert() {
        database.bulkInsert(Employees) {
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 1)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 1)
            }
        }

        assert(database.employees.count() == 6)
    }

    @Test
    fun testBulkInsertOrUpdate() {
        database.bulkInsertOrUpdate(Employees) {
            item {
                set(it.id, 1)
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.id, 5)
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            onDuplicateKey {
                set(it.job, it.job)
                set(it.departmentId, values(it.departmentId))
                set(it.salary, it.salary + 1000)
            }
        }

        database.employees.find { it.id eq 1 }!!.let {
            assert(it.job == "engineer")
            assert(it.department.id == 2)
            assert(it.salary == 1100L)
        }

        database.employees.find { it.id eq 5 }!!.let {
            assert(it.job == "engineer")
            assert(it.department.id == 2)
            assert(it.salary == 1000L)
        }
    }

    @Test
    fun testNaturalJoin() {
        val query = database.from(Employees).naturalJoin(Departments).select()
        assert(query.rowSet.size() == 0)
    }

    @Test
    fun testPagingSql() {
        var query = database
            .from(Employees)
            .leftJoin(Departments, on = Employees.departmentId eq Departments.id)
            .select()
            .orderBy(Departments.id.desc())
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 4)

        query = database
            .from(Employees)
            .select(Employees.name)
            .orderBy((Employees.id + 1).desc())
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 4)

        query = database
            .from(Employees)
            .select(Employees.departmentId, avg(Employees.salary))
            .groupBy(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 2)

        query = database
            .from(Employees)
            .selectDistinct(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 2)

        query = database
            .from(Employees)
            .select(max(Employees.salary))
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 1)

        query = database
            .from(Employees)
            .select(Employees.name)
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 4)
    }

    @Test
    fun testDrop() {
        val employees = database.employees.drop(1).drop(1).drop(1).toList()
        assert(employees.size == 1)
        assert(employees[0].name == "penny")
    }

    @Test
    fun testTake() {
        val employees = database.employees.take(2).take(1).toList()
        assert(employees.size == 1)
        assert(employees[0].name == "vince")
    }

    @Test
    fun testElementAt() {
        val employee = database.employees.drop(2).elementAt(1)

        assert(employee.name == "penny")
        assert(database.employees.elementAtOrNull(4) == null)
    }

    @Test
    fun testMapColumns3() {
        database.employees
            .filter { it.departmentId eq 1 }
            .mapColumns { tupleOf(it.id, it.name, dateDiff(LocalDate.now(), it.hireDate)) }
            .forEach { (id, name, days) ->
                println("$id:$name:$days")
            }
    }

    @Test
    fun testMatchAgainst() {
        val employees = database.employees.filterTo(ArrayList()) {
            match(it.name, it.job).against("vince", SearchModifier.IN_NATURAL_LANGUAGE_MODE)
        }

        employees.forEach { println(it) }
    }

    @Test
    fun testReplace() {
        val names = database.employees.mapColumns { it.name.replace("vince", "VINCE") }
        println(names)
    }

    @Test
    fun testInsertAndGenerateKey() {
        val id = database.insertAndGenerateKey(Employees) {
            set(it.name, "Joe Friend")
            set(it.job, "Tester")
            set(it.managerId, null)
            set(it.salary, 50)
            set(it.hireDate, LocalDate.of(2020, 1, 10))
            set(it.departmentId, 1)
        } as Int

        assert(id > 4)

        assert(database.employees.count() == 5)
    }

    @Test
    fun testToUpperCase() {
        val name = database.employees
            .filter { it.id eq 1 }
            .mapColumns { it.name.toUpperCase() }
            .first()

        assert(name == "VINCE")
    }

    @Test
    fun testIf() {
        val countRich = database
            .from(Employees)
            .select(sum(IF(Employees.salary gte 100L, 1, 0)))
            .map { row -> row.getInt(1) }

        assert(countRich.size == 1)
        assert(countRich.first() == 3)
    }

    @Test
    fun testSum() {
        val countRich = database
            .from(Employees)
            .select(sum(Employees.salary.toDouble()))
            .map { row -> row.getObject(1) }

        assert(countRich.size == 1)
        assert(countRich.first() == 450.0)
    }

    @Test
    fun testJson() {
        val t = object : Table<Nothing>("t_json") {
            val obj = json<Employee>("obj")
            val arr = json<List<Int>>("arr")
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table t_json (obj text, arr text)"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(t) {
            set(it.obj, Employee { name = "vince"; salary = 100 })
            set(it.arr, listOf(1, 2, 3))
        }

        database.insert(t) {
            set(it.obj, null)
            set(it.arr, null)
        }

        database
            .from(t)
            .select(t.obj, t.arr)
            .forEach { row ->
                println("${row.getString(1)}:${row.getString(2)}")
            }

        database
            .from(t)
            .select(t.obj.jsonExtract<Long>("$.salary"), t.arr.jsonContains(0))
            .forEach { row ->
                println("${row.getLong(1)}:${row.getBoolean(2)}")
            }
    }

    @Test
    fun testSelectForUpdate() {
        database.useTransaction {
            val emp = Employees.aliased("emp")

            val employee = database
                .sequenceOf(emp, withReferences = false)
                .filter { it.id eq 1 }
                .locking(LockingMode.FOR_UPDATE, tables = listOf(emp), wait = LockingWait.SKIP_LOCKED)
                .first()

            val future = Executors.newSingleThreadExecutor().submit {
                employee.name = "vince"
                employee.flushChanges()
            }

            try {
                future.get(5, TimeUnit.SECONDS)
                throw AssertionError()
            } catch (e: ExecutionException) {
                // Expected, the record is locked.
                e.printStackTrace()
            } catch (e: TimeoutException) {
                // Expected, the record is locked.
                e.printStackTrace()
            }
        }
    }

    @Test
    fun testSchema() {
        val t = object : Table<Department>("t_department", schema = databaseName) {
            val id = int("id").primaryKey().bindTo { it.id }
            val name = varchar("name").bindTo { it.name }
        }

        database.update(t) {
            set(it.name, "test")
            where {
                it.id eq 1
            }
        }

        assert(database.sequenceOf(t).filter { it.id eq 1 }.mapTo(HashSet()) { it.name } == setOf("test"))
        assert(database.sequenceOf(t.aliased("t")).mapTo(HashSet()) { it.name } == setOf("test", "finance"))
    }

    @Test
    fun testMaxColumnNameLength() {
        val t = object : Table<Nothing>("t_long_name") {
            val col = varchar("a".repeat(database.maxColumnNameLength))
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table t_long_name(${t.col.name} varchar(128))"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(t) {
            set(it.col, "test")
        }

        try {
            val name = database.from(t).select(t.col).map { it[t.col] }.first()
            println(name)
            throw java.lang.AssertionError("unexpected.")
        } catch (e: IllegalStateException) {
            println(e.message)
            assert("too long" in e.message!!)
        }
    }

    @Test
    fun testDateTime() {
        val t = object : Table<Nothing>("t_test_datetime") {
            val id = int("id").primaryKey()
            val d = datetime("d")
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table t_test_datetime(id int not null primary key auto_increment, d datetime not null)"""
                statement.executeUpdate(sql)
            }
        }

        val now = LocalDateTime.now().withNano(0)

        database.insert(t) {
            set(it.d, now)
        }

        val d = database.from(t).select(t.d).map { it[t.d] }[0]
        println(d)
        assert(d == now)
    }

    enum class Mood {
        HAPPY,
        SAD
    }

    object TableWithEnum : Table<Nothing>("t_enum") {
        val id = int("id").primaryKey()
        val current_mood = enum<Mood>("current_mood")
    }

    @Test
    fun testEnum() {
        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table t_enum(id int not null primary key auto_increment, current_mood text)"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(TableWithEnum) {
            set(it.current_mood, Mood.SAD)
        }

        val count = database
            .from(TableWithEnum)
            .select(count())
            .where(TableWithEnum.current_mood eq Mood.SAD)
            .map { it.getInt(1) }
            .first()
        assertThat(count, equalTo(1))

        val mood = database
            .from(TableWithEnum)
            .select(TableWithEnum.current_mood)
            .where(TableWithEnum.id eq 1)
            .map { it[TableWithEnum.current_mood] }
            .first()
        assertThat(mood, equalTo(Mood.SAD))

        database.insert(TableWithEnum) {
            set(it.current_mood, null)
        }

        val mood1 = database
            .from(TableWithEnum)
            .select(TableWithEnum.current_mood)
            .where(TableWithEnum.id eq 2)
            .map { it[TableWithEnum.current_mood] }
            .first()
        assertThat(mood1, equalTo(null))
    }

    interface TestMultiGeneratedKey : Entity<TestMultiGeneratedKey> {
        var id: Int
        var k: String
        var v: String
    }

    object TestMultiGeneratedKeys : Table<TestMultiGeneratedKey>("t_multi_generated_key") {
        val id = int("id").primaryKey().bindTo { it.id }
        val k = varchar("k").bindTo { it.k }
        val v = varchar("v").bindTo { it.v }
    }

    @Test
    fun testMultiGeneratedKey() {
        val e = Entity.create<TestMultiGeneratedKey>()
        e.v = "test~~"
        database.sequenceOf(TestMultiGeneratedKeys).add(e)

        val e1 = database.sequenceOf(TestMultiGeneratedKeys).first()
        println(e1)
        assertEquals(1, e1.id)
        assertEquals("test~~", e1.v)
        assert(e1.k.isNotEmpty())
    }
}
