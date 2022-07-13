package org.ktorm.support.sqlite

import org.junit.Test
import org.ktorm.dsl.eq
import org.ktorm.dsl.less
import org.ktorm.dsl.plus
import org.ktorm.entity.count
import org.ktorm.entity.find
import java.time.LocalDate

class DmlTest : BaseSQLiteTest() {

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
    fun testInsertOrUpdate1() {
        database.insertOrUpdate(Employees) {
            set(it.id, 1)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict {
                set(it.salary, excluded(it.salary))
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

        assert(database.employees.find { it.id eq 1 }!!.salary == 1000L)
        assert(database.employees.find { it.id eq 5 }!!.salary == 1000L)
    }

    @Test
    fun testInsertOrUpdateOnConflictWhere() {
        database.insertOrUpdate(Employees.aliased("t")) {
            set(it.id, 1)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict {
                set(it.salary, it.salary + excluded(it.salary))
                where {
                    it.salary less 1000
                }
            }
        }
        assert(database.employees.find { it.id eq 1 }!!.salary == 1100L)
        database.insertOrUpdate(Employees.aliased("t")) {
            set(it.id, 1)
            set(it.name, "vince")
            set(it.job, "engineer")
            set(it.salary, 1000)
            set(it.hireDate, LocalDate.now())
            set(it.departmentId, 1)
            onConflict(it.id) {
                set(it.salary, it.salary + excluded(it.salary))
                where {
                    it.salary less 1000
                }
            }
        }

        assert(database.employees.find { it.id eq 1 }!!.salary == 1100L)
    }

    @Test
    fun testBulkInsert() {
        database.bulkInsert(Employees.aliased("t")) {
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
        database.bulkInsertOrUpdate(Employees.aliased("t")) {
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
    fun testBulkInsertOrUpdateOnConflictWhere() {
        database.bulkInsertOrUpdate(Employees.aliased("t")) {
            item {
                set(it.id, 1)
                set(it.name, "vince")
                set(it.job, "engineer")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 1)
            }
            item {
                set(it.id, 2)
                set(it.name, "marry")
                set(it.job, "trainee")
                set(it.salary, 1000)
                set(it.hireDate, LocalDate.now())
                set(it.departmentId, 1)
            }
            onConflict(it.id) {
                set(it.salary, it.salary + 1000)
                where {
                    it.job eq "engineer"
                }
            }
        }

        assert(database.employees.find { it.id eq 1 }!!.salary == 1100L)
        assert(database.employees.find { it.id eq 2 }!!.salary == 50L)
    }

    @Test
    fun testBulkInsertOrUpdate1() {
        val bulkInsertWithUpdate = { ignoreErrors: Boolean ->
            database.bulkInsertOrUpdate(Employees.aliased("t")) {
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
}