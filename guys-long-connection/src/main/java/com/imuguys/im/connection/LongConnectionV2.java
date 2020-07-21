package com.imuguys.im.connection;

import java.io.Serializable;

import io.reactivex.Observable;

public class LongConnectionV2 implements ILongConnection {

  private LongConnectionContextV2 mLongConnectionContextV2;
  private LongConnectionManager mLongConnectionManager;

  public LongConnectionV2(LongConnectionParams longConnectionParams) {
    mLongConnectionContextV2 = new LongConnectionContextV2(longConnectionParams);
    mLongConnectionManager = new LongConnectionManager(mLongConnectionContextV2);
  }

  @Override
  public void connect() {
    mLongConnectionManager.connect();
  }

  @Override
  public Observable<Object> rxConnect() {
    return mLongConnectionManager.rxConnect();
  }

  @Override
  public void sendMessage(Serializable serializable) {
    mLongConnectionManager.send(serializable);
  }

  @Override
  public Observable<Object> rxSendMessage(Serializable serializable) {
    return mLongConnectionManager.rxSend(serializable);
  }

  @Override
  public <Message> void registerMessageHandler(Class<Message> clazz,
      SocketMessageListener<Message> messageListener) {
    mLongConnectionContextV2.registerMessageListener(clazz, messageListener);
  }

  @Override
  public <Message> void unregisterMessageHandler(Class<Message> clazz,
      SocketMessageListener<Message> messageListener) {
    mLongConnectionContextV2.unregisterMessageListener(clazz, messageListener);
  }

  @Override
  public void disconnect() {
    mLongConnectionManager.disconnect();
  }

  @Override
  public void release() {
    mLongConnectionManager.release();
  }

  @Override
  public boolean isConnected() {
    return mLongConnectionContextV2.getState().equals(LongConnectionContextV2.State.CONNECTED);
  }

  @Override
  public void setLongConnectionParams(LongConnectionParams longConnectionParams) {
    mLongConnectionContextV2.setLongConnectionParams(longConnectionParams);
  }
}
