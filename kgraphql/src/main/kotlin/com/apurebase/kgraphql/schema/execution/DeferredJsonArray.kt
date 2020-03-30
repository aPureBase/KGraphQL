package com.apurebase.kgraphql.schema.execution

import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.CoroutineContext

class DeferredJsonArray internal constructor(
    ctx: CoroutineContext
): CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext = (ctx + job)

    private val deferredArray = mutableListOf<Deferred<JsonElement>>()
    private var completedArray: List<JsonElement>? = null

    fun addValue(element: JsonElement) {
        addDeferredValue(CompletableDeferred(element))
    }

    fun addDeferredValue(element: Deferred<JsonElement>) {
        deferredArray.add(element)
    }

    suspend fun addDeferredObj(block: suspend DeferredJsonMap.() -> Unit) {
        val map = DeferredJsonMap(coroutineContext)
        block(map)
        addDeferredValue(map.asDeferred())
    }

    // TODO: Add support for this within the [DataLoaderPreparedRequestExecutor]
    suspend fun addDeferredArray(block: suspend DeferredJsonArray.() -> Unit) {
        val array = DeferredJsonArray(coroutineContext)
        block(array)
        addDeferredValue(array.asDeferred())
    }

    fun asDeferred() : Deferred<JsonElement> {
        return async(coroutineContext, start = CoroutineStart.LAZY) {
            awaitAll()
            build()
        }
    }

    suspend fun awaitAll() {
        check(completedArray == null) { "The deferred tree has already been awaited!" }
        completedArray = deferredArray.awaitAll()
        job.complete()
    }

    fun build(): JsonArray {
        checkNotNull(completedArray) { "The deferred tree has not been awaited!" }
        return JsonArray(completedArray!!)
    }
}
