
plugins {
    id("kotlin")
}

val generatedSourceDir = "${project.buildDir.absolutePath}/generated/source/main/kotlin"
val maxTupleNumber = 9

fun generateTuple(writer: java.io.Writer, tupleNumber: Int) {
    val typeParams = (1..tupleNumber).joinToString(separator = ", ") { "out E$it" }
    val propertyDefinitions = (1..tupleNumber).joinToString(separator = ",\n            ") { "val element$it: E$it" }
    val toStringTemplate = (1..tupleNumber).joinToString(separator = ", ") { "\$element$it" }

    writer.write("""
        
        /** 
         * Represents a tuple of $tupleNumber values.
         *
         * There is no meaning attached to values in this class, it can be used for any purpose.
         * Two tuples are equal if all the components are equal.
         */
        public data class Tuple$tupleNumber<$typeParams>(
            $propertyDefinitions
        ) : Serializable { 
        
            override fun toString(): String { 
                return "($toStringTemplate)"
            }
        
            private companion object {
                private const val serialVersionUID = 1L
            }
        }
        
    """.trimIndent())
}

fun generateTupleOf(writer: java.io.Writer, tupleNumber: Int) {
    val typeParams = (1..tupleNumber).joinToString(separator = ", ") { "E$it" }
    val params = (1..tupleNumber).joinToString(separator = ",\n            ") { "element$it: E$it" }
    val elements = (1..tupleNumber).joinToString(separator = ", ") { "element$it" }

    writer.write("""
        
        /**
         * Create a tuple of $tupleNumber values. 
         *
         * @since 2.7
         */
        public fun <$typeParams> tupleOf(
            $params
        ): Tuple$tupleNumber<$typeParams> {
            return Tuple$tupleNumber($elements)
        }
        
    """.trimIndent())
}

fun generateToList(writer: java.io.Writer, tupleNumber: Int) {
    val typeParams = (1..tupleNumber).joinToString(separator = ", ") { "E" }
    val elements = (1..tupleNumber).joinToString(separator = ", ") { "element$it" }

    writer.write("""
        
        /**
         * Convert this tuple into a list. 
         *
         * @since 2.7
         */
        public fun <E> Tuple$tupleNumber<$typeParams>.toList(): List<E> {
            return listOf($elements)
        }
        
    """.trimIndent())
}

fun generateMapColumns(writer: java.io.Writer, tupleNumber: Int) {
    val typeParams = (1..tupleNumber).joinToString(separator = ", ") { "C$it : Any" }
    val columnDeclarings = (1..tupleNumber).joinToString(separator = ", ") { "ColumnDeclaring<C$it>" }
    val resultTypes = (1..tupleNumber).joinToString(separator = ", ") { "C$it?" }
    val variableNames = (1..tupleNumber).joinToString(separator = ", ") { "c$it" }
    val resultExtractors = (1..tupleNumber).joinToString(separator = ", ") { "c${it}.sqlType.getResult(row, $it)" }

    writer.write("""
        
        /**
         * Customize the selected columns of the internal query by the given [columnSelector] function, and return a [List]
         * containing the query results.
         *
         * This function is similar to [EntitySequence.map], but the [columnSelector] closure accepts the current table
         * object [T] as the parameter, so what we get in the closure by `it` is the table object instead of an entity
         * element. Besides, the closure’s return type is a tuple of `ColumnDeclaring<C>`s, and we should return some 
         * columns or expressions to customize the `select` clause of the generated SQL.
         *
         * Ktorm supports selecting two or more columns, we just need to wrap our selected columns by [tupleOf]
         * in the closure, then the function’s return type becomes `List<TupleN<C1?, C2?, .. Cn?>>`.
         *
         * The operation is terminal.
         *
         * @param isDistinct specify if the query is distinct, the generated SQL becomes `select distinct` if it's set to true.
         * @param columnSelector a function in which we should return a tuple of columns or expressions to be selected.
         * @return a list of the query results.
         * @since 3.1.0
         */
        @JvmName("_mapColumns$tupleNumber")
        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        public inline fun <E : Any, T : BaseTable<E>, $typeParams> EntitySequence<E, T>.mapColumns(
            isDistinct: Boolean = false,
            columnSelector: (T) -> Tuple$tupleNumber<$columnDeclarings>
        ): List<Tuple$tupleNumber<$resultTypes>> {
            return mapColumnsTo(ArrayList(), isDistinct, columnSelector)
        }
        
        /**
         * Customize the selected columns of the internal query by the given [columnSelector] function, and append the query
         * results to the given [destination].
         *
         * This function is similar to [EntitySequence.mapTo], but the [columnSelector] closure accepts the current table
         * object [T] as the parameter, so what we get in the closure by `it` is the table object instead of an entity
         * element. Besides, the closure’s return type is a tuple of `ColumnDeclaring<C>`s, and we should return some 
         * columns or expressions to customize the `select` clause of the generated SQL.
         * 
         * Ktorm supports selecting two or more columns, we just need to wrap our selected columns by [tupleOf]
         * in the closure, then the function’s return type becomes `List<TupleN<C1?, C2?, .. Cn?>>`.
         *
         * The operation is terminal.
         *
         * @param destination a [MutableCollection] used to store the results.
         * @param isDistinct specify if the query is distinct, the generated SQL becomes `select distinct` if it's set to true.
         * @param columnSelector a function in which we should return a tuple of columns or expressions to be selected.
         * @return the [destination] collection of the query results.
         * @since 3.1.0
         */
        @JvmName("_mapColumns${tupleNumber}To")
        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        public inline fun <E : Any, T : BaseTable<E>, $typeParams, R> EntitySequence<E, T>.mapColumnsTo(
            destination: R,
            isDistinct: Boolean = false,
            columnSelector: (T) -> Tuple$tupleNumber<$columnDeclarings>
        ): R where R : MutableCollection<in Tuple$tupleNumber<$resultTypes>> {
            val ($variableNames) = columnSelector(sourceTable)
        
            val expr = expression.copy(
                columns = listOf($variableNames).map { it.aliased(null) },
                isDistinct = isDistinct
            )
        
            return Query(database, expr).mapTo(destination) { row -> tupleOf($resultExtractors) }
        }
        
    """.trimIndent())
}

fun generateAggregateColumns(writer: java.io.Writer, tupleNumber: Int) {
    val typeParams = (1..tupleNumber).joinToString(separator = ", ") { "C$it : Any" }
    val columnDeclarings = (1..tupleNumber).joinToString(separator = ", ") { "ColumnDeclaring<C$it>" }
    val resultTypes = (1..tupleNumber).joinToString(separator = ", ") { "C$it?" }
    val variableNames = (1..tupleNumber).joinToString(separator = ", ") { "c$it" }
    val resultExtractors = (1..tupleNumber).joinToString(separator = ", ") { "c${it}.sqlType.getResult(rowSet, $it)" }

    writer.write("""
        
        /**
         * Perform a tuple of aggregations given by [aggregationSelector] for all elements in the sequence,
         * and return the aggregate results.
         *
         * Ktorm supports aggregating two or more columns, we just need to wrap our aggregate expressions by
         * [tupleOf] in the closure, then the function’s return type becomes `TupleN<C1?, C2?, .. Cn?>`.
         *
         * The operation is terminal.
         *
         * @param aggregationSelector a function that accepts the source table and returns a tuple of aggregate expressions.
         * @return a tuple of the aggregate results.
         * @since 3.1.0
         */
        @JvmName("_aggregateColumns$tupleNumber")
        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        public inline fun <E : Any, T : BaseTable<E>, $typeParams> EntitySequence<E, T>.aggregateColumns(
            aggregationSelector: (T) -> Tuple$tupleNumber<$columnDeclarings>
        ): Tuple$tupleNumber<$resultTypes> {
            val ($variableNames) = aggregationSelector(sourceTable)
        
            val expr = expression.copy(
                columns = listOf($variableNames).map { it.aliased(null) }
            )
        
            val rowSet = Query(database, expr).rowSet
        
            if (rowSet.size() == 1) {
                check(rowSet.next())
                return tupleOf($resultExtractors)
            } else {
                val (sql, _) = database.formatExpression(expr, beautifySql = true)
                throw IllegalStateException("Expected 1 row but ${'$'}{rowSet.size()} returned from sql: \n\n${'$'}sql")
            }
        }
        
    """.trimIndent())
}

fun generateGroupingAggregateColumns(writer: java.io.Writer, tupleNumber: Int) {
    val typeParams = (1..tupleNumber).joinToString(separator = ", ") { "C$it : Any" }
    val columnDeclarings = (1..tupleNumber).joinToString(separator = ", ") { "ColumnDeclaring<C$it>" }
    val resultTypes = (1..tupleNumber).joinToString(separator = ", ") { "C$it?" }
    val variableNames = (1..tupleNumber).joinToString(separator = ", ") { "c$it" }
    val resultExtractors = (1..tupleNumber).joinToString(separator = ", ") { "c${it}.sqlType.getResult(row, ${it + 1})" }

    writer.write("""
        
        /**
         * Group elements from the source sequence by key and perform the given aggregations for elements in each group,
         * then store the results in a new [Map].
         *
         * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
         * `select key, aggregation from source group by key`.
         *
         * Ktorm supports aggregating two or more columns, we just need to wrap our aggregate expressions by [tupleOf]
         * in the closure, then the function’s return type becomes `Map<K?, TupleN<C1?, C2?, .. Cn?>>`.
         *
         * @param aggregationSelector a function that accepts the source table and returns a tuple of aggregate expressions.
         * @return a [Map] associating the key of each group with the results of aggregations of the group elements.
         * @since 3.1.0
         */
        @JvmName("_aggregateColumns$tupleNumber")
        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        public inline fun <E : Any, T : BaseTable<E>, K : Any, $typeParams> EntityGrouping<E, T, K>.aggregateColumns(
            aggregationSelector: (T) -> Tuple$tupleNumber<$columnDeclarings>
        ): Map<K?, Tuple$tupleNumber<$resultTypes>> {
            return aggregateColumnsTo(LinkedHashMap(), aggregationSelector)
        }
        
        /**
         * Group elements from the source sequence by key and perform the given aggregations for elements in each group,
         * then store the results in the [destination] map.
         *
         * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
         * `select key, aggregation from source group by key`.
         *
         * Ktorm supports aggregating two or more columns, we just need to wrap our aggregate expressions by [tupleOf]
         * in the closure, then the function’s return type becomes `Map<K?, TupleN<C1?, C2?, .. Cn?>>`.
         *
         * @param destination a [MutableMap] used to store the results.
         * @param aggregationSelector a function that accepts the source table and returns a tuple of aggregate expressions.
         * @return the [destination] map associating the key of each group with the result of aggregations of the group elements.
         * @since 3.1.0
         */
        @JvmName("_aggregateColumns${tupleNumber}To")
        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        public inline fun <E : Any, T : BaseTable<E>, K : Any, $typeParams, M> EntityGrouping<E, T, K>.aggregateColumnsTo(
            destination: M,
            aggregationSelector: (T) -> Tuple$tupleNumber<$columnDeclarings>
        ): M where M : MutableMap<in K?, in Tuple$tupleNumber<$resultTypes>> {
            val keyColumn = keySelector(sequence.sourceTable)
            val ($variableNames) = aggregationSelector(sequence.sourceTable)
        
            val expr = sequence.expression.copy(
                columns = listOf(keyColumn, $variableNames).map { it.aliased(null) },
                groupBy = listOf(keyColumn.asExpression())
            )
        
            for (row in Query(sequence.database, expr)) {
                val key = keyColumn.sqlType.getResult(row, 1)
                destination[key] = tupleOf($resultExtractors)
            }
        
            return destination
        }
        
    """.trimIndent())
}

val generateTuples by tasks.registering {
    doLast {
        val outputFile = file("$generatedSourceDir/org/ktorm/entity/Tuples.kt")
        outputFile.parentFile.mkdirs()

        outputFile.bufferedWriter().use { writer ->
            writer.write("""
                /*
                 * Copyright 2018-2023 the original author or authors.
                 *
                 * Licensed under the Apache License, Version 2.0 (the "License");
                 * you may not use this file except in compliance with the License.
                 * You may obtain a copy of the License at
                 *
                 *      http://www.apache.org/licenses/LICENSE-2.0
                 *
                 * Unless required by applicable law or agreed to in writing, software
                 * distributed under the License is distributed on an "AS IS" BASIS,
                 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                 * See the License for the specific language governing permissions and
                 * limitations under the License.
                 */
                
                // Auto-generated by ktorm.tuples-codegen.gradle.kts, DO NOT EDIT!
                
                package org.ktorm.entity
                
                import org.ktorm.dsl.Query
                import org.ktorm.dsl.mapTo
                import org.ktorm.schema.ColumnDeclaring
                import org.ktorm.schema.BaseTable
                import java.io.Serializable
                import kotlin.experimental.ExperimentalTypeInference
                
                /**
                 * Set a typealias `Tuple2` for `Pair`. 
                 */
                public typealias Tuple2<E1, E2> = Pair<E1, E2>
                
                /**
                 * Set a typealias `Tuple3` for `Triple`.
                 */
                public typealias Tuple3<E1, E2, E3> = Triple<E1, E2, E3>
                
            """.trimIndent())

            for (num in (4..maxTupleNumber)) {
                generateTuple(writer, num)
            }

            for (num in (2..maxTupleNumber)) {
                generateTupleOf(writer, num)
            }

            for (num in (4..maxTupleNumber)) {
                generateToList(writer, num)
            }

            for (num in (2..maxTupleNumber)) {
                generateMapColumns(writer, num)
            }

            for (num in (2..maxTupleNumber)) {
                generateAggregateColumns(writer, num)
            }

            for (num in (2..maxTupleNumber)) {
                generateGroupingAggregateColumns(writer, num)
            }
        }
    }
}

tasks {
    compileKotlin {
        dependsOn(generateTuples)
    }
    "jarSources" {
        dependsOn(generateTuples)
    }
}

sourceSets.main {
    kotlin.srcDir(generatedSourceDir)
}
