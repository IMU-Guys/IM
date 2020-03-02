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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
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
import javassist.Modifier;
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
            Set<CtMethod> mHostMethodSet = new HashSet<>();
            Set<CtField> mHostFieldSet = new HashSet<>();
            // 复制类，防止在处理语句时发生的找不到类异常
            cloneCtClass(srcClass, patchClass, mHostMethodSet, mHostFieldSet);
            // 添加宿主成员变量
            addHostField(srcClass, patchClass);
            // 用于注入宿主
            addPatchConstructor(srcClassName, patchClass);
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
                    try {
                      System.out.println(f.getField().getDeclaringClass().getName());
                      String fieldDeclaredClassName; // 属性所在的类名称
                      String fieldOwner; // 执行反射的对象的名称
                      if (f.getField().getDeclaringClass().getName().equals(patchClass.getName())) {
                        fieldDeclaredClassName = srcClass.getName();
                        fieldOwner = "mHost";
                      } else if (patchClass.subclassOf(f.getField().getDeclaringClass())) {
                        fieldDeclaredClassName = f.getField().getDeclaringClass().getName();
                        fieldOwner = "mHost";
                      } else {
                        fieldDeclaredClassName = f.getField().getDeclaringClass().getName();
                        fieldOwner = "$0";
                      }
                      if (f.isReader()) {
                        StringBuilder sb = new StringBuilder("{");
                        sb.append("$_ = (").append("$r").append(")")
                            .append("com.imuguys.hotfix.common.GuysReflectUtils.getFieldValue(")
                            .append("\"").append(fieldDeclaredClassName).append("\"").append(",")
                            .append("\"").append(f.getFieldName()).append("\"").append(",")
                            .append(fieldOwner).append(");");
                        sb.append("}");
                        System.out.println("f: " + sb.toString());
                        f.replace(sb.toString());
                      } else {
                        StringBuilder sb = new StringBuilder("{");
                        sb.append("com.imuguys.hotfix.common.GuysReflectUtils.setFieldValue(")
                            .append("\"").append(fieldDeclaredClassName).append("\"").append(",")
                            .append("\"").append(f.getFieldName()).append("\"").append(",")
                            .append(fieldOwner).append(",")
                            .append("$1")
                            .append(");");
                        sb.append("}");
                        System.out.println("f: " + sb.toString());
                        f.replace(sb.toString());
                      }
                    } catch (NotFoundException e) {
                      e.printStackTrace();
                    }
                  }

                  @Override
                  public void edit(NewExpr e) throws CannotCompileException {
                    super.edit(e);
                    System.out.println("e: " + e.getClassName());
                    StringBuilder sb = new StringBuilder("{");
                    sb.append("$_ = (").append("$r").append(")")
                        .append("com.imuguys.hotfix.common.GuysReflectUtils.invokeConstructor(")
                        .append("\"").append(e.getClassName()).append("\"").append(",")
                        .append("$sig").append(",")
                        .append("$args").append(");}");
                    e.replace(sb.toString());
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
                      System.out.println(m.getMethod().getDeclaringClass().getName());
                      String methodDeclaredClassName; // 函数所在的类名称
                      String methodOwner; // 执行反射的对象的名称
                      if (m.getMethod().getDeclaringClass().getName()
                          .equals(patchClass.getName())) {
                        methodDeclaredClassName = srcClass.getName();
                        methodOwner = "mHost";
                      } else {
                        methodDeclaredClassName = m.getMethod().getDeclaringClass().getName();
                        methodOwner = "$0";
                      }
                      StringBuilder sb = new StringBuilder("{");
                      sb.append("$_ = (").append("$r").append(")")
                          .append("com.imuguys.hotfix.common.GuysReflectUtils.invokeMethod(")
                          .append("\"")
                          .append(methodDeclaredClassName)
                          .append("\"").append(",")
                          .append("\"").append(m.getMethodName()).append("\"").append(",")
                          .append("$sig").append(",")
                          .append("$args").append(",")
                          .append(methodOwner).append(");");
                      sb.append("}");
                      System.out.println("m: " + sb.toString());
                      m.replace(sb.toString());
                    } catch (NotFoundException e) {
                      e.printStackTrace();
                    }
                  }
                });
                System.out.println("xxx");
                patchClass.addMethod(methodInPatch);
              }
            }

            // 删除从宿主中复制到补丁中的方法和属性
            clearCtClass(patchClass, mHostMethodSet, mHostFieldSet);

            CtClass patchControllerCtClass = createPatchControllerCtClass(srcClass, patchClass);
            System.out.println("write :" + patchClass.getName());
            // 测试用Class文件
            writeTestClassFile(patchClass, patchControllerCtClass);
            // 补丁文件
            writePatchClassAndConfig(patchJarOutputStream, patchConfigPrintWriter, jarEntry,
                srcClassName, patchClass, patchControllerCtClass);
          }
        }
      } catch (NotFoundException | CannotCompileException | IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

  private void writePatchClassAndConfig(JarOutputStream patchJarOutputStream,
      PrintWriter patchConfigPrintWriter, JarEntry jarEntry, String srcClassName,
      CtClass patchClass, CtClass patchControllerCtClass) {
    writeClassToPatchJar(patchClass, patchJarOutputStream, jarEntry.getName(),
        HotFixConfig.PATCH_CLASS_SUFFIX);
    writeClassToPatchJar(patchControllerCtClass, patchJarOutputStream, jarEntry.getName(),
        HotFixConfig.PATCH_CONTROLLER_CLASS_SUFFIX);

    patchConfigPrintWriter
        .println(srcClassName + HotFixConfig.CONFIG_FILE_SPLIT + srcClassName
            + HotFixConfig.PATCH_CLASS_SUFFIX + HotFixConfig.CONFIG_FILE_SPLIT
            + patchControllerCtClass.getName()
            + HotFixConfig.PATCH_CONTROLLER_CLASS_SUFFIX);
  }

  private void writeTestClassFile(CtClass patchClass, CtClass patchControllerCtClass)
      throws NotFoundException, IOException, CannotCompileException {
    patchClass.writeFile();
    patchControllerCtClass.writeFile();
  }

  private CtClass createPatchControllerCtClass(CtClass srcClass, CtClass patchClass)
      throws NotFoundException, CannotCompileException, IOException {
    CtClass patchControllerCtClass = mGuysPatchMakerGeneratorConfig.getClassPool()
        .makeClass(srcClass.getName() + HotFixConfig.PATCH_CONTROLLER_CLASS_SUFFIX);
    CtClass interfaceCtClass = mGuysPatchMakerGeneratorConfig.getClassPool()
        .get(mGuysPatchMakerGeneratorConfig.getPatchControllerClassName());
    patchControllerCtClass.addInterface(interfaceCtClass);
    for (CtMethod m : interfaceCtClass.getDeclaredMethods()) {
      CtMethod methodInPatchController = new CtMethod(m, patchControllerCtClass, null);
      patchControllerCtClass.addMethod(methodInPatchController);
    }
    CtMethod invokePatchMethod = patchControllerCtClass.getDeclaredMethod("invokePatch");
    StringBuilder sb = new StringBuilder();
    sb.append("{try {")
        .append("if ($4) {")
        .append("return ($r)").append(patchClass.getName())
        .append(
            ".class.getDeclaredMethod($1, $3).invoke(null, $2);}")
        .append(
            "Object hostPatch = $6.getClass().getDeclaredField(\"mGuysHotFixPatch\").get($6);")
        .append("if (hostPatch == null) { hostPatch = ").append("new ").append(patchClass.getName())
        .append("($6);")
        .append("$5.getClass().getDeclaredField(\"mGuysHotFixPatch\").set($6,hostPatch);")
        .append("}")
        .append(
            "return ($r)hostPatch.getClass().getDeclaredMethod($1, $3).invoke($6, $2);")
        .append("}catch (Exception e) {e.printStackTrace();} return null;}");
    System.out.println(sb.toString());
    invokePatchMethod.setBody(sb.toString());
    invokePatchMethod.setModifiers(invokePatchMethod.getModifiers() & ~Modifier.ABSTRACT);
    patchControllerCtClass.setModifiers(patchControllerCtClass.getModifiers() & ~Modifier.ABSTRACT);
    return patchControllerCtClass;
  }

  private void clearCtClass(CtClass patchClass, Set<CtMethod> mHostMethodSet,
      Set<CtField> mHostFieldSet) {
    mHostFieldSet.forEach(ctField -> {
      try {
        patchClass.removeField(ctField);
      } catch (NotFoundException e) {
        e.printStackTrace();
      }
    });

    mHostMethodSet.forEach(method -> {
      try {
        patchClass.removeMethod(method);
      } catch (NotFoundException e) {
        e.printStackTrace();
      }
    });
  }

  @NotNull
  private void addHostField(CtClass srcClass, CtClass patchClass)
      throws CannotCompileException, NotFoundException {
    CtField hostField =
        new CtField(mGuysPatchMakerGeneratorConfig.getClassPool().get(srcClass.getName()),
            "mHost", patchClass);
    patchClass.addField(hostField);
  }

  private void addPatchConstructor(String srcClassName, CtClass patchClass)
      throws CannotCompileException {
    StringBuilder patchConstructStringBuilder = new StringBuilder();
    patchConstructStringBuilder.append(" public Patch(Object o) {").append("mHost=(")
        .append(srcClassName).append(")o;")
        .append("}");
    CtConstructor patchConstructor =
        CtNewConstructor.make(patchConstructStringBuilder.toString(),
            patchClass);
    patchClass.addConstructor(patchConstructor);
  }

  private void cloneCtClass(CtClass srcClass, CtClass patchClass, Set<CtMethod> methods,
      Set<CtField> fields)
      throws CannotCompileException, NotFoundException {
    patchClass.setSuperclass(srcClass.getSuperclass());
    for (CtMethod srcMethod : srcClass.getDeclaredMethods()) {
      if (!srcMethod
          .hasAnnotation(
              mGuysPatchMakerGeneratorConfig.getPatchMethodAnnotationClassName())) {
        CtMethod methodInPatch = new CtMethod(srcMethod, patchClass, null);
        patchClass.addMethod(methodInPatch);
        methods.add(methodInPatch);
      }
    }
    for (CtField srcField : srcClass.getDeclaredFields()) {
      CtField fieldInPatch = new CtField(srcField, patchClass);
      patchClass.addField(fieldInPatch);
      fields.add(fieldInPatch);
    }
  }

  private void writeClassToPatchJar(CtClass patchClass, JarOutputStream patchJarOutputStream,
      String sourceJarEntryName, String suffix) {
    try {
      String targetJarEntryName =
          sourceJarEntryName.substring(0, sourceJarEntryName.lastIndexOf("."))
              + suffix + ".class";
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
