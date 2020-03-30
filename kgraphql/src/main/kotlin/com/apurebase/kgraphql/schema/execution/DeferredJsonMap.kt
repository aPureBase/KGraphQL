package com.apurebase.kgraphql.schema.execution

import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext

class DeferredJsonMap internal constructor(
    ctx: CoroutineContext
): CoroutineScope {

    internal val job = Job()
    override val coroutineContext = (ctx + job)

    private val deferredMap = mutableMapOf<String, Deferred<JsonElement>>()
    private val unDefinedDeferredMap = mutableMapOf<String, DeferredJsonMap>()
    private val moreJobs = mutableListOf<Deferred<Unit>>()
    private var completedMap: Map<String, JsonElement>? = null

    infix fun String.toValue(element: JsonElement) {
        this toDeferredValue CompletableDeferred(element)
    }

    infix fun String.toDeferredValue(element: Deferred<JsonElement>) {
        deferredMap[this] = element
    }

    suspend infix fun String.toDeferredObj(block: suspend DeferredJsonMap.() -> Unit) {
        if (unDefinedDeferredMap[this] != null) {
            val prevMap = unDefinedDeferredMap.getValue(this)
            block(prevMap)
        } else {
            val map = DeferredJsonMap(coroutineContext)
            block(map)
            unDefinedDeferredMap[this] = map
        }
    }

    suspend infix fun String.toDeferredArray(block: suspend DeferredJsonArray.() -> Unit) {
        val array = DeferredJsonArray(coroutineContext)
        block(array)
        this@toDeferredArray toDeferredValue array.asDeferred()
    }

    fun asDeferred() : Deferred<JsonElement> {
        return async(coroutineContext, start = CoroutineStart.LAZY) {
            awaitAll()
            build()
        }
    }

    suspend fun awaitAll() {
        check(completedMap == null) { "The deferred tree has already been awaited!" }

        (moreJobs + deferredMap.values).awaitAll()
        unDefinedDeferredMap.map { (key, map) ->
            key toDeferredValue map.asDeferred()
        }
        (moreJobs + deferredMap.values).awaitAll()
        completedMap = deferredMap.mapValues { it.value.await() }

        job.complete()
    }

    fun deferredLaunch(lazy: Boolean = true, block: suspend DeferredJsonMap.() -> Unit) {
        moreJobs.add(async(job, start = if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT) {
            block(this@DeferredJsonMap)
        })
    }

    fun build(): JsonObject {
        checkNotNull(completedMap) { "The deferred tree has not been awaited!" }
        return JsonObject(completedMap!!)
    }
}
