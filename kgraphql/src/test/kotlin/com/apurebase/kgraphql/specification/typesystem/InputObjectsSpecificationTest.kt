package com.apurebase.kgraphql.specification.typesystem

import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.extract
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class InputObjectsSpecificationTest {

    enum class MockEnum { M1, M2 }

    data class InputOne(val enum: MockEnum, val id: String)

    data class InputTwo(val one: InputOne, val quantity: Int, val tokens: List<String>)

    data class Circular(val ref: Circular? = null, val value: String? = null)

    val objectMapper = jacksonObjectMapper()

    val schema = KGraphQL.schema {
        enum<MockEnum>()
        inputType<InputTwo>()
        query("test") { resolver { input: InputTwo -> "success: $input" } }
    }

    @Test
    fun `An Input Object defines a set of input fields - scalars, enums, or other input objects`() {
        val two = object {
            val two = InputTwo(InputOne(MockEnum.M1, "M1"), 3434, listOf("23", "34", "21", "434"))
        }
        val variables = objectMapper.writeValueAsString(two)
        val response = deserialize(schema.executeBlocking("query(\$two: InputTwo!){test(input: \$two)}", variables))
        assertThat(response.extract<String>("data/test"), startsWith("success"))
    }

    @Test
    fun `Input objects may contain nullable circular references`() {
        val schema = KGraphQL.schema {
            inputType<Circular>()
            query("circular") {
                resolver { cir: Circular -> cir.ref?.value }
            }
        }

        val variables = object {
            val cirNull = Circular(Circular(null))
            val cirSuccess = Circular(Circular(null, "SUCCESS"))
        }
        val response = deserialize(
            schema.executeBlocking(
                "query(\$cirNull: Circular!, \$cirSuccess: Circular!){" +
                        "null: circular(cir: \$cirNull)" +
                        "success: circular(cir: \$cirSuccess)}",
                objectMapper.writeValueAsString(variables)
            )
        )
        assertThat(response.extract("data/success"), equalTo("SUCCESS"))
        assertThat(response.extract("data/null"), nullValue())
    }
}
