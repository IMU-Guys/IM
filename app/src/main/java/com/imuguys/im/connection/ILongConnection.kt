package com.imuguys.im.connection

import com.imuguys.im.connection.callback.LongConnectionCallback
import java.io.Serializable

/**
 * 长链接接口
 */
interface ILongConnection {
    /**
     * 发起连接
     */
    fun connect()

    /**
     * 发送消息
     * @param anyObject 发送的对象
     */
    fun sendMessage(anyObject: Serializable)

    /**
     * 发送消息
     * @param anyObject 发送的对象
     * @param longConnectionCallback 结果回调
     */
    fun sendMessage(anyObject: Serializable, longConnectionCallback: LongConnectionCallback)

    /**
     * 注册消息监听器
     * @param messageClassName 监听的消息对应的类名
     * @param messageListener 监听器
     */
    fun <Message> registerMessageHandler(
        messageClassName: String,
        messageListener: SocketMessageListener<Message>
    )

    /**
     * 断开连接
     */
    fun disconnect()
}
