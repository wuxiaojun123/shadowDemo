package com.tencent.shadow.dynamic.host;

import android.content.Context;
import android.os.Bundle;

/**
 * 使用方持有的接口
 */
public interface PluginManager {

    /***
     *
     * @param context
     * @param formId 标识本次请求的来源位置
     * @param bundle 参数列表
     * @param callback 用于从PluginManager实现中返回view
     */
    void enter(Context context, long formId, Bundle bundle,EnterCallback callback);

}
