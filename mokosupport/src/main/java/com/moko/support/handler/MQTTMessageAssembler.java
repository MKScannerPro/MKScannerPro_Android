package com.moko.support.handler;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moko.support.MQTTConstants;
import com.moko.support.entity.ConnectionTimeout;
import com.moko.support.entity.DataReportTimeout;
import com.moko.support.entity.DuplicateDataFilter;
import com.moko.support.entity.FilterCondition;
import com.moko.support.entity.FilterRelationWrite;
import com.moko.support.entity.IndicatorLightStatus;
import com.moko.support.entity.MsgConfigReq;
import com.moko.support.entity.MsgDeviceInfo;
import com.moko.support.entity.MsgReadReq;
import com.moko.support.entity.NetworkReportPeriod;
import com.moko.support.entity.OTAParams;
import com.moko.support.entity.ResetState;
import com.moko.support.entity.Restart;
import com.moko.support.entity.ScanConfig;
import com.moko.support.entity.ScanTimeout;
import com.moko.support.entity.SystemTime;
import com.moko.support.entity.TypeFilter;
import com.moko.support.entity.UploadDataOption;

public class MQTTMessageAssembler {
    public static String assembleReadScanConfig(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_SCAN_CONFIG;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteScanConfig(MsgDeviceInfo deviceInfo, ScanConfig scanConfig) {
        MsgConfigReq<ScanConfig> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_SCAN_CONFIG;
        configReq.data = scanConfig;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadLEDStatus(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_INDICATOR_STATUS;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteLEDStatus(MsgDeviceInfo deviceInfo, IndicatorLightStatus lightStatus) {
        MsgConfigReq<IndicatorLightStatus> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_INDICATOR_STATUS;
        configReq.data = lightStatus;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDataReportTimeout(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_DATA_REPORT_TIMEOUT;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteDataReportTimeout(MsgDeviceInfo deviceInfo, DataReportTimeout interval) {
        MsgConfigReq<DataReportTimeout> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_DATA_REPORT_TIMEOUT;
        configReq.data = interval;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadNetworkReportPeriod(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_NETWORK_REPORT_PERIOD;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteNetworkReportPeriod(MsgDeviceInfo deviceInfo, NetworkReportPeriod period) {
        MsgConfigReq<NetworkReportPeriod> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_NETWORK_REPORT_PERIOD;
        configReq.data = period;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadConnectionTimeout(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_CONN_TIMEOUT;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteConnectionTimeout(MsgDeviceInfo deviceInfo, ConnectionTimeout timeout) {
        MsgConfigReq<ConnectionTimeout> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_CONN_TIMEOUT;
        configReq.data = timeout;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadScanTimeout(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_BLE_SCAN_TIMEOUT;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteScanTimeout(MsgDeviceInfo deviceInfo, ScanTimeout timeout) {
        MsgConfigReq<ScanTimeout> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_BLE_SCAN_TIMEOUT;
        configReq.data = timeout;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadBeaconTypeFilter(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_BEACON_TYPE_FILTER;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteBeaconTypeFilter(MsgDeviceInfo deviceInfo, TypeFilter typeFilter) {
        MsgConfigReq<TypeFilter> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_BEACON_TYPE_FILTER;
        configReq.data = typeFilter;
        Gson gs = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
        String message = gs.toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadSystemTime(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_UTC;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteSystemTime(MsgDeviceInfo deviceInfo, SystemTime timeout) {
        MsgConfigReq<SystemTime> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_UTC;
        configReq.data = timeout;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOTA(MsgDeviceInfo deviceInfo, OTAParams params) {
        MsgConfigReq<OTAParams> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_OTA;
        configReq.data = params;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDeviceInfo(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_DEVICE_INFO;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDeviceConfigInfo(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_CONFIG_INFO;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadFilterRelation(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_FILTER_RELATION;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteFilterRelation(MsgDeviceInfo deviceInfo, FilterRelationWrite relation) {
        MsgConfigReq<FilterRelationWrite> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_FILTER_RELATION;
        configReq.data = relation;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadFilterA(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_FILTER_A;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteFilterA(MsgDeviceInfo deviceInfo, FilterCondition condition) {
        MsgConfigReq<FilterCondition> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_FILTER_A;
        configReq.data = condition;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadFilterB(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_FILTER_B;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteFilterB(MsgDeviceInfo deviceInfo, FilterCondition condition) {
        MsgConfigReq<FilterCondition> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_FILTER_B;
        configReq.data = condition;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadUploadDataOption(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_UPLOAD_DATA_OPTION;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteUploadDataOption(MsgDeviceInfo deviceInfo, UploadDataOption uploadDataOption) {
        MsgConfigReq<UploadDataOption> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_UPLOAD_DATA_OPTION;
        configReq.data = uploadDataOption;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDuplicateDataFilter(MsgDeviceInfo deviceInfo) {
        MsgReadReq readReq = new MsgReadReq();
        readReq.device_info = deviceInfo;
        readReq.msg_id = MQTTConstants.READ_MSG_ID_DUPLICATE_DATA_FILTER;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteDuplicateDataFilter(MsgDeviceInfo deviceInfo, DuplicateDataFilter dataFilter) {
        MsgConfigReq<DuplicateDataFilter> configReq = new MsgConfigReq();
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_DUPLICATE_DATA_FILTER;
        configReq.data = dataFilter;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteReset(MsgDeviceInfo deviceInfo) {
        MsgConfigReq<ResetState> configReq = new MsgConfigReq();
        ResetState resetState = new ResetState();
        resetState.reset_state = 1;
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_RESET;
        configReq.data = resetState;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteReboot(MsgDeviceInfo deviceInfo) {
        MsgConfigReq<Restart> configReq = new MsgConfigReq();
        Restart restart = new Restart();
        restart.restart = 1;
        configReq.device_info = deviceInfo;
        configReq.msg_id = MQTTConstants.CONFIG_MSG_ID_REBOOT;
        configReq.data = restart;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }
}
