package org.ktorm.support.postgresql

import org.junit.Test
import org.ktorm.dsl.eq
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.dsl.plus
import org.ktorm.dsl.update
import org.ktorm.entity.count
import org.ktorm.entity.find
import java.time.LocalDate

class DmlTest : BasePostgreSqlTest() {

    @Test
    fun testUpdate() {
        database.update(Employees) {
            set(it.job, "engineer")
            set(it.managerId, null)
            set(it.salary, 100)

            where {
                it.id eq 2
            }
        }

        val employee = database.employees.find { it.id eq 2 } ?: throw AssertionError()
        assert(employee.name == "marry")
        assert(employee.job == "engineer")
        assert(employee.manager == null)
        assert(employee.salary == 100L)
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
            onConflict {
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
            onConflict(it.id) {
                set(it.salary, it.salary + 1000)
            }
        }

        assert(database.employees.find { it.id eq 1 }!!.salary == 1100L)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1000L)
    }

    @Test
    fun testInsertOrUpdateReturning() {
        database.insertOrUpdateReturning(Employees, Employees.id) {
            set(it.name, "pedro")
            set(it.job, "engineer")
            set(it.salary, 1500)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict {
                set(it.salary, it.salary + 900)
            }
        }.let { id ->
            assert(id == 5)
        }

        database.insertOrUpdateReturning(Employees, Pair(Employees.id, Employees.job)) {
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict {
                set(it.salary, it.salary + 900)
            }
        }.let { (id, job) ->
            assert(id == 6)
            assert(job == "engineer")
        }

        val t = Employees.aliased("t")
        database.insertOrUpdateReturning(t, Triple(t.id, t.job, t.salary)) {
            set(it.id, 6)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict(it.id) {
                set(it.salary, it.salary + 900)
            }
        }.let { (id, job, salary) ->
            assert(id == 6)
            assert(job == "engineer")
            assert(salary == 1900L)
        }

        database.insertOrUpdateReturning(t, Triple(t.id, t.job, t.salary)) {
            set(it.id, 6)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict(it.id) {
                doNothing()
            }
        }.let { (id, job, salary) ->
            assert(id == null)
            assert(job == null)
            assert(salary == null)
        }
    }

    @Test
    fun testInsertReturning() {
        database.insertReturning(Employees, Employees.id) {
            set(it.name, "pedro")
            set(it.job, "engineer")
            set(it.salary, 1500)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
        }.let { id ->
            assert(id == 5)
        }

        database.insertReturning(Employees, Pair(Employees.id, Employees.job)) {
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
        }.let { (id, job) ->
            assert(id == 6)
            assert(job == "engineer")
        }

        val t = Employees.aliased("t")
        database.insertReturning(t, Triple(t.id, t.job, t.salary)) {
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
        }.let { (id, job, salary) ->
            assert(id == 7)
            assert(job == "engineer")
            assert(salary == 1000L)
        }
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
    fun testBulkInsertReturning() {
        database.bulkInsertReturning(Employees, Employees.id) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(5, 6))
        }

        database.bulkInsertReturning(Employees, Pair(Employees.id, Employees.job)) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(Pair(7, "trainee"), Pair(8, "engineer")))
        }

        val t = Employees.aliased("t")
        database.bulkInsertReturning(t, Triple(t.id, t.job, t.salary)) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(Triple(9, "trainee", 1000L), Triple(10, "engineer", 1000L)))
        }
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
            onConflict(it.id) {
                set(it.job, it.job)
                set(it.departmentId, excluded(it.departmentId))
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
    fun testBulkInsertOrUpdate1() {
        val bulkInsertWithUpdate = { ignoreErrors: Boolean ->
            database.bulkInsertOrUpdate(Employees) {
                item {
                    set(it.id, 5)
                    set(it.name, "vince")
                    set(it.job, "engineer")
                    set(it.salary, 1000)
                    set(it.hireDate, LocalDate.now())
                    set(it.departmentId, 1)
                }
                item {
                    set(it.id, 6)
                    set(it.name, "vince")
                    set(it.job, "engineer")
                    set(it.salary, 1000)
                    set(it.hireDate, LocalDate.now())
                    set(it.departmentId, 1)
                }
                onConflict {
                    if (ignoreErrors) doNothing() else set(it.salary, it.salary + 900)
                }
            }
        }

        bulkInsertWithUpdate(false)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1000L)
        assert(database.employees.find { it.id eq 6 }!!.salary == 1000L)

        bulkInsertWithUpdate(false)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1900L)
        assert(database.employees.find { it.id eq 6 }!!.salary == 1900L)

        bulkInsertWithUpdate(true)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1900L)
        assert(database.employees.find { it.id eq 6 }!!.salary == 1900L)
    }

    @Test
    fun testBulkInsertOrUpdateReturning() {
        database.bulkInsertOrUpdateReturning(Employees, Employees.id) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            onConflict {
                doNothing()
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(5, 6))
        }

        database.bulkInsertOrUpdateReturning(Employees, Pair(Employees.id, Employees.job)) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            onConflict {
                doNothing()
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(Pair(7, "trainee"), Pair(8, "engineer")))
        }

        val t = Employees.aliased("t")
        database.bulkInsertOrUpdateReturning(t, Triple(t.id, t.job, t.salary)) {
            item {
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            onConflict {
                doNothing()
            }
        }.let { results ->
            assert(results.size == 2)
            assert(results == listOf(Triple(9, "trainee", 1000L), Triple(10, "engineer", 1000L)))
        }

        database.bulkInsertOrUpdateReturning(t, Triple(t.id, t.job, t.salary)) {
            item {
                set(it.id, 10)
                set(it.name, "vince")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            item {
                set(it.id, 11)
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 2)
            }
            onConflict {
                doNothing()
            }
        }.let { results ->
            assert(results.size == 1)
            assert(results == listOf(Triple(11, "engineer", 1000L)))
        }
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
}