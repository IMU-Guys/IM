package com.imuguys.im.connection

import com.imuguys.im.connection.codec.SocketJsonMessageCodec
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
            ch.pipeline().addLast(JsonObjectDecoder()) // ByteBuf to Json ByteBuf
            ch.pipeline().addLast(StringEncoder()) // Json String to ByteBuf
            ch.pipeline().addLast(SocketJsonMessageCodec()) // Json ByteBuf to POJO && POJO to Json String
            ch.pipeline().addLast(MessageChannelHandler())
        }
    }
}
