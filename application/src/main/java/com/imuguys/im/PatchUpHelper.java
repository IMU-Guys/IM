package com.imuguys.im;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import com.imuguys.hotfix.common.HotFixConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;

public class PatchUpHelper {

  private static final String TAG = "PatchUpHelper";
  private static final String PATCH_DIR_PATH =
      Environment.getExternalStorageDirectory() + "/shxy-test/patch/";
  private static final String PATCH_DEX_OPT_DIR_NAME = "patchDex";

  public static void tryPatchUp(Context context) {
    File patchDexFile =
        new File(PATCH_DIR_PATH + HotFixConfig.PATCH_DEX_FILE_NAME);
    File patchConfigFile =
        new File(PATCH_DIR_PATH + HotFixConfig.PATCH_CONFIG_FILE_NAME);
    if (!patchDexFile.exists() || !patchConfigFile.exists()) {
      return;
    }

    BufferedReader patchConfigFileReader = null;
    try {
      patchConfigFileReader =
          new BufferedReader(new InputStreamReader(new FileInputStream(patchConfigFile)));
      String line;
      if (!checkVersion(context, patchConfigFileReader.readLine())) {
        return;
      }
      DexClassLoader patchClassLoader = new DexClassLoader(
          patchDexFile.getAbsolutePath(),
          context.getDir(PATCH_DEX_OPT_DIR_NAME, Context.MODE_PRIVATE).getAbsolutePath(),
          null,
          Thread.currentThread().getContextClassLoader());
      while ((line = patchConfigFileReader.readLine()) != null) {
        String[] classNameArray = line.split(HotFixConfig.CONFIG_FILE_SPLIT);
        Class<?> bugClass =
            Thread.currentThread().getContextClassLoader().loadClass(classNameArray[0]);
        Class<?> patchClass = patchClassLoader.loadClass(classNameArray[1]);
        Field sGuysHotFixPatch = bugClass.getDeclaredField(HotFixConfig.PATCH_FIELD_NAME);
        sGuysHotFixPatch.setAccessible(true);
        sGuysHotFixPatch.set(null, patchClass.newInstance());
      }
      Log.i(TAG, "patch ok");
    } catch (IOException | ClassNotFoundException | NoSuchFieldException | IllegalAccessException
        | InstantiationException e) {
      e.printStackTrace();
    } finally {
      try {
        patchConfigFileReader.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static boolean checkVersion(Context context, String patchVersionName) {
    PackageManager manager = context.getPackageManager();
    String appVersionName = "";
    try {
      PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
      appVersionName = info.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    return appVersionName.equals(patchVersionName);
  }
}
