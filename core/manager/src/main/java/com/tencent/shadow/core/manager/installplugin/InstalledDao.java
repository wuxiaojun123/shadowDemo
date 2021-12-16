package com.tencent.shadow.core.manager.installplugin;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstalledDao {

    final private InstalledPluginDBHelper mDBHelper;

    public InstalledDao(InstalledPluginDBHelper dbHelper) {
        mDBHelper = dbHelper;
    }

    /***
     * 根据插件配置信息插入一组数据
     * @param pluginConfig 插件配置信息
     * @param soDir
     * @param oDexDir
     */
    public void insert(PluginConfig pluginConfig,String soDir,String oDexDir) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        List<ContentValues> contentValuesList = parseConfig(pluginConfig, soDir, oDexDir);
        db.beginTransaction();
        try {
            for (ContentValues contentValues : contentValuesList) {
                db.replace(InstalledPluginDBHelper.TABLE_NAME_MANAGER, null, contentValues);
            }
            //把最后一次uuid的插件安装时间作为所有相同uuid的插件的安装时间
            ContentValues values = new ContentValues();
            values.put(InstalledPluginDBHelper.COLUMN_INSTALL_TIME, pluginConfig.storageDir.lastModified());
            db.update(InstalledPluginDBHelper.TABLE_NAME_MANAGER, values, InstalledPluginDBHelper.COLUMN_UUID + " = ?", new String[]{pluginConfig.UUID});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int deleteByUUID(String UUID) {
        int row;
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            row = db.delete(InstalledPluginDBHelper.TABLE_NAME_MANAGER, InstalledPluginDBHelper.COLUMN_UUID + " =?", new String[]{UUID});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return row;
    }

    public List<InstalledPlugin> getLastPlugins(int limit) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select uuid from shadowPluginManager where  type = ?   order by installedTime desc limit " + limit, new String[]{String.valueOf(InstalledType.TYPE_UUID)});
        List<String> uuids = new ArrayList<>();
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String uuid = cursor.getString(cursor.getColumnIndex(InstalledPluginDBHelper.COLUMN_UUID));
            uuids.add(uuid);
        }
        cursor.close();
        List<InstalledPlugin> installedPlugins = new ArrayList<>();
        for (String uuid:uuids) {
            installedPlugins.add(getInstalledPluginByUUID(uuid));
        }
        return installedPlugins;
    }

    /**
     * 根据uuid和APPID获取对应的插件信息
     *
     * @param UUID  插件的发布id
     * @return 插件安装数据
     */
    @SuppressLint("Range")
    public InstalledPlugin getInstalledPluginByUUID(String UUID) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from shadowPluginManager where uuid = ?", new String[]{UUID});
        InstalledPlugin installedPlugin = new InstalledPlugin();
        installedPlugin.UUID = UUID;
        while (cursor.moveToNext()) {
            int type = cursor.getInt(cursor.getColumnIndex(InstalledPluginDBHelper.COLUMN_TYPE));
            if (type == InstalledType.TYPE_UUID) {
                installedPlugin.UUID_NickName = cursor.getString(cursor.getColumnIndex(InstalledPluginDBHelper.COLUMN_VERSION));
            } else {
                File pluginFile = new File(cursor.getString(cursor.getColumnIndex(InstalledPluginDBHelper.COLUMN_PATH)));
                String oDexPath = cursor.getString(cursor.getColumnIndex(InstalledPluginDBHelper.COLUMN_PLUGIN_ODEX));
                File oDexDir = oDexPath == null ? null : new File(oDexPath);
                String libPath = cursor.getString(cursor.getColumnIndex(InstalledPluginDBHelper.COLUMN_PLUGIN_LIB));
                File libDir = libPath == null ? null : new File(libPath);
                if (type == InstalledType.TYPE_PLUGIN_LOADER) {
                    installedPlugin.pluginLoaderFile = new InstalledPlugin.Part(type, pluginFile, oDexDir, libDir);
                } else if (type == InstalledType.TYPE_PLUGIN_RUNTIME) {
                    installedPlugin.runtimeFile = new InstalledPlugin.Part(type, pluginFile, oDexDir, libDir);
                } else {
                    String businessName = cursor.getString(cursor.getColumnIndex(InstalledPluginDBHelper.COLUMN_BUSINESS_NAME));

                    String partKey = cursor.getString(cursor.getColumnIndex(InstalledPluginDBHelper.COLUMN_PARTKEY));

                    if (type == InstalledType.TYPE_PLUGIN) {
                        String[] dependsOn = getArrayStringByColumnName(InstalledPluginDBHelper.COLUMN_DEPENDSON, cursor);
                        String[] hostWhiteList = getArrayStringByColumnName(InstalledPluginDBHelper.COLUMN_HOST_WHITELIST, cursor);
                        installedPlugin.plugins.put(partKey, new InstalledPlugin.PluginPart(type, businessName, pluginFile, oDexDir, libDir, dependsOn, hostWhiteList));
                    } else {
                        throw new RuntimeException("出现不认识的type==" + type);
                    }
                }
            }
        }
        cursor.close();
        return installedPlugin;
    }

    private String[] getArrayStringByColumnName(String columnName, Cursor cursor) {
        int columnIndex = cursor.getColumnIndex(columnName);
        boolean hasColumn = !cursor.isNull(columnIndex);
        String[] arrayString;
        if (hasColumn) {
            String string = cursor.getString(columnIndex);
            try {
                JSONArray jsonArray = new JSONArray(string);
                arrayString = new String[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    arrayString[i] = jsonArray.getString(i);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            arrayString = null;
        }
        return arrayString;
    }

    private List<ContentValues> parseConfig(PluginConfig pluginConfig, String soDir, String oDexDir) {
        List<InstalledRow> installedRows = new ArrayList<>();
        if (pluginConfig.pluginLoader != null) {
            installedRows.add(new InstalledRow(pluginConfig.pluginLoader.hash, null, pluginConfig.pluginLoader.file.getAbsolutePath(), InstalledType.TYPE_PLUGIN_LOADER,
                    soDir, oDexDir));
        }
        if (pluginConfig.runTime != null) {
            installedRows.add(new InstalledRow(pluginConfig.runTime.hash, null, pluginConfig.runTime.file.getAbsolutePath(), InstalledType.TYPE_PLUGIN_RUNTIME,
                    soDir, oDexDir));
        }
        if (pluginConfig.plugins != null) {
            Set<Map.Entry<String, PluginConfig.PluginFileInfo>> plugins = pluginConfig.plugins.entrySet();
            for (Map.Entry<String, PluginConfig.PluginFileInfo> plugin : plugins) {
                PluginConfig.PluginFileInfo fileInfo = plugin.getValue();
                installedRows.add(
                        new InstalledRow(
                                fileInfo.hash,
                                fileInfo.businessName,
                                plugin.getKey(),
                                fileInfo.dependsOn,
                                fileInfo.file.getAbsolutePath(),
                                InstalledType.TYPE_PLUGIN,
                                fileInfo.hostWhiteList,
                                soDir, oDexDir
                        )
                );
            }
        }
        InstalledRow uuidRow = new InstalledRow();
        uuidRow.type = InstalledType.TYPE_UUID;
        uuidRow.filePath = pluginConfig.UUID;
        installedRows.add(uuidRow);
        List<ContentValues> contentValues = new ArrayList<>();
        for (InstalledRow row : installedRows) {
            row.installedTime = pluginConfig.storageDir.lastModified();
            row.UUID = pluginConfig.UUID;
            row.version = pluginConfig.UUID_NickName;
            contentValues.add(row.toContentValues());
        }
        return contentValues;
    }


    /**
     * 释放资源
     */
    public void close() {
        mDBHelper.close();
    }

}
