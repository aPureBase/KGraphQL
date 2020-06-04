package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.execution.Execution
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.UnstableDefault
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class FieldsTest {
    data class Node(val stuff: String, val moreStuff: String)

    @UnstableDefault
    @KtorExperimentalAPI
    @Test
    fun `All fields can be read is case of a single entity`() {
        val server = withServer {
            query("actor") {
                resolver { ctx: Context ->
                    val nodes = ctx.nodes().flatMap {
                        it.children
                    }.mapNotNull {
                        when (it) {
                            is Execution.Node -> it
                            else -> null
                        }
                    }.map {
                        it.aliasOrKey
                    }

                    Node(nodes.toString(), nodes.toString())
                }
            }
        }

        server {
            query {
                fieldObject("actor") {
                    field("stuff")
                    field("moreStuff")
                }
            }
        } shouldBeEqualTo "{\"data\":{\"actor\":{\"stuff\":\"[stuff, moreStuff]\",\"moreStuff\":\"[stuff, moreStuff]\"}}}"
    }
}