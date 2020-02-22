package com.imuguys.hotfix.stub;

import java.util.function.Predicate;

import org.gradle.api.Project;

import com.android.build.gradle.AppExtension;
import com.imuguys.hotfix.common.HotFixConfig;
import com.imuguys.hotfix.common.IGuysHotFixPatch;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

// 插桩上下文
public class GuysStubMakerContext {
  private ClassPool mClassPool = ClassPool.getDefault();
  private Project mProject;
  // class name过滤
  private Predicate<String> mTargetClassPredicate;
  // 不对lambda插桩
  private boolean mExcludeLambda;
  // 补丁CtClassName
  public static final String mIPatchClassName = IGuysHotFixPatch.class.getName();

  public void addAndroidJarToClassPool() {
    mProject.getExtensions().getByType(AppExtension.class).getBootClasspath().forEach(file -> {
      try {
        mClassPool.insertClassPath(file.getAbsolutePath());
      } catch (NotFoundException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });
  }

  public boolean isExcludeLambda() {
    return mExcludeLambda;
  }

  public Predicate<String> getTargetClassPredicate() {
    return mTargetClassPredicate;
  }

  public ClassPool getClassPool() {
    return mClassPool;
  }

  public static class Builder {
    private Project mProject;
    private Predicate<String> mTargetClassPredicate;
    private boolean mExcludeLambda = false;

    public Builder setTargetClassPredicate(Predicate<String> targetClassPredicate) {
      mTargetClassPredicate = targetClassPredicate;
      return this;
    }

    public Builder setProject(Project project) {
      mProject = project;
      return this;
    }

    public Builder setExcludeLambda(boolean excludeLambda) {
      mExcludeLambda = excludeLambda;
      return this;
    }

    public GuysStubMakerContext build() {
      GuysStubMakerContext guysStubMakerContext = new GuysStubMakerContext();
      guysStubMakerContext.mProject = mProject;
      guysStubMakerContext.mTargetClassPredicate = mTargetClassPredicate;
      guysStubMakerContext.mExcludeLambda = mExcludeLambda;
      return guysStubMakerContext;
    }
  }
}
