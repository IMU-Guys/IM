package com.imuguys.im.connection.op;

import android.util.Log;

import com.imuguys.im.connection.LongConnectionContextV2;

import java.io.Serializable;

import io.netty.channel.ChannelFuture;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * 发送消息操作
 */
public class SendMessageTask implements Task<Boolean> {
  private static final String TAG = "SendMessageTask";
  private LongConnectionContextV2 mLongConnectionContextV2;
  private Serializable mPendingSendMessage;

  public SendMessageTask(LongConnectionContextV2 longConnectionContextV2,
      Serializable pendingSendMessage) {
    mLongConnectionContextV2 = longConnectionContextV2;
    mPendingSendMessage = pendingSendMessage;
  }

  @Override
  public Observable<Boolean> getTaskObservable() {
    if (mLongConnectionContextV2.getConnectionClient() != null) {
      Log.v(TAG, "send message " + mPendingSendMessage.toString());
      ChannelFuture channelFuture = mLongConnectionContextV2.getConnectionClient().getChannel()
          .writeAndFlush(mPendingSendMessage);
      PublishSubject<Boolean> publishSubject = PublishSubject.create();
      channelFuture.addListener(future -> {
        if (future.isCancelled()) {
          publishSubject.onError(new RuntimeException("cancelled by user"));
        } else if (!future.isSuccess()) {
          publishSubject.onError(future.cause());
        } else {
          publishSubject.onNext(true);
        }
      });
      return publishSubject.observeOn(AndroidSchedulers.mainThread());
    } else {
      Log.w(TAG, "ConnectionClient has not benn initialization, message ignored");
      return Observable
          .error(new IllegalStateException("ConnectionClient has not bean initialized"))
          .observeOn(AndroidSchedulers.mainThread())
          .map(o -> true);
    }
  }
}
