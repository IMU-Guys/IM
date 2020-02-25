package com.imuguys.im.connection.test;

import android.graphics.Point;
import android.util.Log;

import com.imuguys.hotfix.common.ModifyClass;
import com.imuguys.hotfix.common.ModifyMethod;

import java.util.Random;

@ModifyClass
public class Test extends Parent implements ITest {

  private static final String TAG = "Test";
  private Random mRandom = new Random();

  private int mCount = 0;
  private Point mPoint;

  @ModifyMethod
  @Override
  public String getTestString() {
    super.getTestString();
    mCount++;
    System.out.println(mCount);
    mPoint = new Point();
    System.out.println(mPoint.toString());
    String testString = randomString();
    Log.i(TAG, testString);
    return testString;
  }

  @Override
  public String randomString() {
    return String.valueOf(mRandom.nextLong());
  }
}
