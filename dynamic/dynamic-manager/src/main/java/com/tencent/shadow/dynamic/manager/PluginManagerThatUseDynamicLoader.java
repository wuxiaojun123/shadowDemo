package com.tencent.shadow.dynamic.manager;

import android.content.Context;

import com.tencent.shadow.core.common.Logger;
import com.tencent.shadow.core.common.LoggerFactory;
import com.tencent.shadow.dynamic.host.PluginManagerImpl;

public abstract class PluginManagerThatUseDynamicLoader extends BaseDynamicPluginManager implements PluginManagerImpl {

    private static final Logger mLogger = LoggerFactory.getLogger(PluginManagerThatUseDynamicLoader.class);

    protected PpsC


    public PluginManagerThatUseDynamicLoader(Context context) {
        super(context);
    }



}
