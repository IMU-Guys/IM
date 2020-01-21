package com.imuguys.im.connection;

import androidx.annotation.Nullable;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * 长链接上下文
 */
public class LongConnectionContext {

  private static final String TAG = "LongConnectionContext";

  private LongConnectionParams mLongConnectionParams;
  private ConnectionClient mConnectionClient;
  private LongConnectionTaskDispatcher mLongConnectionTaskDispatcher;
  private Map<String, SocketMessageListenerGroup> mSocketMessageListeners =
      new ConcurrentHashMap<>();
  // 还未建立连接时，无法建立连接
  private Subject<Boolean> mOnConnectFailedSubject = PublishSubject.create();
  // 已经建立连接，被服务端断开
  private Subject<Boolean> mOnRemoteDisconnectSubject = PublishSubject.create();
  // 连接建立成功
  private Subject<Boolean> mOnConnectSuccessSubject = PublishSubject.create();
  // 心跳超时
  private Subject<Boolean> mOnHeartbeatOvertime = PublishSubject.create();
  private Disposable mOnConnectionFailedDisposable;
  private Disposable mOnConnectionSuccessDisposable;
  private int mReConnectCount;

  public LongConnectionContext(LongConnectionParams longConnectionParams) {
    mLongConnectionParams = longConnectionParams;
    // 记录重连次数，重置重连次数
    mOnConnectionFailedDisposable =
        mOnConnectFailedSubject.subscribe(ignored -> mReConnectCount++);
    mOnConnectionSuccessDisposable =
        mOnConnectSuccessSubject.subscribe(ignored -> mReConnectCount = 0);
  }

  @SuppressWarnings("unchecked")
  public <Message> void registerMessageListener(String messageClassName,
      SocketMessageListener<Message> socketMessageListener) {
    Optional.ofNullable(mSocketMessageListeners.get(messageClassName))
        .orElseGet(() -> {
          SocketMessageListenerGroup<Message> group =
              new SocketMessageListenerGroup<>(messageClassName);
          mSocketMessageListeners.put(messageClassName, group);
          return group;
        }).addMessageListener(socketMessageListener);
  }

  @SuppressWarnings("unchecked")
  public void registerMessageListenerToChannelHandler() {
    for (Map.Entry<String, SocketMessageListenerGroup> entry : mSocketMessageListeners.entrySet()) {
      mConnectionClient.getChannelHandler().addMessageListener(entry.getKey(), entry.getValue());
    }
    // 不要clear，在重连的时候还需要从这里取Listeners
    // mSocketMessageListeners.clear();
  }

  public InetSocketAddress getInetSocketAddress() {
    return new InetSocketAddress(mLongConnectionParams.getHost(), mLongConnectionParams.getPort());
  }

  /**
   * 获取长链接客户端
   * @return mConnectionClient 如果已经成功建立连接 , null else
   *
   */
  @Nullable
  public ConnectionClient getConnectionClient() {
    return mConnectionClient;
  }

  public void setConnectionClient(ConnectionClient connectionClient) {
    mConnectionClient = connectionClient;
  }

  public LongConnectionTaskDispatcher getLongConnectionTaskDispatcher() {
    return mLongConnectionTaskDispatcher;
  }

  public void setLongConnectionTaskDispatcher(
      LongConnectionTaskDispatcher longConnectionTaskDispatcher) {
    mLongConnectionTaskDispatcher = longConnectionTaskDispatcher;
  }

  public LongConnectionParams getLongConnectionParams() {
    return mLongConnectionParams;
  }

  public void setLongConnectionParams(LongConnectionParams longConnectionParams) {
    mLongConnectionParams = longConnectionParams;
  }

  public Subject<Boolean> getOnConnectSuccessSubject() {
    return mOnConnectSuccessSubject;
  }

  public Subject<Boolean> getOnConnectFailedSubject() {
    return mOnConnectFailedSubject;
  }

  public Subject<Boolean> getOnRemoteDisconnectSubject() {
    return mOnRemoteDisconnectSubject;
  }

  public Subject<Boolean> getOnHeartbeatOvertime() {
    return mOnHeartbeatOvertime;
  }

  public int getReConnectCount() {
    return mReConnectCount;
  }

  public void release() {
    if (mOnConnectionFailedDisposable != null && !mOnConnectionFailedDisposable.isDisposed()) {
      mOnConnectionFailedDisposable.dispose();
    }
    if (mOnConnectionSuccessDisposable != null && !mOnConnectionSuccessDisposable.isDisposed()) {
      mOnConnectionSuccessDisposable.dispose();
    }
  }
}
