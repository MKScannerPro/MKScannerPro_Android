package com.moko.support.entity;


import java.io.Serializable;

public enum ParamsKeyEnum implements Serializable {

    KEY_EXIT_CONFIG_MODE(0x01),
    KEY_WIFI_SSID(0x02),
    KEY_WIFI_PASSWORD(0x03),
    KEY_MQTT_CONNECT_MODE(0x04),
    KEY_MQTT_HOST(0x05),
    KEY_MQTT_PORT(0x06),
    KEY_MQTT_CLEAN_SESSION(0x07),
    KEY_MQTT_KEEP_ALIVE(0x08),
    KEY_MQTT_QOS(0x09),
    KEY_MQTT_CLIENT_ID(0x0A),
    KEY_MQTT_DEVICE_ID(0x0B),
    KEY_MQTT_SUBSCRIBE_TOPIC(0x0C),
    KEY_MQTT_PUBLISH_TOPIC(0x0D),
    KEY_NTP_URL(0x0E),
    KEY_NTP_TIME_ZONE(0x0F),
    KEY_DEVICE_MAC(0x10),
    KEY_DEVICE_NAME(0x11),
    KEY_PRODUCT_MODEL(0x12),
    KEY_MANUFACTURER(0x13),
    KEY_HARDWARE_VERSION(0x14),
    KEY_SOFTWARE_VERSION(0x15),
    KEY_FIRMWARE_VERSION(0x16),
    KEY_DEVICE_TYPE(0x17),
    KEY_CONNECTION_TIMEOUT(0x18),

    KEY_MQTT_USERNAME(0x01),
    KEY_MQTT_PASSWORD(0x02),
    KEY_MQTT_CA(0x03),
    KEY_MQTT_CLIENT_CERT(0x04),
    KEY_MQTT_CLIENT_KEY(0x05),
    ;

    private int paramsKey;

    ParamsKeyEnum(int paramsKey) {
        this.paramsKey = paramsKey;
    }


    public int getParamsKey() {
        return paramsKey;
    }

    public static ParamsKeyEnum fromParamKey(int paramsKey) {
        for (ParamsKeyEnum paramsKeyEnum : ParamsKeyEnum.values()) {
            if (paramsKeyEnum.getParamsKey() == paramsKey) {
                return paramsKeyEnum;
            }
        }
        return null;
    }
}
