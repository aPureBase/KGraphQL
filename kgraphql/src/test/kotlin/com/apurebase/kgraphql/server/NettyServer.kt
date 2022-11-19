package com.apurebase.kgraphql.server

import com.apurebase.kgraphql.schema.DefaultSchema
import com.apurebase.kgraphql.schema.Schema
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LoggingHandler

class NettyServer {

    companion object {

        fun run(schema: Schema, port: Int) {

            val workerGroup = NioEventLoopGroup()
            try {
                val channel = ServerBootstrap()
                    .channel(NioServerSocketChannel::class.java)
                    .localAddress(port)
                    .group(workerGroup)
                    .handler(LoggingHandler())
                    .childHandler(NettyServerPipelineConfig(HttpRequestHandler(schema as DefaultSchema)))
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .bind().syncUninterruptibly().channel()

                channel.closeFuture().syncUninterruptibly()
            } finally {
                workerGroup.shutdownGracefully()
            }
        }
    }
}