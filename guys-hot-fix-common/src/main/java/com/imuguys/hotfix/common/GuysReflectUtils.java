package com.imuguys.hotfix.common;

import java.lang.reflect.InvocationTargetException;

public class GuysReflectUtils {

  public static Object getFieldValue(String className, String fieldName, Object host) {
    try {
      return Class.forName(className).getDeclaredField(fieldName).get(host);
    } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static void setFieldValue(String className, String fieldName, Object host, Object value) {
    try {
      Class.forName(className).getDeclaredField(fieldName).set(host, value);
    } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static void setFieldValue(String className, String fieldName, Object host, int value) {
    try {
      Class.forName(className).getDeclaredField(fieldName).set(host, value);
    } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static Object invokeMethod(String className, String methodName, Class[] paramsTypes,
      Object[] params, Object host) {
    try {
      return Class.forName(className).getDeclaredMethod(methodName, paramsTypes).invoke(host, params);
    } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException
        | InvocationTargetException e) {
      e.printStackTrace();
    }
    return null;
  }
}
