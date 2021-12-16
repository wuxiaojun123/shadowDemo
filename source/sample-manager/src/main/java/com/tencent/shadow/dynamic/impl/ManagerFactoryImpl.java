package com.tencent.shadow.dynamic.impl;

import android.content.Context;

import com.tencent.shadow.dynamic.host.ManagerFactory;
import com.tencent.shadow.dynamic.host.PluginManagerImpl;

public class ManagerFactoryImpl implements ManagerFactory {


    @Override
    public PluginManagerImpl buildManager(Context context) {
        return new Sample;
    }



}
