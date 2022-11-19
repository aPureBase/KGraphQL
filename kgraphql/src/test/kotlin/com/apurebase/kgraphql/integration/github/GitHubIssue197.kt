package com.apurebase.kgraphql.integration.github

import com.apurebase.kgraphql.defaultSchema
import com.apurebase.kgraphql.deserialize
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class GitHubIssue197 {

    data class Outer(val inner1: Inner, val inner2: Inner)
    data class Inner(val name: String)

    private val testedSchema = defaultSchema {
        query("outer") {
            resolver { ->
                Outer(Inner("test1"), Inner("test2"))
            }
        }
        type<Inner> {
            property("testProperty1") {
                resolver { "${it.name}.testProperty1" }
            }
            property("testProperty2") {
                resolver { "${it.name}.testProperty2" }
            }
        }
    }

    @Test
    fun `executor should merge fragment declaration and field declaration`() {
        val response = testedSchema.executeBlocking(//language=graphql
            """
            { 
                outer { 
                    ...TestFragment
                    inner1 { testProperty1 } 
                    inner2 { testProperty2 } 
                } 
            } 
            fragment TestFragment on Outer {
                inner1 { name, testProperty2 }
                inner2 { name, testProperty1 }
            }
        """.trimIndent()
        ).also(::println)

        val deserialized = deserialize(response)
        assertThat(
            deserialized, equalTo(
                mapOf(
                    "data" to mapOf(
                        "outer" to mapOf(
                            "inner1" to mapOf(
                                "name" to "test1",
                                "testProperty2" to "test1.testProperty2",
                                "testProperty1" to "test1.testProperty1",
                            ),
                            "inner2" to mapOf(
                                "name" to "test2",
                                "testProperty1" to "test2.testProperty1",
                                "testProperty2" to "test2.testProperty2",
                            ),
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `executor should merge several fragment declarations and field declaration`() {
        val response = testedSchema.executeBlocking(//language=graphql
            """
            { 
                outer { 
                    ...TestFragment1
                    ...TestFragment2
                    inner2 {
                        testProperty2
                    }
                } 
            } 
            fragment TestFragment1 on Outer {
                inner1 { name, testProperty2 }
            }
            fragment TestFragment2 on Outer {
                inner2 { name, testProperty1 }
            }
        """.trimIndent()
        ).also(::println)

        val deserialized = deserialize(response)
        assertThat(
            deserialized, equalTo(
                mapOf(
                    "data" to mapOf(
                        "outer" to mapOf(
                            "inner1" to mapOf(
                                "name" to "test1",
                                "testProperty2" to "test1.testProperty2",
                            ),
                            "inner2" to mapOf(
                                "name" to "test2",
                                "testProperty1" to "test2.testProperty1",
                                "testProperty2" to "test2.testProperty2",
                            ),
                        )
                    )
                )
            )
        )
    }

}
