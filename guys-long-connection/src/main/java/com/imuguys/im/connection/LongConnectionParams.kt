package com.imuguys.im.connection;

/**
 * 长链接参数
 */
// todo 因为生成的Getter/Setter会带着M，所有成员没有用m开头，以后处理
data class LongConnectionParams(
    val host: String, // 主机名
    val port: Int, // 端口
    val attemptToReconnectionCountLimit: Int = 5, // 建立连接失败后的重连次数
    val attemptToReconnectionIntervalLimitMs: Long = 3000, // 重连间隔
    val connectTimeOut: Int = 10 * 1000 // 建立连接超时时间
)
