package com.imuguys.im.connection.op;

import android.util.Log;

import com.imuguys.im.connection.LongConnectionContext;
import com.imuguys.im.connection.message.SocketJsonMessage;
import com.imuguys.im.utils.Gsons;

import java.io.Serializable;

/**
 * 发送消息操作
 */
// todo 添加回调，通知消息发送失败
public class SendMessageOp implements Runnable {
  private static final String TAG = "SendMessageOp";
  private LongConnectionContext mLongConnectionContext;
  private Serializable mPendingSendMessage;

  public SendMessageOp(LongConnectionContext longConnectionContext,
      Serializable pendingSendMessage) {
    mLongConnectionContext = longConnectionContext;
    mPendingSendMessage = pendingSendMessage;
  }

  @Override
  public void run() {
    if (mLongConnectionContext.getConnectionClient() != null) {
      Log.v(TAG, "send message " + mPendingSendMessage.toString());
      // todo 拆到编码器中
      mLongConnectionContext.getConnectionClient().getChannel()
          .writeAndFlush(Gsons.Companion.getGUYS_GSON()
              .toJson(new SocketJsonMessage(mPendingSendMessage.getClass().getName(),
                  Gsons.Companion.getGUYS_GSON().toJson(mPendingSendMessage))));
    } else {
      Log.w(TAG, "ConnectionClient has not benn initialization, message ignored");
    }
  }
}
