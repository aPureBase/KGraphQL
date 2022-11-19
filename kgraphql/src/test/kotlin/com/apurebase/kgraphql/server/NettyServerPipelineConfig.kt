package com.apurebase.kgraphql.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder

class NettyServerPipelineConfig(val httpRequestHandler: HttpRequestHandler) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()
        //handle HTTP
        pipeline.addLast(HttpRequestDecoder())
        pipeline.addLast(HttpObjectAggregator(10 * 1024 * 1024))
//        pipeline.addLast(HttpResponseEncoder())
        //handle http query
        pipeline.addLast(httpRequestHandler)
    }
}
