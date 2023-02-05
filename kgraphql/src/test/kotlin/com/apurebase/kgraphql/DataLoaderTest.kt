package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.DefaultSchema
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.apurebase.kgraphql.schema.execution.Executor
import kotlinx.coroutines.*
import nidomiro.kdataloader.ExecutionResult
import org.amshove.kluent.shouldBeEqualTo
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestFactory
import java.time.Duration.ofSeconds
import java.util.concurrent.atomic.AtomicInteger

// This is just for safety, so when the tests fail and
// end up in an endless waiting state, they'll fail after this amount
val timeout = ofSeconds(60)!!
const val repeatTimes = 2

class DataLoaderTest {

    data class Person(val id: Int, val firstName: String, val lastName: String)

    private val jogvan = Person(1, "Jógvan", "Olsen")
    private val beinisson = Person(2, "Høgni", "Beinisson")
    private val juul = Person(3, "Høgni", "Juul")
    private val otherOne = Person(4, "The other one", "??")

    val allPeople = listOf(jogvan, beinisson, juul, otherOne)

    private val colleagues = mapOf(
        jogvan.id to listOf(beinisson, juul),
        beinisson.id to listOf(jogvan, juul, otherOne),
        juul.id to listOf(beinisson, jogvan),
        otherOne.id to listOf(beinisson)
    )

    private val boss = mapOf(
        jogvan.id to juul,
        juul.id to beinisson,
        beinisson.id to otherOne
    )

    data class Tree(val id: Int, val value: String)

    data class ABC(val value: String, val personId: Int? = null)

    data class AtomicProperty(
        val loader: AtomicInteger = AtomicInteger(),
        val prepare: AtomicInteger = AtomicInteger()
    )

    data class AtomicCounters(
        val abcB: AtomicProperty = AtomicProperty(),
        val abcChildren: AtomicProperty = AtomicProperty(),
        val treeChild: AtomicProperty = AtomicProperty()
    )

    fun schema(
        block: SchemaBuilder.() -> Unit = {}
    ): Pair<DefaultSchema, AtomicCounters> {
        val counters = AtomicCounters()

        val schema = defaultSchema {
            configure {
                useDefaultPrettyPrinter = true
                executor = Executor.DataLoaderPrepared
            }

            query("people") {
                resolver { -> allPeople }
            }

            type<Person> {
                property<String>("fullName") {
                    resolver { "${it.firstName} ${it.lastName}" }
                }

                dataProperty<Int, Person?>("respondsTo") {
                    prepare { it.id }
                    loader { keys ->
                        println("== Running [respondsTo] loader with keys: $keys ==")
                        keys.map { ExecutionResult.Success(boss[it]) }
                    }
                }
                dataProperty<Int, List<Person>>("colleagues") {
                    prepare { it.id }
                    loader { keys ->
                        delay(10)
                        println("== Running [colleagues] loader with keys: $keys ==")
                        keys.map { ExecutionResult.Success(colleagues[it] ?: listOf()) }
                    }
                }
            }

            query("tree") {
                resolver { ->
                    listOf(
                        Tree(1, "Fisk"),
                        Tree(2, "Fisk!")
                    )
                }
            }

            query("abc") {
                resolver { ->
                    (1..3).map { ABC("Testing $it", if (it == 2) null else it) }
                }
            }

            type<ABC> {

                dataProperty<String, Int>("B") {
                    loader { keys ->
                        println("== Running [B] loader with keys: $keys ==")
                        counters.abcB.loader.incrementAndGet()
                        keys.map {
                            ExecutionResult.Success(it.map(Char::toInt).fold(0) { a, b -> a + b })
                        }
                    }
                    prepare { parent: ABC ->
                        counters.abcB.prepare.incrementAndGet()
                        parent.value
                    }
                }

                @Suppress("BlockingMethodInNonBlockingContext")
                property<ABC>("simpleChild") {
                    resolver {
                        delay((1..5L).random())
                        Thread.sleep((1..5L).random())
                        delay((1..5L).random())
                        ABC("NewChild!")
                    }
                }

                dataProperty<Int?, Person?>("person") {
                    prepare { it.personId }
                    loader { personIds ->
                        personIds.map {
                            delay(1)
                            ExecutionResult.Success(
                                if (it == null || it < 1) null else allPeople[it - 1]
                            )
                        }
                    }
                }
                property<Person?>("personOld") {
                    resolver {
                        delay(1)
                        if (it.personId == null || it.personId < 1) null else allPeople[it.personId - 1]
                    }
                }

                dataProperty<String, List<ABC>>("children") {
//                    setReturnType { listOf() }
                    loader { keys ->
                        println("== Running [children] loader with keys: $keys ==")
                        counters.abcChildren.loader.incrementAndGet()
                        keys.map {
                            val (a1, a2) = when (it) {
                                "Testing 1" -> "Hello" to "World"
                                "Testing 2" -> "Fizz" to "Buzz"
                                "Testing 3" -> "Jógvan" to "Høgni"
                                else -> "${it}Nest-0" to "${it}Nest-1"
                            }
                            delay(1)
                            ExecutionResult.Success(
                                (1..7).map { if (it % 2 == 0) ABC(a1) else ABC(a2) }
                            )
                        }
                    }
                    prepare { parent ->
                        counters.abcChildren.prepare.incrementAndGet()
                        parent.value
                    }
                }

                property<List<ABC>>("childrenOld") {
                    resolver { parent ->
                        when (parent.value) {
                            "Testing 1" -> listOf(ABC("Hello"), ABC("World"))
                            "Testing 2" -> listOf(ABC("Fizz"), ABC("Buzz"))
                            "Testing 3" -> listOf(ABC("Jógvan"), ABC("Høgni"))
                            else -> listOf(ABC("${parent.value}Nest-0"), ABC("${parent.value}Nest-1"))
                        }
                    }
                }

            }


            type<Tree> {
                dataProperty<Int, Tree>("child") {
                    loader { keys ->
                        println("== Running [child] loader with keys: $keys ==")
                        counters.treeChild.loader.incrementAndGet()
                        keys.map { num -> ExecutionResult.Success(Tree(10 + num, "Fisk - $num")) }
                    }

                    prepare { parent, buzz: Int ->
                        counters.treeChild.prepare.incrementAndGet()
                        parent.id + buzz
                    }
                }
            }

            block(this)
        }

        return schema to counters
    }

    @TestFactory
    fun `stress test`(): List<DynamicTest> {
        val (schema) = schema()
        val fn: (Boolean) -> List<DynamicTest> = { withTypeName ->
            val query = """
                {
                    abc {
                        __typename
                        value
                        person: personOld {
                            fullName
                            __typename
                        }
                        simpleChild {
                            __typename
                            value
                            person: personOld {
                                __typename
                                fullName
                            }
                            simpleChild {
                                __typename
                                value
                            }
                        }
                    }
                }
            """.trimIndent().let {
                if (withTypeName) it
                else it.replace("__typename", "")
            }

            val test: (Int) -> DynamicTest = {
                DynamicTest.dynamicTest("test-$it") {
                    val result = schema.executeBlocking(query).also(::println).deserialize()

                    result.extract<String>("data/abc[0]/value") shouldBeEqualTo "Testing 1"
                    result.extract<String>("data/abc[0]/person/fullName") shouldBeEqualTo "Jógvan Olsen"
                    result.extract<String>("data/abc[0]/simpleChild/value") shouldBeEqualTo "NewChild!"
                    result.extract<String?>("data/abc[0]/simpleChild/person") shouldBeEqualTo null
                    result.extract<String?>("data/abc[0]/simpleChild/simpleChild/value") shouldBeEqualTo "NewChild!"

                    result.extract<String>("data/abc[1]/value") shouldBeEqualTo "Testing 2"
                    result.extract<String?>("data/abc[1]/person") shouldBeEqualTo null
                    result.extract<String>("data/abc[1]/simpleChild/value") shouldBeEqualTo "NewChild!"
                    result.extract<String?>("data/abc[1]/simpleChild/person") shouldBeEqualTo null
                    result.extract<String?>("data/abc[1]/simpleChild/simpleChild/value") shouldBeEqualTo "NewChild!"

                    result.extract<String>("data/abc[2]/value") shouldBeEqualTo "Testing 3"
                    result.extract<String?>("data/abc[2]/person/fullName") shouldBeEqualTo "Høgni Juul"
                    result.extract<String>("data/abc[2]/simpleChild/value") shouldBeEqualTo "NewChild!"
                    result.extract<String?>("data/abc[2]/simpleChild/person") shouldBeEqualTo null
                    result.extract<String?>("data/abc[2]/simpleChild/simpleChild/value") shouldBeEqualTo "NewChild!"

                    if (withTypeName) {
                        result.extract<String>("data/abc[0]/__typename") shouldBeEqualTo "ABC"
                        result.extract<String>("data/abc[0]/person/__typename") shouldBeEqualTo "Person"
                        result.extract<String>("data/abc[0]/simpleChild/__typename") shouldBeEqualTo "ABC"
                        result.extract<String>("data/abc[1]/__typename") shouldBeEqualTo "ABC"
                        result.extract<String>("data/abc[1]/simpleChild/__typename") shouldBeEqualTo "ABC"
                        result.extract<String>("data/abc[2]/__typename") shouldBeEqualTo "ABC"
                        result.extract<String>("data/abc[2]/simpleChild/__typename") shouldBeEqualTo "ABC"
                    }
                }
            }

            (1..100).chunked(25).flatMap { chunk ->
                runBlocking {
                    coroutineScope {
                        chunk.map {
                            async { test(it) }
                        }.awaitAll()
                    }
                }
            }
        }


        return fn(true) + fn(false)
    }

    @RepeatedTest(repeatTimes, name = "Nested array loaders")
    fun `Nested array loaders`() {
        assertTimeoutPreemptively(timeout) {
            val (schema) = schema()
            val query = """
                {
                    people {
                        fullName
                        colleagues {
                            fullName
                            respondsTo {
                                fullName
                            }
                        }
                    }
                }                
            """.trimIndent()

            schema.executeBlocking(query).also(::println).deserialize()
        }
    }

    @RepeatedTest(repeatTimes, name = "Old basic resolvers in new executor")
    fun `Old basic resolvers in new executor`() {
        assertTimeoutPreemptively(timeout) {
            val (schema) = schema()
            val query = """
                {
                    abc {
                        value
                        simpleChild {
                            value
                        }
                    }
                }
            """.trimIndent()

            val result = schema.executeBlocking(query).also(::println).deserialize()

            MatcherAssert.assertThat(result.extract<String>("data/abc[0]/simpleChild/value"), CoreMatchers.equalTo("NewChild!"))
        }
    }

    @RepeatedTest(repeatTimes, name = "Very basic new Level executor")
    fun `Very basic new Level executor`() {
        assertTimeoutPreemptively(timeout) {
            val (schema) = schema()

            val query = """
                {
                    people {
                        id
                        fullName
                        respondsTo {
                            fullName
                            respondsTo {
                                fullName
                            }
                        }
                    }
                }
            """.trimIndent()

            schema.executeBlocking(query).also(::println).deserialize()
        }
    }

    @RepeatedTest(repeatTimes, name = "dataloader with nullable prepare keys")
    fun `dataloader with nullable prepare keys`() {
        assertTimeoutPreemptively(timeout) {
            val (schema) = schema()

            val query = """
                {
                    abc {
                        value
                        personId
                        person {
                            id
                            fullName
                        }
                    }
                }
            """.trimIndent()
            val result = schema.executeBlocking(query).also(::println).deserialize()

            result.extract<String>("data/abc[0]/person/fullName") shouldBeEqualTo "${jogvan.firstName} ${jogvan.lastName}"
            result.extract<String?>("data/abc[1]/person") shouldBeEqualTo null
            result.extract<String>("data/abc[2]/person/fullName") shouldBeEqualTo "${juul.firstName} ${juul.lastName}"
        }
    }

    @RepeatedTest(repeatTimes, name = "Basic dataloader test")
    fun `Basic dataloader test`() {
        assertTimeoutPreemptively(timeout) {
            val (schema) = schema()

            val query = """
                {
                    people {
                        ...PersonInfo
                        respondsTo { ...PersonInfo }
                        colleagues { ...PersonInfo }
                    }
                }
                fragment PersonInfo on Person {
                    id
                    fullName
                }
            """.trimIndent()
            val result = schema.executeBlocking(query).also(::println).deserialize()


            result.extract<String>("data/people[0]/respondsTo/fullName") shouldBeEqualTo "${juul.firstName} ${juul.lastName}"
            result.extract<String>("data/people[1]/colleagues[0]/fullName") shouldBeEqualTo "${jogvan.firstName} ${jogvan.lastName}"
        }
    }

    @RepeatedTest(2, name = "basic data loader")
    fun `basic data loader`() {
        assertTimeoutPreemptively(timeout) {
            val (schema, counters) = schema()

            val query = """
                {
                    tree { # <-- 2
                        id
                        child(buzz: 3) {
                            id
                            value
                        }
                    }
                }
            """.trimIndent()

            val result = schema.executeBlocking(query).also(::println).deserialize()
            counters.treeChild.prepare.get() shouldBeEqualTo 2
            counters.treeChild.loader.get() shouldBeEqualTo 1


            result.extract<Int>("data/tree[1]/id") shouldBeEqualTo 2
            result.extract<Int>("data/tree[0]/child/id") shouldBeEqualTo 14
            result.extract<Int>("data/tree[1]/child/id") shouldBeEqualTo 15
        }
    }

    @RepeatedTest(repeatTimes, name = "data loader cache per request only")
    fun `data loader cache per request only`() {
        assertTimeoutPreemptively(timeout) {
            val (schema, counters) = schema()

            val query = """
                {
                    first: tree { id, child(buzz: 3) { id, value } }
                    second: tree { id, child(buzz: 3) { id, value } }
                }
            """.trimIndent()

            val result = schema.executeBlocking(query).also(::println).deserialize()

            counters.treeChild.prepare.get() shouldBeEqualTo 4
            counters.treeChild.loader.get() shouldBeEqualTo 1

            result.extract<Int>("data/first[1]/id") shouldBeEqualTo 2
            result.extract<Int>("data/first[0]/child/id") shouldBeEqualTo 14
            result.extract<Int>("data/first[1]/child/id") shouldBeEqualTo 15
            result.extract<Int>("data/second[1]/child/id") shouldBeEqualTo 15

        }
    }

    @RepeatedTest(repeatTimes, name = "multiple layers of dataLoaders")
    fun `multiple layers of dataLoaders`() {
        assertTimeoutPreemptively(timeout) {
            val (schema) = schema()

            val query = """
                {
                    abc {
                        value
                        B
                        children {
                            value
                            B
                            children {
                                value
                                B
                                children {
                                    value
                                    B
                                    children {
                                        value
                                        B
                                        children {
                                            value
                                            B
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent()

            schema.executeBlocking(query).also(::println).deserialize()

//            throw TODO("Assert results")
        }
    }
}
