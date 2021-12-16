package com.tencent.shadow.dynamic.host;

import com.tencent.shadow.dynamic.apk.ImplLoader;

public class ManagerImplLoader extends ImplLoader {

    private static final String MANAGER_FACTORY_CLASS_NAME = "";



    @Override
    protected String[] getCustomWhiteList() {
        return new String[0];
    }


}
