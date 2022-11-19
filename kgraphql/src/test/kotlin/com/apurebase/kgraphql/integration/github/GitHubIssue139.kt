package com.apurebase.kgraphql.integration.github

import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("This is an example of what we would like to work")
class GitHubIssue139 {

    interface Criteria {
        val first: Int
    }

    interface Repository<T, C : Criteria> {
        fun get(criteria: C): List<T>
        fun get(): List<T>
        fun count(): Int
    }

    data class Criteria1(override val first: Int) : Criteria
    data class Criteria2(override val first: Int) : Criteria
    data class Criteria3(override val first: Int) : Criteria

    data class Type1(val value: String)
    data class Type2(val value: String)
    data class Type3(val value: String)

    object Repo1 : Repository<Type1, Criteria1> {
        override fun get(criteria: Criteria1) = listOf(Type1("Hello"))
        override fun get() = get(Criteria1(5))
        override fun count() = 0
    }

    object Repo2 : Repository<Type2, Criteria2> {
        override fun get(criteria: Criteria2) = emptyList<Type2>()
        override fun get() = get(Criteria2(5))
        override fun count() = 0
    }

    object Repo3 : Repository<Type3, Criteria3> {
        override fun get(criteria: Criteria3) = emptyList<Type3>()
        override fun get() = get(Criteria3(5))
        override fun count() = 0
    }

    private inline fun <reified T : Any, reified C : Criteria, R : Repository<T, C>> SchemaBuilder.connection(
        resourceName: String,
        repo: R,
    ) {
        query("${resourceName}s") {
            resolver { criteria: C -> repo.get(criteria) }.returns<List<T>>()
        }
        query("${resourceName}Count") {
            resolver { -> repo.count() }
        }
        type<T> {
            property<Int>("hash") {
                resolver { it.hashCode() }
            }
        }
    }

    @Test
    fun `custom factory definitions`() {
        KGraphQL.schema {
            connection("item", Repo1)
            connection("element", Repo2)
            connection("thing", Repo3)
        }.executeBlocking(
            """
            {
                itemsCount
                items(criteria: {first:5}) {
                    value
                    hash
                }
            }
        """.trimIndent()
        )
            .also(::println)
            .deserialize()
            .let(::println)
    }
}
