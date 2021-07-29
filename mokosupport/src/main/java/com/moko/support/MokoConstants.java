package com.moko.support;

public class MokoConstants {

    // 发现状态
    public static final String ACTION_DISCOVER_SUCCESS = "com.moko.mkscannerpro.ACTION_DISCOVER_SUCCESS";
    public static final String ACTION_DISCOVER_TIMEOUT = "com.moko.mkscannerpro.ACTION_DISCOVER_TIMEOUT";
    // 断开连接
    public static final String ACTION_CONN_STATUS_DISCONNECTED = "com.moko.mkscannerpro.ACTION_CONN_STATUS_DISCONNECTED";
    // 命令结果
    public static final String ACTION_ORDER_RESULT = "com.moko.mkscannerpro.ACTION_ORDER_RESULT";
    public static final String ACTION_ORDER_TIMEOUT = "com.moko.mkscannerpro.ACTION_ORDER_TIMEOUT";
    public static final String ACTION_ORDER_FINISH = "com.moko.mkscannerpro.ACTION_ORDER_FINISH";
    public static final String ACTION_CURRENT_DATA = "com.moko.mkscannerpro.ACTION_CURRENT_DATA";

    // extra_key
    public static final String EXTRA_KEY_RESPONSE_ORDER_TASK = "EXTRA_KEY_RESPONSE_ORDER_TASK";
    public static final String EXTRA_KEY_CURRENT_DATA_TYPE = "EXTRA_KEY_CURRENT_DATA_TYPE";
    public static final String EXTRA_KEY_SELECTED_DEVICE_MAC = "EXTRA_KEY_SELECTED_DEVICE_MAC";
    public static final String EXTRA_KEY_SELECTED_DEVICE_NAME = "EXTRA_KEY_SELECTED_DEVICE_NAME";

    public static final int HEADER_SEND = 0xEA;
    public static final int HEADER_RECEIVE = 0xEB;
    // header
    public static final int HEADER_GET_DEVICE_INFO = 4001;
    public static final int HEADER_SET_MQTT_INFO = 4002;
    public static final int HEADER_SET_MQTT_SSL = 4003;
    public static final int HEADER_SET_TOPIC = 4004;
    public static final int HEADER_SET_SWITCH_STATUS = 4005;
    public static final int HEADER_SET_WIFI_INFO = 4006;
    public static final int HEADER_SET_DEVICE_ID = 4007;
    // device_to_app
    public static final int MSG_ID_D_2_A_SWITCH_STATE = 1001;
    public static final int MSG_ID_D_2_A_DEVICE_INFO = 1002;
    public static final int MSG_ID_D_2_A_TIMER_INFO = 1003;
    public static final int MSG_ID_D_2_A_OTA_INFO = 1004;
    public static final int MSG_ID_D_2_A_DELETE = 1005;
    public static final int MSG_ID_D_2_A_POWER_INFO = 1006;
    public static final int MSG_ID_D_2_A_POWER_STATUS = 1008;
    // app_to_device
    public static final int MSG_ID_A_2_D_SWITCH_STATE = 2001;
    public static final int MSG_ID_A_2_D_SET_TIMER = 2002;
    public static final int MSG_ID_A_2_D_RESET = 2003;
    public static final int MSG_ID_A_2_D_SET_OTA = 2004;
    public static final int MSG_ID_A_2_D_DEVICE_INFO = 2005;
    public static final int MSG_ID_A_2_D_SET_POWER_STATUS = 2006;
    public static final int MSG_ID_A_2_D_GET_POWER_STATUS = 2007;
    // response
    public static final int RESPONSE_SUCCESS = 0;
    public static final int RESPONSE_FAILED_LENGTH = 1;
    public static final int RESPONSE_FAILED_DATA_FORMAT = 2;
    public static final int RESPONSE_FAILED_MQTT_WIFI = 3;
    // conn status
    public static final int CONN_STATUS_SUCCESS = 0;
    public static final int CONN_STATUS_CONNECTING = 1;
    public static final int CONN_STATUS_FAILED = 2;
    public static final int CONN_STATUS_TIMEOUT = 3;
    // mqtt conn status
    public static final int MQTT_CONN_STATUS_LOST = 0;
    public static final int MQTT_CONN_STATUS_SUCCESS = 1;
    public static final int MQTT_CONN_STATUS_FAILED = 2;
    // mqtt state
    public static final int MQTT_STATE_SUCCESS = 1;
    public static final int MQTT_STATE_FAILED = 0;
    // action
    public static final String ACTION_AP_CONNECTION = "com.moko.mkscannerpro.action.ACTION_AP_CONNECTION";
    public static final String ACTION_AP_SET_DATA_RESPONSE = "com.moko.mkscannerpro.action.ACTION_AP_SET_DATA_RESPONSE";
    public static final String ACTION_MQTT_CONNECTION = "com.moko.mkscannerpro.action.ACTION_MQTT_CONNECTION";
    public static final String ACTION_MQTT_RECEIVE = "com.moko.mkscannerpro.action.ACTION_MQTT_RECEIVE";
    public static final String ACTION_MQTT_SUBSCRIBE = "com.moko.mkscannerpro.action.ACTION_MQTT_SUBSCRIBE";
    public static final String ACTION_MQTT_PUBLISH = "com.moko.mkscannerpro.action.ACTION_MQTT_PUBLISH";
    public static final String ACTION_MQTT_UNSUBSCRIBE = "com.moko.mkscannerpro.action.ACTION_MQTT_UNSUBSCRIBE";
    // extra
    public static final String EXTRA_AP_CONNECTION = "EXTRA_AP_CONNECTION";
    public static final String EXTRA_AP_SET_DATA_RESPONSE = "EXTRA_AP_SET_DATA_RESPONSE";
    public static final String EXTRA_MQTT_CONNECTION_STATE = "EXTRA_MQTT_CONNECTION_STATE";
    public static final String EXTRA_MQTT_RECEIVE_TOPIC = "EXTRA_MQTT_RECEIVE_TOPIC";
    public static final String EXTRA_MQTT_RECEIVE_STATE = "EXTRA_MQTT_RECEIVE_STATE";
    public static final String EXTRA_MQTT_RECEIVE_MESSAGE = "EXTRA_MQTT_RECEIVE_MESSAGE";
    public static final String EXTRA_MQTT_STATE = "EXTRA_MQTT_STATE";

}
