package com.imuguys.hotfix.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

  public static void setFieldValue(String className, String fieldName, Object host, Object value) {
    try {
      Field declaredField = Class.forName(className).getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.set(host, value);
    } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
      e.printStackTrace();
    }
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

  public static void setFieldValue(String className, String fieldName, Object host, int value) {
    try {
      Field declaredField = Class.forName(className).getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.set(host, value);
    } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static void setFieldValue(Class clazz, String fieldName, Object host, int value) {
    try {
      Field declaredField = clazz.getDeclaredField(fieldName);
      if (!declaredField.isAccessible()) {
        declaredField.setAccessible(true);
      }
      declaredField.set(host, value);
    } catch (IllegalAccessException | NoSuchFieldException  e) {
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
