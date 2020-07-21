package com.imuguys.im.connection.op;

import com.imuguys.im.connection.LongConnectionContextV2;

import io.reactivex.Observable;

/**
 * 断开连接操作
 */
public class DisconnectTask implements Task<Boolean> {

  private LongConnectionContextV2 mLongConnectionContextV2;

  public DisconnectTask(LongConnectionContextV2 longConnectionContext) {
    mLongConnectionContextV2 = longConnectionContext;
  }

  /**
   * 会立即返回，由netty线程去执行断开连接操作
   */
  @Override
  public Observable<Boolean> getTaskObservable() {
    if (mLongConnectionContextV2.getConnectionClient() == null) {
      return Observable.just(true);
    }
    mLongConnectionContextV2.getConnectionClient().close();
    return Observable.just(true);
  }
}
