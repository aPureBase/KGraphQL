package com.apurebase.kgraphql.specification.typesystem

import com.apurebase.kgraphql.*
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test


@Specification("3.1.5 Enums")
class EnumsSpecificationTest {

    enum class Coolness {
        NOT_COOL, COOL, TOTALLY_COOL
    }

    val schema = KGraphQL.schema {
        enum<Coolness> {
            description = "State of coolness"
            value(Coolness.COOL) {
                description = "really cool"
            }
        }

        query("cool") {
            resolver { cool: Coolness -> cool.toString() }
        }
    }

    @Test
    fun `string literals must not be accepted as an enum input`() {
        invoking {
            schema.executeBlocking("{cool(cool : \"COOL\")}")
        } shouldThrow GraphQLError::class withMessage "String literal '\"COOL\"' is invalid value for enum type Coolness"
    }

    @Test
    fun `string constants are accepted as an enum input`() {
        val response = deserialize(schema.executeBlocking("{cool(cool : COOL)}"))
        assertThat(response.extract<String>("data/cool"), equalTo("COOL"))
    }

}
