package com.imuguys.im.connection.callback

/**
 * LongConnectionCallback 默认实现
 */
open class SimpleLongConnectionCallback : LongConnectionCallback {
    override fun onSuccess() {}

    override fun onFailed(throwable: Throwable) {}

    override fun onCancelled() {}
}
