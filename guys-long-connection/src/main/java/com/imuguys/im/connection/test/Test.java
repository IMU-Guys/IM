package com.imuguys.im.connection.test;

import android.graphics.Point;

import com.imuguys.hotfix.common.ModifyClass;
import com.imuguys.hotfix.common.ModifyMethod;

import java.util.Random;

@ModifyClass
public class Test extends Parent implements ITest {

  private static final String TAG = "Test";
  public static String KK = null;
  private Random mRandom = new Random();

  private int mCount = 0;
  private Point mPoint;

  @ModifyMethod
  @Override
  public String getTestString() {
//    super.getTestString();
    mCount++;
//    System.out.println(mCount);
//    System.out.println(mParentString);
    mParentString = "child";
    Point tmpPoint = new Point(1,2);
    mParentPoint = new Point(tmpPoint);
    tmpPoint.x = 3;
    mPoint = new Point(tmpPoint);
//    System.out.println(mPoint.toString());
//    String testString = randomString();
//    Log.i(TAG, testString);
    return mParentString;
  }

  @Override
  public String randomString() {
    return String.valueOf(mRandom.nextLong());
  }
}
