package com.imuguys.im;

import android.annotation.SuppressLint;
import android.content.Context;

public class GlobalConfig {

  // application中的ContextImpl 不会产生泄漏
  @SuppressLint("StaticFieldLeak")
  public static Context CONTEXT;

}
