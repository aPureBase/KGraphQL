package com.apurebase.kgraphql.specification.typesystem

import com.apurebase.kgraphql.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.amshove.kluent.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

@Specification("3.1.7 Lists")
class ListsSpecificationTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `list arguments are valid`() {
        val schema = KGraphQL.schema {
            query("list") {
                resolver { list: Iterable<String> -> list }
            }
        }

        val variables = objectMapper.writeValueAsString(object {
            @Suppress("unused")
            val list = listOf("GAGA", "DADA", "PADA")
        })

        val response = deserialize(schema.executeBlocking("query(\$list: [String!]!){list(list: \$list)}", variables))
        assertThat(response.extract<String>("data/list[0]"), equalTo("GAGA"))
        assertThat(response.extract<String>("data/list[1]"), equalTo("DADA"))
        assertThat(response.extract<String>("data/list[2]"), equalTo("PADA"))
    }

    @Test
    fun `lists with nullable entries are valid`() {
        val schema = KGraphQL.schema {
            query("list") {
                resolver { list: Iterable<String?> -> list }
            }
        }

        val variables = objectMapper.writeValueAsString(object {
            @Suppress("unused")
            val list = listOf("GAGA", null, "DADA", "PADA")
        })

        val response = deserialize(schema.executeBlocking("query(\$list: [String!]!){list(list: \$list)}", variables))
        assertThat(response.extract<String>("data/list[1]"), nullValue())
    }

    @Test
    fun `lists with non-nullable entries should not accept list with null element`() {
        val schema = KGraphQL.schema {
            query("list") {
                resolver { list: Iterable<String> -> list }
            }
        }

        val variables = objectMapper.writeValueAsString(object {
            @Suppress("unused")
            val list = listOf("GAGA", null, "DADA", "PADA")
        })

        invoking {
            schema.executeBlocking("query(\$list: [String!]!){list(list: \$list)}", variables)
        } shouldThrow GraphQLError::class with {
            println(prettyPrint())
            message shouldBeEqualTo "Invalid argument value [GAGA, null, DADA, PADA] from variable \$list, " +
                    "expected list with non null arguments"
        }
    }

    @Test
    fun `by default coerce single element input as collection`() {
        val schema = KGraphQL.schema {
            query("list") {
                resolver { list: Iterable<String> -> list }
            }
        }


        val variables = objectMapper.writeValueAsString(object {
            @Suppress("unused")
            val list = "GAGA"
        })

        val response = deserialize(schema.executeBlocking("query(\$list: [String!]!){list(list: \$list)}", variables))
        assertThat(response.extract<String>("data/list[0]"), equalTo("GAGA"))
    }

    @Test
    fun `null value is not coerced as single element collection`() {
        val schema = KGraphQL.schema {
            query("list") {
                resolver { list: Iterable<String>? -> list }
            }
        }


        val variables = objectMapper.writeValueAsString(object {
            @Suppress("unused")
            val list = null
        })

        val response = deserialize(schema.executeBlocking("query(\$list: [String!]!){list(list: \$list)}", variables))
        assertThat(response.extract<String>("data/list"), nullValue())
    }

    @Test
    fun `list argument can be declared non-nullable`() {
        val schema = KGraphQL.schema {
            query("list") {
                resolver { list: Iterable<String> -> list }
            }
        }

        val variables = objectMapper.writeValueAsString(object {
            @Suppress("unused")
            val list = listOf("GAGA", "DADA", "PADA")
        })

        val response = deserialize(schema.executeBlocking("query(\$list: [String!]!){list(list: \$list)}", variables))
        assertThat(response.extract<Any>("data/list"), notNullValue())
    }

    @Test
    fun `Iterable implementations are treated as list`() {

        fun getResult(): Iterable<String> = listOf("POTATO", "BATATO", "ROTATO")

        val schema = KGraphQL.schema {
            query("list") {
                resolver { -> getResult() }
            }
        }

        val response = deserialize(schema.executeBlocking("{list}"))
        assertThat(response.extract<Iterable<String>>("data/list"), equalTo(getResult()))
    }
}
