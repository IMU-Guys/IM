package com.imuguys.im.connection;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 长链接上下文
 * 1、持有当前长链接状态
 * 2、持有注册过的消息监听器
 * 3、持有连接客户端
 * 4、持有长链接参数
 */
public class LongConnectionContextV2 {
  private LongConnectionParams mLongConnectionParams;
  private State mState = State.DISCONNECTED;
  private ConnectionClient mConnectionClient;
  private Map<String, SocketMessageListenerGroup> mSocketMessageListeners =
      new ConcurrentHashMap<>();

  public LongConnectionContextV2(LongConnectionParams longConnectionParams) {
    mLongConnectionParams = longConnectionParams;
  }

  public State getState() {
    return mState;
  }

  public void updateState(State state) {
    mState = state;
  }

  public void setLongConnectionParams(LongConnectionParams longConnectionParams) {
    mLongConnectionParams = longConnectionParams;
  }

  public LongConnectionParams getLongConnectionParams() {
    return mLongConnectionParams;
  }

  public void setConnectionClient(ConnectionClient connectionClient) {
    mConnectionClient = connectionClient;
  }

  public ConnectionClient getConnectionClient() {
    return mConnectionClient;
  }

  public SocketAddress getServerAddress() {
    return new InetSocketAddress(mLongConnectionParams.getHost(), mLongConnectionParams.getPort());
  }


  @SuppressWarnings("unchecked")
  public <Message> void registerMessageListener(Class<Message> clazz,
      SocketMessageListener<Message> socketMessageListener) {
    Optional.ofNullable(mSocketMessageListeners.get(clazz.getName()))
        .orElseGet(() -> {
          SocketMessageListenerGroup<Message> group =
              new SocketMessageListenerGroup<>(clazz.getName());
          mSocketMessageListeners.put(clazz.getName(), group);
          return group;
        }).addMessageListener(socketMessageListener);
  }

  @SuppressWarnings("unchecked")
  public <Message> void unregisterMessageListener(Class<Message> clazz,
      SocketMessageListener<Message> socketMessageListener) {
    Optional.ofNullable(mSocketMessageListeners.get(clazz.getName()))
        .ifPresent(socketMessageListenerGroup -> {
          socketMessageListenerGroup.removeMessageListener(socketMessageListener);
        });
  }

  /**
   * 更新消息监听器到ChannelHandler中
   */
  @SuppressWarnings("unchecked")
  public void updateMessageListenerToChannelHandler() {
    for (Map.Entry<String, SocketMessageListenerGroup> entry : mSocketMessageListeners.entrySet()) {
      mConnectionClient.getChannelHandler().addMessageListener(entry.getKey(), entry.getValue());
    }
  }

  /**
   * 长链接状态
   */
  public enum State {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
  }
}
