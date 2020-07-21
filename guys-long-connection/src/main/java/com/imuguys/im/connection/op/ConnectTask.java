package com.imuguys.im.connection.op;

import android.util.Log;

import com.imuguys.im.connection.ConnectionBootstrap;
import com.imuguys.im.connection.ConnectionClient;
import com.imuguys.im.connection.LongConnectionContextV2;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 连接操作,会产生阻塞
 */
public class ConnectTask implements Task<ConnectionClient> {

  public static final String TAG = "ConnectTask";

  private LongConnectionContextV2 mLongConnectionContextV2;

  public ConnectTask(LongConnectionContextV2 longConnectionContextV2) {
    mLongConnectionContextV2 = longConnectionContextV2;
  }

  @Override
  public Observable<ConnectionClient> getTaskObservable() {
    ConnectionBootstrap connectionBootstrap = new ConnectionBootstrap(
        mLongConnectionContextV2.getLongConnectionParams().getConnectTimeOut());
    Log.i(TAG, "try to connect...");
    return connectionBootstrap.connect(mLongConnectionContextV2.getServerAddress())
        .doOnError(throwable -> {
          throwable.printStackTrace();
          Log.i(TAG, "connect failed!");
          connectionBootstrap.shutdown();
        }).observeOn(AndroidSchedulers.mainThread());
  }
}
