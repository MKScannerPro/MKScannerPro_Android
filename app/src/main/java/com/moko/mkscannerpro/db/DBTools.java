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
        cv.put(DBConstants.DEVICE_FIELD_MAC, mokoDevice.mac);
        cv.put(DBConstants.DEVICE_FIELD_NICK_NAME, mokoDevice.nickName);
        cv.put(DBConstants.DEVICE_FIELD_MQTT_INFO, mokoDevice.mqttInfo);
        cv.put(DBConstants.DEVICE_FIELD_DEVICE_ID, mokoDevice.deviceId);
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
            mokoDevice.mac = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_MAC));
            mokoDevice.nickName = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NICK_NAME));
            mokoDevice.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDevice.deviceId = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_DEVICE_ID));
            mokoDevice.topicPublish = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDevice.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            mokoDevices.add(mokoDevice);
        }
        return mokoDevices;
    }

    public MokoDevice selectDevice(String deviceId) {
        Cursor cursor = db.query(DBConstants.TABLE_NAME_DEVICE, null, DBConstants.DEVICE_FIELD_DEVICE_ID + " = ?", new String[]{deviceId}, null, null, null);
        MokoDevice mokoDevice = null;
        while (cursor.moveToFirst()) {
            mokoDevice = new MokoDevice();
            mokoDevice.id = cursor.getInt(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_ID));
            mokoDevice.name = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NAME));
            mokoDevice.mac = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_MAC));
            mokoDevice.nickName = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NICK_NAME));
            mokoDevice.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDevice.deviceId = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_DEVICE_ID));
            mokoDevice.topicPublish = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDevice.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            break;
        }
        return mokoDevice;
    }

    public MokoDevice selectDeviceByMac(String mac) {
        Cursor cursor = db.query(DBConstants.TABLE_NAME_DEVICE, null, DBConstants.DEVICE_FIELD_MAC + " = ?", new String[]{mac}, null, null, null);
        MokoDevice mokoDevice = null;
        while (cursor.moveToFirst()) {
            mokoDevice = new MokoDevice();
            mokoDevice.id = cursor.getInt(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_ID));
            mokoDevice.name = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NAME));
            mokoDevice.mac = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_MAC));
            mokoDevice.nickName = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_NICK_NAME));
            mokoDevice.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDevice.deviceId = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_DEVICE_ID));
            mokoDevice.topicPublish = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDevice.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            break;
        }
        return mokoDevice;
    }


    public void updateDevice(MokoDevice mokoDevice) {
        String where = DBConstants.DEVICE_FIELD_MAC + " = ?";
        String[] whereValue = {mokoDevice.mac};
        ContentValues cv = new ContentValues();
        cv.put(DBConstants.DEVICE_FIELD_NICK_NAME, mokoDevice.nickName);
        cv.put(DBConstants.DEVICE_FIELD_MAC, mokoDevice.mac);
        cv.put(DBConstants.DEVICE_FIELD_MQTT_INFO, mokoDevice.mqttInfo);
        cv.put(DBConstants.DEVICE_FIELD_TOPIC_PUBLISH, mokoDevice.topicPublish);
        cv.put(DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE, mokoDevice.topicSubscribe);
        cv.put(DBConstants.DEVICE_FIELD_DEVICE_ID, mokoDevice.deviceId);
        db.update(DBConstants.TABLE_NAME_DEVICE, cv, where, whereValue);
    }

    public void deleteAllData() {
        db.delete(DBConstants.TABLE_NAME_DEVICE, null, null);
    }

    public void deleteDevice(MokoDevice device) {
        String where = DBConstants.DEVICE_FIELD_DEVICE_ID + " = ?";
        String[] whereValue = {device.deviceId + ""};
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
