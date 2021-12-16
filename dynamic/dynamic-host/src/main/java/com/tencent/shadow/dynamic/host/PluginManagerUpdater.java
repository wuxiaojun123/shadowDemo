package com.tencent.shadow.dynamic.host;


import java.io.File;
import java.util.concurrent.Future;

/***
 * pluginManager文件升级器
 * 注意这个类不负责什么时候该升级pluginmanager
 * 它只提供需要升级时的功能，入下载和向远端查询文件是否还可用
 */
public interface PluginManagerUpdater {

    /**
     * 表示之前更新过程中意外中断了
     * @return
     */
    boolean wasUpdating();

    /***
     * 更新
     * @return 当前最新的PluginManager 可能是之前已经返回过的文件，但它是最新的了
     */
    Future<File> update();

    /***
     * 获取本地最新可用的
     * @return 表示本地没有可用的
     */
    File getLatest();

    /**
     * 查询是否可用
     * @param file
     * @return
     */
    Future<Boolean> isAvailable(File file);

}
