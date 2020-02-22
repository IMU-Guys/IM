package com.imuguys.hotfix.patch;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformOutputProvider;
import com.imuguys.hotfix.common.HotFixConfig;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class GuysPatchMakerGenerator {

  private CtClass mModifyClassAnnotationCtClass;
  private GuysPatchMakerGeneratorConfig mGuysPatchMakerGeneratorConfig;

  public GuysPatchMakerGenerator(GuysPatchMakerGeneratorConfig guysPatchMakerGeneratorConfig) {
    mGuysPatchMakerGeneratorConfig = guysPatchMakerGeneratorConfig;
  }

  public void processJatInput(JarInput jarInput, TransformOutputProvider outputProvider,
      JarOutputStream patchJarOutputStream, PrintWriter patchConfigPrintWriter) {
    try {
      tryInitAnnotationCtClasses();
      File dest = outputProvider.getContentLocation(jarInput.getName(), jarInput.getContentTypes(),
          jarInput.getScopes(), Format.JAR);
      JarFile jarFile = new JarFile(jarInput.getFile());
      if (needGeneratePathClass(jarFile.entries())) {
        System.out.println("find a patch class : " + jarFile.getName());
        doGeneratePatch(jarFile.entries(), patchJarOutputStream, patchConfigPrintWriter);
      }
      FileUtils.copyFile(jarInput.getFile(), dest);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void doGeneratePatch(Enumeration<JarEntry> entries,
      JarOutputStream patchJarOutputStream, PrintWriter patchConfigPrintWriter) {
    while (entries.hasMoreElements()) {
      JarEntry jarEntry = entries.nextElement();
      try {
        if (jarEntry.getName().endsWith(".class")) {
          String className = convertClassFileNameToClassName(jarEntry.getName());
          CtClass srcClass = mGuysPatchMakerGeneratorConfig.getClassPool()
              .get(className);
          if (srcClass
              .hasAnnotation(mGuysPatchMakerGeneratorConfig.getPatchClassAnnotationClassName())) {
            System.out.println("class : " + className);
            CtClass patchClass =
                mGuysPatchMakerGeneratorConfig.getClassPool()
                    .makeClass(className + HotFixConfig.PATCH_CLASS_SUFFIX);
            patchClass.setSuperclass(mGuysPatchMakerGeneratorConfig.getClassPool()
                .get(mGuysPatchMakerGeneratorConfig.getPatchClassName()));
            for (CtMethod srcMethod : srcClass.getMethods()) {
              if (srcMethod
                  .hasAnnotation(
                      mGuysPatchMakerGeneratorConfig.getPatchMethodAnnotationClassName())) {
                System.out.println("srcMethod : " + srcMethod.getName());
                CtMethod methodInPatch = new CtMethod(srcMethod, patchClass, null);
                // todo 目前只是简单的语句可以注入，涉及到原有host的属性、参数都会有问题，后续处理
                patchClass.addMethod(methodInPatch);
              }
            }
            System.out.println("write :" + patchClass.getName());
            patchClass.writeFile();
            writeClassToPatchJar(patchClass, patchJarOutputStream, jarEntry.getName());
            patchConfigPrintWriter
                .println(className + HotFixConfig.CONFIG_FILE_SPLIT + className
                    + HotFixConfig.PATCH_CLASS_SUFFIX);
          }
        }
      } catch (NotFoundException | CannotCompileException | IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

  private void writeClassToPatchJar(CtClass patchClass, JarOutputStream patchJarOutputStream,
      String sourceJarEntryName) {
    try {
      String targetJarEntryName =
          sourceJarEntryName.substring(0, sourceJarEntryName.lastIndexOf("."))
              + HotFixConfig.PATCH_CLASS_SUFFIX + ".class";
      patchJarOutputStream.putNextEntry(new ZipEntry(targetJarEntryName));
      patchJarOutputStream.write(patchClass.toBytecode());
      patchJarOutputStream.closeEntry();
      patchJarOutputStream.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private boolean needGeneratePathClass(Enumeration<JarEntry> entries)
      throws NotFoundException {
    while (entries.hasMoreElements()) {
      JarEntry jarEntry = entries.nextElement();
      if (jarEntry.getName().endsWith(".class")) {
        CtClass ctClass = mGuysPatchMakerGeneratorConfig.getClassPool()
            .get(convertClassFileNameToClassName(jarEntry.getName()));
        if (ctClass == null) {
          continue;
        }
        if (ctClass
            .hasAnnotation(mGuysPatchMakerGeneratorConfig.getPatchClassAnnotationClassName())) {
          return true;
        }
      }
    }
    return false;
  }

  private void tryInitAnnotationCtClasses() {
    if (mModifyClassAnnotationCtClass == null) {
      try {
        mModifyClassAnnotationCtClass = mGuysPatchMakerGeneratorConfig.getClassPool()
            .get(mGuysPatchMakerGeneratorConfig.getPatchClassAnnotationClassName());
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

  private String convertClassFileNameToClassName(String classFileName) {
    return classFileName.substring(0, classFileName.lastIndexOf(".")).replaceAll("/", ".");
  }
}
