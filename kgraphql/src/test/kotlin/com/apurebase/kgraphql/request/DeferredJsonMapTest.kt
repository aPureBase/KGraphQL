package com.apurebase.kgraphql.request

import com.apurebase.kgraphql.schema.execution.deferredJsonBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.json
import org.amshove.kluent.shouldEqualUnordered
import org.junit.jupiter.api.RepeatedTest


class DeferredJsonMapTest {

    private fun log(lvl: Int, msg: String) = println("[test-$lvl]: $msg")


    @RepeatedTest(50)
    fun `No coroutines should be leaking`() = runBlockingTest {
        val def1 = CompletableDeferred<JsonElement>()
        val def2 = CompletableDeferred<JsonElement>()
        val def3 = CompletableDeferred<JsonElement>()


        log(0, "initializing builder")

        val mapJob = async {
            deferredJsonBuilder {
                "hello" toDeferredValue def1
                log(1, "initializing toDeferredObj")
                "extra" toDeferredObj {
                    log(2, "initializing toDeferredObj")
                    "v1" toDeferredValue def2
                    "v2" toDeferredValue def3
                }
            }
        }

        launch {
            def1.complete(JsonPrimitive("world"))
            def2.complete(JsonPrimitive("new world"))
            launch {
                def3.complete(JsonPrimitive(""))
            }
        }

        log(0, "completing builder")
        mapJob.await() shouldEqualUnordered json {
            "hello" to "world"
            "extra" to json {
                "v1" to "new world"
                "v2" to ""
            }
        }
    }


}


