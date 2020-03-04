package com.imuguys.hotfix.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class GuysReflectUtils {

  public static Object getFieldValue(String className, String fieldName, Object host) {
    try {
      Field declaredField = Class.forName(className).getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      return declaredField.get(host);
    } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static Object getFieldValue(Class clazz, String fieldName, Object host) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      return declaredField.get(host);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static void setFieldValue(Class clazz, String fieldName, Object host, Object value) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.set(host, value);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static void setFieldValue(Class clazz, String fieldName, Object host, int value) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.setInt(host, value);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static void setFieldValue(Class clazz, String fieldName, Object host, byte value) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.setByte(host, value);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static void setFieldValue(Class clazz, String fieldName, Object host, boolean value) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.setBoolean(host, value);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static void setFieldValue(Class clazz, String fieldName, Object host, char value) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.setChar(host, value);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static void setFieldValue(Class clazz, String fieldName, Object host, short value) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.setShort(host, value);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static void setFieldValue(Class clazz, String fieldName, Object host, long value) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.setLong(host, value);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static void setFieldValue(Class clazz, String fieldName, Object host, float value) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.setFloat(host, value);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static void setFieldValue(Class clazz, String fieldName, Object host, double value) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.setDouble(host, value);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static Object invokeMethod(String className, String methodName, Class[] paramsTypes,
      Object[] params, Object host) {
    try {
      Method declaredMethod = Class.forName(className).getDeclaredMethod(methodName, paramsTypes);
      if (!declaredMethod.isAccessible()) {
        declaredMethod.setAccessible(true);
      }
      return declaredMethod.invoke(host, params);
    } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException
        | InvocationTargetException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static Object invokeMethod(Class<?> clazz, String methodName, Class[] paramsTypes,
      Object[] params, Object host) {
    try {
      Method declaredMethod = clazz.getDeclaredMethod(methodName, paramsTypes);
      if (!declaredMethod.isAccessible()) {
        declaredMethod.setAccessible(true);
      }
      return declaredMethod.invoke(host, params);
    } catch (IllegalAccessException | NoSuchMethodException
        | InvocationTargetException e) {
      System.err.println("invokeMethod : " + clazz.getCanonicalName() + "," + methodName + ","
          + Arrays.toString(paramsTypes) + "," + Arrays.toString(params));
      e.printStackTrace();
    }
    return null;
  }

  public static Object invokeConstructor(String className, Class[] paramsType, Object[] params) {
    try {
      Constructor<?> constructor = Class.forName(className).getConstructor(paramsType);
      if (!constructor.isAccessible()) {
        constructor.setAccessible(true);
      }
      return constructor.newInstance(params);
    } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException
        | InvocationTargetException | InstantiationException e) {
      e.printStackTrace();
    }
    return null;
  }
}
