package com.moko.mkscannerpro.entity;


import java.io.Serializable;

public class MokoDevice implements Serializable {

    public int id;
    public String name;
    public String nickName;
    public String uniqueId;
    public String company_name;
    public String production_date;
    public String product_model;
    public String firmware_version;
    public String mqttInfo;
    public String topicPublish;
    public String topicSubscribe;
    public String mac;
    public boolean isOnline;
}
