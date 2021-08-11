package com.moko.support.event;

public class MQTTPublishFailureEvent {
    private String topic;
    private int msgId;

    public MQTTPublishFailureEvent(String topic, int msgId) {
        this.topic = topic;
        this.msgId = msgId;
    }

    public String getTopic() {
        return topic;
    }

    public int getMsgId() {
        return msgId;
    }
}
