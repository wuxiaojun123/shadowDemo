package com.tencent.shadow.dynamic.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;

import com.tencent.shadow.core.common.InstalledApk;
import com.tencent.shadow.core.common.Logger;
import com.tencent.shadow.core.common.LoggerFactory;
import com.tencent.shadow.core.load_parameters.LoadParameters;
import com.tencent.shadow.core.manager.BasePluginManager;
import com.tencent.shadow.core.manager.installplugin.InstalledPlugin;
import com.tencent.shadow.core.manager.installplugin.InstalledType;
import com.tencent.shadow.dynamic.host.FailedException;
import com.tencent.shadow.dynamic.host.NotFoundException;
import com.tencent.shadow.dynamic.host.UuidManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

abstract public class BaseDynamicPluginManager extends BasePluginManager implements UuidManagerImpl {

    private static final Logger mLogger = LoggerFactory.getLogger(BaseDynamicPluginManager.class);

    public BaseDynamicPluginManager(Context context) {
        super(context);
    }

    /**
     * 防止绑定service重入
     */
    private AtomicBoolean mServiceConnection = new AtomicBoolean(false);

    /***
     * 等待service绑定完成的计数器
     */
    private AtomicReference<CountDownLatch> mConnectCountDownLatch = new AtomicReference<>();

    /***
     * 启动pluginprocessservice
     * @param serviceName 注册在宿主中的插件进程管理service完整名字
     */
    public final void bindPluginProcessService(final String serviceName) {
        if(mServiceConnection.get()) {
            if(mLogger.isInfoEnabled()) {
                mLogger.info("pps service connection");
            }
            return;
        }
        if(mLogger.isInfoEnabled()) {
            mLogger.info("bindpluginprocessservice" + serviceName);
        }

        mConnectCountDownLatch.set(new CountDownLatch(1));

        mServiceConnection.set(true);

        final CountDownLatch startBindingLatch = new CountDownLatch(1);
        final boolean[] asyncResult = new boolean[1];
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(mHostContext,serviceName));
                boolean binding = mHostContext.bindService(intent,new ServiceConnection(){

                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        if(mLogger.isInfoEnabled()) {
                            mLogger.info("onServiceConnected connectCountDownLatch:" + mConnectCountDownLatch);
                        }
                        mServiceConnection.set(false);

                        // service connect 后处理逻辑
                        onPluginServiceConnected(name,service);

                        mConnectCountDownLatch.get().countDown();

                        if(mLogger.isInfoEnabled()) {
                            mLogger.info("onServiceConnected countDown:" + mConnectCountDownLatch);
                        }

                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        if (mLogger.isInfoEnabled()) {
                            mLogger.info("onServiceDisconnected");
                        }
                        mServiceConnection.set(false);
                        onPluginServiceDisconnected(name);
                    }
                },Context.BIND_AUTO_CREATE);
            }
        });
        try {
            //等待bindservice真正开始
            startBindingLatch.await(10,TimeUnit.SECONDS);
            if (!asyncResult[0]) {
                throw new IllegalArgumentException("无法绑定pps:" + serviceName);
            }

        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public final void waitServiceConnected(int timeout, TimeUnit timeUnit) throws TimeoutException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("waitserviceconnected 不能在主线程中调用");
        }
        try {
            if(mLogger.isInfoEnabled()) {
                mLogger.info("waiting service connect connectCountDownLatch:" + mConnectCountDownLatch);
            }

            long s = System.currentTimeMillis();
            boolean isTimeout = !mConnectCountDownLatch.get().await(timeout,timeUnit);
            if(isTimeout) {
                throw new TimeoutException("连接service超时 等待了" + (System.currentTimeMillis()-s));
            }
            if(mLogger.isInfoEnabled()) {
                mLogger.info("service connected" + (System.currentTimeMillis() - s));
            }

        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    protected abstract void onPluginServiceConnected(ComponentName name,IBinder service);

    protected abstract void onPluginServiceDisconnected(ComponentName name);

    /***
     * pluginManager对象创建的时候回调
     * @param bundle 当pluginManager有更新时会回调老的pluginManager 对象onSaveInstanceState存储数据，bundle不为null说明发生了更新
     *               为null说明是首次创建
     */
    public void onCreate(Bundle bundle) {
        if(mLogger.isInfoEnabled()) {
            mLogger.info("onCreate bundle:" + bundle);
        }
    }

    /***
     * 当pluginManager有更新时会先回调老的PluginManager对象 onSaveInstanceState存储数据
     * @param bundle 存储的数据
     */
    public void onSaveInstanceState(Bundle bundle) {
        if(mLogger.isInfoEnabled()) {
            mLogger.info("onsaveinstancestate:" +bundle);
        }
    }

    /**
     * 当pluginManager有更新时会先销毁老的pluginManager独享，回调对应的onDestroy
     */
    public void onDestroy() {
        if(mLogger.isInfoEnabled()) {
            mLogger.info("onDestroy:");
        }
    }

    public InstalledApk getPlugin(String uuid,String partKey) throws FailedException, NotFoundException {
        try {
            InstalledPlugin.Part part;
            try {
                part = getPluginPartByParKey(uuid,partKey);
            } catch (RuntimeException e) {
                throw new NotFoundException("uuid==" + uuid + "partKey==" + partKey + "的plugin找不到");
            }
            String businessName = part instanceof InstalledPlugin.PluginPart ? ((InstalledPlugin.PluginPart)part).businessName : null;
            String[] dependsOn = part instanceof InstalledPlugin.PluginPart ? ((InstalledPlugin.PluginPart)part).dependsOn:null;
            String[] hostWhiteList = part instanceof InstalledPlugin.PluginPart ? ((InstalledPlugin.PluginPart)part).hostWhiteList:null;
            LoadParameters loadParameters = new LoadParameters(businessName,partKey,dependsOn,hostWhiteList);

            Parcel parcelExtras = Parcel.obtain();
            loadParameters.writeToParcel(parcelExtras,0);
            byte[] parcelBytes = parcelExtras.marshall();
            parcelExtras.recycle();

            return new InstalledApk(
                    part.pluginFile.getAbsolutePath(),
                    part.oDexDir == null ? null : part.oDexDir.getAbsolutePath(),
                    part.libraryDir == null ? null : part.libraryDir.getAbsolutePath(),
                    parcelBytes
            );
        } catch (RuntimeException e) {
            if (mLogger.isErrorEnabled()) {
                mLogger.error("getPlugin exception:",e);
            }
            throw new FailedException(e);
        }
    }


    private InstalledApk getInstalledPL(String uuid,int type) throws FailedException,NotFoundException {
        try {
            InstalledPlugin.Part part;
            try {
                part = getLoaderOrRunTimePart(uuid,type);
            }catch (RuntimeException e) {
                if(mLogger.isErrorEnabled()) {
                    mLogger.error("getInstalledPL exception:",e);
                }
                throw new NotFoundException("uuid==" + uuid + "type == " + type + "没找到 cause :" + e.getMessage());
            }
            return new InstalledApk(
                    part.pluginFile.getAbsolutePath(),
                    part.oDexDir==null?null:part.oDexDir.getAbsolutePath(),
                    part.libraryDir==null?null:part.libraryDir.getAbsolutePath()
            );
        }catch (RuntimeException e) {
            throw new FailedException(e);
        }
    }

    public InstalledApk getPluginLoader(String uuid) throws FailedException,NotFoundException {
        return getInstalledPL(uuid, InstalledType.TYPE_PLUGIN_LOADER);
    }

    public InstalledApk getRuntime(String uuid) throws FailedException,NotFoundException {
        return getInstalledPL(uuid,InstalledType.TYPE_PLUGIN_RUNTIME);
    }


}
