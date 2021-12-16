package com.wxj.sample_host;

import android.content.Context;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PluginHelper {

    /**
     * 动态加载的插件管理apk
     */
    public final static String sPluginManagerName = "pluginmanager.apk";

    /**
     * 动态加载的插件包，里面包含以下几个部分，插件apk，插件框架apk(loader  apk 和runtime apk)，apk 信息配置关系json文件
     */
    public final static String sPluginZip = "plugin-debug.zip";

    private static PluginHelper sInstance = new PluginHelper();

    public static PluginHelper getInstance() {
        return sInstance;
    }

    private PluginHelper(){}

    private File pluginManagerFile;
    private File pluginZipFile;
    private Context mContext;
    public ExecutorService singlePool = Executors.newSingleThreadExecutor();

    public void init(Context context) {
        pluginManagerFile = new File(context.getFilesDir(),sPluginManagerName);
        pluginZipFile = new File(context.getFilesDir(),sPluginZip);

        mContext = context.getApplicationContext();

        singlePool.execute(new Runnable() {
            @Override
            public void run() {
                preparePlugin();
            }
        });
    }

    private void preparePlugin() {
        try {
            InputStream is = mContext.getAssets().open(sPluginManagerName);
            FileUtils.copyInputStreamToFile(is,pluginManagerFile);

            InputStream zip = mContext.getAssets().open(sPluginZip);
            FileUtils.copyInputStreamToFile(zip,pluginZipFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
