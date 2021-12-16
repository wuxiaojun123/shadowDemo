package com.tencent.shadow.dynamic.apk;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.view.LayoutInflater;

/**
 * 修改context的apk路径的wrapper，可将原context的resource和classloader重新修改为新的apk
 */
public class ChangeApkContextWrapper extends ContextWrapper {

    private Resources mResources;

    private LayoutInflater mLayoutInflater;

    final private ClassLoader mClassloader;

    public ChangeApkContextWrapper(Context base,String apkPath,ClassLoader classLoader) {
        super(base);
        this.mClassloader = classLoader;
        mResources = createResources(apkPath,base);
    }

    private Resources createResources(String apkPath, Context base) {
        PackageManager packageManager = base.getPackageManager();
        PackageInfo packageArchiveInfo = packageManager.getPackageArchiveInfo(apkPath,PackageManager.GET_META_DATA);
        packageArchiveInfo.applicationInfo.publicSourceDir = apkPath;
        packageArchiveInfo.applicationInfo.sourceDir = apkPath;
        try {
            return packageManager.getResourcesForApplication(packageArchiveInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AssetManager getAssets() {
        return mResources.getAssets();
    }

    @Override
    public Resources getResources() {
        return mResources;
    }

    @Override
    public Resources.Theme getTheme() {
        return mResources.newTheme();
    }

    @Override
    public Object getSystemService(String name) {

        if (Context.LAYOUT_INFLATER_SERVICE.equals(name)) {
            if(mLayoutInflater == null) {
                LayoutInflater layoutInflater = (LayoutInflater) super.getSystemService(name);
                mLayoutInflater = layoutInflater.cloneInContext(this);
            }
            return mLayoutInflater;
        }

        return super.getSystemService(name);
    }

    @Override
    public ClassLoader getClassLoader() {
        return mClassloader;
    }
}
