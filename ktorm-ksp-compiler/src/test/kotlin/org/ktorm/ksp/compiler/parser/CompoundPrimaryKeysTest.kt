package org.ktorm.ksp.compiler.parser

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest

class CompoundPrimaryKeysTest : BaseKspTest() {

    @Test
    fun `multi primary key`() = runKotlin("""
        @Table(name = "province")
        data class Province(
            @PrimaryKey
            val country:String,
            @PrimaryKey
            val province:String,
            var population:Int
        )
        
        fun run() {
            database.provinces.add(Province("China", "Guangdong", 150000))
            assert(database.provinces.toList().contains(Province("China", "Guangdong", 150000)))
            
            var province = database.provinces.first { (it.country eq "China") and (it.province eq "Hebei") }
            province.population = 200000
            database.provinces.update(province)
            
            province = database.provinces.first { (it.country eq "China") and (it.province eq "Hebei") }
            assert(province.population == 200000)
        }
    """.trimIndent())
}
