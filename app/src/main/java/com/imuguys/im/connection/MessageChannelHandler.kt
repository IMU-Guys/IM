package com.imuguys.im.connection

import android.util.Log
import com.imuguys.im.connection.message.SocketJsonMessage
import com.imuguys.im.utils.Gsons
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.CharsetUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * Json处理
 * 处理收到的Json，通知MessageHandler
 */
// todo JsonObjectDecode解码结果还是ByteBuf，暂时先把泛型改成ByteBuf，后面通过修改解码器解决这个问题
class MessageChannelHandler : SimpleChannelInboundHandler<ByteBuf>() {

    companion object {
        private const val TAG = "MessageChannelHandler"
    }

    var mLongConnectionContext: LongConnectionContext? = null
    private val mSocketMessageHandlers = ConcurrentHashMap<String, SocketMessageListener<Any>>()

    /**
     * 连接断开，可能是由于服务端主动断开
     */
    override fun channelInactive(ctx: ChannelHandlerContext?) {
        mLongConnectionContext?.onDisconnectionSubject!!.onNext(false)
        super.channelInactive(ctx)
    }

    /**
     * 收到读事件后的处理
     */
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ByteBuf) {
        val socketJsonMessage = Gsons.GUYS_GSON.fromJson(
            msg.toString(CharsetUtil.UTF_8),
            SocketJsonMessage::class.java
        )
        val innerMessage =
            Gsons.GUYS_GSON.fromJson(
                socketJsonMessage.classBytes,
                Class.forName(socketJsonMessage.classType)
            )
        val targetSocketJsonMessage = mSocketMessageHandlers[socketJsonMessage.classType]
        if (targetSocketJsonMessage != null) {
            targetSocketJsonMessage.handleMessage(innerMessage)
        } else {
            Log.w(TAG, "no compatible listener for message type: " + socketJsonMessage.classType)
        }
    }

    /**
     * 添加消息观察者
     */
    @Suppress("UNCHECKED_CAST")
    fun <Message> addMessageListener(
        messageClassName: String,
        messageListener: SocketMessageListener<Message>
    ) {
        mSocketMessageHandlers[messageClassName] = messageListener as SocketMessageListener<Any>
    }
}
