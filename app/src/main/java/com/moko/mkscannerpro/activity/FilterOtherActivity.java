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
import com.moko.mkscannerpro.dialog.BottomDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.entity.DataTypeEnum;
import com.moko.support.entity.FilterCondition;
import com.moko.support.entity.FilterOther;
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

import androidx.constraintlayout.widget.ConstraintLayout;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FilterOtherActivity extends BaseActivity {


    @BindView(R.id.cb_other)
    CheckBox cbOther;
    @BindView(R.id.ll_filter_condition)
    LinearLayout llFilterCondition;
    @BindView(R.id.tv_other_relationship)
    TextView tvOtherRelationship;
    @BindView(R.id.cl_other_relationship)
    ConstraintLayout clOtherRelationship;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;

    private List<FilterCondition.RawDataBean> filterOther;

    private ArrayList<String> mValues;
    private int mSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_other);
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
        getFilterOther();
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
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_OTHER) {
            Type type = new TypeToken<MsgReadResult<FilterOther>>() {
            }.getType();
            MsgReadResult<FilterOther> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            cbOther.setChecked(result.data.onOff == 1);
            if (result.data.array_num > 0) {
                clOtherRelationship.setVisibility(View.VISIBLE);
                filterOther = result.data.rule;
                if (filterOther.size() > 0) {
                    for (int i = 0, l = filterOther.size(); i < l; i++) {
                        FilterCondition.RawDataBean rawDataBean = filterOther.get(i);
                        View v = LayoutInflater.from(FilterOtherActivity.this).inflate(R.layout.item_other_filter, llFilterCondition, false);
                        TextView tvCondition = v.findViewById(R.id.tv_condition);
                        EditText etDataType = v.findViewById(R.id.et_data_type);
                        EditText etMin = v.findViewById(R.id.et_min);
                        EditText etMax = v.findViewById(R.id.et_max);
                        EditText etRawData = v.findViewById(R.id.et_raw_data);
                        if (i == 0) {
                            tvCondition.setText("Condition A");
                        } else if (i == 1) {
                            tvCondition.setText("Condition B");
                        } else {
                            tvCondition.setText("Condition C");
                        }
                        etDataType.setText(rawDataBean.type);
                        etMin.setText(String.valueOf(rawDataBean.start));
                        etMax.setText(String.valueOf(rawDataBean.end));
                        etRawData.setText(rawDataBean.data);
                        llFilterCondition.addView(v);
                    }
                }
                if (result.data.relationship < 1) {
                    mValues = new ArrayList<>();
                    mValues.add("A");
                    mSelected = 0;
                } else if (result.data.relationship < 3) {
                    mValues = new ArrayList<>();
                    mValues.add("A & B");
                    mValues.add("A | B");
                    mSelected = result.data.relationship - 1;
                } else if (result.data.relationship < 6) {
                    mValues = new ArrayList<>();
                    mValues.add("A & B & C");
                    mValues.add("(A & B) | C");
                    mValues.add("A | B | C");
                    mSelected = result.data.relationship - 3;
                }
                tvOtherRelationship.setText(mValues.get(mSelected));
            } else {
                filterOther = new ArrayList<>();
                clOtherRelationship.setVisibility(View.GONE);
            }

        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_OTHER) {
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

    private void getFilterOther() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadFilterOther(deviceInfo);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_FILTER_OTHER, appMqttConfig.qos);
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
        int count = llFilterCondition.getChildCount();
        if (count > 2) {
            ToastUtils.showToast(this, "You can set up to 3 filters!");
            return;
        }
        View v = LayoutInflater.from(this).inflate(R.layout.item_other_filter, llFilterCondition, false);
        TextView tvCondition = v.findViewById(R.id.tv_condition);
        if (count == 0) {
            tvCondition.setText("Condition A");
        } else if (count == 1) {
            tvCondition.setText("Condition B");
        } else {
            tvCondition.setText("Condition C");
        }
        llFilterCondition.addView(v);
        clOtherRelationship.setVisibility(View.VISIBLE);
        mValues = new ArrayList<>();
        if (count == 0) {
            mValues.add("A");
            mSelected = 0;
        }
        if (count == 1) {
            mValues.add("A & B");
            mValues.add("A | B");
            mSelected = 1;
        }
        if (count == 2) {
            mValues.add("A & B & C");
            mValues.add("(A & B) | C");
            mValues.add("A | B | C");
            mSelected = 2;
        }
        tvOtherRelationship.setText(mValues.get(mSelected));
    }

    public void onDel(View view) {
        if (isWindowLocked())
            return;
        final int c = llFilterCondition.getChildCount();
        if (c == 0) {
            ToastUtils.showToast(this, "There are currently no filters to delete");
            return;
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage("Please confirm whether to delete it, if yes, the last option will be deleted!");
        dialog.setOnAlertConfirmListener(() -> {
            int count = llFilterCondition.getChildCount();
            if (count > 0) {
                llFilterCondition.removeViewAt(count - 1);
                mValues = new ArrayList<>();
                if (count == 1) {
                    clOtherRelationship.setVisibility(View.GONE);
                    return;
                }
                if (count == 2) {
                    mValues.add("A");
                    mSelected = 0;
                }
                if (count == 3) {
                    mValues.add("A & B");
                    mValues.add("A | B");
                    mSelected = 1;
                }
                tvOtherRelationship.setText(mValues.get(mSelected));
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

        FilterOther other = new FilterOther();
        other.onOff = cbOther.isChecked() ? 1 : 0;
        other.array_num = this.filterOther.size();
        if (other.array_num == 1) {
            other.relationship = 0;
        }
        if (other.array_num == 2) {
            other.relationship = mSelected + 1;
        }
        if (other.array_num == 3) {
            other.relationship = mSelected + 3;
        }
        other.rule = filterOther;

        String message = MQTTMessageAssembler.assembleWriteFilterOther(deviceInfo, other);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_FILTER_OTHER, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        final int count = llFilterCondition.getChildCount();
        if (count > 0) {
            // 发送设置的过滤RawData
            filterOther.clear();
            for (int i = 0; i < count; i++) {
                View v = llFilterCondition.getChildAt(i);
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
                final int dataType = Integer.parseInt(dataTypeStr, 16);final DataTypeEnum dataTypeEnum = DataTypeEnum.fromDataType(dataType);
                if (dataTypeEnum == null)
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
                filterOther.add(rawDataBean);
            }
        } else {
            filterOther = new ArrayList<>();
        }
        return true;
    }

    public void onOtherRelationship(View view) {
        if (isWindowLocked())
            return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mValues, mSelected);
        dialog.setListener(value -> {
            mSelected = value;
            tvOtherRelationship.setText(mValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }
}
