package com.imuguys.im.connection.test;

import androidx.annotation.Nullable;

import com.imuguys.hotfix.common.ModifyClass;
import com.imuguys.hotfix.common.ModifyMethod;

@ModifyClass
public class Test {

  @ModifyMethod
  public static String gettt() {
    return null;
  }

  @Nullable
  public String x() {
    return "a";
  }
}
