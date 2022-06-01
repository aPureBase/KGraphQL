package com.apurebase.kgraphql.helpers

import com.apurebase.kgraphql.schema.execution.Execution
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * This returns a list of all scalar fields requested on this type.
 */
fun Execution.getFields(): List<String> = when (this) {
    is Execution.Fragment -> elements.flatMap(Execution::getFields)
    is Execution.Node -> {
        if (children.isEmpty()) listOf(key)
        else children
            .filterNot { (it is Execution.Node && it.children.isNotEmpty()) }
            .flatMap(Execution::getFields)

    }
    else -> listOf()
}.distinct()

/**
 * Collection : Convert to JsonElement
 */
fun Collection<*>.toJsonElement(): JsonElement {
    val list: MutableList<JsonElement> = mutableListOf()
    this.forEach {
        val value = it as? Any ?: return@forEach
        when(value) {
            is Number -> list.add(JsonPrimitive(value))
            is Boolean -> list.add(JsonPrimitive(value))
            is String -> list.add(JsonPrimitive(value))
            is Map<*, *> -> list.add((value).toJsonElement())
            is Collection<*> -> list.add(value.toJsonElement())
            is Array<*> -> list.add(value.toList().toJsonElement())
            else -> list.add(JsonPrimitive(value.toString())) // other type
        }
    }
    return JsonArray(list)
}

/**
 * Map : Convert to JsonElement
 */
fun Map<*, *>.toJsonElement(): JsonElement {
    val map: MutableMap<String, JsonElement> = mutableMapOf()
    this.forEach {
        val key = it.key as? String ?: return@forEach
        val value = it.value ?: return@forEach
        when(value) {
            is Number? -> map[key] = JsonPrimitive(value)
            is Boolean? -> map[key] = JsonPrimitive(value)
            is String? -> map[key] = JsonPrimitive(value)
            is Map<*, *> -> map[key] = (value).toJsonElement()
            is Collection<*> -> map[key] = value.toJsonElement()
            is Array<*> -> map[key] = value.toList().toJsonElement()
            else -> map[key] = JsonPrimitive(value.toString())  // other type
        }
    }
    return JsonObject(map)
}