package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.routing.routing
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.UnstableDefault
import me.lazmaid.kraph.Kraph
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class KtorFeatureTest {

    @UnstableDefault
    @KtorExperimentalAPI
    private fun withServer(ctx: Context? = null, block: SchemaBuilder.() -> Unit): (Kraph.() -> Unit) -> String {
        return {
            withTestApplication({
                routing {
                    graphql(ctx) { block() }
                }
            }) {
                handleRequest {
                    uri = GRAPHQL_ENDPOINT
                    method = HttpMethod.Post
                    addHeader(HttpHeaders.ContentType, "application/json;charset=UTF-8")
                    setBody(Kraph { it(this) }.toRequestString())
                }.response.content!!
            }
        }
    }

    @UnstableDefault
    @KtorExperimentalAPI
    @Test
    fun `Simple query test`() {
        val server = withServer {
            query("hello") {
                resolver { -> "World!" }
            }
        }

        server {
            query {
                field("hello")
            }
        } shouldBeEqualTo "{\"data\":{\"hello\":\"World!\"}}"

    }

    @UnstableDefault
    @KtorExperimentalAPI
    @Test
    fun `Simple mutation test`() {
        val server = withServer {
            mutation("hello") {
                resolver { -> "World! mutation" }
            }
        }

        server {
            mutation {
                field("hello")
            }
        } shouldBeEqualTo "{\"data\":{\"hello\":\"World! mutation\"}}"

    }

    data class Actor(val name : String, val age: Int)
    data class UserData(val username: String, val stuff: String)

    @UnstableDefault
    @KtorExperimentalAPI
    @Test
    fun `Simple context test`() {
        val georgeName = "George"
        val context = context {
            + UserData(georgeName, "STUFF")
            inject("ADA")
        }
        val server = withServer(context) {
                query("actor") {
                    resolver { -> Actor("George", 23) }
                }

                type<Actor> {
                    property<String>("nickname"){
                        resolver { _: Actor, ctx: Context -> "Hodor and ${ctx.get<UserData>()?.username}" }
                    }

                    transformation(Actor::name) { name: String, addStuff: Boolean?, ctx: Context ->
                        if(addStuff == true) name + ctx[UserData::class]?.stuff  else name
                    }
                }
        }

        server {
            query {
                fieldObject("actor") {
                    field("name(addStuff: true)")
                }
            }
        } shouldBeEqualTo "{\"data\":{\"actor\":{\"name\":\"${georgeName}STUFF\"}}}"

        server {
            query {
                fieldObject("actor") {
                    field("nickname")
                }
            }
        } shouldBeEqualTo "{\"data\":{\"actor\":{\"nickname\":\"Hodor and $georgeName\"}}}"
    }

    enum class MockEnum { M1, M2 }

    data class InputOne(val enum: MockEnum, val id : String)

    data class InputTwo(val one : InputOne, val quantity : Int, val tokens : List<String>)

    @UnstableDefault
    @KtorExperimentalAPI
    @Test
    fun `Simple variables test`() {
        val server = withServer {
            enum<MockEnum>()
            inputType<InputTwo>()
            query("test"){ resolver { input: InputTwo -> "success: $input" } }
        }

        val variables = """
            {"one":{"enum":"M1","id":"M1"},"quantity":3434,"tokens":["23","34","21","434"]}
        """.trimIndent()

        server {
            query {
                field("test(input: \$two)")
                variable("two", "InputTwo!", variables)
            }
        } shouldBeEqualTo "{\"data\":{\"test\":\"success: InputTwo(one=InputOne(enum=M1, id=M1), quantity=3434, tokens=[23, 34, 21, 434])\"}}"
    }

}
