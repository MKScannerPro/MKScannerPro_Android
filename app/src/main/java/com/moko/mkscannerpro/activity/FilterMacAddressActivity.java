package com.moko.mkscannerpro.activity;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.dialog.AlertMessageDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.entity.FilterType;
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
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FilterMacAddressActivity extends BaseActivity {


    @BindView(R.id.cb_precise_match)
    CheckBox cbPreciseMatch;
    @BindView(R.id.cb_reverse_filter)
    CheckBox cbReverseFilter;
    @BindView(R.id.ll_mac_address)
    LinearLayout llMacAddress;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;

    private List<String> filterMacAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_mac_address);
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
        getFilterMacAddress();
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
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_MAC_ADDRESS) {
            Type type = new TypeToken<MsgReadResult<FilterType>>() {
            }.getType();
            MsgReadResult<FilterType> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            cbPreciseMatch.setChecked(result.data.precise == 1);
            cbReverseFilter.setChecked(result.data.reverse == 1);
            int number = result.data.array_num;
            if (number == 0) {
                filterMacAddress = new ArrayList<>();
            } else {
                filterMacAddress = result.data.rule;
                if (filterMacAddress.size() > 0) {
                    for (int i = 0, l = filterMacAddress.size(); i < l; i++) {
                        String macAddress = filterMacAddress.get(i);
                        View v = LayoutInflater.from(FilterMacAddressActivity.this).inflate(R.layout.item_mac_address_filter, llMacAddress, false);
                        TextView title = v.findViewById(R.id.tv_mac_address_title);
                        EditText etMacAddress = v.findViewById(R.id.et_mac_address);
                        title.setText(String.format("MAC %d", i + 1));
                        etMacAddress.setText(macAddress);
                        llMacAddress.addView(v);
                    }
                }
            }

        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_MAC_ADDRESS) {
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

    private void getFilterMacAddress() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadFilterMacAddress(deviceInfo);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_FILTER_MAC_ADDRESS, appMqttConfig.qos);
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

    public void onAdd(View view) {
        if (isWindowLocked())
            return;
        int count = llMacAddress.getChildCount();
        if (count > 9) {
            ToastUtils.showToast(this, "You can set up to 10 filters!");
            return;
        }
        View v = LayoutInflater.from(this).inflate(R.layout.item_mac_address_filter, llMacAddress, false);
        TextView title = v.findViewById(R.id.tv_mac_address_title);
        title.setText(String.format("MAC %d", count + 1));
        llMacAddress.addView(v);
    }

    public void onDel(View view) {
        if (isWindowLocked())
            return;
        final int c = llMacAddress.getChildCount();
        if (c == 0) {
            ToastUtils.showToast(this, "There are currently no filters to delete");
            return;
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage("Please confirm whether to delete it, if yes, the last option will be deleted!");
        dialog.setOnAlertConfirmListener(() -> {
            int count = llMacAddress.getChildCount();
            if (count > 0) {
                llMacAddress.removeViewAt(count - 1);
            }
        });
        dialog.show(getSupportFragmentManager());
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

        FilterType filterType = new FilterType();
        filterType.precise = cbPreciseMatch.isChecked() ? 1 : 0;
        filterType.reverse = cbReverseFilter.isChecked() ? 1 : 0;
        filterType.array_num = filterMacAddress.size();
        filterType.rule = filterMacAddress;

        String message = MQTTMessageAssembler.assembleWriteFilterMacAddress(deviceInfo, filterType);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_FILTER_MAC_ADDRESS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        final int c = llMacAddress.getChildCount();
        if (c > 0) {
            // 发送设置的过滤RawData
            int count = llMacAddress.getChildCount();
            if (count == 0) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            filterMacAddress.clear();
            for (int i = 0; i < count; i++) {
                View v = llMacAddress.getChildAt(i);
                EditText etMacAddress = v.findViewById(R.id.et_mac_address);
                final String macAddress = etMacAddress.getText().toString();
                if (TextUtils.isEmpty(macAddress)) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                int length = macAddress.length();
                if (length % 2 != 0) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                filterMacAddress.add(macAddress);
            }
        } else {
            filterMacAddress = new ArrayList<>();
        }
        return true;
    }
}
