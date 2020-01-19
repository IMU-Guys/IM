package com.imuguys.im.connection.op;

import android.util.Log;

import com.imuguys.im.connection.LongConnectionContext;
import com.imuguys.im.connection.callback.LongConnectionCallback;
import com.imuguys.im.connection.message.SocketJsonMessage;
import com.imuguys.im.utils.Gsons;

import java.io.Serializable;
import java.util.Optional;

import io.netty.channel.ChannelFuture;

/**
 * 发送消息操作
 */
public class SendMessageOp implements Runnable {
  private static final String TAG = "SendMessageOp";
  private LongConnectionContext mLongConnectionContext;
  private Serializable mPendingSendMessage;
  private LongConnectionCallback mLongConnectionCallback;

  public SendMessageOp(LongConnectionContext longConnectionContext,
      Serializable pendingSendMessage) {
    mLongConnectionContext = longConnectionContext;
    mPendingSendMessage = pendingSendMessage;
  }

  public SendMessageOp(LongConnectionContext longConnectionContext, Serializable pendingSendMessage,
      LongConnectionCallback longConnectionCallback) {
    mLongConnectionContext = longConnectionContext;
    mPendingSendMessage = pendingSendMessage;
    mLongConnectionCallback = longConnectionCallback;
  }

  @Override
  public void run() {
    if (mLongConnectionContext.getConnectionClient() != null) {
      Log.v(TAG, "send message " + mPendingSendMessage.toString());
      // todo 拆到编码器中
      ChannelFuture channelFuture = mLongConnectionContext.getConnectionClient().getChannel()
          .writeAndFlush(Gsons.Companion.getGUYS_GSON()
              .toJson(new SocketJsonMessage(mPendingSendMessage.getClass().getName(),
                  Gsons.Companion.getGUYS_GSON().toJson(mPendingSendMessage))));
      // 回调之
      Optional.ofNullable(mLongConnectionCallback)
          .ifPresent(longConnectionCallback -> channelFuture.addListener(future -> {
            if (future.isCancelled()) {
              mLongConnectionCallback.onCancelled();
            } else if (!future.isSuccess()) {
              mLongConnectionCallback.onFailed(future.cause());
            } else {
              mLongConnectionCallback.onSuccess();
            }
          }));
    } else {
      Log.w(TAG, "ConnectionClient has not benn initialization, message ignored");
    }
  }
}
