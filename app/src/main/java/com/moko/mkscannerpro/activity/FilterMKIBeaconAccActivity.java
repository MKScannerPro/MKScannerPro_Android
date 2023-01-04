package com.moko.mkscannerpro.activity;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.databinding.ActivityFilterMkibeaconAccBinding;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.entity.FilterIBeacon;
import com.moko.support.entity.MsgConfigResult;
import com.moko.support.entity.MsgDeviceInfo;
import com.moko.support.entity.MsgReadResult;
import com.moko.support.event.DeviceOnlineEvent;
import com.moko.support.event.MQTTMessageArrivedEvent;
import com.moko.support.handler.MQTTMessageAssembler;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class FilterMKIBeaconAccActivity extends BaseActivity {
    private ActivityFilterMkibeaconAccBinding mBind;


    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = ActivityFilterMkibeaconAccBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());

        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getFilterMKIBeaconAcc();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String topic = event.getTopic();
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message))
            return;
        int msg_id;
        try {
            JsonObject object = new Gson().fromJson(message, JsonObject.class);
            JsonElement element = object.get("msg_id");
            msg_id = element.getAsInt();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_MKIBEACON_ACC) {
            Type type = new TypeToken<MsgReadResult<FilterIBeacon>>() {
            }.getType();
            MsgReadResult<FilterIBeacon> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mBind.cbIbeacon.setChecked(result.data.onOff == 1);
            mBind.etIbeaconUuid.setText(result.data.uuid);
            mBind.etIbeaconMajorMin.setText(String.valueOf(result.data.min_major));
            mBind.etIbeaconMajorMax.setText(String.valueOf(result.data.max_major));
            mBind.etIbeaconMinorMin.setText(String.valueOf(result.data.min_minor));
            mBind.etIbeaconMinorMax.setText(String.valueOf(result.data.max_minor));
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_MKIBEACON_ACC) {
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

    private void getFilterMKIBeaconAcc() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadFilterMKIBeaconAcc(deviceInfo);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_FILTER_MKIBEACON_ACC, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onBack(View view) {
        finish();
    }

    public void onSave(View view) {
        if (isWindowLocked())
            return;
        if (isValid()) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            saveParams();
        }
    }


    private void saveParams() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;

        FilterIBeacon filterIBeacon = new FilterIBeacon();
        filterIBeacon.onOff = mBind.cbIbeacon.isChecked() ? 1 : 0;
        filterIBeacon.min_major = Integer.parseInt(mBind.etIbeaconMajorMin.getText().toString());
        filterIBeacon.max_major = Integer.parseInt(mBind.etIbeaconMajorMax.getText().toString());
        filterIBeacon.min_minor = Integer.parseInt(mBind.etIbeaconMinorMin.getText().toString());
        filterIBeacon.max_minor = Integer.parseInt(mBind.etIbeaconMinorMax.getText().toString());
        filterIBeacon.uuid = mBind.etIbeaconUuid.getText().toString();

        String message = MQTTMessageAssembler.assembleWriteFilterMKIBeaconAcc(deviceInfo, filterIBeacon);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_FILTER_MKIBEACON_ACC, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        final String uuid = mBind.etIbeaconUuid.getText().toString();
        final String majorMin = mBind.etIbeaconMajorMin.getText().toString();
        final String majorMax = mBind.etIbeaconMajorMax.getText().toString();
        final String minorMin = mBind.etIbeaconMinorMin.getText().toString();
        final String minorMax = mBind.etIbeaconMinorMax.getText().toString();
        if (!TextUtils.isEmpty(uuid)) {
            int length = uuid.length();
            if (length % 2 != 0) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
        }
        if (!TextUtils.isEmpty(majorMin) && !TextUtils.isEmpty(majorMax)) {
            if (Integer.parseInt(majorMin) > 65535) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            if (Integer.parseInt(majorMax) > 65535) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            if (Integer.parseInt(majorMax) < Integer.parseInt(majorMin)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
        } else if (TextUtils.isEmpty(majorMin) || TextUtils.isEmpty(majorMax)) {
            ToastUtils.showToast(this, "Para Error");
            return false;
        }

        if (!TextUtils.isEmpty(minorMin) && !TextUtils.isEmpty(minorMax)) {
            if (Integer.parseInt(minorMin) > 65535) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            if (Integer.parseInt(minorMax) > 65535) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            if (Integer.parseInt(minorMax) < Integer.parseInt(minorMin)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
        } else if (TextUtils.isEmpty(minorMin) || TextUtils.isEmpty(minorMax)) {
            ToastUtils.showToast(this, "Para Error");
            return false;
        }
        return true;
    }
}
