package com.imuguys.im.connection.message

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * 长链接传输的Json消息
 */
data class SocketJsonMessage(
    @SerializedName("classType") val classType: String,
    @SerializedName("classBytes") val classBytes: String
) : Serializable
