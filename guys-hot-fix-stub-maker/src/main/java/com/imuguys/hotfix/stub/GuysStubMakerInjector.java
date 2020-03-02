package com.imuguys.hotfix.stub;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformOutputProvider;
import com.imuguys.hotfix.common.HotFixConfig;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

// 桩 注入器
public class GuysStubMakerInjector {

  private GuysStubMakerContext mGuysStubMakerContext;

  public GuysStubMakerInjector(GuysStubMakerContext guysStubMakerContext) {
    mGuysStubMakerContext = guysStubMakerContext;
  }

  public void processJatInput(JarInput jarInput, TransformOutputProvider transformOutputProvider) {
    ZipOutputStream jarOutPutStream = null;
    File destJarFile = transformOutputProvider.getContentLocation(jarInput.getName(),
        jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
    try {
      JarFile jarFile = new JarFile(jarInput.getFile());
      Enumeration<JarEntry> enumeration = jarFile.entries();
      if (isJarContainTargetClass(enumeration)) {
        jarOutPutStream = new ZipOutputStream(
            new FileOutputStream(destJarFile));
        System.out.println("find a target jar, do inject :" + jarFile.getName());
        try {
          doInject(jarOutPutStream, jarFile, jarFile.entries());
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          jarOutPutStream.close();
        }
      } else {
        FileUtils.copyFile(jarInput.getFile(), destJarFile);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void doInject(ZipOutputStream jarOutPutStream, JarFile jarFile,
      Enumeration<JarEntry> enumeration) throws NotFoundException, IOException {
    while (enumeration.hasMoreElements()) {
      JarEntry jarEntry = enumeration.nextElement();
      // class文件的名称，这里是全路径类名，包名之间以"/"分隔
      String entryName = jarEntry.getName();
      System.out.println("jarEntry : " + entryName);
      String classname = "";
      if (entryName.endsWith(".class")) { // 避免非 .class文件进入
        classname = entryName.substring(0, entryName.lastIndexOf(".")).replaceAll("/", ".");
      }
      if (mGuysStubMakerContext.getTargetClassPredicate().test(entryName)) {
        CtClass targetClass = mGuysStubMakerContext.getClassPool()
            .get(classname);
        if (targetClass == null) {
          System.err.println("targetClass == null");
          continue;
        }
        if (targetClass.getName().contains("BuildConfig")) {
          writeClassByteCode(targetClass, jarOutPutStream, entryName);
          continue;
        }
        // 抽象类、接口跳过
        if ((targetClass.getModifiers() & (Modifier.ABSTRACT | Modifier.INTERFACE)) > 0) {
          writeClassByteCode(targetClass, jarOutPutStream, entryName);
          continue;
        }
        CtMethod errorMethod = null;
        try {
          CtField patchControllerFiled = new CtField(
              mGuysStubMakerContext.getClassPool()
                  .get(GuysStubMakerContext.mIPatchControllerClassName),
              HotFixConfig.PATCH_CONTROLLER_FIELD_NAME, targetClass);
          patchControllerFiled.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
          targetClass.addField(patchControllerFiled);
          CtField patchField =
              new CtField(mGuysStubMakerContext.getClassPool().get("java.lang.Object"),
                  HotFixConfig.PATCH_FIELD_NAME, targetClass);
          patchField.setModifiers(Modifier.PUBLIC);
          targetClass.addField(patchField);
          for (CtMethod method : targetClass.getDeclaredMethods()) {
            errorMethod = method;
            // 抽象方法，native方法跳过
            if ((method.getModifiers() & (Modifier.ABSTRACT | Modifier.NATIVE)) > 0) {
              continue;
            }
            // lambda配置
            if (mGuysStubMakerContext.isExcludeLambda() && method.getName().contains("lambda")) {
              continue;
            }
            method.insertBefore(createPatchInvokeString(method));
          }
          targetClass.writeFile();
          targetClass.detach();
          writeClassByteCode(targetClass, jarOutPutStream, entryName);
        } catch (Exception e) {
          e.printStackTrace();
          if (errorMethod != null) {
            System.out.println(errorMethod.getLongName());
          }
          System.out.println(targetClass.getName());
          throw new RuntimeException(e);
        }
      } else {
        writeClassByteCode(
            IOUtils.toByteArray(jarFile.getInputStream(new ZipArchiveEntry(entryName))),
            jarOutPutStream, entryName);
      }
    }
  }

  /**
   * 是否包含需要插桩的类
   * 
   * @param enumeration Jar中的所有Entry
   * @return
   */
  private boolean isJarContainTargetClass(Enumeration<JarEntry> enumeration) {
    while (enumeration.hasMoreElements()) {
      JarEntry jarEntry = enumeration.nextElement();
      String entryName = jarEntry.getName();
      if (mGuysStubMakerContext.getTargetClassPredicate().test(entryName)) {
        return true;
      }
    }
    return false;
  }

  // 将bytecode写入到Jar对应的流中
  private void writeClassByteCode(CtClass ctClass, ZipOutputStream zos, String entryName) {
    try {
      // System.out.println("writeClassByteCode : " + ctClass.getName());
      writeClassByteCode(ctClass.toBytecode(), zos, entryName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // 将bytecode写入到Jar对应的流中
  private void writeClassByteCode(byte[] byteCode, ZipOutputStream zos, String entryName) {
    try {
      ZipEntry entry = new ZipEntry(entryName);
      zos.putNextEntry(entry);
      zos.write(byteCode, 0, byteCode.length);
      zos.closeEntry();
      zos.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String createPatchInvokeString(CtMethod method)
      throws NotFoundException {
    StringBuilder sb = new StringBuilder();
    sb.append("if (").append(HotFixConfig.PATCH_CONTROLLER_FIELD_NAME).append(" != null) {");
    if (!method.getReturnType().getName().equals("void")) {
      sb.append("return ")
          .append("($r)");
    }
    String isStaticString;
    String instance;
    if ((method.getModifiers() & Modifier.STATIC) > 0) {
      isStaticString = "true";
      instance = "null";
    } else {
      isStaticString = "false";
      instance = "$0";
    }
    String paramsTypeString = generateParamsTypeString(method);
    sb.append(HotFixConfig.PATCH_CONTROLLER_FIELD_NAME)
        .append(".invokePatch(")
        .append("\"")
        .append(method.getName())
        .append("\"").append(", $args,")
        .append(paramsTypeString).append(",")
        .append(isStaticString).append(",")
        .append("$type").append(",")
        .append(instance)
        .append(");");

    if (method.getReturnType().getName().equals("void")) {
      sb.append("return;}");
    } else {
      sb.append("}");
    }
    System.out.println(sb.toString());
    return sb.toString();
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
