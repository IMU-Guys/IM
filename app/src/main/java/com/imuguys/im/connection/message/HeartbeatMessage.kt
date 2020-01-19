package com.imuguys.im.connection.message

import java.io.Serializable

/**
 * 客户端发出的心跳包
 */
data class HeartbeatMessage(val mTimeStamp : Long) :Serializable