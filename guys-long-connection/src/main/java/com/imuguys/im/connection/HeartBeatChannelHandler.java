package com.imuguys.im.connection;

import com.imuguys.im.connection.message.HeartbeatAckMessage;
import com.imuguys.im.connection.message.HeartbeatMessage;
import com.imuguys.im.connection.message.SocketMessageWrapper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

/**
 * 心跳
 * 最多允许丢失 {@link HeartBeatChannelHandler#MAX_NO_ACK_COUNT}个心跳
 */
public class HeartBeatChannelHandler extends SimpleChannelInboundHandler<SocketMessageWrapper> {

  private int MAX_NO_ACK_COUNT = 5;
  private long HEART_BEAT_INTERVAL_MS = 5000L;
  private LongConnectionManager mLongConnectionManager;
  private AtomicInteger mRemainNoAckCount = new AtomicInteger(-1);
  private Disposable mHeartBeatDisposable;
  private PublishSubject<Boolean> mHeartBeatTimeOutSubject = PublishSubject.create();

  public HeartBeatChannelHandler(
      LongConnectionManager longConnectionManager) {
    mLongConnectionManager = longConnectionManager;
  }

  public HeartBeatChannelHandler(
      int MAX_NO_ACK_COUNT,
      int HEART_BEAT_INTERVAL_MS,
      LongConnectionManager longConnectionManager) {
    this.MAX_NO_ACK_COUNT = MAX_NO_ACK_COUNT;
    this.HEART_BEAT_INTERVAL_MS = HEART_BEAT_INTERVAL_MS;
    mLongConnectionManager = longConnectionManager;
  }

  /**
   * 开始心跳，间隔指定时间 减少剩余可丢失的心跳次数
   * 为0时，认为超时，通知外层
   */
  public void startHeartBeat() {
    mHeartBeatDisposable = Observable
        .interval(0, HEART_BEAT_INTERVAL_MS, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
        .doOnNext(aLong -> {
          int remainTimes = mRemainNoAckCount.decrementAndGet();
          if (remainTimes == 0) {
            mHeartBeatTimeOutSubject.onNext(true);
          } else {
            mLongConnectionManager.send(new HeartbeatMessage(System.currentTimeMillis()));
          }
        }).subscribe();
  }

  public Observable<Boolean> getHeartBeatTimeOutSubject() {
    return mHeartBeatTimeOutSubject;
  }

  /**
   * 如果消息为心跳消息，重置 {@link HeartBeatChannelHandler#mRemainNoAckCount}
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, SocketMessageWrapper msg)
      throws Exception {
    if (msg.getClassName().equals(HeartbeatAckMessage.class.getName())) {
      mRemainNoAckCount.set(MAX_NO_ACK_COUNT);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  public void release() {
    mLongConnectionManager = null;
    mHeartBeatDisposable.dispose();
  }
}
