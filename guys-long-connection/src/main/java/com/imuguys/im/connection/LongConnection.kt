package com.imuguys.im.connection

import android.util.Log
import com.imuguys.im.connection.callback.LongConnectionCallback
import com.imuguys.im.connection.message.AuthorityMessage
import com.imuguys.im.connection.message.HeartbeatAckMessage
import com.imuguys.im.connection.op.ConnectOp
import com.imuguys.im.connection.op.DisconnectOp
import com.imuguys.im.connection.op.HeartbeatOp
import com.imuguys.im.connection.op.SendMessageOp
import io.reactivex.disposables.Disposable
import java.io.Serializable
import java.util.concurrent.TimeUnit

/**
 * 长链接实现类，支持传递Json
 */
class LongConnection(longConnectionParams: LongConnectionParams) : ILongConnection {

    companion object {
        const val TAG = "LongConnection"
    }

    private val mLongConnectionContext: LongConnectionContext =
        LongConnectionContext(longConnectionParams)
    private var mConnectFailedDisposable: Disposable? = null
    private val mConnectSuccessDisposable: Disposable
    private val mDisConnectDisposable: Disposable
    private val mHeartbeatOvertimeDisposable: Disposable

    // 心跳相关
    // 保证只有这一个心跳操作对象
    private val mHeartbeatOp: HeartbeatOp by lazy {
        HeartbeatOp(
            mLongConnectionContext,
            mHeartbeatListener
        )
    }
    // todo
    //  by lazy 会生成名为com.imuguys.im.connection.LongConnection$mHeartbeatListener$2的类
    //  from byte code -> public final static Lcom/imuguys/im/connection/LongConnection$mHeartbeatListener$2; INSTANCE
    private val mHeartbeatListener: HeartbeatMessageListener by lazy { HeartbeatMessageListener() }

    init {
        mLongConnectionContext.longConnectionTaskDispatcher = LongConnectionTaskDispatcher()
        // 连接建立成功，准备鉴权
        mConnectSuccessDisposable = mLongConnectionContext.onConnectSuccessSubject
            .subscribe {
                mConnectFailedDisposable?.dispose()
                sendMessage(AuthorityMessage("user","pwd"))
                startHeartbeat()
            }
        // 连接断开处理
        mDisConnectDisposable = mLongConnectionContext.onRemoteDisconnectSubject.subscribe {
            Log.i(TAG, "remote peer reset this connection!, stop heartbeat, disconnect connection")
            stopHeartbeat()
            disconnect()
        }
        mHeartbeatOvertimeDisposable = mLongConnectionContext.onHeartbeatOvertime.subscribe {
            stopHeartbeat()
            connect()
        }
    }

    private fun startHeartbeat() {
        registerMessageHandler(
            HeartbeatAckMessage::class.java.name,
            mHeartbeatListener
        )
        mLongConnectionContext.longConnectionTaskDispatcher.postRunnable(mHeartbeatOp)
    }

    private fun stopHeartbeat() {
        mLongConnectionContext.longConnectionTaskDispatcher.removeRunnable(mHeartbeatOp)
    }

    override fun sendMessage(anyObject: Serializable) {
        mLongConnectionContext.longConnectionTaskDispatcher.postRunnable(
            SendMessageOp(mLongConnectionContext, anyObject)
        )
    }

    override fun sendMessage(
        anyObject: Serializable,
        longConnectionCallback: LongConnectionCallback
    ) {
        mLongConnectionContext.longConnectionTaskDispatcher.postRunnable(
            SendMessageOp(mLongConnectionContext, anyObject, longConnectionCallback)
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
        mConnectFailedDisposable?.dispose()
        mConnectSuccessDisposable.dispose()
        mLongConnectionContext.release()
        mLongConnectionContext.longConnectionTaskDispatcher.postRunnable(
            DisconnectOp(mLongConnectionContext)
        )
    }

    override fun connect() {
        // 重连次数
        val attemptToReconnectionCountLimit =
            mLongConnectionContext.longConnectionParams.attemptToReconnectionCountLimit
        // 重连间隔
        val attemptToReconnectionIntervalLimitMs =
            mLongConnectionContext.longConnectionParams.attemptToReconnectionIntervalLimitMs
        Log.i(TAG, "connect")
        // 重连处理，3秒间隔，5次重连
        mConnectFailedDisposable?.dispose()
        mConnectFailedDisposable = mLongConnectionContext.onConnectFailedSubject
            .filter { mLongConnectionContext.reConnectCount <= attemptToReconnectionCountLimit }
            .delay(attemptToReconnectionIntervalLimitMs, TimeUnit.MILLISECONDS)
            .subscribe {
                Log.i(TAG, "reconnect")
                mLongConnectionContext.longConnectionTaskDispatcher.postRunnable(
                    ConnectOp(mLongConnectionContext)
                )
            }
        mLongConnectionContext.longConnectionTaskDispatcher.postRunnable(
            ConnectOp(mLongConnectionContext)
        )
    }
}
