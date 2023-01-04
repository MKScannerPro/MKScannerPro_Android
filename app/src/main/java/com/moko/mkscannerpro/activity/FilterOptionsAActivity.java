package com.moko.mkscannerpro.activity;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.databinding.ActivityFilterConditionBinding;
import com.moko.mkscannerpro.dialog.AlertMessageDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.entity.FilterCondition;
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

public class FilterOptionsAActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {
    private final String FILTER_ASCII = "[ -~]*";
    private ActivityFilterConditionBinding mBind;


    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;

    private List<FilterCondition.RawDataBean> filterRawDatas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = ActivityFilterConditionBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());

        mBind.tvTitle.setText("Filter Condition A");
        mBind.tvCondition.setText("Filter Condition A");
        mBind.tvConditionTips.setText(getString(R.string.condition_tips, "A", "A"));

        mBind.sbRssiFilter.setOnSeekBarChangeListener(this);
        InputFilter inputFilter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        mBind.etAdvName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10), inputFilter});
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);

        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getFilterConditionA();
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
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_A) {
            Type type = new TypeToken<MsgReadResult<FilterCondition>>() {
            }.getType();
            MsgReadResult<FilterCondition> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            int rule_switch = result.data.rule_switch;
            filterSwitchEnable = rule_switch == 1;
            mBind.ivCondition.setImageResource(filterSwitchEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            final int rssi = result.data.rssi;
            int progress = rssi + 127;
            mBind.sbRssiFilter.setProgress(progress);
            mBind.tvRssiFilterValue.setText(String.format("%ddBm", rssi));
            mBind.tvRssiFilterTips.setText(getString(R.string.rssi_filter, rssi));
            FilterCondition.NameBean nameBean = result.data.name;
            filterNameEnable = nameBean.flag > 0;
            mBind.ivAdvName.setImageResource(filterNameEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            mBind.etAdvName.setVisibility(filterNameEnable ? View.VISIBLE : View.GONE);
            mBind.cbAdvName.setVisibility(filterNameEnable ? View.VISIBLE : View.GONE);
            mBind.cbAdvName.setChecked(nameBean.flag > 1);
            mBind.etAdvName.setText(nameBean.rule);

            FilterCondition.MacBean macBean = result.data.mac;
            filterMacEnable = macBean.flag > 0;
            mBind.ivMacAddress.setImageResource(filterMacEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            mBind.etMacAddress.setVisibility(filterMacEnable ? View.VISIBLE : View.GONE);
            mBind.cbMacAddress.setVisibility(filterMacEnable ? View.VISIBLE : View.GONE);
            mBind.cbMacAddress.setChecked(macBean.flag > 1);
            mBind.etMacAddress.setText(macBean.rule);

            FilterCondition.UUIDBean uuidBean = result.data.uuid;
            filterUUIDEnable = uuidBean.flag > 0;
            mBind.ivIbeaconUuid.setImageResource(filterUUIDEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            mBind.etIbeaconUuid.setVisibility(filterUUIDEnable ? View.VISIBLE : View.GONE);
            mBind.cbIbeaconUuid.setVisibility(filterUUIDEnable ? View.VISIBLE : View.GONE);
            mBind.cbIbeaconUuid.setChecked(uuidBean.flag > 1);
            mBind.etIbeaconUuid.setText(uuidBean.rule);

            FilterCondition.MajorBean majorBean = result.data.major;
            filterMajorEnable = majorBean.flag > 0;
            mBind.ivIbeaconMajor.setImageResource(filterMajorEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            mBind.llIbeaconMajor.setVisibility(filterMajorEnable ? View.VISIBLE : View.GONE);
            mBind.cbIbeaconMajor.setVisibility(filterMajorEnable ? View.VISIBLE : View.GONE);
            mBind.cbIbeaconMajor.setChecked(majorBean.flag > 1);
            int majorMin = majorBean.min;
            mBind.etIbeaconMajorMin.setText(String.valueOf(majorMin));
            int majorMax = majorBean.max;
            mBind.etIbeaconMajorMax.setText(String.valueOf(majorMax));

            FilterCondition.MinorBean minorBean = result.data.minor;
            filterMinorEnable = minorBean.flag > 0;
            mBind.ivIbeaconMinor.setImageResource(filterMinorEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            mBind.llIbeaconMinor.setVisibility(filterMinorEnable ? View.VISIBLE : View.GONE);
            mBind.cbIbeaconMinor.setVisibility(filterMinorEnable ? View.VISIBLE : View.GONE);
            mBind.cbIbeaconMinor.setChecked(minorBean.flag > 1);
            int minorMin = minorBean.min;
            mBind.etIbeaconMinorMin.setText(String.valueOf(minorMin));
            int minorMax = minorBean.max;
            mBind.etIbeaconMinorMax.setText(String.valueOf(minorMax));

            FilterCondition.RawBean rawBean = result.data.raw;
            filterRawAdvDataEnable = rawBean.flag > 0;
            mBind.ivRawAdvData.setImageResource(filterRawAdvDataEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            mBind.llRawDataFilter.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
            mBind.ivRawDataAdd.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
            mBind.ivRawDataDel.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
            mBind.cbRawAdvData.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
            mBind.cbRawAdvData.setChecked(rawBean.flag > 1);
            filterRawDatas = result.data.raw.rule;
            if (filterRawDatas.size() > 0) {
                for (int i = 0, l = filterRawDatas.size(); i < l; i++) {
                    FilterCondition.RawDataBean rawDataBean = filterRawDatas.get(i);
                    View v = LayoutInflater.from(FilterOptionsAActivity.this).inflate(R.layout.item_raw_data_filter, mBind.llRawDataFilter, false);
                    EditText etDataType = v.findViewById(R.id.et_data_type);
                    EditText etMin = v.findViewById(R.id.et_min);
                    EditText etMax = v.findViewById(R.id.et_max);
                    EditText etRawData = v.findViewById(R.id.et_raw_data);
                    etDataType.setText(rawDataBean.type);
                    etMin.setText(String.valueOf(rawDataBean.start));
                    etMax.setText(String.valueOf(rawDataBean.end));
                    etRawData.setText(rawDataBean.data);
                    mBind.llRawDataFilter.addView(v);
                }
            }
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_A) {
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

    private void getFilterConditionA() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadFilterA(deviceInfo);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_FILTER_A, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean filterSwitchEnable;
    private boolean filterMacEnable;
    private boolean filterNameEnable;
    private boolean filterUUIDEnable;
    private boolean filterMajorEnable;
    private boolean filterMinorEnable;
    private boolean filterRawAdvDataEnable;

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

    public void onMacAddress(View view) {
        filterMacEnable = !filterMacEnable;
        mBind.ivMacAddress.setImageResource(filterMacEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        mBind.etMacAddress.setVisibility(filterMacEnable ? View.VISIBLE : View.GONE);
        mBind.cbMacAddress.setVisibility(filterMacEnable ? View.VISIBLE : View.GONE);
    }

    public void onAdvName(View view) {
        filterNameEnable = !filterNameEnable;
        mBind.ivAdvName.setImageResource(filterNameEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        mBind.etAdvName.setVisibility(filterNameEnable ? View.VISIBLE : View.GONE);
        mBind.cbAdvName.setVisibility(filterNameEnable ? View.VISIBLE : View.GONE);
    }

    public void oniBeaconUUID(View view) {
        filterUUIDEnable = !filterUUIDEnable;
        mBind.ivIbeaconUuid.setImageResource(filterUUIDEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        mBind.etIbeaconUuid.setVisibility(filterUUIDEnable ? View.VISIBLE : View.GONE);
        mBind.cbIbeaconUuid.setVisibility(filterUUIDEnable ? View.VISIBLE : View.GONE);
    }

    public void oniBeaconMajor(View view) {
        filterMajorEnable = !filterMajorEnable;
        mBind.ivIbeaconMajor.setImageResource(filterMajorEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        mBind.llIbeaconMajor.setVisibility(filterMajorEnable ? View.VISIBLE : View.GONE);
        mBind.cbIbeaconMajor.setVisibility(filterMajorEnable ? View.VISIBLE : View.GONE);
    }

    public void oniBeaconMinor(View view) {
        filterMinorEnable = !filterMinorEnable;
        mBind.ivIbeaconMinor.setImageResource(filterMinorEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        mBind.llIbeaconMinor.setVisibility(filterMinorEnable ? View.VISIBLE : View.GONE);
        mBind.cbIbeaconMinor.setVisibility(filterMinorEnable ? View.VISIBLE : View.GONE);
    }

    public void onRawAdvData(View view) {
        filterRawAdvDataEnable = !filterRawAdvDataEnable;
        mBind.ivRawAdvData.setImageResource(filterRawAdvDataEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        mBind.llRawDataFilter.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
        mBind.ivRawDataAdd.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
        mBind.ivRawDataDel.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
        mBind.cbRawAdvData.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
    }

    public void onRawDataAdd(View view) {
        if (isWindowLocked())
            return;
        int count = mBind.llRawDataFilter.getChildCount();
        if (count > 4) {
            ToastUtils.showToast(this, "You can set up to 5 filters!");
            return;
        }
        View v = LayoutInflater.from(this).inflate(R.layout.item_raw_data_filter, mBind.llRawDataFilter, false);
        mBind.llRawDataFilter.addView(v);
    }

    public void onRawDataDel(View view) {
        if (isWindowLocked())
            return;
        final int c = mBind.llRawDataFilter.getChildCount();
        if (c == 0) {
            ToastUtils.showToast(this, "There are currently no filters to delete");
            return;
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage("Please confirm whether to delete it, if yes, the last option will be deleted!");
        dialog.setOnAlertConfirmListener(() -> {
            int count = mBind.llRawDataFilter.getChildCount();
            if (count > 0) {
                mBind.llRawDataFilter.removeViewAt(count - 1);
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onCondition(View view) {
        filterSwitchEnable = !filterSwitchEnable;
        mBind.ivCondition.setImageResource(filterSwitchEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
    }


    private void saveParams() {
        final int progress = mBind.sbRssiFilter.getProgress();
        int filterRssi = progress - 127;
        final String mac = mBind.etMacAddress.getText().toString();
        final String name = mBind.etAdvName.getText().toString();
        final String uuid = mBind.etIbeaconUuid.getText().toString();
        final String majorMin = mBind.etIbeaconMajorMin.getText().toString();
        final String majorMax = mBind.etIbeaconMajorMax.getText().toString();
        final String minorMin = mBind.etIbeaconMinorMin.getText().toString();
        final String minorMax = mBind.etIbeaconMinorMax.getText().toString();
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;
        FilterCondition filterCondition = new FilterCondition();
        filterCondition.rule_switch = filterSwitchEnable ? 1 : 0;
        filterCondition.rssi = filterRssi;

        FilterCondition.NameBean nameBean = new FilterCondition.NameBean();
        nameBean.flag = filterNameEnable ? (mBind.cbAdvName.isChecked() ? 2 : 1) : 0;
        nameBean.rule = filterNameEnable ? name : "";
        filterCondition.name = nameBean;

        FilterCondition.MacBean macBean = new FilterCondition.MacBean();
        macBean.flag = filterMacEnable ? (mBind.cbMacAddress.isChecked() ? 2 : 1) : 0;
        macBean.rule = filterMacEnable ? mac : "";
        filterCondition.mac = macBean;

        FilterCondition.UUIDBean uuidBean = new FilterCondition.UUIDBean();
        uuidBean.flag = filterUUIDEnable ? (mBind.cbIbeaconUuid.isChecked() ? 2 : 1) : 0;
        uuidBean.rule = filterUUIDEnable ? uuid : "";
        filterCondition.uuid = uuidBean;

        FilterCondition.MajorBean majorBean = new FilterCondition.MajorBean();
        majorBean.flag = filterMajorEnable ? (mBind.cbIbeaconMajor.isChecked() ? 2 : 1) : 0;
        majorBean.min = filterMajorEnable ? Integer.parseInt(majorMin) : 0;
        majorBean.max = filterMajorEnable ? Integer.parseInt(majorMax) : 0;
        filterCondition.major = majorBean;

        FilterCondition.MinorBean minorBean = new FilterCondition.MinorBean();
        minorBean.flag = filterMinorEnable ? (mBind.cbIbeaconMinor.isChecked() ? 2 : 1) : 0;
        minorBean.min = filterMinorEnable ? Integer.parseInt(minorMin) : 0;
        minorBean.max = filterMinorEnable ? Integer.parseInt(minorMax) : 0;
        filterCondition.minor = minorBean;

        FilterCondition.RawBean rawBean = new FilterCondition.RawBean();
        rawBean.flag = filterRawAdvDataEnable ? (mBind.cbRawAdvData.isChecked() ? 2 : 1) : 0;
        rawBean.rule = filterRawDatas;
        filterCondition.raw = rawBean;

        String message = MQTTMessageAssembler.assembleWriteFilterA(deviceInfo, filterCondition);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_FILTER_A, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        final String mac = mBind.etMacAddress.getText().toString();
        final String name = mBind.etAdvName.getText().toString();
        final String uuid = mBind.etIbeaconUuid.getText().toString();
        final String majorMin = mBind.etIbeaconMajorMin.getText().toString();
        final String majorMax = mBind.etIbeaconMajorMax.getText().toString();
        final String minorMin = mBind.etIbeaconMinorMin.getText().toString();
        final String minorMax = mBind.etIbeaconMinorMax.getText().toString();
        if (filterMacEnable) {
            if (TextUtils.isEmpty(mac)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            int length = mac.length();
            if (length % 2 != 0) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
        }
        if (filterNameEnable) {
            if (TextUtils.isEmpty(name)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
        }
        if (filterUUIDEnable) {
            if (TextUtils.isEmpty(uuid)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            int length = uuid.length();
            if (length % 2 != 0) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
        }
        if (filterMajorEnable) {
            if (TextUtils.isEmpty(majorMin)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            if (Integer.parseInt(majorMin) > 65535) {
                ToastUtils.showToast(this, "Range Error");
                return false;
            }
            if (TextUtils.isEmpty(majorMax)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            if (Integer.parseInt(majorMax) > 65535) {
                ToastUtils.showToast(this, "Range Error");
                return false;
            }
            if (Integer.parseInt(majorMin) > Integer.parseInt(majorMax)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
        }
        if (filterMinorEnable) {
            if (TextUtils.isEmpty(minorMin)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            if (Integer.parseInt(minorMin) > 65535) {
                ToastUtils.showToast(this, "Range Error");
                return false;
            }
            if (TextUtils.isEmpty(minorMax)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            if (Integer.parseInt(minorMax) > 65535) {
                ToastUtils.showToast(this, "Range Error");
                return false;
            }
            if (Integer.parseInt(minorMin) > Integer.parseInt(minorMax)) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
        }
        filterRawDatas = new ArrayList<>();
        if (filterRawAdvDataEnable) {
            // 发送设置的过滤RawData
            int count = mBind.llRawDataFilter.getChildCount();
            if (count == 0) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            for (int i = 0; i < count; i++) {
                View v = mBind.llRawDataFilter.getChildAt(i);
                EditText etDataType = v.findViewById(R.id.et_data_type);
                EditText etMin = v.findViewById(R.id.et_min);
                EditText etMax = v.findViewById(R.id.et_max);
                EditText etRawData = v.findViewById(R.id.et_raw_data);
                final String dataTypeStr = etDataType.getText().toString();
                final String minStr = etMin.getText().toString();
                final String maxStr = etMax.getText().toString();
                final String rawDataStr = etRawData.getText().toString();

                if (TextUtils.isEmpty(dataTypeStr)) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                final int dataType = Integer.parseInt(dataTypeStr, 16);
//                final DataTypeEnum dataTypeEnum = DataTypeEnum.fromDataType(dataType);
                if (dataType < 0 || dataType > 0xFF)
                    return false;
                if (TextUtils.isEmpty(rawDataStr)) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                int length = rawDataStr.length();
                if (length % 2 != 0) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                int min = 0;
                if (!TextUtils.isEmpty(minStr))
                    min = Integer.parseInt(minStr);
                int max = 0;
                if (!TextUtils.isEmpty(maxStr))
                    max = Integer.parseInt(maxStr);
                if (min == 0 && max != 0) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                if (min > 29) {
                    ToastUtils.showToast(this, "Range Error");
                    return false;
                }
                if (max > 29) {
                    ToastUtils.showToast(this, "Range Error");
                    return false;
                }
                if (max < min) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                if (min > 0) {
                    int interval = max - min;
                    if (length != ((interval + 1) * 2)) {
                        ToastUtils.showToast(this, "Para Error");
                        return false;
                    }
                }
                FilterCondition.RawDataBean rawDataBean = new FilterCondition.RawDataBean();
                rawDataBean.type = String.format("%02x", dataType);
                rawDataBean.start = min;
                rawDataBean.end = max;
                rawDataBean.data = rawDataStr;
                filterRawDatas.add(rawDataBean);
            }
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int rssi = progress - 127;
        mBind.tvRssiFilterValue.setText(String.format("%ddBm", rssi));
        mBind.tvRssiFilterTips.setText(getString(R.string.rssi_filter, rssi));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
