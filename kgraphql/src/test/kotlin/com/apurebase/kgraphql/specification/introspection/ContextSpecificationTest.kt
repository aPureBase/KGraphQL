package com.apurebase.kgraphql.specification.introspection

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.defaultSchema
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.extract
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test

class ContextSpecificationTest {

    @Test
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    fun `query resolver should not return context param`() {
        val schema = defaultSchema {
            query("sample") {
                resolver { ctx: Context, limit: Int -> "SAMPLE" }
            }
        }

        val response = deserialize(schema.executeBlocking("{__schema{queryType{fields{args{name}}}}}"))
        println("response: $response")
        MatcherAssert.assertThat(
            response.extract("data/__schema/queryType/fields[0]/args[0]/name"),
            CoreMatchers.equalTo("limit")
        )
    }

    @Test
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    fun `mutation resolver should not return context param`() {
        val schema = defaultSchema {
            mutation("sample") {
                resolver { ctx: Context, input: String -> "SAMPLE" }
            }
        }

        val response = deserialize(schema.executeBlocking("{__schema{mutationType{fields{args{name}}}}}"))
        println("response: $response")
        MatcherAssert.assertThat(
            response.extract("data/__schema/mutationType/fields[0]/args[0]/name"),
            CoreMatchers.equalTo("input")
        )
    }
}
