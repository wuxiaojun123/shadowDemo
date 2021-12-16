package com.tencent.shadow.dynamic.apk;

import android.annotation.SuppressLint;
import android.os.Build;

import com.tencent.shadow.core.common.InstalledApk;

import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;

public abstract class ImplLoader {

    private static final String WHITE_LIST_CLASS_NAME = "com.tencent.shadow.dynamic.impl.whiteList";
    private static final String WHITE_LIST_FIELD_NAME = "sWhiteList";

    protected abstract String[] getCustomWhiteList();

    public String[] loadWhiteList(InstalledApk installedApk) {
        return loadWhiteList(installedApk,WHITE_LIST_CLASS_NAME,WHITE_LIST_FIELD_NAME);
    }

    public String[] loadWhiteList(InstalledApk installedApk, String whiteListClassName, String whiteListFiledName) {
        @SuppressLint({"NewApi", "LocalSuppress"}) DexClassLoader dexClassLoader = new DexClassLoader(
                installedApk.apkFilePath,
                installedApk.oDexPath,
                installedApk.libraryPath,
                getClass().getClassLoader()
        );

        String[] whiteList = null;
        try {
            Class<?> whiteListClass = dexClassLoader.loadClass(whiteListClassName);
            Field whiteListField = whiteListClass.getDeclaredField(whiteListFiledName);
            Object o = whiteListField.get(null);
            whiteList = (String[]) o;
        } catch (ClassNotFoundException ignored){

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        String[] interfaces;
        if (whiteList != null) {
            interfaces = concatenate(getCustomWhiteList(),whiteList);
        } else {
            interfaces = getCustomWhiteList();
        }
        return interfaces;
    }

    private String[] concatenate(String[] a,String[] b) {
        int aLen = a.length;
        int bLen = b.length;
        String[] c = new String[aLen + bLen];
        System.arraycopy(a,0,c,0,aLen);
        System.arraycopy(b,0,c,aLen,bLen);
        return c;
    }

}
