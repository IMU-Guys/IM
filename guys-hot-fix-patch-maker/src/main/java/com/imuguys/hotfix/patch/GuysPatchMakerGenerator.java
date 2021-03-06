package com.imuguys.hotfix.patch;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformOutputProvider;
import com.google.common.collect.Lists;
import com.imuguys.hotfix.common.HotFixConfig;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
            Set<CtMethod> hostMethodSet = new HashSet<>();
            Set<CtField> hostFieldSet = new HashSet<>();
            Set<String> addMethodLongNameSet = new HashSet<>();
            Map<CtMethod,CtMethod> addMethodMap = new HashMap<>();
            // 复制类，防止在处理语句时发生的找不到类异常
            cloneCtClass(srcClass, patchClass, hostMethodSet, hostFieldSet, addMethodLongNameSet, addMethodMap);
            System.out.println("add method:");
            addMethodLongNameSet.forEach(System.out::println);
            // 添加宿主成员变量
            addHostField(srcClass, patchClass);
            // 用于注入宿主
            addPatchConstructor(srcClassName, patchClass);
            // 添加获取真正参数的方法
            addGetRealParamsMethod(patchClass);
            for (CtMethod srcMethod : srcClass.getDeclaredMethods()) {
              if (isModifiedMethod(srcMethod) || isAddedMethod(srcMethod)) {
                System.out.println("srcMethod : " + srcMethod.getName());
                CtMethod methodInPatch;
                if (isModifiedMethod(srcMethod)) {
                  // modify method
                  methodInPatch = new CtMethod(srcMethod, patchClass, null);
                } else {
                  // add method
                  methodInPatch = addMethodMap.get(srcMethod);
                }
                // todo 目前只是简单的语句可以注入，涉及到原有host的属性、参数都会有问题，后续处理
                methodInPatch.instrument(new ExprEditor() {
                  @Override
                  public void edit(FieldAccess f) throws CannotCompileException {
                    super.edit(f);
                    try {
                      String fieldRealDeclaredClassName =
                          f.getField().getDeclaringClass().getName().endsWith(patchClass.getName())
                              ? srcClass.getName()
                              : f.getField().getDeclaringClass().getName();
                      if (f.isReader()) {
                        StringBuilder sb = new StringBuilder("{");
                        sb.append("java.lang.Object instance;")
                            .append("if ($0 instanceof ").append(patchClass.getName()).append(") {")
                            .append("instance = ((").append(patchClass.getName())
                            .append(")$0).mHost;}")
                            .append("else {instance = $0;}")
                            .append("$_ = ($r) ")
                            .append("com.imuguys.hotfix.common.GuysReflectUtils.getFieldValue(")
                            .append(fieldRealDeclaredClassName).append(".class")
                            .append(",")
                            .append("\"").append(f.getFieldName()).append("\"").append(",")
                            .append("instance").append(");");
                        sb.append("}");
                        System.out.println(sb.toString());
                        f.replace(sb.toString());
                      } else {
                        StringBuilder sb = new StringBuilder("{");
                        sb.append("java.lang.Object instance;")
                            .append("if ($0 instanceof ").append(patchClass.getName()).append(") {")
                            .append("instance = ((").append(patchClass.getName())
                            .append(")$0).mHost;}")
                            .append("else {instance = $0;}")
                            .append("com.imuguys.hotfix.common.GuysReflectUtils.setFieldValue(")
                            .append(fieldRealDeclaredClassName).append(".class")
                            .append(",")
                            .append("\"").append(f.getFieldName()).append("\"").append(",")
                            .append("instance").append(",$1").append(");");
                        sb.append("}");
                        System.out.println(sb.toString());
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
                      if (m.getMethod().getDeclaringClass().getName()
                          .equals(patchClass.getName())
                          && !addMethodLongNameSet.contains(m.getMethod().getLongName())) {
                        methodDeclaredClassName = srcClass.getName();
                      } else {
                        methodDeclaredClassName = m.getMethod().getDeclaringClass().getName();
                      }
                      StringBuilder sb = new StringBuilder("{");
                      if ((m.getMethod().getModifiers() & Modifier.STATIC) > 0) { // 静态方法
                        if ((srcMethod.getModifiers() & Modifier.STATIC) > 0) { // 在静态方法中
                          sb.append("$_ = (").append("$r").append(")")
                              .append("com.imuguys.hotfix.common.GuysReflectUtils.invokeMethod(")
                              .append(methodDeclaredClassName).append(".class")
                              .append(",")
                              .append("\"").append(m.getMethodName()).append("\"").append(",")
                              .append("$sig").append(",")
                              .append("$args").append(",")
                              .append("null").append(");");
                        } else {
                          sb.append("$_ = (").append("$r").append(")")
                              .append("com.imuguys.hotfix.common.GuysReflectUtils.invokeMethod(")
                              .append(methodDeclaredClassName).append(".class")
                              .append(",")
                              .append("\"").append(m.getMethodName()).append("\"").append(",")
                              .append("$sig").append(",")
                              .append("getRealParameter($args)").append(",")
                              .append("null").append(");");
                        }
                      } else {
                        if ((srcMethod.getModifiers() & Modifier.STATIC) > 0) { // 在静态方法中
                          sb.append("$_ = (").append("$r").append(")")
                              .append("com.imuguys.hotfix.common.GuysReflectUtils.invokeMethod(")
                              .append(methodDeclaredClassName).append(".class")
                              .append(",")
                              .append("\"").append(m.getMethodName()).append("\"").append(",")
                              .append("$sig").append(",")
                              .append("$args").append(",")
                              .append("$0").append(");");
                        } else {
                          if (addMethodLongNameSet.contains(m.getMethod().getLongName())) {
                            sb.append("$_ = (").append("$r").append(")")
                                .append("com.imuguys.hotfix.common.GuysReflectUtils.invokeMethod(")
                                .append(methodDeclaredClassName).append(".class")
                                .append(",")
                                .append("\"").append(m.getMethodName()).append("\"").append(",")
                                .append("$sig").append(",")
                                .append("getRealParameter($args)").append(",")
                                .append("$0").append(");");
                          } else {
                            sb.append("java.lang.Object instance;")
                                .append("if ($0 instanceof ").append(patchClass.getName())
                                .append(") {")
                                .append("instance = ((").append(patchClass.getName())
                                .append(")$0).mHost;}")
                                .append("else {instance = $0;}")
                                .append("$_ = (").append("$r").append(")")
                                .append("com.imuguys.hotfix.common.GuysReflectUtils.invokeMethod(")
                                .append(methodDeclaredClassName).append(".class")
                                .append(",")
                                .append("\"").append(m.getMethodName()).append("\"").append(",")
                                .append("$sig").append(",")
                                .append("getRealParameter($args)").append(",")
                                .append("instance").append(");");
                          }
                        }
                      }
                      sb.append("}");
                      System.out.println("m: " + sb.toString());
                      m.replace(sb.toString());
                    } catch (NotFoundException e) {
                      e.printStackTrace();
                    }
                  }
                });
                System.out.println("xxx");
                if (isModifiedMethod(srcMethod)) {
                  patchClass.addMethod(methodInPatch);
                }
              }
            }

            // 删除从宿主中复制到补丁中的方法和属性
            clearCtClass(patchClass, hostMethodSet, hostFieldSet, addMethodLongNameSet);

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

  private void addGetRealParamsMethod(CtClass patchClass) {
    try {
      CtMethod reaLParameterMethod = CtMethod.make(getRealParametersBody(), patchClass);
      patchClass.addMethod(reaLParameterMethod);
    } catch (CannotCompileException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
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
            + srcClassName
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
    addPatchControllerCtClassMap(srcClass, patchControllerCtClass);
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
        .append("$6.getClass().getDeclaredField(\"mGuysHotFixPatch\").set($6,hostPatch);")
        .append("}")
        .append(
            "return ($r)hostPatch.getClass().getDeclaredMethod($1, $3).invoke(hostPatch, $2);")
        .append("}catch (Exception e) {e.printStackTrace();} return null;}");
    System.out.println(sb.toString());
    invokePatchMethod.setBody(sb.toString());
    invokePatchMethod.setModifiers(invokePatchMethod.getModifiers() & ~Modifier.ABSTRACT);

    CtMethod invokePatchCheckedMethod =
        patchControllerCtClass.getDeclaredMethod("needInvokePatchMethod");
    invokePatchCheckedMethod.setBody("return mMethodNameSet.contains($1);");
    invokePatchCheckedMethod
        .setModifiers(invokePatchCheckedMethod.getModifiers() & ~Modifier.ABSTRACT);

    patchControllerCtClass.setModifiers(patchControllerCtClass.getModifiers() & ~Modifier.ABSTRACT);
    return patchControllerCtClass;
  }

  /**
   * 给补丁控制类写入Set<String>,用来判断是否需要执行补丁方法。
   * @param srcClass 原有的bug类
   */
  private void addPatchControllerCtClassMap(CtClass srcClass, CtClass patchControllerCtClass) {
    // todo 抽取成变量
    try {
      CtField patchedMethodsNameSet =
          new CtField(mGuysPatchMakerGeneratorConfig.getClassPool().get("java.util.Set"),
              "mMethodNameSet", patchControllerCtClass);
      patchedMethodsNameSet.setModifiers(Modifier.STATIC | Modifier.PRIVATE);
      patchControllerCtClass.addField(patchedMethodsNameSet,"new java.util.HashSet()");

      CtConstructor patchControllerCtClassStaticBlock =
          patchControllerCtClass.makeClassInitializer();
      StringBuilder sb = new StringBuilder("{");
      for (CtMethod ctMethod : srcClass.getDeclaredMethods()) {
        if (ctMethod
            .hasAnnotation(mGuysPatchMakerGeneratorConfig.getPatchMethodAnnotationClassName())) {
          sb.append("mMethodNameSet.add(\"").append(ctMethod.getLongName()).append("\");");
        }
      }
      sb.append("}");
      System.out.println(sb.toString());
      patchControllerCtClassStaticBlock.setBody(sb.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void clearCtClass(CtClass patchClass, Set<CtMethod> mHostMethodSet,
      Set<CtField> mHostFieldSet, Set<String> addMethodLongNameSet) {
    mHostFieldSet.forEach(ctField -> {
      try {
        patchClass.removeField(ctField);
      } catch (NotFoundException e) {
        e.printStackTrace();
      }
    });

    mHostMethodSet.stream()
        .filter(ctMethod -> !addMethodLongNameSet.contains(ctMethod.getLongName()))
        .forEach(method -> {
          try {
            patchClass.removeMethod(method);
          } catch (NotFoundException e) {
            e.printStackTrace();
          }
        });
    try {
      patchClass
          .setSuperclass(mGuysPatchMakerGeneratorConfig.getClassPool().get("java.lang.Object"));
    } catch (CannotCompileException | NotFoundException e) {
      e.printStackTrace();
    }
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
      Set<CtField> fields, Set<String> addMethodLongNameSet, Map<CtMethod,CtMethod> addMethodMap)
      throws CannotCompileException, NotFoundException {
    patchClass.setSuperclass(srcClass.getSuperclass());
    for (CtMethod srcMethod : srcClass.getDeclaredMethods()) {
      if (!isModifiedMethod(srcMethod)) {
        CtMethod methodInPatch = new CtMethod(srcMethod, patchClass, null);
        patchClass.addMethod(methodInPatch);
        methods.add(methodInPatch);
        if (isAddedMethod(srcMethod)) {
          addMethodLongNameSet.add(methodInPatch.getLongName());
          addMethodMap.put(srcMethod,methodInPatch);
        }
      }
    }
    for (CtField srcField : srcClass.getDeclaredFields()) {
      CtField fieldInPatch = new CtField(srcField, patchClass);
      patchClass.addField(fieldInPatch);
      fields.add(fieldInPatch);
    }
  }

  // copy from Robust
  // 把属于Patch类的属性修改为Patch.mHost对象的属性
  public static String getRealParametersBody() {
    StringBuilder realParameterBuilder = new StringBuilder();
    realParameterBuilder.append("public  Object[] " + "getRealParameter" + " (Object[] args){");
    realParameterBuilder.append("if (args == null || args.length < 1) {");
    realParameterBuilder.append(" return args;");
    realParameterBuilder.append("}");
    realParameterBuilder.append(" Object[] realParameter = new Object[args.length];");
    realParameterBuilder.append("for (int i = 0; i < args.length; i++) {");
    realParameterBuilder.append("if (args[i] instanceof Object[]) {");
    realParameterBuilder
        .append("realParameter[i] =" + "getRealParameter" + "((Object[]) args[i]);");
    realParameterBuilder.append("} else {");
    realParameterBuilder.append("if (args[i] ==this) {");
    realParameterBuilder.append(" realParameter[i] =this." + "mHost" + ";");
    realParameterBuilder.append("} else {");
    realParameterBuilder.append(" realParameter[i] = args[i];");
    realParameterBuilder.append(" }");
    realParameterBuilder.append(" }");
    realParameterBuilder.append(" }");
    realParameterBuilder.append("  return realParameter;");
    realParameterBuilder.append(" }");
    return realParameterBuilder.toString();
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
  
  private boolean isModifiedMethod(CtMethod method) {
    return method.hasAnnotation(mGuysPatchMakerGeneratorConfig.getPatchMethodAnnotationClassName());
  }
  
  private boolean isAddedMethod(CtMethod method) {
    return method.hasAnnotation(mGuysPatchMakerGeneratorConfig.getPatchAddMethodAnnotationClassName());
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
