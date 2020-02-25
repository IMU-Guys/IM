package com.imuguys.hotfix.patch;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformOutputProvider;
import com.imuguys.hotfix.common.HotFixConfig;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

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
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.NotFoundException;
import javassist.expr.Cast;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

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
          String srcClassName = convertClassFileNameToClassName(jarEntry.getName());
          CtClass srcClass = mGuysPatchMakerGeneratorConfig.getClassPool()
              .get(srcClassName);
          if (srcClass
              .hasAnnotation(mGuysPatchMakerGeneratorConfig.getPatchClassAnnotationClassName())) {
            System.out.println("class : " + srcClassName);
            CtClass patchClass =
                mGuysPatchMakerGeneratorConfig.getClassPool()
                    .makeClass(srcClassName + HotFixConfig.PATCH_CLASS_SUFFIX);
            CtField hostField =
                new CtField(mGuysPatchMakerGeneratorConfig.getClassPool().get(srcClassName),
                    "mHost", patchClass);
            patchClass.addField(hostField);
            StringBuilder patchConstructStringBuilder = new StringBuilder();
            patchConstructStringBuilder.append(" public Patch(Object o) {").append("mHost=(")
                .append(srcClassName).append(")o;")
                .append("}");
            CtConstructor patchConstructor =
                CtNewConstructor.make(patchConstructStringBuilder.toString(),
                    patchClass);
            patchClass.addConstructor(patchConstructor);
            for (CtMethod srcMethod : srcClass.getDeclaredMethods()) {
              if (!srcMethod
                  .hasAnnotation(
                      mGuysPatchMakerGeneratorConfig.getPatchMethodAnnotationClassName())) {
                CtMethod methodInPatch = new CtMethod(srcMethod, patchClass, null);
                patchClass.addMethod(methodInPatch);
              }
            }
            for (CtMethod srcMethod : srcClass.getDeclaredMethods()) {
              if (srcMethod
                  .hasAnnotation(
                      mGuysPatchMakerGeneratorConfig.getPatchMethodAnnotationClassName())) {
                System.out.println("srcMethod : " + srcMethod.getName());
                CtMethod methodInPatch = new CtMethod(srcMethod, patchClass, null);
                // todo 目前只是简单的语句可以注入，涉及到原有host的属性、参数都会有问题，后续处理
                methodInPatch.instrument(new ExprEditor() {
                  @Override
                  public void edit(FieldAccess f) throws CannotCompileException {
                    super.edit(f);
                    if (f.isReader()) {
                      StringBuilder sb = new StringBuilder("{");
                      sb.append("$_ = (").append("$r").append(")")
                          .append("com.imuguys.hotfix.common.GuysReflectUtils.getFieldValue(")
                          .append("\"").append(srcClassName).append("\"").append(",")
                          .append("\"").append(f.getFieldName()).append("\"").append(",")
                          .append("mHost").append(");");
                      sb.append("}");
                      System.out.println(sb.toString());
                      f.replace(sb.toString());
                    } else {
                      StringBuilder sb = new StringBuilder("{");
                      sb.append("com.imuguys.hotfix.common.GuysReflectUtils.setFieldValue(")
                          .append("\"").append(srcClassName).append("\"").append(",")
                          .append("\"").append(f.getFieldName()).append("\"").append(",")
                          .append("mHost").append(",")
                          .append("$1")
                          .append(");");
                      sb.append("}");
                      System.out.println(sb.toString());
                      f.replace(sb.toString());
                    }
                  }

                  @Override
                  public void edit(NewExpr e) throws CannotCompileException {
                    super.edit(e);
                  }

                  @Override
                  public void edit(Cast c) throws CannotCompileException {
                    super.edit(c);
                    System.out.println("c" + c.toString());
                  }

                  @Override
                  public void edit(MethodCall m) throws CannotCompileException {
                    super.edit(m);
                    try {
                      if (false) {

                      } else {
                        StringBuilder sb = new StringBuilder("{");
                        sb.append("$_ = (").append("$r").append(")")
                            .append("com.imuguys.hotfix.common.GuysReflectUtils.invokeMethod(")
                            .append("\"");
                        if (m.getMethod().getDeclaringClass().getName()
                            .equals(patchClass.getName())) {
                          sb.append(hostField.getType().getName());
                        } else {
                          sb.append(m.getMethod().getDeclaringClass().getName());
                        }
                        sb.append("\"").append(",")
                            .append("\"").append(m.getMethodName()).append("\"").append(",")
                            .append("$sig").append(",")
                            .append("$args").append(",");
                        if (m.getMethod().getDeclaringClass().getName()
                            .equals(patchClass.getName())) {
                          sb.append("mHost").append(");");
                        } else {
                          sb.append("$0").append(");");
                        }

                        sb.append("}");
                        System.out.println(sb.toString());
                        m.replace(sb.toString());
                      }
                    } catch (NotFoundException e) {
                      e.printStackTrace();
                    }
                  }
                });
                System.out.println("xxx");
                patchClass.addMethod(methodInPatch);
              }
            }
            System.out.println("write :" + patchClass.getName());
            patchClass.writeFile();
            writeClassToPatchJar(patchClass, patchJarOutputStream, jarEntry.getName());
            patchConfigPrintWriter
                .println(srcClassName + HotFixConfig.CONFIG_FILE_SPLIT + srcClassName
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

  @NotNull
  private String generateParamsTypeString(CtMethod method) throws NotFoundException {
    StringBuilder paramsTypeStringBuilder = new StringBuilder();
    if (method.getParameterTypes().length == 0) {
      paramsTypeStringBuilder.append("null");
    } else {
      paramsTypeStringBuilder.append("new java.lang.Class[]{");
      for (int i = 0; i < method.getParameterTypes().length; i++) {
        CtClass paramTypeCtClass = method.getParameterTypes()[i];
        if (i == 0) {
          paramsTypeStringBuilder.append(paramTypeCtClass.getName()).append(".class");
        } else {
          paramsTypeStringBuilder.append(",").append(paramTypeCtClass.getName()).append(".class");
        }
      }
      paramsTypeStringBuilder.append("}");
    }
    return paramsTypeStringBuilder.toString();
  }
}
