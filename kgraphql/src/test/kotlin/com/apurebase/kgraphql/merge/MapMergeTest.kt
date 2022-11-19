package com.apurebase.kgraphql.merge

import com.apurebase.kgraphql.expect
import com.apurebase.kgraphql.schema.execution.merge
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class MapMergeTest {
    private val jsonNodeFactory = JsonNodeFactory.instance

    @Test
    fun `merge should add property`() {
        val existing = createMap("param1" to jsonNodeFactory.textNode("value1"))
        val update: JsonNode? = jsonNodeFactory.textNode("value2")

        existing.merge("param2", update)

        assertThat(existing.get("param2"), equalTo(update))
    }

    @Test
    fun `merge should add nested property`() {
        val existing = createMap("param1" to jsonNodeFactory.textNode("value1"))
        val update: JsonNode? = jsonNodeFactory.objectNode().put("param2", "value2")

        existing.merge("sub", update)

        assertThat(existing.get("sub"), equalTo(update))
    }

    @Test
    fun `merge should not change simple node`() {
        val existingValue: JsonNode? = jsonNodeFactory.textNode("value1")
        val existing = createMap("param" to existingValue)
        val update = jsonNodeFactory.textNode("value2")

        expect<IllegalStateException>("different simple nodes") { existing.merge("param", update) }

        assertThat(existing.get("param"), equalTo(existingValue))
    }

    @Test
    fun `merge should not merge simple node with object node`() {
        val existingValue: JsonNode? = jsonNodeFactory.textNode("value1")
        val existing = createMap("param" to existingValue)
        val update = jsonNodeFactory.objectNode()

        expect<IllegalStateException>("merge object with simple node") { existing.merge("param", update) }

        val expected: JsonNode? = jsonNodeFactory.textNode("value1")
        assertThat(existing.get("param"), equalTo(expected))
    }

    @Test
    fun `merge should not merge object node with simple node`() {
        val existingObj: JsonNode? = jsonNodeFactory.objectNode().put("other", "value1")
        val existing = createMap("param" to existingObj)
        val update = jsonNodeFactory.textNode("value2")

        expect<IllegalStateException>("merge simple node with object node") { existing.merge("param", update) }

        assertThat(existing.get("param"), equalTo(existingObj))
    }

    private fun createMap(vararg pairs: Pair<String, JsonNode?>) = mutableMapOf(*pairs)
}
