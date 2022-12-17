package com.moko.support.entity;


import java.io.Serializable;

public enum ParamsLongKeyEnum implements Serializable {

    KEY_MQTT_USERNAME(0x01),
    KEY_MQTT_PASSWORD(0x02),
    KEY_MQTT_CA(0x03),
    KEY_MQTT_CLIENT_CERT(0x04),
    KEY_MQTT_CLIENT_KEY(0x05),
    ;

    private int paramsKey;

    ParamsLongKeyEnum(int paramsKey) {
        this.paramsKey = paramsKey;
    }


    public int getParamsKey() {
        return paramsKey;
    }

    public static ParamsLongKeyEnum fromParamKey(int paramsKey) {
        for (ParamsLongKeyEnum paramsKeyEnum : ParamsLongKeyEnum.values()) {
            if (paramsKeyEnum.getParamsKey() == paramsKey) {
                return paramsKeyEnum;
            }
        }
        return null;
    }
}
