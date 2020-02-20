package com.imuguys.im.hotfix;

/**
 * 补丁接口
 */
public interface IGuysHotFixPatch {
  /**
   * 补丁入口
   */
  Object invokePatch(String methodName, Object[] methodParams, Object host);
}
