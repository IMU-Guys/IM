package com.imuguys.im.connection

import java.io.Serializable

/**
 * 长链接接口
 */
interface IMessageLongConnection {
    fun connect()
    fun sendMessage(anyObject: Serializable)
    fun <Message> registerMessageHandler(
        messageClassName: String,
        messageListener: SocketMessageListener<Message>
    )
    fun disconnect()
}
