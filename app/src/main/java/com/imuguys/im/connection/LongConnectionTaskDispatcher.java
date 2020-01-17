package com.imuguys.im.connection;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * 长链接操作调度器
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
}
