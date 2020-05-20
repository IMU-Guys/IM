package com.imuguys.im.connection

/**
 * 监听相同Message类型的SocketMessageListener组
 */
class SocketMessageListenerGroup<Message>(messageClassName: String) :
    SocketMessageListener<Message> {
    override fun handleMessage(message: Message) {
        mSocketMessageListenerSet.forEach {
            it.handleMessage(message)
        }
    }

    val mMessageClassName: String = messageClassName
    var mSocketMessageListenerSet = HashSet<SocketMessageListener<Message>>()

    fun addMessageListener(messageListener: SocketMessageListener<Message>) {
        mSocketMessageListenerSet.add(messageListener)
    }

    fun removeMessageListener(messageListener: SocketMessageListener<Message>) {
        mSocketMessageListenerSet.remove(messageListener)
    }
}
