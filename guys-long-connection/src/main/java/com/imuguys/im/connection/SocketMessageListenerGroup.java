package com.imuguys.im.connection;

import java.util.HashSet;

/**
 * 监听相同Message类型的SocketMessageListener组
 */
public class SocketMessageListenerGroup<Message> implements SocketMessageListener<Message> {

  public SocketMessageListenerGroup(String messageClassName) {
    mMessageClassName = messageClassName;
  }

  private String mMessageClassName;
  private HashSet<SocketMessageListener<Message>> mSocketMessageListenerSet = new HashSet<>();

  void addMessageListener(SocketMessageListener<Message> messageListener) {
    mSocketMessageListenerSet.add(messageListener);
  }

  void removeMessageListener(SocketMessageListener<Message> messageListener) {
    mSocketMessageListenerSet.remove(messageListener);
  }

  @Override
  public void handleMessage(Message message) {
    mSocketMessageListenerSet.forEach(socketMessageListener -> {
      socketMessageListener.handleMessage(message);
    });
  }

  public String getMessageClassName() {
    return mMessageClassName;
  }
}
