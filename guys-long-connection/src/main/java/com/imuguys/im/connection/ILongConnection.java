package com.imuguys.im.connection;

import java.io.Serializable;

import io.reactivex.Observable;

/**
 * 长链接接口
 */
public interface ILongConnection {
  /**
   * 发起连接
   */
  void connect();

  Observable<Object> rxConnect();

  /**
   * 发送消息
   */
  void sendMessage(Serializable serializable);

  /**
   * 发送消息
   */
  Observable<Object> rxSendMessage(Serializable serializable);

  /**
   * 注册消息监听器
   * 
   * @param clazz 监听的消息类
   * @param messageListener 监听器
   */
  <Message> void registerMessageHandler(
          Class<Message> clazz,
      SocketMessageListener<Message> messageListener);

  /**
   * 移除消息监听器
   * 
   * @param clazz 监听的消息类
   * @param messageListener 监听器
   */
  <Message> void unregisterMessageHandler(
      Class<Message> clazz,
      SocketMessageListener<Message> messageListener);

  /**
   * 断开连接
   */
  void disconnect();

  /**
   * 释放资源
   */
  void release();

  /**
   * 已经建立了连接
   */
  boolean isConnected();

  void setLongConnectionParams(LongConnectionParams longConnectionParams);
}
