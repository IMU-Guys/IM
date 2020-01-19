package com.imuguys.im.connection;

import com.imuguys.im.connection.message.HeartbeatAckMessage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 心跳消息监听类
 */
public class HeartbeatMessageListener implements SocketMessageListener<HeartbeatAckMessage> {

  // 是否收到了ACK
  private boolean mHasReceiveAck = false;
  // 连续多少个周期没有收到ACK，在Netty的IO线程和LongConnectionTaskDispatcher线程中都会修改
  private AtomicInteger mNoAckCount = new AtomicInteger(0);

  @Override
  public void handleMessage(HeartbeatAckMessage heartbeatAckMessage) {
    mNoAckCount.set(0);
    mHasReceiveAck = true;
  }

  public boolean isHasReceiveAck() {
    return mHasReceiveAck;
  }

  public void setHasReceiveAck(boolean hasReceiveAck) {
    mHasReceiveAck = hasReceiveAck;
  }

  public int getNoAckCount() {
    return mNoAckCount.get();
  }

  public void incNoAckCount() {
    mNoAckCount.incrementAndGet();
  }

  public void resetNoAckCount() {
    mNoAckCount.set(0);
  }
}
