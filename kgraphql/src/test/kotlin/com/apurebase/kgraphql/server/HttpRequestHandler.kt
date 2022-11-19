package com.apurebase.kgraphql.server

import com.apurebase.kgraphql.schema.DefaultSchema
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.AsciiString
import java.nio.charset.Charset
import java.util.logging.Level
import java.util.logging.Logger

/**
 * TODO: refactor and splitOperationsAndFragments to two handlers : for queries and for API docs
 */
@ChannelHandler.Sharable
class HttpRequestHandler(val schema: DefaultSchema) : SimpleChannelInboundHandler<FullHttpRequest>() {

    val logger: Logger = Logger.getLogger(HttpRequestHandler::class.qualifiedName)

    val objectMapper = ObjectMapper()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        when {
            msg.uri().startsWith("/graphql") -> handleQuery(ctx, msg)
            else -> exceptionCaught(ctx, IllegalArgumentException("Invalid path"))
        }
    }

    private fun handleQuery(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        val content = msg.content().toString(Charset.defaultCharset())
        val query = objectMapper.readTree(content)["query"].textValue()
            ?: throw IllegalArgumentException("Please specify only one query")
        try {
            val response = schema.executeBlocking(query, null)
            writeResponse(ctx, response)
        } catch (e: Exception) {
            writeResponse(
                ctx,
                "{\"errors\" : { \"message\": \"Caught ${e.javaClass.canonicalName}: ${
                    e.message?.replace(
                        "\"",
                        "\\\""
                    )
                }\"}}"
            )
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.log(Level.INFO, cause.message)
        writeErrorMessage(cause, ctx)
    }

    private fun writeResponse(
        ctx: ChannelHandlerContext,
        response: String,
        contentType: AsciiString = HttpHeaderValues.APPLICATION_JSON,
    ) {

        val httpResponse = io.netty.handler.codec.http.DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.wrappedBuffer(response.toByteArray())
        )

        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes())
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType)

        ctx.writeAndFlush(httpResponse)
    }

    private fun writeErrorMessage(cause: Throwable, ctx: ChannelHandlerContext) {
        val httpResponse = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            Unpooled.wrappedBuffer(objectMapper.writeValueAsBytes(cause.message))
        )

        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes())
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE)
    }
}
