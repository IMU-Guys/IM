package com.imuguys.im.connection;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * 长链接操作调度器
 * 执行
 * {@link com.imuguys.im.connection.op.ConnectOp 有限时间阻塞}
 * {@link com.imuguys.im.connection.op.DisconnectOp 非阻塞}
 * {@link com.imuguys.im.connection.op.HeartbeatOp 非阻塞}
 * {@link com.imuguys.im.connection.op.SendMessageOp 非阻塞}
 * 操作
 */
public class LongConnectionTaskDispatcher {
  private HandlerThread mHandlerThread = new HandlerThread("LongConnectionTaskDispatcher");
  private Handler mHandler;

  public void postRunnable(Runnable runnable) {
    if (mHandlerThread.getState() == Thread.State.NEW) {
      mHandlerThread.start();
      mHandler = new Handler(mHandlerThread.getLooper());
    }
    mHandler.post(runnable);
  }

  public void postRunnableDelay(Runnable runnable, long delay) {
    mHandler.postDelayed(runnable, delay);
  }

  public void removeRunnable(Runnable runnable) {
    mHandler.removeCallbacks(runnable);
  }

  public void stop() {
    mHandlerThread.quit();
  }
}
