package com.tencent.shadow.core.manager;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.tencent.shadow.core.common.Logger;
import com.tencent.shadow.core.common.LoggerFactory;
import com.tencent.shadow.core.manager.installplugin.AppCacheFolderManager;
import com.tencent.shadow.core.manager.installplugin.CopySoBloc;
import com.tencent.shadow.core.manager.installplugin.InstallPluginException;
import com.tencent.shadow.core.manager.installplugin.InstalledDao;
import com.tencent.shadow.core.manager.installplugin.InstalledPlugin;
import com.tencent.shadow.core.manager.installplugin.InstalledPluginDBHelper;
import com.tencent.shadow.core.manager.installplugin.InstalledType;
import com.tencent.shadow.core.manager.installplugin.ODexBloc;
import com.tencent.shadow.core.manager.installplugin.PluginConfig;
import com.tencent.shadow.core.manager.installplugin.UnpackManager;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class BasePluginManager {

    private static final Logger mLogger = LoggerFactory.getLogger(BasePluginManager.class);

    /***
     * 宿主的context对象
     */
    public Context mHostContext;

    /***
     * 插件信息查询数据库接口
     */
    private UnpackManager mUnpackManager;

    final private InstalledDao mInstallDao;

    /**
     * UI线程的handler
     */
    protected Handler mUiHandler = new Handler(Looper.getMainLooper());

    public BasePluginManager(Context context) {
        this.mHostContext = context.getApplicationContext();
        this.mUnpackManager = new UnpackManager(mHostContext.getFilesDir(),getName());
        this.mInstallDao = new InstalledDao(new InstalledPluginDBHelper(mHostContext,getName()));
    }

    /***
     * pluginManager的名字
     * 用于区分和其他pluginmanager区分持续化存储的名字
     * @return
     */
    abstract protected String getName();

    /**
     * 从文件中解压插件
     * @param dir
     * @return
     */
    public final PluginConfig installPluginFromDir(File dir) {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * 从压缩包中解压插件
     * @param zip
     * @param hash
     * @return
     * @throws IOException
     * @throws JSONException
     */
    public final PluginConfig installPluginFromZip(File zip,String hash) throws IOException, JSONException {
        return mUnpackManager.unpackPlugin(hash,zip);
    }

    /***
     * 安装完成时调用
     * @param pluginConfig
     */
    public final void onIntallCompleted(PluginConfig pluginConfig) {
        File root = mUnpackManager.getAppDir();
        String soDir = AppCacheFolderManager.getLibDir(root,pluginConfig.UUID).getAbsolutePath();
        String oDexDir = AppCacheFolderManager.getODexDir(root, pluginConfig.UUID).getAbsolutePath();

        mInstallDao.insert(pluginConfig,soDir,oDexDir);
    }

    protected InstalledPlugin.Part getPluginPartByParKey(String uuid,String partKey) {
        InstalledPlugin installedPlugin = mInstallDao.getInstalledPluginByUUID(uuid);
        if (installedPlugin != null) {
            return installedPlugin.getPart(partKey);
        }
        throw new RuntimeException("没有找到part partKey:" + partKey);
    }

    protected InstalledPlugin getInstalledPlugin(String uuid) {
        return mInstallDao.getInstalledPluginByUUID(uuid);
    }

    protected InstalledPlugin.Part getLoaderOrRunTimePart(String uuid,int type) {
        if(type != InstalledType.TYPE_PLUGIN_LOADER && type != InstalledType.TYPE_PLUGIN_RUNTIME) {
            throw new RuntimeException("不支持的type:" + type);
        }
        InstalledPlugin installedPlugin = mInstallDao.getInstalledPluginByUUID(uuid);
        if (type == InstalledType.TYPE_PLUGIN_RUNTIME) {
            if(installedPlugin.runtimeFile != null) {
                return installedPlugin.runtimeFile;
            }
        } else if (type == InstalledType.TYPE_PLUGIN_LOADER){
            if (installedPlugin.pluginLoaderFile != null) {
                return installedPlugin.pluginLoaderFile;
            }
        }
        throw new RuntimeException("没有找到part type:" + type);
    }

    /**
     * odex优化
     * @param uuid
     * @param partKey
     * @param apkFile
     * @throws InstallPluginException
     */
    public final void oDexPlugin(String uuid,String partKey,File apkFile) throws InstallPluginException {
        try {
            File root = mUnpackManager.getAppDir();
            File oDexDir = AppCacheFolderManager.getODexDir(root,uuid);
            ODexBloc.oDexPlugin(apkFile,oDexDir,AppCacheFolderManager.getODexCopiedFile(oDexDir,partKey));
        }catch (InstallPluginException e) {
            if(mLogger.isErrorEnabled()) {
                mLogger.error("oDexPlugin exception:",e);
            }
            throw e;
        }
    }

    /***
     * odex优化
     * @param uuid 插件的uuid
     * @param type 要odex的插件类型 intalledType  loader or runtime
     * @param apkFile 插件apk文件
     * @throws InstallPluginException
     */
    public final void oDexPluginLoaderOrRunTime(String uuid,int type,File apkFile) throws InstallPluginException {
        try {
            File root = mUnpackManager.getAppDir();
            File oDexDir = AppCacheFolderManager.getODexDir(root,uuid);
            String key = type == InstalledType.TYPE_PLUGIN_LOADER ? "loader":"runtime";
            ODexBloc.oDexPlugin(apkFile,oDexDir,AppCacheFolderManager.getODexCopiedFile(oDexDir,key));

        }catch (InstallPluginException e) {
            if (mLogger.isErrorEnabled()) {
                mLogger.error("oDexpluginLoaderOrRuntime exception:",e);
            }
            throw e;
        }
    }

    /***
     * 插件apk的so解压
     * @param uuid
     * @param partKey
     * @param apkFile
     * @throws InstallPluginException
     */
    public final void extractSo(String uuid,String partKey,File apkFile) throws InstallPluginException {
        try {
            File root = mUnpackManager.getAppDir();
            String filter = "lib/" + getAbi() +"/";
            File soDir = AppCacheFolderManager.getLibDir(root,uuid);
            CopySoBloc.copySo(apkFile,soDir,AppCacheFolderManager.getLibCopiedFile(soDir,partKey),filter);
        } catch (InstallPluginException e) {
            if (mLogger.isErrorEnabled()) {
                mLogger.error("extractSo exception:", e);
            }
            throw e;
        }
    }

    /**
     * 插件apk的so解压
     *
     * @param uuid 插件包的uuid
     * @param type 要oDex的插件类型 @class IntalledType  loader or runtime
     * @param apkFile 插件apk文件
     */
    public final void extractLoaderOrRunTimeSo(String uuid, int type, File apkFile) throws InstallPluginException {
        try {
            File root = mUnpackManager.getAppDir();
            String key = type == InstalledType.TYPE_PLUGIN_LOADER ? "loader" : "runtime";
            String filter = "lib/" + getAbi() + "/";
            File soDir = AppCacheFolderManager.getLibDir(root, uuid);
            CopySoBloc.copySo(apkFile, soDir
                    , AppCacheFolderManager.getLibCopiedFile(soDir, key), filter);
        } catch (InstallPluginException e) {
            if (mLogger.isErrorEnabled()) {
                mLogger.error("extractLoaderOrRunTimeSo exception:", e);
            }
            throw e;
        }
    }

    /***
     * 获取已经安装的插件，最后安装的排在返回list的最前面
     * @param limit
     * @return
     */
    public final List<InstalledPlugin> getInstalledPlugins(int limit) {
        return mInstallDao.getLastPlugins(limit);
    }

    /**
     * 删除指定uuid的插件
     *
     * @param uuid 插件包的uuid
     * @return 是否全部执行成功
     */
    public boolean deleteInstalledPlugin(String uuid) {
        InstalledPlugin installedPlugin = mInstallDao.getInstalledPluginByUUID(uuid);
        boolean suc = true;
        if (installedPlugin.runtimeFile != null) {
            if (!deletePart(installedPlugin.runtimeFile)) {
                suc = false;
            }
        }
        if (installedPlugin.pluginLoaderFile != null) {
            if (!deletePart(installedPlugin.pluginLoaderFile)) {
                suc = false;
            }
        }
        for (Map.Entry<String, InstalledPlugin.PluginPart> plugin : installedPlugin.plugins.entrySet()) {
            if (!deletePart(plugin.getValue())) {
                suc = false;
            }
        }
        if (mInstallDao.deleteByUUID(uuid) <= 0) {
            suc = false;
        }
        return suc;
    }

    private boolean deletePart(InstalledPlugin.Part part) {
        boolean suc = true;
        if (!part.pluginFile.delete()) {
            suc = false;
        }
        if (part.oDexDir != null) {
            if (!part.oDexDir.delete()) {
                suc = false;
            }
        }
        if (part.libraryDir != null) {
            if (!part.libraryDir.delete()) {
                suc = false;
            }
        }
        return suc;
    }

    public String getAbi() {
        return null;
    }

    public void close() {
        mInstallDao.close();
    }

}
