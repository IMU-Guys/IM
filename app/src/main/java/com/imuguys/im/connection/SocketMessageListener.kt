package com.imuguys.im.connection

/**
 * Socket消息回调
 */
interface SocketMessageListener<Message> {
    fun handleMessage(message: Message)
}
