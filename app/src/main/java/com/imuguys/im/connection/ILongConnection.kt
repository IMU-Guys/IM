package com.imuguys.im.connection

import java.io.Serializable

/**
 * 长链接接口
 */
interface ILongConnection {
    fun connect()
    fun sendMessage(anyObject: Serializable)
    fun <Message> registerMessageHandler(
        messageClassName: String,
        messageListener: SocketMessageListener<Message>
    )
    fun disconnect()
}
