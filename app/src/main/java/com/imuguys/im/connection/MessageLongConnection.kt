package com.imuguys.im.connection

import com.imuguys.im.connection.message.AuthorityMessage
import com.imuguys.im.connection.op.ConnectOp
import com.imuguys.im.connection.op.SendMessageOp
import io.reactivex.disposables.Disposable
import java.io.Serializable
import java.util.concurrent.TimeUnit

/**
 * 长链接实现类，支持传递Json
 */
class MessageLongConnection : IMessageLongConnection {

    companion object {
        const val TAG = "MessageLongConnection"
    }

    // todo 不要暴露敏感信息
    private val mLongConnectionContext: LongConnectionContext =
        LongConnectionContext("0", 0)
    private val mConnectFailedDisposable: Disposable
    private val mConnectSuccessDisposable: Disposable
    private val mDisConnectDisposable: Disposable

    init {
        mLongConnectionContext.longConnectionTaskDispatcher = LongConnectionTaskDispatcher()
        // 重连处理，3秒间隔，5次重连
        mConnectFailedDisposable = mLongConnectionContext.onConnectFailedSubject
            .filter { mLongConnectionContext.reConnectCount <= 5 }
            .delay(3L, TimeUnit.SECONDS)
            .subscribe { connect() }
        // 连接建立成功，准备鉴权
        mConnectSuccessDisposable = mLongConnectionContext.onConnectSuccessSubject
            .subscribe { sendMessage(AuthorityMessage("nice")) }
        // 连接断开处理
        mDisConnectDisposable = mLongConnectionContext.onDisconnectionSubject.subscribe()
    }

    override fun sendMessage(anyObject: Serializable) {
        mLongConnectionContext.longConnectionTaskDispatcher.postRunnable(
            SendMessageOp(mLongConnectionContext, anyObject)
        )
    }

    override fun <Message> registerMessageHandler(
        messageClassName: String,
        messageListener: SocketMessageListener<Message>
    ) {
        mLongConnectionContext.registerMessageListener(messageClassName, messageListener)
        mLongConnectionContext.connectionClient?.let {
            mLongConnectionContext.registerMessageListenerToChannelHandler()
        }
    }

    override fun disconnect() {
        mConnectFailedDisposable.dispose()
        mConnectSuccessDisposable.dispose()
        mLongConnectionContext.release()
    }

    override fun connect() {
        mLongConnectionContext.longConnectionTaskDispatcher.postRunnable(
            ConnectOp(mLongConnectionContext)
        )
    }
}
