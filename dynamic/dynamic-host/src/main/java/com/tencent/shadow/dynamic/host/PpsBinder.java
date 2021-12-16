package com.tencent.shadow.dynamic.host;

import android.os.Parcel;
import android.os.RemoteException;

public class PpsBinder extends android.os.Binder {

    static final String DESCRIPTOR = PpsBinder.class.getName();

    static final int TRANSACTION_CODE_NO_EXCEPTION = 0;
    static final int TRANSACTION_CODE_FAILED_EXCEPTION = 1;

    static final int TRANSACTION_loadRuntime = FIRST_CALL_TRANSACTION;



    private final PluginProcessService mPps;

    PpsBinder(PluginProcessService pps) {
        this.mPps = pps;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return super.onTransact(code, data, reply, flags);
    }



}
