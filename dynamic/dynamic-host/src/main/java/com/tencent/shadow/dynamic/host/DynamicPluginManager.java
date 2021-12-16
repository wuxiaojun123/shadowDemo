package com.tencent.shadow.dynamic.host;

import static com.tencent.shadow.core.common.Md5.md5File;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.tencent.shadow.core.common.Logger;
import com.tencent.shadow.core.common.LoggerFactory;

import java.io.File;

public class DynamicPluginManager implements PluginManager{

    private final PluginManagerUpdater mUpdater;
    private PluginManagerImpl mManagerImpl;
    private String mCurrentImplMd5;
    private static final Logger mLogger = LoggerFactory.getLogger(DynamicPluginManager.class);


    public DynamicPluginManager(PluginManagerUpdater updater) {
        if (updater.getLatest() == null) {
            throw new IllegalArgumentException("构造DynamicPluginManager时传入的PluginManagerUpdater 必须已经有本地文件，也就是getLatest() != null");
        }
        mUpdater = updater;
    }


    @Override
    public void enter(Context context, long formId, Bundle bundle, EnterCallback callback) {
        if (mLogger.isInfoEnabled()) {
            mLogger.info("enter fromId:" + formId + " callback:" + callback);
        }
        updateManagerImpl(context);
        mManagerImpl.enter(context,formId,bundle,callback);
        mUpdater.update();
    }

    private void updateManagerImpl(Context context) {
        File latestManagerImplApk = mUpdater.getLatest();
        String md5 = md5File(latestManagerImplApk);
        if (mLogger.isInfoEnabled()) {
            mLogger.info("TextUtils.equals(mCurrentImplMd5 md5) " + (TextUtils.equals(mCurrentImplMd5,md5)));
        }
        if (!TextUtils.equals(mCurrentImplMd5,md5)) {
//            ManagerImplLoader

        }

    }


}
