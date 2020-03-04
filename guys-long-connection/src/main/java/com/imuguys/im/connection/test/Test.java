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

  private static void staticMethod(Point point) {
    System.out.println(point);
  }

  private void method(Point point) {
    System.out.println(point);
  }

  @ModifyMethod
  public static void staticBugMethod() {
    Point point = new Point();
    staticMethod(point);
    String s = point.toString();
    System.out.println(s);
  }

  @ModifyMethod
  @Override
  public String getTestString() {
    mCount++;
    mParentString = "child";
    Point tmpPoint = new Point(1,2);
    mParentPoint = new Point(tmpPoint);
    tmpPoint.x = 3;
    mPoint = new Point(tmpPoint);
    staticMethod(mPoint);
    method(mPoint);
    System.out.println(this);
    return mParentString;
  }

  @Override
  public String randomString() {
    return String.valueOf(mRandom.nextLong());
  }
}
