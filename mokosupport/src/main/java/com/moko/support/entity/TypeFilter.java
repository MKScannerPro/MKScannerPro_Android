package com.moko.support.entity;

import com.google.gson.annotations.SerializedName;

public class TypeFilter {

    /**
     * ibeacon : 1
     * eddyston_uid : 1
     * eddyston_url : 1
     * eddyston_tlm : 1
     * MK_ibeacon : 1
     * MK_ACC : 1
     * BXP_ACC : 1
     * BXP_T&H : 1
     * unknown : 1
     */

    public int ibeacon;
    public int eddyston_uid;
    public int eddyston_url;
    public int eddyston_tlm;
    public int MK_ibeacon;
    public int MK_ACC;
    public int BXP_ACC;
    @SerializedName("BXP_T&H")
    public int BXP_TH; // FIXME check this code
    public int unknown;
}
