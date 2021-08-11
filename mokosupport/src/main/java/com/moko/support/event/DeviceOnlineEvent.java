package com.moko.support.event;

public class DeviceOnlineEvent {

    private String deviceId;
    private boolean online;

    public DeviceOnlineEvent(String deviceId, boolean online) {
        this.deviceId = deviceId;
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
