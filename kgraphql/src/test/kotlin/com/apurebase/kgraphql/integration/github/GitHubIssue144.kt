package com.apurebase.kgraphql.integration.github

import com.apurebase.kgraphql.KGraphQL
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GitHubIssue144 {

    data class Agenda(
        val date: LocalDate,
        val slots: List<Slots>,
        val hasSlotsAvailable: Boolean,
    )

    data class Slots(
        val hour: Int,
    )

    @Test
    fun simpleTestWithArgs() {
        // This should be able to execute without any problems
        KGraphQL.schema {
            stringScalar<LocalDate> {
                serialize = { date -> date.toString() }
                deserialize = { dateString -> LocalDate.parse(dateString) }
            }

            query("slots") {
                resolver { limit: Int, tags: List<String> ->
                    listOf(Agenda(date = LocalDate.now(), slots = listOf(Slots(1)), hasSlotsAvailable = true))
                }.withArgs {
                    arg<Int> { name = "limit"; defaultValue = 7 }
                    arg<List<String>> { name = "tags"; defaultValue = emptyList() }
                }
            }
        }.executeBlocking(
            """
            {
                slots {
                    date
                    hasSlotsAvailable
                    slots {
                        hour
                    }
                }
            }
        """.trimIndent()
        ).also(::println)

    }

}
