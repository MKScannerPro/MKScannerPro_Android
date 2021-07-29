package com.moko.support.handler;

import com.moko.support.entity.FilterRawData;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;

import java.util.ArrayList;

public class MQTTMessageAssembler {
    public static byte[] assembleReadScanSwitch(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x17;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadScanInterval(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x18;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteScanSwitch(String id, boolean scanSwitch) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 5];
        b[0] = 0x26;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 1;
        b[dataLength + 4] = (byte) (scanSwitch ? 1 : 0);
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteScanInterval(String id, int interval) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 6];
        b[0] = 0x27;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 2;
        byte[] intervalBytes = MokoUtils.toByteArray(interval, 2);
        b[dataLength + 4] = intervalBytes[0];
        b[dataLength + 5] = intervalBytes[1];
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadFilterRSSI(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x19;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }


    public static byte[] assembleReadFilterName(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x20;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadFilterMac(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x1d;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadFilterRawData(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x1c;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteFilterRSSI(String id, int rssi) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 5];
        b[0] = 0x25;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 1;
        b[dataLength + 4] = (byte) rssi;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteFilterName(String id, String name) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        int nameLength = name.length();
        byte[] b = new byte[dataLength + 4 + nameLength];
        b[0] = 0x29;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = (byte) nameLength;
        byte[] nameBytes = name.getBytes();
        for (int i = 0; i < nameLength; i++) {
            b[dataLength + i + 4] = nameBytes[i];
        }
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteFilterMAC(String id, String mac) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        int macLength = mac.length() / 2;
        byte[] b = new byte[dataLength + 4 + macLength];
        b[0] = 0x2f;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = (byte) macLength;
        byte[] macBytes = MokoUtils.hex2bytes(mac);
        for (int i = 0; i < macLength; i++) {
            b[dataLength + i + 4] = macBytes[i];
        }
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteFilterRawData(String id, int rawDataSumLength, ArrayList<FilterRawData> rawDatas) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4 + rawDataSumLength];
        b[0] = 0x2e;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = (byte) rawDataSumLength;
        for (int i = 0, k = 0; i < rawDataSumLength && k < rawDatas.size(); k++) {
            FilterRawData rawData = rawDatas.get(k);
            b[dataLength + i + 4] = (byte) rawData.rawDataLength;
            b[dataLength + i + 5] = (byte) rawData.deviceType;
            b[dataLength + i + 6] = (byte) rawData.min;
            b[dataLength + i + 7] = (byte) rawData.max;
            byte[] rawDataBytes = MokoUtils.hex2bytes(rawData.rawData);
            for (int j = 0, length = rawDataBytes.length; j < length; j++) {
                b[dataLength + i + 8 + j] = rawDataBytes[j];
            }
            i += rawData.rawDataLength + 1;
        }
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteReset(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 5];
        b[0] = 0x28;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 1;
        b[dataLength + 4] = 1;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadCompanyName(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x12;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadProductDate(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x13;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadProductModel(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x1A;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadFirmwareVersion(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x15;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadMac(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x16;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteOTAType(String id, int type) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 5];
        b[0] = 0x2A;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 1;
        b[dataLength + 4] = (byte) type;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteHostAndPort(String id, String host, int port) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        int hostLength = host.length();
        byte[] b = new byte[dataLength + 4 + hostLength + 2];
        b[0] = 0x2B;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        byte[] hostAndPortLength = MokoUtils.toByteArray(hostLength + 2, 2);
        b[dataLength + 2] = hostAndPortLength[0];
        b[dataLength + 3] = hostAndPortLength[1];
        byte[] hostBytes = host.getBytes();
        for (int i = 0; i < hostLength; i++) {
            b[dataLength + i + 4] = hostBytes[i];
        }
        byte[] portBytes = MokoUtils.toByteArray(port, 2);
        b[dataLength + hostLength + 4] = portBytes[0];
        b[dataLength + hostLength + 5] = portBytes[1];
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteCatalogue(String id, String catalogue) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        int catalogueLength = catalogue.length();
        byte[] b = new byte[dataLength + 4 + catalogueLength];
        b[0] = 0x2C;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = (byte) catalogueLength;
        byte[] catalogueBytes = catalogue.getBytes();
        for (int i = 0; i < catalogueLength; i++) {
            b[dataLength + i + 4] = catalogueBytes[i];
        }
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadLEDStatus(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x1b;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteLEDStatus(String id, int bleBroadcastEnable, int bleConnectedEnable, int serverConnectingEnable, int serverConnectedEnable) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 8];
        b[0] = 0x2d;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 4;
        b[dataLength + 4] = (byte) serverConnectingEnable;
        b[dataLength + 5] = (byte) serverConnectedEnable;
        b[dataLength + 6] = (byte) bleBroadcastEnable;
        b[dataLength + 7] = (byte) bleConnectedEnable;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleReadDataReportInterval(String id) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 4];
        b[0] = 0x1e;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 0;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }

    public static byte[] assembleWriteDataReportInterval(String id, int interval) {
        byte[] dataBytes = id.getBytes();
        int dataLength = dataBytes.length;
        byte[] b = new byte[dataLength + 5];
        b[0] = 0x30;
        b[1] = (byte) dataLength;
        for (int i = 0; i < dataLength; i++) {
            b[i + 2] = dataBytes[i];
        }
        b[dataLength + 2] = 0;
        b[dataLength + 3] = 1;
        b[dataLength + 4] = (byte) interval;
        LogModule.e("app_to_device--->" + MokoUtils.bytesToHexString(b));
        return b;
    }
}
