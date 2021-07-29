package com.moko.mkscannerpro.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.dialog.CustomDialog;
import com.moko.mkscannerpro.dialog.RemoveDialog;
import com.moko.mkscannerpro.dialog.ResetDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.service.MokoService;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.handler.MQTTMessageAssembler;
import com.moko.support.log.LogModule;

import org.eclipse.paho.client.mqttv3.MqttException;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @Date 2019/10/21
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.mkscannerpro.activity.MoreActivity
 */
public class MoreActivity extends BaseActivity {

    public static String TAG = "MoreActivity";

    @BindView(R.id.tv_device_name)
    TextView tvDeviceName;
    private MokoDevice mokoDevice;
    private int publishTopic;
    private MQTTConfig appMqttConfig;
    private MokoService mokoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);
        ButterKnife.bind(this);
        if (getIntent().getExtras() != null) {
            mokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
            tvDeviceName.setText(mokoDevice.nickName);
        }
        String mqttConfigAppStr = SPUtiles.getStringValue(MoreActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        bindService(new Intent(this, MokoService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mokoService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_MQTT_CONNECTION);
            filter.addAction(MokoConstants.ACTION_MQTT_RECEIVE);
            filter.addAction(MokoConstants.ACTION_MQTT_SUBSCRIBE);
            filter.addAction(MokoConstants.ACTION_MQTT_UNSUBSCRIBE);
            filter.addAction(MokoConstants.ACTION_MQTT_PUBLISH);
            filter.addAction(AppConstants.ACTION_DEVICE_STATE);
            registerReceiver(mReceiver, filter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MokoConstants.ACTION_MQTT_CONNECTION.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_CONNECTION_STATE, 0);
            }
            if (MokoConstants.ACTION_MQTT_RECEIVE.equals(action)) {
                String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                byte[] receive = intent.getByteArrayExtra(MokoConstants.EXTRA_MQTT_RECEIVE_MESSAGE);
                int header = receive[0] & 0xFF;
            }
            if (MokoConstants.ACTION_MQTT_PUBLISH.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_STATE, 0);
                if (state == MokoConstants.MQTT_STATE_SUCCESS) {
                    if (publishTopic == 2) {
                        LogModule.i("重置设备成功");
                        if (TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
                            // 取消订阅
                            try {
                                mokoService.unSubscribe(mokoDevice.topicPublish);
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                        }
                        DBTools.getInstance(MoreActivity.this).deleteDevice(mokoDevice);
                        Intent i = new Intent(AppConstants.ACTION_DELETE_DEVICE);
                        i.putExtra(AppConstants.EXTRA_DELETE_DEVICE_ID, mokoDevice.id);
                        MoreActivity.this.sendBroadcast(i);
                        tvDeviceName.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                dismissLoadingProgressDialog();
                                // 跳转首页，刷新数据
                                Intent intent = new Intent(MoreActivity.this, MainActivity.class);
                                intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
                                startActivity(intent);
                            }
                        }, 500);
                    }
                }
            }
            if (MokoConstants.ACTION_MQTT_SUBSCRIBE.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_STATE, 0);
            }
            if (MokoConstants.ACTION_MQTT_UNSUBSCRIBE.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_STATE, 0);
                dismissLoadingProgressDialog();
            }
            if (AppConstants.ACTION_DEVICE_STATE.equals(action)) {
                String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                if (topic.equals(mokoDevice.topicPublish)) {
                    boolean isOnline = intent.getBooleanExtra(MokoConstants.EXTRA_MQTT_RECEIVE_STATE, false);
                    mokoDevice.isOnline = isOnline;
                    if (!isOnline) {
                        finish();
                    }
                }
            }
        }
    };

    public void back(View view) {
        finish();
    }

    private InputFilter filter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source.equals(" ") || source.toString().contentEquals("\n")) return "";
            else return null;
        }
    };

    public void modifyName(View view) {
        View content = LayoutInflater.from(this).inflate(R.layout.modify_name, null);
        final EditText etDeviceName = content.findViewById(R.id.et_device_name);
        String deviceName = tvDeviceName.getText().toString();
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
                            ToastUtils.showToast(MoreActivity.this, R.string.more_modify_name_tips);
                            return;
                        }
                        mokoDevice.nickName = nickName;
                        DBTools.getInstance(MoreActivity.this).updateDevice(mokoDevice);
                        Intent intent = new Intent(AppConstants.ACTION_MODIFY_NAME);
                        MoreActivity.this.sendBroadcast(intent);
                        tvDeviceName.setText(nickName);
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();

        tvDeviceName.postDelayed(new Runnable() {
            @Override
            public void run() {
                showKeyboard(etDeviceName);
            }
        }, 300);
    }

    public void deviceInfo(View view) {
        if (!mokoService.isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }

        Intent i = new Intent(MoreActivity.this, DeviceInfoActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
        startActivity(i);
    }


    public void checkNewFirmware(View view) {
        if (!mokoService.isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent intent = new Intent(this, CheckFirmwareUpdateActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
        startActivity(intent);
    }

    public void removeDevice(View view) {
        RemoveDialog dialog = new RemoveDialog(this);
        dialog.setListener(new RemoveDialog.RemoveListener() {
            @Override
            public void onConfirmClick(RemoveDialog dialog) {
                if (!mokoService.isConnected()) {
                    ToastUtils.showToast(MoreActivity.this, R.string.network_error);
                    return;
                }
                showLoadingProgressDialog(getString(R.string.wait));
                if (TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
                    // 取消订阅
                    try {
                        mokoService.unSubscribe(mokoDevice.topicPublish);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                LogModule.i("删除设备");
                DBTools.getInstance(MoreActivity.this).deleteDevice(mokoDevice);
                Intent i = new Intent(AppConstants.ACTION_DELETE_DEVICE);
                i.putExtra(AppConstants.EXTRA_DELETE_DEVICE_ID, mokoDevice.id);
                MoreActivity.this.sendBroadcast(i);
                tvDeviceName.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismissLoadingProgressDialog();
                        // 跳转首页，刷新数据
                        Intent intent = new Intent(MoreActivity.this, MainActivity.class);
                        intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
                        startActivity(intent);
                    }
                }, 500);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void resetDevice(View view) {
        ResetDialog dialog = new ResetDialog(this);
        dialog.setListener(new ResetDialog.ResetListener() {
            @Override
            public void onConfirmClick(ResetDialog dialog) {
                if (!mokoService.isConnected()) {
                    ToastUtils.showToast(MoreActivity.this, R.string.network_error);
                    return;
                }
                if (!mokoDevice.isOnline) {
                    ToastUtils.showToast(MoreActivity.this, R.string.device_offline);
                    return;
                }
                showLoadingProgressDialog(getString(R.string.wait));
                LogModule.i("重置设备");
                String appTopic;
                if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
                    appTopic = mokoDevice.topicSubscribe;
                } else {
                    appTopic = appMqttConfig.topicPublish;
                }
                publishTopic = 2;
                byte[] message = MQTTMessageAssembler.assembleWriteReset(mokoDevice.uniqueId);
                try {
                    mokoService.publish(appTopic, message, appMqttConfig.qos);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    public void mqttSettingForDevice(View view) {
        Intent i = new Intent(MoreActivity.this, SettingForDeviceActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
        startActivity(i);
    }


    public void ledSetting(View view) {
        Intent i = new Intent(MoreActivity.this, LEDSettingActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
        startActivity(i);
    }

    public void dataReportSetting(View view) {
        Intent i = new Intent(MoreActivity.this, DataReportSettingActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(serviceConnection);
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
