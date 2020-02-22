package com.imuguys.hotfix.patch;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.FileUtils;
import com.imuguys.hotfix.common.HotFixConfig;

import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;
import java.util.jar.JarOutputStream;

import javassist.NotFoundException;

public class GuysPatchMakerTransform extends Transform {

  private static final String DX_JAR_PATH = "guys-hot-fix-patch-maker/libs/dx.jar";
  private static final String PATCH_OUTPUT_DIR_NAME = "patchDir/";
  private static final String PATCH_JAR_FILE_NAME =
      PATCH_OUTPUT_DIR_NAME + HotFixConfig.PATCH_JAR_FILE_NAME;
  private static final String PATCH_CONFIG_FILE_NAME =
      PATCH_OUTPUT_DIR_NAME + HotFixConfig.PATCH_CONFIG_FILE_NAME;
  private static final String PATCH_DEX_FILE_NAME =
      PATCH_OUTPUT_DIR_NAME + HotFixConfig.PATCH_DEX_FILE_NAME;
  private Project mProject;
  private GuysPatchMakerGeneratorConfig mGuysPatchMakerGeneratorConfig;
  private GuysPatchMakerGenerator mGuysPatchMakerGenerator;

  public GuysPatchMakerTransform(Project project) {
    mProject = project;
    mGuysPatchMakerGeneratorConfig = new GuysPatchMakerGeneratorConfig();
    mGuysPatchMakerGenerator = new GuysPatchMakerGenerator(mGuysPatchMakerGeneratorConfig);
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Set<QualifiedContent.ContentType> getInputTypes() {
    return TransformManager.CONTENT_CLASS;
  }

  @Override
  public Set<? super QualifiedContent.Scope> getScopes() {
    return TransformManager.SCOPE_FULL_PROJECT;
  }

  @Override
  public boolean isIncremental() {
    return false;
  }

  @Override
  public void transform(TransformInvocation transformInvocation)
      throws TransformException, InterruptedException, IOException {
    super.transform(transformInvocation);
    transformInvocation.getOutputProvider().deleteAll();
    addAllClassIntoClassPool(transformInvocation);
    JarOutputStream jarOutputStream = createPatchJarOutputStream();
    PrintWriter patchInfoPrintWriter =
        new PrintWriter(new OutputStreamWriter(new FileOutputStream(PATCH_CONFIG_FILE_NAME)));
    patchInfoPrintWriter.println("1.0");
    transformInvocation.getInputs().forEach(transformInput -> {
      transformInput.getJarInputs().forEach(jarInput -> {
        mGuysPatchMakerGenerator.processJatInput(jarInput,
            transformInvocation.getOutputProvider(), jarOutputStream, patchInfoPrintWriter);
      });

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
    jarOutputStream.flush();
    jarOutputStream.close();
    patchInfoPrintWriter.flush();
    patchInfoPrintWriter.close();
    Process jar2DexProcess = Runtime.getRuntime()
        .exec(
            "java -jar " + DX_JAR_PATH + " --dex --output=" + PATCH_DEX_FILE_NAME
                + " " + PATCH_JAR_FILE_NAME);
    System.out.println(new String(IOUtils.toByteArray(jar2DexProcess.getErrorStream())));
    jar2DexProcess.waitFor();
    System.out.println(getName() + " process finished");
  }

  @NotNull
  private JarOutputStream createPatchJarOutputStream() throws IOException {
    FileUtils.mkdirs(new File(PATCH_OUTPUT_DIR_NAME));
    File patchJarFile = new File(PATCH_JAR_FILE_NAME);
    if (patchJarFile.exists()) {
      patchJarFile.delete();
    }
    if (!patchJarFile.exists()) {
      patchJarFile.createNewFile();
    }
    return new JarOutputStream(new FileOutputStream(PATCH_JAR_FILE_NAME));
  }

  private void addAllClassIntoClassPool(TransformInvocation transformInvocation) {
    // 加入AndroidJar
    mProject.getExtensions().getByType(AppExtension.class).getBootClasspath().forEach(file -> {
      try {
        mGuysPatchMakerGeneratorConfig.getClassPool().insertClassPath(file.getAbsolutePath());
      } catch (NotFoundException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });
    transformInvocation.getInputs().forEach(transformInput -> {
      transformInput.getJarInputs().forEach(jarInput -> {
        try {
          // 加入第三方jar
          mGuysPatchMakerGeneratorConfig.getClassPool()
              .insertClassPath(jarInput.getFile().getAbsolutePath());
        } catch (NotFoundException e) {
          e.printStackTrace();
        }
      });

      transformInput.getDirectoryInputs().forEach(directoryInput -> {
        try {
          mGuysPatchMakerGeneratorConfig.getClassPool()
              .insertClassPath(directoryInput.getFile().getAbsolutePath());
        } catch (NotFoundException e) {
          e.printStackTrace();
        }
      });
    });
  }
}
