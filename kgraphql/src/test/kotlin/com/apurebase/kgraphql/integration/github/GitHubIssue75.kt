package com.apurebase.kgraphql.integration.github

import com.apurebase.kgraphql.KGraphQL
import org.junit.jupiter.api.Test

class GitHubIssue75 {
    data class SearchHit(val spanData: Span)
    data class Tag(val key: String, val type: String, val value: String)
    data class LogPoint(val timestamp: Int, val fields: List<LogPointField>)
    data class LogPointField(val key: String, val value: String)
    data class Trace(val traceID: String, val spans: ArrayList<Span>) {
        companion object {
            fun fromSearchHits(traceID: String, hits: Array<SearchHit>): Trace {
                val spansArray = hits.map {
                    Span.fromSearchHit(
                        it
                    )
                }.toTypedArray<Span>()
                return Trace(
                    traceID,
                    ArrayList(spansArray.sortedBy { it.startTime })
                )
            }
        }
    }

    data class Span(
        val traceID: String,
        val spanID: String,
        val parentSpanID: String?,
        val duration: Int,
        val startTime: Long,
        val operationName: String,
        val serviceName: String,
        val logs: ArrayList<LogPoint>?,
        val tags: ArrayList<Tag>?,
    ) {
        companion object {
            fun fromSearchHit(hit: SearchHit) = hit.spanData
        }
    }

    @Test
    fun `issue-75 object is not of declaring class - full sample`() {
        val schema = KGraphQL.schema {
            configure {
                useDefaultPrettyPrinter = true
            }

            query("findTrace") {
                resolver { traceID: String ->
                    Trace(
                        traceID = "646851f15cb2dad1",
                        spans = arrayListOf(
                            Span(
                                traceID = "646851f15cb2dad1",
                                spanID = "32b1133c2e838c56",
                                parentSpanID = "646851f15cb2dad1",
                                duration = 1701547,
                                startTime = 1581974975400889,
                                operationName = "banana",
                                serviceName = "example1",
                                logs = arrayListOf(),
                                tags = arrayListOf(
                                    Tag(
                                        type = "string",
                                        value = "sample text",
                                        key = "_tracestep_stack"
                                    ),
                                    Tag(
                                        type = "bool",
                                        value = "true",
                                        key = "_tracestep_main"
                                    ),
                                    Tag(
                                        type = "string",
                                        value = "proto",
                                        key = "internal.span.format"
                                    )
                                )
                            ),
                            Span(
                                traceID = "646851f15cb2dad1",
                                spanID = "7381e3787bb621db",
                                parentSpanID = "32b1133c2e838c56",
                                duration = 1000503,
                                startTime = 1581974975401257,
                                operationName = "start-req2",
                                serviceName = "example2",
                                logs = arrayListOf(),
                                tags = arrayListOf(
                                    Tag(
                                        type = "string",
                                        value = "sample text",
                                        key = "_tracestep_stack"
                                    ),
                                    Tag(
                                        type = "bool",
                                        value = "false",
                                        key = "_tracestep_main"
                                    ),
                                    Tag(
                                        type = "string",
                                        value = "proto",
                                        key = "internal.span.format"
                                    )
                                )
                            )
                        )
                    )
                }.withArgs {
                    arg<String> { name = "traceID" }
                }
            }
        }

        schema.executeBlocking(
            """
            query findTrace(${'$'}traceID: String!) {
              findTrace(traceID: ${'$'}traceID) {
                traceID
                spans {
                  spanID
                  tags {
                    key
                    value
                    __typename
                  }
                  __typename
                }
                __typename
              }
            }
        """, "{\"traceID\": \"646851f15cb2dad1\"}"
        ).let(::println)
    }
}
