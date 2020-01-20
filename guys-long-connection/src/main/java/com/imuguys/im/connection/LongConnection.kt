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
class LongConnection : ILongConnection {

    companion object {
        const val TAG = "LongConnection"
    }

    // todo LongConnectionContext 从外面传入进来
    private val mLongConnectionContext: LongConnectionContext =
        LongConnectionContext("172.17.236.130", 8880)
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
                sendMessage(AuthorityMessage("nice"))
                startHeartbeat()
            }
        // 连接断开处理
        mDisConnectDisposable = mLongConnectionContext.onDisconnectionSubject.subscribe()
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
        // 停止心跳
        stopHeartbeat()
        mConnectFailedDisposable?.dispose()
        mConnectSuccessDisposable.dispose()
        mLongConnectionContext.release()
        mLongConnectionContext.longConnectionTaskDispatcher.postRunnable(
            DisconnectOp(mLongConnectionContext)
        )
    }

    override fun connect() {
        Log.i(TAG, "connect")
        // 重连处理，3秒间隔，5次重连
        mConnectFailedDisposable?.dispose()
        // todo 把重连次数和间隔放入到LongConnectionContext
        mConnectFailedDisposable = mLongConnectionContext.onConnectFailedSubject
            .filter { mLongConnectionContext.reConnectCount <= 5 }
            .delay(3L, TimeUnit.SECONDS)
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
