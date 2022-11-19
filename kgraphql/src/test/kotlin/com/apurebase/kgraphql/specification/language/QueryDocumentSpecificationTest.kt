package com.apurebase.kgraphql.specification.language

import com.apurebase.kgraphql.*
import com.apurebase.kgraphql.Actor
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

@Specification("2.2 Query Document")
class QueryDocumentSpecificationTest {

    val schema = defaultSchema {
        query("fizz") {
            resolver { -> "buzz" }
        }

        mutation("createActor") {
            resolver { name: String -> Actor(name, 11) }
        }
    }

    @Test
    fun `anonymous operation must be the only defined operation`() {
        invoking {
            deserialize(schema.executeBlocking("query {fizz} mutation BUZZ {createActor(name : \"Kurt Russel\"){name}}"))
        } shouldThrow GraphQLError::class withMessage "anonymous operation must be the only defined operation"
    }

    @Test
    fun `must provide operation name when multiple named operations`() {
        invoking {
            deserialize(schema.executeBlocking("query FIZZ {fizz} mutation BUZZ {createActor(name : \"Kurt Russel\"){name}}"))
        } shouldThrow GraphQLError::class withMessage "Must provide an operation name from: [FIZZ, BUZZ]"
    }

    @Test
    fun `execute operation by name in variable`() {
        val map = deserialize(
            schema.executeBlocking(
                "query FIZZ {fizz} mutation BUZZ {createActor(name : \"Kurt Russel\"){name}}",
                "{\"operationName\":\"FIZZ\"}"
            )
        )
        assertNoErrors(map)
        assertThat(map.extract<String>("data/fizz"), equalTo("buzz"))
    }
}
