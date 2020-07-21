package com.imuguys.im.connection;

import android.util.Log;

import androidx.annotation.Nullable;

import com.imuguys.im.connection.op.ConnectTask;
import com.imuguys.im.connection.op.DisconnectTask;
import com.imuguys.im.connection.op.SendMessageTask;
import com.imuguys.im.connection.utils.RxJavaUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Functions;

/**
 * 长链接管理
 * 1、连接
 * 2、断开连接
 * 3、发送消息
 * 4、开启心跳
 * 5、重连
 */
public class LongConnectionManager {
  private static final String TAG = "LongConnectionManager";
  private LongConnectionContextV2 mLongConnectionContextV2;
  @Nullable
  private HeartBeatChannelHandler mHeartBeatChannelHandler; // 心跳
  @Nullable
  private MessageChannelHandler mMessageChannelHandler; // 消息处理
  private ConnectTask mConnectTask; // 连接任务
  private Disposable mConnectTaskDisposable; // 取消连接
  private DisconnectTask mDisconnectTask; // 断连任务
  private Disposable mDisconnectTaskDisposable; // todo 取消断连，可能不需要这个
  private Set<Disposable> mSendMessageTaskDisposableSet = new HashSet<>(); // 发送消息的Disposable
  private int reconnectCount; // 在一次连接中发生的重连次数
  private Disposable mDelayConnectDisposable; // 取消延迟重连
  // 连接成功
  private Consumer<ConnectionClient> mConnectConsumer = connectionClient -> {
    mLongConnectionContextV2.setConnectionClient(connectionClient);
    mLongConnectionContextV2.updateState(LongConnectionContextV2.State.CONNECTED);
    mConnectTask = null;
    initAndAddChannelHandler();
    // 将消息监听器注册到 MessageChannelHandler中
    mLongConnectionContextV2.updateMessageListenerToChannelHandler();
    reconnectCount = 0;
  };
  // 连接失败
  private Consumer<Throwable> mConnectThrowableConsumer = throwable -> {
    mConnectTask = null;
    reconnectCount++;
    // 未达到重连上限，间隔指定时间后继续重连
    if (reconnectCount < mLongConnectionContextV2.getLongConnectionParams()
        .getAttemptToReconnectionCountLimit()) {
      mDelayConnectDisposable = Observable
          .timer(mLongConnectionContextV2.getLongConnectionParams()
              .getAttemptToReconnectionIntervalLimitMs(), TimeUnit.MILLISECONDS)
          .doOnNext(aLong -> connect())
          .subscribe();
    } else {
      Log.i(TAG, "reconnect max count, stop try connect");
    }
  };
  // 心跳超时
  private Consumer<Boolean> mHeartBeatTimeOutConsumer = ignore -> {
    releaseHeartBeatChannelHandler();
    disconnect();
    connect();
  };

  // 定时清除mSendMessageTaskDisposableSet中已经disposed的对象
  private Observable<Long> mClearTaskObservable =
      Observable.interval(0, 10, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
          .doOnNext(aLong -> mSendMessageTaskDisposableSet.removeIf(Disposable::isDisposed));
  private Disposable mClearTaskDisposable; // 取消定时清除任务
  private Disposable mHearBeatTimeOutDisposable; // 取消心跳超时监听

  public LongConnectionManager(LongConnectionContextV2 longConnectionContextV2) {
    mLongConnectionContextV2 = longConnectionContextV2;
    mClearTaskDisposable = mClearTaskObservable.subscribe();
  }

  public void connect() {
    if (mConnectTask != null) {
      Log.i(TAG, "connect: already in connecting state");
      return;
    }
    mLongConnectionContextV2.updateState(LongConnectionContextV2.State.CONNECTING);
    mConnectTask = new ConnectTask(mLongConnectionContextV2);
    mConnectTaskDisposable = mConnectTask.getTaskObservable()
        .subscribe(mConnectConsumer, mConnectThrowableConsumer);
  }

  /**
   * 初始化并且将ChannelHandler添加到pipeline中
   */
  private void initAndAddChannelHandler() {
    mHeartBeatChannelHandler = new HeartBeatChannelHandler(this);
    mLongConnectionContextV2.getConnectionClient().getPipeline().addLast(mHeartBeatChannelHandler);
    mHeartBeatChannelHandler.startHeartBeat();
    mHearBeatTimeOutDisposable = mHeartBeatChannelHandler.getHeartBeatTimeOutSubject()
        .doOnNext(mHeartBeatTimeOutConsumer)
        .subscribe();
    mMessageChannelHandler = new MessageChannelHandler();
    mMessageChannelHandler.setLongConnectionContext(mLongConnectionContextV2);
    mLongConnectionContextV2.getConnectionClient().getPipeline().addLast(mMessageChannelHandler);
  }

  public Observable<Object> rxConnect() {
    mLongConnectionContextV2.updateState(LongConnectionContextV2.State.CONNECTING);
    mConnectTask = new ConnectTask(mLongConnectionContextV2);
    return mConnectTask.getTaskObservable()
        .doOnNext(mConnectConsumer)
        .doOnError(mConnectThrowableConsumer)
        .map(o -> new Object());
  }

  public void disconnect() {
    if (mDisconnectTask != null) {
      Log.i(TAG, "disconnect: already in disconnecting state");
      return;
    }
    mLongConnectionContextV2.updateState(LongConnectionContextV2.State.DISCONNECTING);
    mDisconnectTask = new DisconnectTask(mLongConnectionContextV2);
    mDisconnectTaskDisposable = mDisconnectTask.getTaskObservable()
        .subscribe(ignore -> {
          mLongConnectionContextV2.updateState(LongConnectionContextV2.State.DISCONNECTED);
          mLongConnectionContextV2.setConnectionClient(null);
          releaseHeartBeatChannelHandler();
          mDisconnectTask = null;
        }, Throwable::printStackTrace);
  }

  public void send(Serializable pendingSendMessage) {
    if (mLongConnectionContextV2.getState().equals(LongConnectionContextV2.State.DISCONNECTING)
        || mLongConnectionContextV2.getState().equals(LongConnectionContextV2.State.DISCONNECTED)) {
      Log.i(TAG, "send: long connection is disconnected");
      return;
    }
    SendMessageTask sendMessageTask =
        new SendMessageTask(mLongConnectionContextV2, pendingSendMessage);
    Disposable disposable = sendMessageTask.getTaskObservable()
        .subscribe(Functions.emptyConsumer(), Throwable::printStackTrace);
    mSendMessageTaskDisposableSet.add(disposable);
  }

  public Observable<Object> rxSend(Serializable pendingSendMessage) {
    if (mLongConnectionContextV2.getState().equals(LongConnectionContextV2.State.DISCONNECTING)
        || mLongConnectionContextV2.getState().equals(LongConnectionContextV2.State.DISCONNECTED)) {
      Log.i(TAG, "send: long connection is disconnected");
      return Observable.error(new RuntimeException("send: long connection is disconnected"));
    }
    SendMessageTask sendMessageTask =
        new SendMessageTask(mLongConnectionContextV2, pendingSendMessage);
    return sendMessageTask.getTaskObservable()
        .map(o -> new Object());
  }

  /**
   * 取消心跳
   */
  private void releaseHeartBeatChannelHandler() {
    if (mHeartBeatChannelHandler != null) {
      mHeartBeatChannelHandler.release();
    }
  }

  public void release() {
    disconnect();
    RxJavaUtils.dispose(mConnectTaskDisposable);
    RxJavaUtils.dispose(mDisconnectTaskDisposable);
    mSendMessageTaskDisposableSet.forEach(Disposable::dispose);
    RxJavaUtils.dispose(mClearTaskDisposable);
    releaseHeartBeatChannelHandler();
    RxJavaUtils.dispose(mHearBeatTimeOutDisposable);
    RxJavaUtils.dispose(mDelayConnectDisposable);
  }
}
