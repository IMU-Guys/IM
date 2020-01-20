package com.imuguys.im.connection.op;

import android.util.Log;

import com.imuguys.im.connection.HeartbeatMessageListener;
import com.imuguys.im.connection.LongConnectionContext;
import com.imuguys.im.connection.message.HeartbeatMessage;

/**
 * 发送心跳操作，心跳超时回调
 */
public class HeartbeatOp implements Runnable {
  private static final String TAG = "HeartbeatOp";
  private static final int NO_ACK_COUNT_LIMIT = 5;
  private static final int HEARTBEAT_INTERVAL_MS = 3 * 1000; // 10s
  private LongConnectionContext mLongConnectionContext;
  private HeartbeatMessageListener mHeartbeatMessageListener;

  public HeartbeatOp(LongConnectionContext longConnectionContext,
      HeartbeatMessageListener heartbeatMessageListener) {
    mLongConnectionContext = longConnectionContext;
    mHeartbeatMessageListener = heartbeatMessageListener;
  }

  @Override
  public void run() {
    Log.i(TAG, "no ack count = " + mHeartbeatMessageListener.getNoAckCount());
    // 发送心跳前检查是否收到了上一个心跳包的响应，如果没收到，noAckCount自增1
    if (!mHeartbeatMessageListener.isHasReceiveAck()) {
      mHeartbeatMessageListener.incNoAckCount();
    }
    if (mHeartbeatMessageListener.getNoAckCount() < NO_ACK_COUNT_LIMIT) {
      mHeartbeatMessageListener.setHasReceiveAck(false);
      Log.w(TAG, "send heartbeat message");
      mLongConnectionContext.getLongConnectionTaskDispatcher().postRunnable(new SendMessageOp(
          mLongConnectionContext, new HeartbeatMessage(System.currentTimeMillis())));
      mLongConnectionContext.getLongConnectionTaskDispatcher().postRunnableDelay(this,
          HEARTBEAT_INTERVAL_MS);
    } else {
      Log.w(TAG, "heartbeat over time !!!");
      // 心跳超时了
      mHeartbeatMessageListener.setHasReceiveAck(false);
      mHeartbeatMessageListener.resetNoAckCount();
      mLongConnectionContext.getOnHeartbeatOvertime().onNext(false);
    }
  }
}
