package com.imuguys.hotfix.common;

import java.lang.reflect.InvocationTargetException;

public class GuysHotFixPatchImpl implements IGuysHotFixPatch {
  @Override
  public Object invokePatch(String methodName, Object[] methodParams, Class[] methodParamsTypes,
      Object host) {
    try {
      return this.getClass().getDeclaredMethod(methodName, methodParamsTypes).invoke(this,
          methodParams);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      e.printStackTrace();
    }
    return null;
  }
}
