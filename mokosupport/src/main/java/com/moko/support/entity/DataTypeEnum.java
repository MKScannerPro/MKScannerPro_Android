package com.moko.support.entity;

import java.io.Serializable;

public enum DataTypeEnum implements Serializable {
    TYPE_01(0x01),// Flags
    TYPE_02(0x02),// Incomplete List of 16-bit Service Class UUIDs
    TYPE_03(0x03),// Complete List of 16-bit Service Class UUIDs
    TYPE_04(0x04),// Incomplete List of 32-bit Service Class UUIDs
    TYPE_05(0x05),// Complete List of 32-bit Service Class UUIDs
    TYPE_06(0x06),// Incomplete List of 128-bit Service Class UUIDs
    TYPE_07(0x07),// Complete List of 128-bit Service Class UUIDs
    TYPE_08(0x08),// Shortened Local Name
    TYPE_09(0x09),// Complete Local Name
    TYPE_0A(0x0A),// Tx Power Level
    TYPE_0D(0x0D),// Class of Device
    TYPE_0E(0x0E),// Simple Pairing Hash C/Simple Pairing Hash C-192
    TYPE_0F(0x0F),// Simple Pairing Randomizer R-192
    TYPE_10(0x10),// Device ID/Security Manager TK Value
    TYPE_11(0x11),// Security Manager Out of Band Flags
    TYPE_12(0x12),// Slave Connection Interval Range
    TYPE_14(0x14),// List of 16-bit Service Solicitation UUIDs
    TYPE_15(0x15),// List of 128-bit Service Solicitation UUIDs
    TYPE_16(0x16),// Service Data/Service Data - 16-bit UUID
    TYPE_17(0x17),// Public Target Address
    TYPE_18(0x18),// Random Target Address
    TYPE_19(0x19),// Appearance
    TYPE_1A(0x1A),// Advertising Interval
    TYPE_1B(0x1B),// LE Bluetooth Device Address
    TYPE_1C(0x1C),// LE Role
    TYPE_1D(0x1D),// Simple Pairing Hash C-256
    TYPE_1E(0x1E),// Simple Pairing Randomizer R-256
    TYPE_1F(0x1F),// List of 32-bit Service Solicitation UUIDs
    TYPE_20(0x20),// Service Data - 32-bit UUID
    TYPE_21(0x21),// Service Data - 128-bit UUID
    TYPE_22(0x22),// LE Secure Connections Confirmation Value
    TYPE_23(0x23),// LE Secure Connections Random Value
    TYPE_24(0x24),// URI
    TYPE_25(0x25),// Indoor Positioning
    TYPE_26(0x26),// Transport Discovery Data
    TYPE_27(0x27),// LE Supported Features
    TYPE_28(0x28),// Channel Map Update Indication
    TYPE_29(0x29),// PB-ADV
    TYPE_2A(0x2A),// Mesh Message
    TYPE_2B(0x2B),// Mesh Beacon
    TYPE_2C(0x2C),// BIGInfo
    TYPE_2D(0x2D),// Broadcast_Code
    TYPE_3D(0x3D),// 3D Information Data
    TYPE_FF(0xFF),// Manufacturer Specific Data
    ;


    public static DataTypeEnum fromDataType(int deviceType) {
        for (DataTypeEnum deviceTypeEnum : DataTypeEnum.values()) {
            if (deviceTypeEnum.getDeviceType() == deviceType)
                return deviceTypeEnum;
        }
        return null;
    }

    private int deviceType;

    DataTypeEnum(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getDeviceType() {
        return deviceType;
    }
}
