package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.request.receive
import io.ktor.serialization.serialization
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

const val GRAPHQL_ENDPOINT = "graphql"

@KtorExperimentalAPI
@UnstableDefault
fun Route.graphql(ctxBuilder: ContextBuilder.(ApplicationCall) -> Unit = {}, block: SchemaBuilder.() -> Unit) {
    val schema = KGraphQL.schema(block)
    install(ContentNegotiation) {
        register(ContentType.Application.Json, GraphqlSerializationConverter())
        serialization(json = Json.nonstrict)
    }
    post(GRAPHQL_ENDPOINT) {
        val request = call.receive<GraphqlRequest>()
        val ctx = context { ctxBuilder(this, call) }
        val result = schema.execute(request.query, request.variables.toString(), ctx)
        call.respond(result)
    }
}