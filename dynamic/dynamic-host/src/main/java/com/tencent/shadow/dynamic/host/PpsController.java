package com.tencent.shadow.dynamic.host;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

public class PpsController {

    final private IBinder mRemote;

    PpsController(IBinder remote) {
        mRemote = remote;
    }

    public void loadRuntime(String uuid) throws RemoteException,FailedException {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        try {
            _data.writeInterfaceToken(PpsBinder.);
        }

    }

}
