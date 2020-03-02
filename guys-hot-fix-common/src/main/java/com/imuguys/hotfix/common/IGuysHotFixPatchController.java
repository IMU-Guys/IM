package com.imuguys.hotfix.common;

/**
 * 补丁接口
 */
public interface IGuysHotFixPatchController {
  /**
   * 补丁入口
   */
  Object invokePatch(
      String methodName,
      Object[] methodParams,
      Class[] methodParamsTypes,
      boolean isStatic,
      Class<?> returnType,
      Object host);
}
