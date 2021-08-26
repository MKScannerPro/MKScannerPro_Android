package com.moko.mkscannerpro.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.dialog.AlertMessageDialog;
import com.moko.mkscannerpro.dialog.CustomDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.entity.MsgConfigResult;
import com.moko.support.entity.MsgDeviceInfo;
import com.moko.support.entity.MsgNotify;
import com.moko.support.entity.ResetResult;
import com.moko.support.event.DeviceDeletedEvent;
import com.moko.support.event.DeviceModifyNameEvent;
import com.moko.support.event.DeviceOnlineEvent;
import com.moko.support.event.MQTTMessageArrivedEvent;
import com.moko.support.handler.MQTTMessageAssembler;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceSettingActivity extends BaseActivity {
    private final String FILTER_ASCII = "[ -~]*";
    public static String TAG = DeviceSettingActivity.class.getSimpleName();
    @BindView(R.id.tv_name)
    TextView tvName;

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    private Handler mHandler;
    private InputFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_setting);
        ButterKnife.bind(this);
        filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };

        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
            tvName.setText(mMokoDevice.nickName);
        }

        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mHandler = new Handler(Looper.getMainLooper());
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
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_REBOOT) {
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
//        if (msg_id == MQTTConstants.CONFIG_MSG_ID_RESET) {
//            Type type = new TypeToken<MsgConfigResult>() {
//            }.getType();
//            MsgConfigResult result = new Gson().fromJson(message, type);
//            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
//                return;
//            }
//            dismissLoadingProgressDialog();
//            mHandler.removeMessages(0);
//            if (result.result_code == 0) {
//                ToastUtils.showToast(this, "Set up succeed");
//            } else {
//                ToastUtils.showToast(this, "Set up failed");
//            }
//        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_RESET_RESULT) {
            Type type = new TypeToken<MsgNotify<ResetResult>>() {
            }.getType();
            MsgNotify<ResetResult> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.data.reset_result == 1) {
                ToastUtils.showToast(this, "Set up succeed");
                XLog.i("重置设备成功");
                if (TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
                    // 取消订阅
                    try {
                        MQTTSupport.getInstance().unSubscribe(mMokoDevice.topicPublish);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                DBTools.getInstance(this).deleteDevice(mMokoDevice);
                EventBus.getDefault().post(new DeviceDeletedEvent(mMokoDevice.id));
                tvName.postDelayed(() -> {
                    dismissLoadingProgressDialog();
                    // 跳转首页，刷新数据
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
                    startActivity(intent);
                }, 500);
            } else {
                ToastUtils.showToast(this, "Set up failed");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        MokoDevice device = DBTools.getInstance(this).selectDevice(mMokoDevice.deviceId);
        mMokoDevice.nickName = device.nickName;
        tvName.setText(mMokoDevice.nickName);
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


    public void onEditName(View view) {
        if (isWindowLocked())
            return;
        View content = LayoutInflater.from(this).inflate(R.layout.modify_name, null);
        final EditText etDeviceName = content.findViewById(R.id.et_device_name);
        String deviceName = etDeviceName.getText().toString();
        etDeviceName.setText(deviceName);
        etDeviceName.setSelection(deviceName.length());
        etDeviceName.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(20)});
        CustomDialog dialog = new CustomDialog.Builder(this)
                .setContentView(content)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nickName = etDeviceName.getText().toString();
                        if (TextUtils.isEmpty(nickName)) {
                            ToastUtils.showToast(DeviceSettingActivity.this, R.string.more_modify_name_tips);
                            return;
                        }
                        mMokoDevice.nickName = nickName;
                        DBTools.getInstance(DeviceSettingActivity.this).updateDevice(mMokoDevice);
                        EventBus.getDefault().post(new DeviceModifyNameEvent(mMokoDevice.deviceId));
                        etDeviceName.setText(nickName);
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();

        etDeviceName.postDelayed(() -> showKeyboard(etDeviceName), 300);
    }

    public void onLEDStatusOption(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(this, LEDSettingActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onDataReportTimeout(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(this, DataReportTimeoutActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onNetworkStatusReportPeriod(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(this, NetworkReportPeriodActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onConnTimeoutOption(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(this, ConnectionTimeoutActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onScanTimeoutOption(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(this, ScanTimeoutActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onSystemTime(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(this, SystemTimeActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onOTA(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent intent = new Intent(this, OTAActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(intent);
    }

    public void onDeviceInfo(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(this, DeviceInfoActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onMQTTSettingForDevice(View view) {
        Intent i = new Intent(this, SettingForDeviceActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onRebootDevice(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reboot Device");
        dialog.setMessage("Please confirm again whether to \n reboot the device");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            if (!mMokoDevice.isOnline) {
                ToastUtils.showToast(this, R.string.device_offline);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            XLog.i("重启设备");
            String appTopic;
            if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
                appTopic = mMokoDevice.topicSubscribe;
            } else {
                appTopic = appMqttConfig.topicPublish;
            }
            MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
            deviceInfo.device_id = mMokoDevice.deviceId;
            deviceInfo.mac = mMokoDevice.mac;
            String message = MQTTMessageAssembler.assembleWriteReboot(deviceInfo);
            try {
                MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_REBOOT, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onResetDevice(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset Device");
        dialog.setMessage("After reset,the device will be removed  from the device list,and relevant data will be totally cleared.");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            if (!mMokoDevice.isOnline) {
                ToastUtils.showToast(this, R.string.device_offline);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            XLog.i("重置设备");
            String appTopic;
            if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
                appTopic = mMokoDevice.topicSubscribe;
            } else {
                appTopic = appMqttConfig.topicPublish;
            }
            MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
            deviceInfo.device_id = mMokoDevice.deviceId;
            deviceInfo.mac = mMokoDevice.mac;
            String message = MQTTMessageAssembler.assembleWriteReset(deviceInfo);
            try {
                MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_RESET, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    //弹出软键盘
    public void showKeyboard(EditText editText) {
        //其中editText为dialog中的输入框的 EditText
        if (editText != null) {
            //设置可获得焦点
            editText.setFocusable(true);
            editText.setFocusableInTouchMode(true);
            //请求获得焦点
            editText.requestFocus();
            //调用系统输入法
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(editText, 0);
        }
    }
}
