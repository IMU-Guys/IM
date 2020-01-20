package com.imuguys.im.connection.op;

import com.imuguys.im.connection.ConnectionClient;
import com.imuguys.im.connection.LongConnectionContext;

import java.util.Optional;

/**
 * 断开连接操作
 */
public class DisconnectOp implements Runnable {

  private LongConnectionContext mLongConnectionContext;

  public DisconnectOp(LongConnectionContext longConnectionContext) {
    mLongConnectionContext = longConnectionContext;
  }

  @Override
  public void run() {
    Optional.ofNullable(mLongConnectionContext.getConnectionClient())
        .ifPresent(ConnectionClient::close);
  }
}
