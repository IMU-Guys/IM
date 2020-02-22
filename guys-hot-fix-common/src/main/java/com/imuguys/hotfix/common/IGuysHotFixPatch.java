package com.imuguys.hotfix.common;

/**
 * 补丁接口
 */
public interface IGuysHotFixPatch {
  /**
   * 补丁入口
   */
  Object invokePatch(
      String methodName,
      Object[] methodParams,
      Class[] methodParamsTypes,
      Object host);
}
