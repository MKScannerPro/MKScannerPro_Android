package com.moko.mkscannerpro;

public class AppConstants {
    // data time pattern
    public static final String PATTERN_HH_MM = "HH:mm";
    public static final String PATTERN_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String PATTERN_MM_DD = "MM/dd";
    public static final String PATTERN_MM_DD_2 = "MM-dd";
    public static final String PATTERN_YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    public static final String PATTERN_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    // action
    public static final String ACTION_FINISH_ACTIVITY = "com.moko.mkscannerpro.action.finishActivity";
    public static final String ACTION_MODIFY_NAME = "com.moko.mkscannerpro.action.ACTION_MODIFY_NAME";
    public static final String ACTION_DELETE_DEVICE = "com.moko.mkscannerpro.action.ACTION_DELETE_DEVICE";
    public static final String ACTION_DEVICE_STATE = "com.moko.mkscannerpro.action.ACTION_DEVICE_STATE";
    // sp
    public static final String SP_NAME = "sp_name_scanner";
//    public static final String SP_KEY_MQTT_CONFIG = "SP_KEY_MQTT_CONFIG";
    public static final String SP_KEY_MQTT_CONFIG_APP = "SP_KEY_MQTT_CONFIG_APP";


    public static final String SP_KEY_DEVICE_ADDRESS = "sp_key_device_address";
    public static final String SP_KEY_PASSWORD = "SP_KEY_PASSWORD";
    // extra_key
    // 设备列表
    public static final String EXTRA_KEY_RESPONSE_ORDER_TYPE = "EXTRA_KEY_RESPONSE_ORDER_TYPE";
    public static final String EXTRA_KEY_RESPONSE_VALUE = "EXTRA_KEY_RESPONSE_VALUE";
    public static final String EXTRA_KEY_DEVICE_CONFIG = "EXTRA_KEY_DEVICE_CONFIG";
    public static final String EXTRA_KEY_TEMP_TARGET = "EXTRA_KEY_TEMP_TARGET";
    public static final String EXTRA_KEY_TEMP_HOUR = "EXTRA_KEY_TEMP_HOUR";
    public static final String EXTRA_KEY_TEMP_MINUTE = "EXTRA_KEY_TEMP_MINUTE";
    public static final String EXTRA_KEY_FROM_ACTIVITY = "EXTRA_KEY_FROM_ACTIVITY";
    public static final String EXTRA_KEY_DEVICE_ID = "EXTRA_KEY_UNIQUE_ID";
    public static final String EXTRA_KEY_DEVICE= "EXTRA_KEY_DEVICE";
    public static final String EXTRA_DELETE_DEVICE_ID = "EXTRA_DELETE_DEVICE_ID";
    public static final String EXTRA_KEY_SCAN_SWITCH = "EXTRA_KEY_SCAN_SWITCH";
    public static final String EXTRA_KEY_SCAN_INTERVAL = "EXTRA_KEY_SCAN_INTERVAL";
    public static final String EXTRA_KEY_FILTER_RSSI = "EXTRA_KEY_FILTER_RSSI";
    public static final String EXTRA_KEY_FILTER_NAME = "EXTRA_KEY_FILTER_NAME";
    public static final String EXTRA_KEY_MQTT_CONFIG_APP = "EXTRA_KEY_MQTT_CONFIG_APP";
    public static final String EXTRA_KEY_SELECTED_DEVICE_MAC = "EXTRA_KEY_SELECTED_DEVICE_MAC";
    public static final String EXTRA_KEY_SELECTED_DEVICE_NAME = "EXTRA_KEY_SELECTED_DEVICE_NAME";
    // request_code
    public static final int REQUEST_CODE_TEMP_TARGET = 100;
    public static final int REQUEST_CODE_TIMER = 101;
    public static final int REQUEST_CODE_DELAY = 102;
    public static final int REQUEST_CODE_SELECT_FIRMWARE = 103;

    public static final int REQUEST_CODE_WIFI_SETTING = 0x10;
    public static final int REQUEST_CODE_OPERATION_STEP = 0x11;
    public static final int REQUEST_CODE_PERMISSION = 120;
    public static final int REQUEST_CODE_PERMISSION_2 = 121;
    public static final int REQUEST_CODE_LOCATION_SETTINGS = 122;
    public static final int REQUEST_CODE_CA_FILE = 123;
    public static final int REQUEST_CODE_CLIENT_KEY_FILE = 124;
    public static final int REQUEST_CODE_CLIENT_CEAR_FILE = 125;
    public static final int REQUEST_CODE_SET_DEVICE_MQTT = 126;
    public static final int REQUEST_CODE_MQTT_CONFIG_APP = 127;
    public static final int REQUEST_CODE_FILTER_CONDITION = 128;

    public static final int PERMISSION_REQUEST_CODE = 1;
    // result_code
    public static final int RESULT_CONN_DISCONNECTED = 2;
}
