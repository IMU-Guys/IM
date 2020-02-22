package com.imuguys.hotfix.stub;

import java.io.IOException;
import java.util.Set;

import org.gradle.api.Project;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.FileUtils;

import javassist.NotFoundException;

public class GuysStubMakerTransform extends Transform {

  private GuysStubMakerInjector mGuysStubMakerInjector;
  private GuysStubMakerContext mGuysStubMakerContext;

  public GuysStubMakerTransform(Project project) {
    mGuysStubMakerContext = new GuysStubMakerContext.Builder()
        .setProject(project)
        .setTargetClassPredicate(className -> className.startsWith("com/imuguys/im/connection"))
        .build();
    mGuysStubMakerInjector = new GuysStubMakerInjector(mGuysStubMakerContext);
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  // 输入类型
  @Override
  public Set<QualifiedContent.ContentType> getInputTypes() {
    return TransformManager.CONTENT_CLASS;
  }

  // 处理范围
  @Override
  public Set<? super QualifiedContent.Scope> getScopes() {
    return TransformManager.SCOPE_FULL_PROJECT;
  }

  // 是否增量
  @Override
  public boolean isIncremental() {
    return false;
  }

  @Override
  public void transform(TransformInvocation transformInvocation)
      throws TransformException, InterruptedException, IOException {
    super.transform(transformInvocation);
    // 不支持增量，需要调用这个
    transformInvocation.getOutputProvider().deleteAll();
    // 把所有的类加入到ClassPool中
    addAllClassIntoClassPool(transformInvocation);
    // 遍历transformInput
    transformInvocation.getInputs().forEach(transformInput -> {
      transformInput.getJarInputs()
          .forEach(jarInput -> mGuysStubMakerInjector.processJatInput(jarInput,
              transformInvocation.getOutputProvider()));
      transformInput.getDirectoryInputs().forEach(directoryInput -> {
        try {
          FileUtils.copyDirectory(directoryInput.getFile(),
              transformInvocation.getOutputProvider().getContentLocation(directoryInput.getName(),
                  directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY));
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    });
    System.out.println(this.getClass().getSimpleName() + " process finished");
  }

  private void addAllClassIntoClassPool(TransformInvocation transformInvocation) {
    // 加入AndroidJar
    mGuysStubMakerContext.addAndroidJarToClassPool();
    transformInvocation.getInputs().forEach(transformInput -> {
      transformInput.getJarInputs().forEach(jarInput -> {
        try {
          // 加入第三方jar
          mGuysStubMakerContext.getClassPool()
              .insertClassPath(jarInput.getFile().getAbsolutePath());
        } catch (NotFoundException e) {
          e.printStackTrace();
        }
      });

      transformInput.getDirectoryInputs().forEach(directoryInput -> {
        try {
          mGuysStubMakerContext.getClassPool()
              .insertClassPath(directoryInput.getFile().getAbsolutePath());
        } catch (NotFoundException e) {
          e.printStackTrace();
        }
      });
    });
  }
}
