package com.imuguys.hotfix.patch;

import com.imuguys.hotfix.common.IGuysHotFixPatch;
import com.imuguys.hotfix.common.ModifyClass;
import com.imuguys.hotfix.common.ModifyMethod;

import javassist.ClassPool;

// 补丁包生成器上下文
public class GuysPatchMakerGeneratorConfig {
  private ClassPool mClassPool = ClassPool.getDefault();
  private String mPatchClassAnnotationClassName = ModifyClass.class.getName();
  private String mPatchMethodAnnotationClassName = ModifyMethod.class.getName();
  private String mPatchClassName = IGuysHotFixPatch.class.getName();

  public ClassPool getClassPool() {
    return mClassPool;
  }

  public String getPatchClassAnnotationClassName() {
    return mPatchClassAnnotationClassName;
  }

  public String getPatchMethodAnnotationClassName() {
    return mPatchMethodAnnotationClassName;
  }

  public String getPatchClassName() {
    return mPatchClassName;
  }
}
