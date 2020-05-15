package com.apurebase.kgraphql

import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.UnstableDefault
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class FieldsTest {
    data class Fields(val stuff: String, val moreStuff: String)

    @UnstableDefault
    @KtorExperimentalAPI
    @Test
    fun `Fields test`() {
        val server = withServer {
            query("actor") {
                resolver { ctx: Context ->
                    val fields = ctx.fields().joinToString(",")
                    Fields(stuff = fields, moreStuff = fields)
                }
            }
        }

        server {
            query {
                fieldObject("actor") {
                    field("stuff")
                    field("moreStuff")
                }
            }
        } shouldBeEqualTo "{\"data\":{\"actor\":{\"stuff\":\"stuff,moreStuff\",\"moreStuff\":\"stuff,moreStuff\"}}}"
    }
}