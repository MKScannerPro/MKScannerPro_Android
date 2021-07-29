package com.moko.mkscannerpro.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.dialog.UpdateTypeDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.service.MokoService;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.handler.MQTTMessageAssembler;
import com.moko.support.log.LogModule;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @Date 2019/10/30
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.mkscannerpro.activity.CheckFirmwareUpdateActivity
 */
public class CheckFirmwareUpdateActivity extends BaseActivity {

    public static String TAG = "CheckFirmwareUpdateActivity";
    @BindView(R.id.et_host_content)
    EditText etHostContent;
    @BindView(R.id.et_host_port)
    EditText etHostPort;
    @BindView(R.id.et_host_catalogue)
    EditText etHostCatalogue;
    @BindView(R.id.tv_update_type)
    TextView tvUpdateType;


    private MokoDevice mokoDevice;
    private MQTTConfig appMqttConfig;

    private String[] mUpdateType;

    private MokoService mokoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_firmware);
        ButterKnife.bind(this);
        if (getIntent().getExtras() != null) {
            mokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        mUpdateType = getResources().getStringArray(R.array.update_type);
        tvUpdateType.setText(mUpdateType[0]);

        String mqttConfigAppStr = SPUtiles.getStringValue(CheckFirmwareUpdateActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
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
                if (header == 0x22)// 升级结果
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    if (mokoDevice.uniqueId.equals(new String(id))) {
                        dismissLoadingProgressDialog();
                        if (receive[receive.length - 1] == 0) {
                            ToastUtils.showToast(CheckFirmwareUpdateActivity.this, R.string.update_failed);
                        } else {
                            ToastUtils.showToast(CheckFirmwareUpdateActivity.this, R.string.update_success);
                        }
                    }
                }
            }
            if (MokoConstants.ACTION_MQTT_SUBSCRIBE.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_STATE, 0);
            }
            if (MokoConstants.ACTION_MQTT_UNSUBSCRIBE.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_STATE, 0);
            }
            if (AppConstants.ACTION_DEVICE_STATE.equals(action)) {
                String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                if (topic.equals(mokoDevice.topicPublish)) {
                    boolean isOnline = intent.getBooleanExtra(MokoConstants.EXTRA_MQTT_RECEIVE_STATE, false);
                    mokoDevice.isOnline = isOnline;
                }
            }
        }
    };

    public void back(View view) {
        finish();
    }

    public void startUpdate(View view) {
        if (!mokoService.isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        String hostStr = etHostContent.getText().toString();
        String portStr = etHostPort.getText().toString();
        String catalogueStr = etHostCatalogue.getText().toString();
        if (TextUtils.isEmpty(hostStr)) {
            ToastUtils.showToast(this, R.string.mqtt_verify_host);
            return;
        }
        if (!TextUtils.isEmpty(portStr) && Integer.parseInt(portStr) > 65535) {
            ToastUtils.showToast(this, R.string.mqtt_verify_port_empty);
            return;
        }
        if (TextUtils.isEmpty(catalogueStr)) {
            ToastUtils.showToast(this, R.string.mqtt_verify_catalogue);
            return;
        }
        LogModule.i("升级固件");
        showLoadingProgressDialog(getString(R.string.wait));
        setOTAType();
        setHostAndPort(hostStr, Integer.parseInt(portStr));
        setCatalogue(catalogueStr);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(serviceConnection);
    }

    private int mSelected;

    public void updateType(View view) {
        UpdateTypeDialog dialog = new UpdateTypeDialog();
        dialog.setSelected(mSelected);
        dialog.setListener(new UpdateTypeDialog.OnDataSelectedListener() {
            @Override
            public void onDataSelected(int selected) {
                mSelected = selected;
                tvUpdateType.setText(mUpdateType[mSelected]);
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    private void setOTAType() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteOTAType(mokoDevice.uniqueId, mSelected + 1);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setHostAndPort(String host, int port) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteHostAndPort(mokoDevice.uniqueId, host, port);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setCatalogue(String catalogue) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteCatalogue(mokoDevice.uniqueId, catalogue);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
