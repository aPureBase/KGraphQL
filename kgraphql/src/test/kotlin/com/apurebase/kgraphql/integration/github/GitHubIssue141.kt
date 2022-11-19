package com.apurebase.kgraphql.integration.github

import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.extract
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class GitHubIssue141 {

    sealed class TopUnion(val field: String) {
        class Union1(val names: List<String>) : TopUnion("union1")
        class Union2(val numbers: List<Int>) : TopUnion("union2")
    }

    @Test
    fun fragmentsOnUnions() {
        KGraphQL.schema {

            unionType<TopUnion>()

            query("unions") {
                resolver { isOne: Boolean ->
                    if (isOne) TopUnion.Union1(listOf("name1", "name2"))
                    else TopUnion.Union2(listOf(1, 2))
                }
            }

        }.executeBlocking(
            """
            {
                unions(isOne: true) {
                    ...abc
                }
            }
            fragment abc on TopUnion {
                ... on Union1 { names }
                ... on Union2 { numbers }
            }
        """.trimIndent()
        )
            .also(::println)
            .deserialize()
            .extract<List<String>>("data/unions/names") shouldBeEqualTo listOf("name1", "name2")
    }

}
