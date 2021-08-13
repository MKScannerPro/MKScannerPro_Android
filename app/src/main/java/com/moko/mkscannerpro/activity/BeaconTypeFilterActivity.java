package com.moko.mkscannerpro.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.entity.MsgConfigResult;
import com.moko.support.entity.MsgDeviceInfo;
import com.moko.support.entity.MsgReadResult;
import com.moko.support.entity.TypeFilter;
import com.moko.support.event.DeviceOnlineEvent;
import com.moko.support.event.MQTTMessageArrivedEvent;
import com.moko.support.handler.MQTTMessageAssembler;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BeaconTypeFilterActivity extends BaseActivity {

    @BindView(R.id.cb_type_ibeacon)
    CheckBox cbTypeIbeacon;
    @BindView(R.id.cb_type_eddystone_uid)
    CheckBox cbTypeEddystoneUid;
    @BindView(R.id.cb_type_eddystone_url)
    CheckBox cbTypeEddystoneUrl;
    @BindView(R.id.cb_type_eddystone_tlm)
    CheckBox cbTypeEddystoneTlm;
    @BindView(R.id.cb_type_mkibeacon)
    CheckBox cbTypeMkibeacon;
    @BindView(R.id.cb_type_mkibeacon_acc)
    CheckBox cbTypeMkibeaconAcc;
    @BindView(R.id.cb_type_bxp_acc)
    CheckBox cbTypeBxpAcc;
    @BindView(R.id.cb_type_bxp_th)
    CheckBox cbTypeBxpTh;
    @BindView(R.id.cb_type_unknown)
    CheckBox cbTypeUnknown;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_type_filter);
        ButterKnife.bind(this);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getBeaconTypeFilter();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String topic = event.getTopic();
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message))
            return;
        JsonObject object = new Gson().fromJson(message, JsonObject.class);
        JsonElement element = object.get("msg_id");
        int msg_id = element.getAsInt();
        if (msg_id == MQTTConstants.READ_MSG_ID_BEACON_TYPE_FILTER) {
            Type type = new TypeToken<MsgReadResult<TypeFilter>>() {
            }.getType();
            MsgReadResult<TypeFilter> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            cbTypeIbeacon.setChecked(result.data.ibeacon == 1);
            cbTypeEddystoneUid.setChecked(result.data.eddystone_uid == 1);
            cbTypeEddystoneUrl.setChecked(result.data.eddystone_url == 1);
            cbTypeEddystoneTlm.setChecked(result.data.eddystone_tlm == 1);
            cbTypeMkibeacon.setChecked(result.data.MK_iBeacon == 1);
            cbTypeMkibeaconAcc.setChecked(result.data.MK_ACC == 1);
            cbTypeBxpAcc.setChecked(result.data.BXP_ACC == 1);
            cbTypeBxpTh.setChecked(result.data.BXP_TH == 1);
            cbTypeUnknown.setChecked(result.data.unknown == 1);
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_BEACON_TYPE_FILTER) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.result_code == 0) {
                ToastUtils.showToast(this, "Set up succeed");
            } else {
                ToastUtils.showToast(this, "Set up failed");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        String deviceId = event.getDeviceId();
        if (!mMokoDevice.deviceId.equals(deviceId)) {
            return;
        }
        boolean online = event.isOnline();
        if (!online) {
            finish();
        }
    }

    public void back(View view) {
        finish();
    }

    private void setBeaconTypeFilter() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;
        TypeFilter typeFilter = new TypeFilter();
        typeFilter.ibeacon = cbTypeIbeacon.isChecked() ? 1 : 0;
        typeFilter.eddystone_uid = cbTypeEddystoneUid.isChecked() ? 1 : 0;
        typeFilter.eddystone_url = cbTypeEddystoneUrl.isChecked() ? 1 : 0;
        typeFilter.eddystone_tlm = cbTypeEddystoneTlm.isChecked() ? 1 : 0;
        typeFilter.MK_iBeacon = cbTypeMkibeacon.isChecked() ? 1 : 0;
        typeFilter.MK_ACC = cbTypeMkibeaconAcc.isChecked() ? 1 : 0;
        typeFilter.BXP_ACC = cbTypeBxpAcc.isChecked() ? 1 : 0;
        typeFilter.BXP_TH = cbTypeBxpTh.isChecked() ? 1 : 0;
        typeFilter.unknown = cbTypeUnknown.isChecked() ? 1 : 0;
        String message = MQTTMessageAssembler.assembleWriteBeaconTypeFilter(deviceInfo, typeFilter);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_BEACON_TYPE_FILTER, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void getBeaconTypeFilter() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadBeaconTypeFilter(deviceInfo);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_BEACON_TYPE_FILTER, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSave(View view) {
        if (isWindowLocked())
            return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setBeaconTypeFilter();
    }
}
