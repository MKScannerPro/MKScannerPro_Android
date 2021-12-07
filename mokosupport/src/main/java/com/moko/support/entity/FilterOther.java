package com.moko.support.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FilterOther {
    @SerializedName("switch")
    public int onOff;
    public int relationship;
    public int array_num;
    public List<FilterCondition.RawDataBean> rule;
}
