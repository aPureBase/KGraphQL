package com.apurebase.kgraphql.specification.typesystem

import com.apurebase.kgraphql.Actor
import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.Specification
import com.apurebase.kgraphql.expect
import com.apurebase.kgraphql.schema.SchemaException
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.apurebase.kgraphql.schema.execution.Executor
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test

@Specification("3.1.2 Objects")
class ObjectsSpecificationTest {
    data class Underscore(val __field: Int)

    // TODO: We want to have [Executor.DataLoaderPrepared] working with these tests before reaching stable release of that executor!
    fun schema(executor: Executor = Executor.Parallel, block: SchemaBuilder.() -> Unit) = KGraphQL.schema {
        configure {
            this@configure.executor = executor
        }
        block()
    }

    @Test
    fun `All fields defined within an Object type must not have a name which begins with __`() {
        expect<SchemaException>("Illegal name '__field'. Names starting with '__' are reserved for introspection system") {
            schema {
                query("underscore") {
                    resolver { -> Underscore(0) }
                }
            }
        }
    }

    data class ManyFields(
        val id: String = "Many",
        val id2: String = "Fields",
        val value: Int = 0,
        val smooth: Boolean = false,
        val active: Boolean = false,
    )

    data class FewFields(val name: String = "Boguś", val surname: String = "Linda")

    @Test
    fun `fields are conceptually ordered in the same order in which they were encountered during query execution`() {
        val schema = schema { // TODO: Update this test to use the new [DataLoaderPrepared] executor!
            query("many") { resolver { -> ManyFields() } }
            type<ManyFields> {
                property<String>("name") {
                    resolver { _ -> "Boguś" }
                }
            }
        }

        val result = schema.executeBlocking("{many{id, id2, value, active, smooth, name}}")
        with(result) {
            assertThat(indexOf("\"name\""), greaterThan(indexOf("\"smooth\"")))
            assertThat(indexOf("\"smooth\""), greaterThan(indexOf("\"active\"")))
            assertThat(indexOf("\"active\""), greaterThan(indexOf("\"value\"")))
            assertThat(indexOf("\"value\""), greaterThan(indexOf("\"id2\"")))
            assertThat(indexOf("\"id2\""), greaterThan(indexOf("\"id\"")))
        }

        val result2 = schema.executeBlocking("{many{name, active, id2, value, smooth, id}}")
        with(result2) {
            assertThat(indexOf("\"id\""), greaterThan(indexOf("\"smooth\"")))
            assertThat(indexOf("\"smooth\""), greaterThan(indexOf("\"value\"")))
            assertThat(indexOf("\"value\""), greaterThan(indexOf("\"id2\"")))
            assertThat(indexOf("\"id2\""), greaterThan(indexOf("\"active\"")))
            assertThat(indexOf("\"active\""), greaterThan(indexOf("\"name\"")))
        }
    }

    @Test
    fun `fragment spread fields occur before the following fields`() {
        val schema = schema {
            query("many") { resolver { -> ManyFields() } }
        }

        val result =
            schema.executeBlocking("{many{active, ...Fields , smooth, id}} fragment Fields on ManyFields { id2, value }")
        with(result) {
            assertThat(indexOf("\"id\""), greaterThan(indexOf("\"smooth\"")))
            assertThat(indexOf("\"smooth\""), greaterThan(indexOf("\"value\"")))
            assertThat(indexOf("\"value\""), greaterThan(indexOf("\"id2\"")))
            assertThat(indexOf("\"id2\""), greaterThan(indexOf("\"active\"")))
        }
    }

    @Test
    fun `fragments for which the type does not apply does not affect ordering`() {
        val schema = schema {
            query("many") { resolver { -> ManyFields() } }
            type<FewFields>()
        }

        val result = schema.executeBlocking(
            "{many{active, ...Fields, ...Few , smooth, id}} " +
                    "fragment Fields on ManyFields { id2, value }" +
                    "fragment Few on FewFields { name } "
        )
        with(result) {
            assertThat(indexOf("\"id\""), greaterThan(indexOf("\"smooth\"")))
            assertThat(indexOf("\"smooth\""), greaterThan(indexOf("\"value\"")))
            assertThat(indexOf("\"value\""), greaterThan(indexOf("\"id2\"")))
            assertThat(indexOf("\"id2\""), greaterThan(indexOf("\"active\"")))
        }
    }

    @Test
    fun `If a field is queried multiple times in a selection, it is ordered by the first time it is encountered`() {
        val schema = schema {
            query("many") { resolver { -> ManyFields() } }
        }

        val result = schema.executeBlocking("{many{id, id2, value, id, active, smooth}}")
        with(result) {
            //ensure that "id" appears only once
            assertThat(indexOf("\"id\""), equalTo(lastIndexOf("\"id\"")))

            assertThat(indexOf("\"smooth\""), greaterThan(indexOf("\"active\"")))
            assertThat(indexOf("\"active\""), greaterThan(indexOf("\"value\"")))
            assertThat(indexOf("\"value\""), greaterThan(indexOf("\"id2\"")))
            assertThat(indexOf("\"id2\""), greaterThan(indexOf("\"id\"")))
        }

        val resultFragment =
            schema.executeBlocking("{many{id, id2, ...Many, active, smooth}} fragment Many on ManyFields{value, id}")
        with(resultFragment) {
            //ensure that "id" appears only once
            assertThat(indexOf("\"id\""), equalTo(lastIndexOf("\"id\"")))

            assertThat(indexOf("\"smooth\""), greaterThan(indexOf("\"active\"")))
            assertThat(indexOf("\"active\""), greaterThan(indexOf("\"value\"")))
            assertThat(indexOf("\"value\""), greaterThan(indexOf("\"id2\"")))
            assertThat(indexOf("\"id2\""), greaterThan(indexOf("\"id\"")))
        }
    }

    @Test
    fun `All arguments defined within a field must not have a name which begins with __`() {
        expect<SchemaException>("Illegal name '__id'. Names starting with '__' are reserved for introspection system") {
            schema {
                query("many") { resolver { __id: String -> ManyFields(__id) } }
            }
        }
    }

    class Empty

    @Test
    fun `An Object type must define one or more fields`() {
        expect<SchemaException>("An Object type must define one or more fields. Found none on type Empty") {
            schema { type<Empty>() }
        }
    }

    @Test
    fun `field resolution order does not affect response field order`() {
        val schema = schema {
            type<Actor> {
                property<String>("long") {
                    resolver {
                        Thread.sleep(20)
                        "FINISHED LONG"
                    }
                }

                property<String>("short") {
                    resolver {
                        "FINISHED SHORT"
                    }
                }
            }

            query("actor") {
                resolver<Actor> { Actor("Harden", 22) }
            }
        }

        val responseShortAfterLong = (schema.executeBlocking("{actor{long, short}}"))
        with(responseShortAfterLong) {
            assertThat(indexOf("short"), greaterThan(indexOf("long")))
        }

        val responseLongAfterShort = (schema.executeBlocking("{actor{short, long}}"))
        with(responseLongAfterShort) {
            assertThat(indexOf("long"), greaterThan(indexOf("short")))
        }
    }

    @Test
    fun `operation resolution order does not affect response field order`() {
        val schema = schema {
            query("long") {
                resolver<String> {
                    Thread.sleep(100)
                    "FINISHED LONG"
                }
            }

            query("short") {
                resolver<String> {
                    "FINISHED SHORT"
                }
            }
        }

        val responseShortAfterLong = schema.executeBlocking("{long, short}")
        with(responseShortAfterLong) {
            assertThat(indexOf("short"), greaterThan(indexOf("long")))
        }
    }
}
