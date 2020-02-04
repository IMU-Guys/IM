package com.imuguys.im.connection.message

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * 测试用鉴权信息
 */
data class AuthorityMessage(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
) : Serializable