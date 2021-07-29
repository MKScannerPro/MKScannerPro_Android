package com.moko.mkscannerpro.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.moko.mkscannerpro.entity.MokoDevice;

import java.util.ArrayList;

public class DBTools {
    private DBOpenHelper myDBOpenHelper;
    private SQLiteDatabase db;
    private static DBTools dbTools;

    public static DBTools getInstance(Context context) {
        if (dbTools == null) {
            dbTools = new DBTools(context);
            return dbTools;
        }
        return dbTools;
    }

    public DBTools(Context context) {
        myDBOpenHelper = new DBOpenHelper(context);
        db = myDBOpenHelper.getWritableDatabase();
    }

    public long insertDevice(MokoDevice mokoDevice) {
        ContentValues cv = new ContentValues();
        cv.put(DBConstants.DEVICE_FIELD_NAME, mokoDevice.name);
        cv.put(DBConstants.DEVICE_FIELD_NICK_NAME, mokoDevice.nickName);
        cv.put(DBConstants.DEVICE_FIELD_MQTT_INFO, mokoDevice.mqttInfo);
        cv.put(DBConstants.DEVICE_FIELD_UNIQUE_ID, mokoDevice.uniqueId);
        cv.put(DBConstants.DEVICE_FIELD_TOPIC_PUBLISH, mokoDevice.topicPublish);
        cv.put(DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE, mokoDevice.topicSubscribe);
        long row = db.insert(DBConstants.TABLE_NAME_DEVICE, null, cv);
        return row;
    }

    public ArrayList<MokoDevice> selectAllDevice() {
        Cursor cursor = db.query(DBConstants.TABLE_NAME_DEVICE, null, null, null,
                null, null, DBConstants.DEVICE_FIELD_ID + " DESC");
        ArrayList<MokoDevice> mokoDevices = new ArrayList<>();
        while (cursor.moveToNext()) {
            MokoDevice mokoDevice = new MokoDevice();
            mokoDevice.id = cursor.getInt(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_ID));
            mokoDevice.name = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NAME));
            mokoDevice.nickName = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NICK_NAME));
            mokoDevice.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDevice.uniqueId = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_UNIQUE_ID));
            mokoDevice.topicPublish = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDevice.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            mokoDevices.add(mokoDevice);
        }
        return mokoDevices;
    }

    public MokoDevice selectDevice(String uniqueId) {
        Cursor cursor = db.query(DBConstants.TABLE_NAME_DEVICE, null, DBConstants.DEVICE_FIELD_UNIQUE_ID + " = ?", new String[]{uniqueId}, null, null, null);
        MokoDevice mokoDevice = null;
        while (cursor.moveToFirst()) {
            mokoDevice = new MokoDevice();
            mokoDevice.id = cursor.getInt(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_ID));
            mokoDevice.name = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NAME));
            mokoDevice.nickName = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NICK_NAME));
            mokoDevice.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDevice.uniqueId = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_UNIQUE_ID));
            mokoDevice.topicPublish = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDevice.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            break;
        }
        return mokoDevice;
    }

    public MokoDevice selectDeviceByName(String name) {
        Cursor cursor = db.query(DBConstants.TABLE_NAME_DEVICE, null, DBConstants.DEVICE_FIELD_NAME + " = ?", new String[]{name}, null, null, null);
        MokoDevice mokoDevice = null;
        while (cursor.moveToFirst()) {
            mokoDevice = new MokoDevice();
            mokoDevice.id = cursor.getInt(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_ID));
            mokoDevice.name = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NAME));
            mokoDevice.nickName = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NICK_NAME));
            mokoDevice.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDevice.uniqueId = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_UNIQUE_ID));
            mokoDevice.topicPublish = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDevice.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            break;
        }
        return mokoDevice;
    }


    public void updateDevice(MokoDevice mokoDevice) {
        String where = DBConstants.DEVICE_FIELD_NAME + " = ?";
        String[] whereValue = {mokoDevice.name};
        ContentValues cv = new ContentValues();
        cv.put(DBConstants.DEVICE_FIELD_NICK_NAME, mokoDevice.nickName);
        cv.put(DBConstants.DEVICE_FIELD_MQTT_INFO, mokoDevice.mqttInfo);
        cv.put(DBConstants.DEVICE_FIELD_TOPIC_PUBLISH, mokoDevice.topicPublish);
        cv.put(DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE, mokoDevice.topicSubscribe);
        cv.put(DBConstants.DEVICE_FIELD_UNIQUE_ID, mokoDevice.uniqueId);
        db.update(DBConstants.TABLE_NAME_DEVICE, cv, where, whereValue);
    }

    public void deleteAllData() {
        db.delete(DBConstants.TABLE_NAME_DEVICE, null, null);
    }

    public void deleteDevice(MokoDevice device) {
        String where = DBConstants.DEVICE_FIELD_UNIQUE_ID + " = ?";
        String[] whereValue = {device.uniqueId + ""};
        db.delete(DBConstants.TABLE_NAME_DEVICE, where, whereValue);
    }

    // drop table;
    public void droptable(String tablename) {
        db.execSQL("DROP TABLE IF EXISTS " + tablename);
    }

    // close database;
    public void close(String databasename) {
        db.close();
    }

}
