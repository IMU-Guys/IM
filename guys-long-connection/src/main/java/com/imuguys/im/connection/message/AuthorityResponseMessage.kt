package com.imuguys.im.connection.message

import com.google.gson.annotations.SerializedName

data class AuthorityResponseMessage(@SerializedName("state") val state: Boolean)