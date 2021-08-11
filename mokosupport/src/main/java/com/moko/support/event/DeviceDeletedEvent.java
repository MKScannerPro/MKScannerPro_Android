package com.moko.support.event;

public class DeviceDeletedEvent {

    private int id;

    public DeviceDeletedEvent(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
