package com.moko.mkscannerpro.activity;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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

import butterknife.BindView;
import butterknife.ButterKnife;

public class FilterOptionsAActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {
    private final String FILTER_ASCII = "[ -~]*";
    @BindView(R.id.sb_rssi_filter)
    SeekBar sbRssiFilter;
    @BindView(R.id.tv_rssi_filter_value)
    TextView tvRssiFilterValue;
    @BindView(R.id.tv_rssi_filter_tips)
    TextView tvRssiFilterTips;
    @BindView(R.id.iv_mac_address)
    ImageView ivMacAddress;
    @BindView(R.id.et_mac_address)
    EditText etMacAddress;
    @BindView(R.id.iv_adv_name)
    ImageView ivAdvName;
    @BindView(R.id.et_adv_name)
    EditText etAdvName;
    @BindView(R.id.iv_ibeacon_major)
    ImageView ivIbeaconMajor;
    @BindView(R.id.iv_ibeacon_minor)
    ImageView ivIbeaconMinor;
    @BindView(R.id.iv_raw_adv_data)
    ImageView ivRawAdvData;
    @BindView(R.id.et_ibeacon_major_min)
    EditText etIbeaconMajorMin;
    @BindView(R.id.et_ibeacon_major_max)
    EditText etIbeaconMajorMax;
    @BindView(R.id.ll_ibeacon_major)
    LinearLayout llIbeaconMajor;
    @BindView(R.id.et_ibeacon_minor_min)
    EditText etIbeaconMinorMin;
    @BindView(R.id.et_ibeacon_minor_max)
    EditText etIbeaconMinorMax;
    @BindView(R.id.ll_ibeacon_minor)
    LinearLayout llIbeaconMinor;
    @BindView(R.id.iv_raw_data_del)
    ImageView ivRawDataDel;
    @BindView(R.id.iv_raw_data_add)
    ImageView ivRawDataAdd;
    @BindView(R.id.ll_raw_data_filter)
    LinearLayout llRawDataFilter;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.cb_mac_address)
    CheckBox cbMacAddress;
    @BindView(R.id.cb_adv_name)
    CheckBox cbAdvName;
    @BindView(R.id.iv_ibeacon_uuid)
    ImageView ivIbeaconUuid;
    @BindView(R.id.cb_ibeacon_uuid)
    CheckBox cbIbeaconUuid;
    @BindView(R.id.et_ibeacon_uuid)
    EditText etIbeaconUuid;
    @BindView(R.id.cb_ibeacon_major)
    CheckBox cbIbeaconMajor;
    @BindView(R.id.cb_ibeacon_minor)
    CheckBox cbIbeaconMinor;
    @BindView(R.id.cb_raw_adv_data)
    CheckBox cbRawAdvData;
    @BindView(R.id.tv_condition)
    TextView tvCondition;
    @BindView(R.id.iv_condition)
    ImageView ivCondition;
    @BindView(R.id.tv_condition_tips)
    TextView tvConditionTips;

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;

    private List<FilterCondition.RawDataBean> filterRawDatas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_condition);
        ButterKnife.bind(this);

        tvTitle.setText("Filter Condition A");
        tvCondition.setText("Filter Condition A");
        tvConditionTips.setText(getString(R.string.condition_tips, "A", "A"));

        sbRssiFilter.setOnSeekBarChangeListener(this);
        InputFilter inputFilter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etAdvName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10), inputFilter});
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
        JsonObject object = new Gson().fromJson(message, JsonObject.class);
        JsonElement element = object.get("msg_id");
        int msg_id = element.getAsInt();
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
            ivCondition.setImageResource(filterSwitchEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            final int rssi = result.data.rssi;
            int progress = rssi + 127;
            sbRssiFilter.setProgress(progress);
            tvRssiFilterValue.setText(String.format("%ddBm", rssi));
            tvRssiFilterTips.setText(getString(R.string.rssi_filter, rssi));
            FilterCondition.NameBean nameBean = result.data.name;
            filterNameEnable = nameBean.flag > 0;
            ivAdvName.setImageResource(filterNameEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            etAdvName.setVisibility(filterNameEnable ? View.VISIBLE : View.GONE);
            cbAdvName.setVisibility(filterNameEnable ? View.VISIBLE : View.GONE);
            cbAdvName.setChecked(nameBean.flag > 1);
            etAdvName.setText(nameBean.rule);

            FilterCondition.MacBean macBean = result.data.mac;
            filterMacEnable = macBean.flag > 0;
            ivMacAddress.setImageResource(filterMacEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            etMacAddress.setVisibility(filterMacEnable ? View.VISIBLE : View.GONE);
            cbMacAddress.setVisibility(filterMacEnable ? View.VISIBLE : View.GONE);
            cbMacAddress.setChecked(macBean.flag > 1);
            etMacAddress.setText(macBean.rule);

            FilterCondition.UUIDBean uuidBean = result.data.uuid;
            filterUUIDEnable = uuidBean.flag > 0;
            ivIbeaconUuid.setImageResource(filterUUIDEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            etIbeaconUuid.setVisibility(filterUUIDEnable ? View.VISIBLE : View.GONE);
            cbIbeaconUuid.setVisibility(filterUUIDEnable ? View.VISIBLE : View.GONE);
            cbIbeaconUuid.setChecked(uuidBean.flag > 1);
            etIbeaconUuid.setText(uuidBean.rule);

            FilterCondition.MajorBean majorBean = result.data.major;
            filterMajorEnable = majorBean.flag > 0;
            ivIbeaconMajor.setImageResource(filterMajorEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            llIbeaconMajor.setVisibility(filterMajorEnable ? View.VISIBLE : View.GONE);
            cbIbeaconMajor.setVisibility(filterMajorEnable ? View.VISIBLE : View.GONE);
            cbIbeaconMajor.setChecked(majorBean.flag > 1);
            int majorMin = majorBean.min;
            etIbeaconMajorMin.setText(String.valueOf(majorMin));
            int majorMax = majorBean.max;
            etIbeaconMajorMax.setText(String.valueOf(majorMax));

            FilterCondition.MinorBean minorBean = result.data.minor;
            filterMinorEnable = minorBean.flag > 0;
            ivIbeaconMinor.setImageResource(filterMinorEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            llIbeaconMinor.setVisibility(filterMinorEnable ? View.VISIBLE : View.GONE);
            cbIbeaconMinor.setVisibility(filterMinorEnable ? View.VISIBLE : View.GONE);
            cbIbeaconMinor.setChecked(minorBean.flag > 1);
            int minorMin = minorBean.min;
            etIbeaconMinorMin.setText(String.valueOf(minorMin));
            int minorMax = minorBean.max;
            etIbeaconMinorMax.setText(String.valueOf(minorMax));

            FilterCondition.RawBean rawBean = result.data.raw;
            filterRawAdvDataEnable = rawBean.flag > 0;
            ivRawAdvData.setImageResource(filterRawAdvDataEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            llRawDataFilter.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
            ivRawDataAdd.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
            ivRawDataDel.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
            cbRawAdvData.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
            cbRawAdvData.setChecked(rawBean.flag > 1);
            filterRawDatas = result.data.raw.rule;
            if (filterRawDatas.size() > 0) {
                for (int i = 0, l = filterRawDatas.size(); i < l; i++) {
                    FilterCondition.RawDataBean rawDataBean = filterRawDatas.get(i);
                    View v = LayoutInflater.from(FilterOptionsAActivity.this).inflate(R.layout.item_raw_data_filter, llRawDataFilter, false);
                    EditText etDataType = v.findViewById(R.id.et_data_type);
                    EditText etMin = v.findViewById(R.id.et_min);
                    EditText etMax = v.findViewById(R.id.et_max);
                    EditText etRawData = v.findViewById(R.id.et_raw_data);
                    etDataType.setText(rawDataBean.type);
                    etMin.setText(String.valueOf(rawDataBean.start));
                    etMax.setText(String.valueOf(rawDataBean.end));
                    etRawData.setText(rawDataBean.data);
                    llRawDataFilter.addView(v);
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
        ivMacAddress.setImageResource(filterMacEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        etMacAddress.setVisibility(filterMacEnable ? View.VISIBLE : View.GONE);
        cbMacAddress.setVisibility(filterMacEnable ? View.VISIBLE : View.GONE);
    }

    public void onAdvName(View view) {
        filterNameEnable = !filterNameEnable;
        ivAdvName.setImageResource(filterNameEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        etAdvName.setVisibility(filterNameEnable ? View.VISIBLE : View.GONE);
        cbAdvName.setVisibility(filterNameEnable ? View.VISIBLE : View.GONE);
    }

    public void oniBeaconUUID(View view) {
        filterUUIDEnable = !filterUUIDEnable;
        ivIbeaconUuid.setImageResource(filterUUIDEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        etIbeaconUuid.setVisibility(filterUUIDEnable ? View.VISIBLE : View.GONE);
        cbIbeaconUuid.setVisibility(filterUUIDEnable ? View.VISIBLE : View.GONE);
    }

    public void oniBeaconMajor(View view) {
        filterMajorEnable = !filterMajorEnable;
        ivIbeaconMajor.setImageResource(filterMajorEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        llIbeaconMajor.setVisibility(filterMajorEnable ? View.VISIBLE : View.GONE);
        cbIbeaconMajor.setVisibility(filterMajorEnable ? View.VISIBLE : View.GONE);
    }

    public void oniBeaconMinor(View view) {
        filterMinorEnable = !filterMinorEnable;
        ivIbeaconMinor.setImageResource(filterMinorEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        llIbeaconMinor.setVisibility(filterMinorEnable ? View.VISIBLE : View.GONE);
        cbIbeaconMinor.setVisibility(filterMinorEnable ? View.VISIBLE : View.GONE);
    }

    public void onRawAdvData(View view) {
        filterRawAdvDataEnable = !filterRawAdvDataEnable;
        ivRawAdvData.setImageResource(filterRawAdvDataEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
        llRawDataFilter.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
        ivRawDataAdd.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
        ivRawDataDel.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
        cbRawAdvData.setVisibility(filterRawAdvDataEnable ? View.VISIBLE : View.GONE);
    }

    public void onRawDataAdd(View view) {
        if (isWindowLocked())
            return;
        int count = llRawDataFilter.getChildCount();
        if (count > 4) {
            ToastUtils.showToast(this, "You can set up to 5 filters!");
            return;
        }
        View v = LayoutInflater.from(this).inflate(R.layout.item_raw_data_filter, llRawDataFilter, false);
        llRawDataFilter.addView(v);
    }

    public void onRawDataDel(View view) {
        if (isWindowLocked())
            return;
        final int c = llRawDataFilter.getChildCount();
        if (c == 0) {
            ToastUtils.showToast(this, "There are currently no filters to delete");
            return;
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage("Please confirm whether to delete it, if yes, the last option will be deleted!");
        dialog.setOnAlertConfirmListener(() -> {
            int count = llRawDataFilter.getChildCount();
            if (count > 0) {
                llRawDataFilter.removeViewAt(count - 1);
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onCondition(View view) {
        filterSwitchEnable = !filterSwitchEnable;
        ivCondition.setImageResource(filterSwitchEnable ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
    }


    private void saveParams() {
        final int progress = sbRssiFilter.getProgress();
        int filterRssi = progress - 127;
        final String mac = etMacAddress.getText().toString();
        final String name = etAdvName.getText().toString();
        final String uuid = etIbeaconUuid.getText().toString();
        final String majorMin = etIbeaconMajorMin.getText().toString();
        final String majorMax = etIbeaconMajorMax.getText().toString();
        final String minorMin = etIbeaconMinorMin.getText().toString();
        final String minorMax = etIbeaconMinorMax.getText().toString();
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
        nameBean.flag = filterNameEnable ? (cbAdvName.isChecked() ? 2 : 1) : 0;
        nameBean.rule = filterNameEnable ? name : "";
        filterCondition.name = nameBean;

        FilterCondition.MacBean macBean = new FilterCondition.MacBean();
        macBean.flag = filterMacEnable ? (cbMacAddress.isChecked() ? 2 : 1) : 0;
        macBean.rule = filterMacEnable ? mac : "";
        filterCondition.mac = macBean;

        FilterCondition.UUIDBean uuidBean = new FilterCondition.UUIDBean();
        uuidBean.flag = filterUUIDEnable ? (cbIbeaconUuid.isChecked() ? 2 : 1) : 0;
        uuidBean.rule = filterUUIDEnable ? uuid : "";
        filterCondition.uuid = uuidBean;

        FilterCondition.MajorBean majorBean = new FilterCondition.MajorBean();
        majorBean.flag = filterMajorEnable ? (cbIbeaconMajor.isChecked() ? 2 : 1) : 0;
        majorBean.min = filterMajorEnable ? Integer.parseInt(majorMin) : 0;
        majorBean.max = filterMajorEnable ? Integer.parseInt(majorMax) : 0;
        filterCondition.major = majorBean;

        FilterCondition.MinorBean minorBean = new FilterCondition.MinorBean();
        minorBean.flag = filterMinorEnable ? (cbIbeaconMinor.isChecked() ? 2 : 1) : 0;
        minorBean.min = filterMinorEnable ? Integer.parseInt(minorMin) : 0;
        minorBean.max = filterMinorEnable ? Integer.parseInt(minorMax) : 0;
        filterCondition.minor = minorBean;

        FilterCondition.RawBean rawBean = new FilterCondition.RawBean();
        rawBean.flag = filterRawAdvDataEnable ? (cbRawAdvData.isChecked() ? 2 : 1) : 0;
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
        final String mac = etMacAddress.getText().toString();
        final String name = etAdvName.getText().toString();
        final String uuid = etIbeaconUuid.getText().toString();
        final String majorMin = etIbeaconMajorMin.getText().toString();
        final String majorMax = etIbeaconMajorMax.getText().toString();
        final String minorMin = etIbeaconMinorMin.getText().toString();
        final String minorMax = etIbeaconMinorMax.getText().toString();
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
            int count = llRawDataFilter.getChildCount();
            if (count == 0) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            for (int i = 0; i < count; i++) {
                View v = llRawDataFilter.getChildAt(i);
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
        tvRssiFilterValue.setText(String.format("%ddBm", rssi));
        tvRssiFilterTips.setText(getString(R.string.rssi_filter, rssi));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
