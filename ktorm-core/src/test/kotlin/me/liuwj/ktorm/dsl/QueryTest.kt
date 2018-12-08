package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.BaseTest
import org.junit.Test

/**
 * Created by vince on Dec 07, 2018.
 */
class QueryTest : BaseTest() {

    @Test
    fun testQuery() {
        for (row in Departments.select()) {
            logger.debug(row[Departments.name])
        }
    }

    @Test
    fun testQuery1() {
        for (row in Departments.select()) {
            logger.debug(row[Departments.location])
        }
    }
}