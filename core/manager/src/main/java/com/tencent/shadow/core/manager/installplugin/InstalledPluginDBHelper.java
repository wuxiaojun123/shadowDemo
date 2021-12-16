package com.tencent.shadow.core.manager.installplugin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InstalledPluginDBHelper extends SQLiteOpenHelper {

    final static String DB_NAME_PREFIX = "shadow_installed_plugin_db";

    final static String TABLE_NAME_MANAGER = "shadowPluginManager";

    public final static String COLUMN_ID = "id";

    public final static String COLUMN_HASH = "hash";

    public final static String COLUMN_TYPE = "type";

    public final static String COLUMN_PATH = "filePath";

    public final static String COLUMN_BUSINESS_NAME = "businessName";

    public final static String COLUMN_PARTKEY = "partKey";

    public final static String COLUMN_DEPENDSON = "dependsOn";

    public final static String COLUMN_UUID = "uuid";

    public final static String COLUMN_VERSION = "version";

    public final static String COLUMN_INSTALL_TIME = "installedTime";

    public final static String COLUMN_PLUGIN_ODEX = "odexPath";

    public final static String COLUMN_PLUGIN_LIB = "libPath";

    public final static String COLUMN_HOST_WHITELIST = "hostWhiteList";

    private final static int VERSION = 4;


    public InstalledPluginDBHelper(@Nullable Context context, @Nullable String name) {
        super(context, DB_NAME_PREFIX+name, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_MANAGER + " ( "
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_HASH + " VARCHAR , "
                + COLUMN_PATH + " VARCHAR, "
                + COLUMN_TYPE + " INTEGER, "
                + COLUMN_BUSINESS_NAME + " VARCHAR, "
                + COLUMN_PARTKEY + " VARCHAR, "
                + COLUMN_DEPENDSON + " VARCHAR, "
                + COLUMN_UUID + " VARCHAR, "
                + COLUMN_VERSION + " VARCHAR, "
                + COLUMN_INSTALL_TIME + " INTEGER ,"
                + COLUMN_PLUGIN_ODEX + " VARCHAR ,"
                + COLUMN_PLUGIN_LIB + " VARCHAR ,"
                + COLUMN_HOST_WHITELIST + " VARCHAR "
                + ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.beginTransaction();
            try {
                Cursor cursor = db.query(
                        true,
                        TABLE_NAME_MANAGER,
                        new String[]{COLUMN_UUID, COLUMN_TYPE},
                        COLUMN_TYPE + " = ?",
                        new String[]{"2"},//Interface Type
                        null, null, null, null
                );
                List<String> uuids = new ArrayList<>();
                while (cursor.moveToNext()) {
                    @SuppressLint("Range") String uuid = cursor.getString(cursor.getColumnIndex(COLUMN_UUID));
                    uuids.add(uuid);
                }
                cursor.close();

                for (String uuid : uuids) {
                    db.delete(TABLE_NAME_MANAGER, COLUMN_UUID + " = ?", new String[]{uuid});
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        if(oldVersion < 3){
            db.beginTransaction();
            try {
                //添加列COLUMN_HOST_WHITELIST
                db.execSQL("ALTER TABLE " + TABLE_NAME_MANAGER + " ADD " + COLUMN_HOST_WHITELIST + " VARCHAR");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        }
        if (oldVersion < 4) {
            db.beginTransaction();
            try {
                //添加列COLUMN_BUSINESS_NAME。所有旧行保持空值即可，表示同宿主相同业务。
                db.execSQL("ALTER TABLE " + TABLE_NAME_MANAGER + " ADD " + COLUMN_BUSINESS_NAME + " VARCHAR");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }



}
