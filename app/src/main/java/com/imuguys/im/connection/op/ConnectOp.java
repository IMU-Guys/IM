package com.imuguys.im.connection.op;

import android.util.Log;

import com.imuguys.im.connection.ConnectionBootstrap;
import com.imuguys.im.connection.ConnectionClient;
import com.imuguys.im.connection.LongConnectionContext;

/**
 * 连接操作
 */
public class ConnectOp implements Runnable {

  public static final String TAG = "ConnectOp";

  private LongConnectionContext mLongConnectionContext;

  public ConnectOp(LongConnectionContext longConnectionContext) {
    mLongConnectionContext = longConnectionContext;
  }

  @Override
  public void run() {
    new DisconnectOp(mLongConnectionContext).run();
    ConnectionBootstrap connectionBootstrap = new ConnectionBootstrap();
    connectionBootstrap.configureBootstrap();
    try {
      Log.i(TAG, "try to connect...");
      ConnectionClient connectionClient =
          connectionBootstrap.connect(mLongConnectionContext.getInetSocketAddress());
      // todo M开头的成员生成的getter/setter如何去掉M？ 何解？
      connectionClient.getChannelHandler()
          .setMLongConnectionContext(mLongConnectionContext);
      // 连接建立成功，notified
      mLongConnectionContext.getOnConnectSuccessSubject().onNext(false);
      mLongConnectionContext.setConnectionClient(connectionClient);
      mLongConnectionContext.registerMessageListenerToChannelHandler();
    } catch (Exception e) {
      e.printStackTrace();
      Log.i(TAG, "connect failed!");
      connectionBootstrap.shutdown();
      mLongConnectionContext.getOnConnectFailedSubject().onNext(false);
    }

  }
}
