package com.moko.support.entity;

import java.io.Serializable;
import java.util.UUID;

public enum OrderCHAR implements Serializable {
    // AA00
    CHAR_PASSWORD(UUID.fromString("0000AA00-0000-1000-8000-00805F9B34FB")),
    CHAR_DISCONNECTED_NOTIFY(UUID.fromString("0000AA01-0000-1000-8000-00805F9B34FB")),
    CHAR_PARAMS(UUID.fromString("0000AA03-0000-1000-8000-00805F9B34FB")),
    ;

    private UUID uuid;

    OrderCHAR(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
