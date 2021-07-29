package com.moko.support.entity;

import java.io.Serializable;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 命令枚举
 * @ClassPath com.fitpolo.support.entity.OrderEnum
 */
public enum OrderEnum implements Serializable {
    OPEN_NOTIFY("打开设备通知", 0),

    WRITE_HOST_PACKAGE_SUM("设置HOST包数", 0x81),
    WRITE_HOST("设置HOST", 0x01),
    WRITE_PORT("设置PORT", 0x02),
    WRITE_SESSION("设置Session", 0x03),
    WRITE_DEVICE_ID_PACKAGE_SUM("设置DeviceID包数", 0x82),
    WRITE_DEVICE_ID("设置DeviceID", 0x04),
    WRITE_CLIENT_ID_PACKAGE_SUM("设置ClientID包数", 0x83),
    WRITE_CLIENT_ID("设置ClientID", 0x05),
    WRITE_USERNAME_PACKAGE_SUM("设置Username包数", 0x84),
    WRITE_USERNAME("设置Username", 0x06),
    WRITE_PASSWORD_PACKAGE_SUM("设置Password包数", 0x85),
    WRITE_PASSWORD("设置Password", 0x07),
    WRITE_KEEPALIVE("设置KeepAlive", 0x08),
    WRITE_QOS("设置Qos", 0x09),
    WRITE_CONNECTMODE("设置connectMode", 0x0A),
    WRITE_CA_PACKAGE_SUM("设置CA包数", 0x86),
    WRITE_CA("设置CA", 0x0B),
    WRITE_CLIENTCERT_PACKAGE_SUM("设置ClientCert包数", 0x87),
    WRITE_CLIENTCERT("设置ClientCert", 0x0C),
    WRITE_CLIENTPRIVATE_PACKAGE_SUM("设置ClientPrivate包数", 0x88),
    WRITE_CLIENTPRIVATE("设置ClientPrivate", 0x0D),
    WRITE_START_CONNECT("设置联网", 0x10),
    WRITE_RESET("设置恢复出厂设置", 0x30),
    WRITE_PUBLISH_PACKAGE_SUM("设置发布主题包数", 0x89),
    WRITE_PUBLISH("设置发布主题", 0x0E),
    WRITE_SUBSCRIBE_PACKAGE_SUM("设置订阅主题包数", 0x8A),
    WRITE_SUBSCRIBE("设置订阅主题", 0x0F),
    WRITE_STA_NAME_PACKAGE_SUM("设置SAT名称包数", 0x8B),
    WRITE_STA_NAME("设置SAT名称", 0x31),
    WRITE_STA_PASSWORD_PACKAGE_SUM("设置SAT密码包数", 0x8C),
    WRITE_STA_PASSWORD("设置SAT密码", 0x32),
    ;


    private String orderName;
    private int orderHeader;

    OrderEnum(String orderName, int orderHeader) {
        this.orderName = orderName;
        this.orderHeader = orderHeader;
    }

    public int getOrderHeader() {
        return orderHeader;
    }

    public String getOrderName() {
        return orderName;
    }
}
