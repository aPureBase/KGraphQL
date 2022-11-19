package com.apurebase.kgraphql.specification.language

import com.apurebase.kgraphql.*
import com.apurebase.kgraphql.Actor
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test

@Specification("2.5 Fields")
class FieldsSpecificationTest {

    data class ActorWrapper(val id: String, val actualActor: Actor)

    val age = 432

    val schema = defaultSchema {
        query("actor") {
            resolver { -> ActorWrapper("BLinda", Actor("Boguś Linda", age)) }
        }
    }

    @Test
    fun `field may itself contain a selection set`() {
        val response = deserialize(schema.executeBlocking("{actor{id, actualActor{name, age}}}"))
        val map = response.extract<Map<String, Any>>("data/actor/actualActor")
        MatcherAssert.assertThat(map, CoreMatchers.equalTo(mapOf("name" to "Boguś Linda", "age" to age)))
    }
}

