package com.apurebase.kgraphql.specification.language

import com.apurebase.kgraphql.*
import com.apurebase.kgraphql.Actor
import com.apurebase.kgraphql.schema.execution.Executor
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

@Specification("2.6 Arguments")
class ArgumentsSpecificationTest {
    val age = 432

    val schema = defaultSchema {

        configure {
            executor = Executor.Parallel
        }

        query("actor") {
            resolver { -> Actor("Boguś Linda", age) }
        }

        type<Actor> {
            property<List<String>>("favDishes") {
                resolver { _: Actor, size: Int, prefix: String? ->
                    listOf("steak", "burger", "soup", "salad", "bread", "bird").let { dishes ->
                        if (prefix != null) {
                            dishes.filter { it.startsWith(prefix) }
                        } else {
                            dishes
                        }
                    }.take(size)
                }
            }
            property<Int>("none") {
                resolver { actor -> actor.age }
            }
            property<Int>("one") {
                resolver { actor, one: Int -> actor.age + one }
            }
            property<Int>("two") {
                resolver { actor, one: Int, two: Int -> actor.age + one + two }
            }
            property<Int>("three") {
                resolver { actor, one: Int, two: Int, three: Int ->
                    actor.age + one + two + three
                }
            }
            property<Int>("four") {
                resolver { actor, one: Int, two: Int, three: Int, four: Int ->
                    actor.age + one + two + three + four
                }
            }
            property<Int>("five") {
                resolver { actor, one: Int, two: Int, three: Int, four: Int, five: Int ->
                    actor.age + one + two + three + four + five
                }
            }
        }
    }

    @Test
    fun `arguments are unordered`() {
        executeEqualQueries(
            schema,
            mapOf("data" to mapOf("actor" to mapOf("favDishes" to listOf("burger", "bread")))),
            "{actor{favDishes(size: 2, prefix: \"b\")}}",
            "{actor{favDishes(prefix: \"b\", size: 2)}}"
        )
    }

    @Test
    fun `many arguments can exist on given field`() {
        val response =
            deserialize(schema.executeBlocking("{actor{favDishes(size: 2, prefix: \"b\")}}")) as Map<String, Any>
        assertThat(
            response,
            equalTo(mapOf<String, Any>("data" to mapOf("actor" to mapOf("favDishes" to listOf("burger", "bread")))))
        )
    }

    @Test
    fun `all arguments to suspendResolvers`() {
        val request = """
            {
                actor {
                    none
                    one(one: 1)
                    two(one: 2, two: 3)
                    three(one: 4, two: 5, three: 6)
                    four(one: 7, two: 8, three: 9, four: 10)
                    five(one: 11, two: 12, three: 13, four: 14, five: 15)
                }
            }
        """.trimIndent()
        val response = deserialize(schema.executeBlocking(request)) as Map<String, Any>
        assertThat(
            response, equalTo(
                mapOf<String, Any>(
                    "data" to mapOf(
                        "actor" to mapOf(
                            "none" to age,
                            "one" to age + 1,
                            "two" to age + 2 + 3,
                            "three" to age + 4 + 5 + 6,
                            "four" to age + 7 + 8 + 9 + 10,
                            "five" to age + 11 + 12 + 13 + 14 + 15
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `property arguments should accept default values`() {
        val schema = defaultSchema {
            query("actor") {
                resolver {
                    ->
                    Actor("John Doe", age)
                }
            }

            type<Actor> {
                property<String>("greeting") {
                    resolver { actor: Actor, suffix: String ->
                        "$suffix, ${actor.name}!"
                    }.withArgs {
                        arg<String> { name = "suffix"; defaultValue = "Hello" }
                    }
                }
            }
        }

        val request = """
            {
                actor {
                    greeting
                }
            }
        """.trimIndent()

        val response = deserialize(schema.executeBlocking(request)) as Map<String, Any>
        assertThat(
            response, equalTo(
                mapOf<String, Any>(
                    "data" to mapOf<String, Any>(
                        "actor" to mapOf<String, Any>(
                            "greeting" to "Hello, John Doe!"
                        )
                    )
                )
            )
        )
    }
}
