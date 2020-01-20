package com.imuguys.im.connection.callback;

/**
 * Netty通用回调
 * 回调发生在NettyIO线程，后续考虑改成主线程
 */
interface LongConnectionCallback {
    /**
     * 成功
     */
    fun onSuccess()

    /**
     * 失败
     * @param throwable 异常
     */
    fun onFailed(throwable: Throwable)

    /**
     * 取消
     */
    fun onCancelled()
}
