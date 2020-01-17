package com.imuguys.im.connection

/**
 * 监听相同Message类型的SocketMessageListener应当被组合到一起统一处理
 */
class SocketMessageListenerGroup(messageClassName: String) {
    val mMessageClassName: String = messageClassName
    var mSocketMessageListenerList = ArrayList<SocketMessageListener<*>>()
}
