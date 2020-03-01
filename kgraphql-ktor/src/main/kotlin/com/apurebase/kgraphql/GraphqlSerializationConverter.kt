package com.apurebase.kgraphql

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.serialization.DefaultJsonConfiguration
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.serialization.json.*
import java.nio.charset.Charset

@KtorExperimentalAPI
class GraphqlSerializationConverter(private val json: Json = Json(DefaultJsonConfiguration)) : ContentConverter {
    override suspend fun convertForSend(
            context: PipelineContext<Any, ApplicationCall>,
            contentType: ContentType,
            value: Any
    ): Any? {
        @Suppress("UNCHECKED_CAST")
        val content = json.stringify(GraphqlRequest.serializer(), value as GraphqlRequest)
        return TextContent(content, contentType.withCharset(context.call.suitableCharset()))
    }

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        fun ContentType.defaultCharset(): Charset = when (this) {
            ContentType.Application.Json -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        // Kotlinx.serialization does not accept a string attribute as null
        //fun String.removeNullFromOperationName(): String = this.replace("\"operationName\": null", "\"operationName\": \"\"")
        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null
        //val charset = context.call.request.contentCharset() ?: Charsets.UTF_8
        //val content = channel.readRemaining().readText(charset)

        val contentType = context.call.request.contentType()
        val suitableCharset = contentType.charset() ?: contentType.defaultCharset()
        val content = channel.readRemaining().readText(suitableCharset)

        return json.parse(GraphqlRequest.serializer(), content)
    }

}

