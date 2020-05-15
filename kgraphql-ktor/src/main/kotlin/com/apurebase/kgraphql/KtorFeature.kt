package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.response.respondFile
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.serialization.json
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

@KtorExperimentalAPI
@UnstableDefault
fun Route.graphql(ctxBuilder: ContextBuilder.(ApplicationCall) -> Unit = {}, block: SchemaBuilder.() -> Unit) {
    val schema = KGraphQL.schema(block)
    val conf = schema.configuration[KtorGraphQLConfiguration::class] ?: KtorConfigurationDSL().build()


    install(ContentNegotiation) {
        val jsonConfig = Json(JsonConfiguration.Stable.copy(
                isLenient = true,
                ignoreUnknownKeys = true,
                serializeSpecialFloatingPointValues = true,
                useArrayPolymorphism = true
        ))
        register(ContentType.Application.Json, GraphqlSerializationConverter(jsonConfig))
        json(json = jsonConfig)
    }

    post(conf.endpoint) {
        val request = call.receive<GraphqlRequest>()
        val ctx = context { ctxBuilder(this, call) }
        val result = schema.execute(request.query, request.variables.toString(), ctx)
        call.respond(result)
    }

    if (conf.playground) get(conf.endpoint) {
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        call.respondBytes(KtorGraphQLConfiguration::class.java.classLoader.getResource("playground.html").readBytes(),
                contentType = ContentType.Text.Html)
    }

}
