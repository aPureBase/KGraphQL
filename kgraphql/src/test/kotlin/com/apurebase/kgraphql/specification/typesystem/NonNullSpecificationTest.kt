package com.apurebase.kgraphql.specification.typesystem

import com.apurebase.kgraphql.*
import com.apurebase.kgraphql.schema.execution.Executor
import org.amshove.kluent.*
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

@Specification("3.1.8 Non-null")
class NonNullSpecificationTest {

    @Test
    fun `if the result of non-null type is null, error should be raised`() {
        val schema = KGraphQL.schema {
            query("nonNull") {
                resolver { string: String? -> string!! }
            }
        }
        invoking {
            schema.executeBlocking("{nonNull}")
        } shouldThrow GraphQLError::class with {
            originalError shouldBeInstanceOf java.lang.NullPointerException::class
        }
    }

    @Test
    fun `nullable input types are always optional`() {
        val schema = KGraphQL.schema {
            query("nullable") {
                resolver { input: String? -> input }
            }
        }

        val responseOmittedInput = deserialize(schema.executeBlocking("{nullable}"))
        assertThat(responseOmittedInput.extract<Any?>("data/nullable"), nullValue())

        val responseNullInput = deserialize(schema.executeBlocking("{nullable(input: null)}"))
        assertThat(responseNullInput.extract<Any?>("data/nullable"), nullValue())
    }

    @Test
    fun `non-null types are always required`() {
        val schema = KGraphQL.schema {
            query("nonNull") {
                resolver { input: String -> input }
            }
        }
        invoking {
            schema.executeBlocking("{nonNull}")
        } shouldThrow GraphQLError::class with {
            message shouldBeEqualTo "Missing value for non-nullable argument input on the field 'nonNull'"
        }
    }

    @Test
    fun `variable of a nullable type cannot be provided to a non-null argument`() {
        val schema = KGraphQL.schema {
            query("nonNull") {
                resolver { input: String -> input }
            }
        }

        schema.executeBlocking("query(\$arg: String!){nonNull(input: \$arg)}", "{\"arg\":\"SAD\"}")
    }

    data class Type1(val value: String)
    data class Type2(val items: List<Type1?>)

    @Test
    fun `null within arrays should work`() {
        val schema = KGraphQL.schema {
            configure {
                executor = Executor.DataLoaderPrepared
            }
            query("data") {
                resolver { ->
                    Type2(
                        items = listOf(
                            Type1("Stuff"),
                            null
                        )
                    )
                }
            }
        }

        schema.executeBlocking(
            """
            {
                data {
                    items {
                        value
                    }
                }
            }
        """.trimIndent()
        ).also(::println).deserialize().run {
            extract<String>("data/data/items[0]/value") shouldBeEqualTo "Stuff"
            extract<String?>("data/data/items[1]").shouldBeNull()
        }
    }

    data class MyInput(val value1: String, val value2: String?)

    @Test
    fun `nullable values without default values should raise an error`() {

        val schema = KGraphQL.schema {
            query("main") {
                resolver { input: MyInput -> "${input.value1} - ${input.value2 ?: "Nada"}" }
            }
        }

        invoking {
            schema.executeBlocking(
                """
                {
                    main(input: { value1: "Hello" })
                }
            """
            )
        } shouldThrow GraphQLError::class with {
            message shouldBeEqualTo "You are missing non optional input fields: value2"
        }
    }

    data class MyOptionalInput(val value1: String, val value2: String? = null)

    @Test
    fun `nullable values with default values should execute successfully`() {
        val schema = KGraphQL.schema {
            query("main") {
                resolver { input: MyOptionalInput -> "${input.value1} - ${input.value2 ?: "Nada"}" }
            }
        }

        schema.executeBlocking(
            """
            {
                main(input: { value1: "Hello" })
            }
        """.trimIndent()
        ).also(::println).deserialize().run {
            extract<String>("data/main") shouldBeEqualTo "Hello - Nada"
        }
    }
}
