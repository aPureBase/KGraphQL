package com.apurebase.kgraphql.integration

import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.defaultSchema
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test


class LongScalarTest {

    @Test
    fun testLongField() {
        val schema = defaultSchema {
            query("long") {
                resolver { -> Long.MAX_VALUE }
            }
        }

        val response = schema.executeBlocking("{long}")
        val long = deserialize(response).extract<Long>("data/long")
        assertThat(long, equalTo(Long.MAX_VALUE))
    }

    @Test
    fun testLongArgument() {
        val schema = defaultSchema {
            query("isLong") {
                resolver { long: Long -> if (long > Int.MAX_VALUE) "YES" else "NO" }
            }
        }

        val isLong =
            deserialize(schema.executeBlocking("{isLong(long: ${Int.MAX_VALUE.toLong() + 1})}")).extract<String>("data/isLong")
        assertThat(isLong, equalTo("YES"))
    }

    data class VeryLong(val long: Long)

    @Test
    fun `Schema may declare custom long scalar type`() {
        val schema = KGraphQL.schema {
            longScalar<VeryLong> {
                deserialize = ::VeryLong
                serialize = { (long) -> long }
            }

            query("number") {
                resolver { number: VeryLong -> number }
            }
        }

        val value = Int.MAX_VALUE.toLong() + 2
        val response = deserialize(schema.executeBlocking("{number(number: $value)}"))
        assertThat(response.extract<Long>("data/number"), equalTo(value))
    }

}
