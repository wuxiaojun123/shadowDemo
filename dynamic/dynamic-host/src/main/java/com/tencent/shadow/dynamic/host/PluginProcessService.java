package com.tencent.shadow.dynamic.host;

import android.app.Application;
import android.content.Intent;
import android.os.IBinder;

public class PluginProcessService extends BasePluginProcessService{

    private final PpsBinder mPpsControllerBinder = new PpsBinder(this);

    static final ActivityHolder sActivityHolder = new ActivityHolder();

    public static Application.ActivityLifecycleCallbacks getActivityHolder() {
        return sActivityHolder;
    }

    public static PpsCon

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
