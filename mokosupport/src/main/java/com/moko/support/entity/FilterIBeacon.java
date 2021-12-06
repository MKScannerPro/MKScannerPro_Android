package com.moko.support.entity;

import com.google.gson.annotations.SerializedName;

public class FilterIBeacon {
    @SerializedName("switch")
    public int onOff;
    public int min_major;
    public int max_major;
    public int min_minor;
    public int max_minor;
    public String uuid;
}
