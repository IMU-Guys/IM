package com.imuguys.im.connection.message

import java.io.Serializable

/**
 * 服务端的心跳响应包
 */
data class HeartbeatAckMessage(val mTimeStamp : Long) : Serializable