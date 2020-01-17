package com.imuguys.im.connection

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.json.JsonObjectDecoder
import io.netty.handler.codec.string.StringEncoder

/**
 * 添加Netty Handler
 * 包括Json解码和Json处理
 */
class MessageChannelHandlerInitializer : ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel?) {
        ch!!.let {
            ch.pipeline().addLast(JsonObjectDecoder())
            ch.pipeline().addLast(StringEncoder())
            ch.pipeline().addLast(MessageChannelHandler())
        }
    }
}
