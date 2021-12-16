package com.tencent.shadow.core.load_parameters;

import android.os.Parcel;
import android.os.Parcelable;

public class LoadParameters implements Parcelable {

    public final String businessName;
    public final String partKey;
    public final String[] dependsOn;
    public final String[] hostWhiteList;

    public LoadParameters(String businessName,String partKey,String[] dependsOn,String[] hostWhiteList) {
        this.businessName = businessName;
        this.partKey = partKey;
        this.dependsOn = dependsOn;
        this.hostWhiteList = hostWhiteList;
    }


    protected LoadParameters(Parcel in) {
        businessName = in.readString();
        partKey = in.readString();
        dependsOn = in.createStringArray();
        hostWhiteList = in.createStringArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(businessName);
        dest.writeString(partKey);
        dest.writeStringArray(dependsOn);
        dest.writeStringArray(hostWhiteList);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LoadParameters> CREATOR = new Creator<LoadParameters>() {
        @Override
        public LoadParameters createFromParcel(Parcel in) {
            return new LoadParameters(in);
        }

        @Override
        public LoadParameters[] newArray(int size) {
            return new LoadParameters[size];
        }
    };
}
