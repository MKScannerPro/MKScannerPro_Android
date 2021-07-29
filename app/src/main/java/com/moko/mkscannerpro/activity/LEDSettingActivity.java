package com.moko.mkscannerpro.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;

import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.service.MokoService;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.handler.BaseMessageHandler;
import com.moko.support.handler.MQTTMessageAssembler;
import com.moko.support.utils.MokoUtils;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LEDSettingActivity extends BaseActivity {

    @BindView(R.id.cb_ble_broadcast)
    CheckBox cbBleBroadcast;
    @BindView(R.id.cb_ble_connected)
    CheckBox cbBleConnected;
    @BindView(R.id.cb_server_connecting)
    CheckBox cbServerConnecting;
    @BindView(R.id.cb_server_connected)
    CheckBox cbServerConnected;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private MokoService mokoService;

    private int bleBroadcastEnable;
    private int bleConnectedEnable;
    private int serverConnectingEnable;
    private int serverConnectedEnable;


    private int mPublishType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_setting);
        ButterKnife.bind(this);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);

        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mHandler = new MessageHandler(this);
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
            filter.addAction(MokoConstants.ACTION_MQTT_PUBLISH);
            filter.addAction(AppConstants.ACTION_MODIFY_NAME);
            filter.addAction(AppConstants.ACTION_DEVICE_STATE);
            registerReceiver(mReceiver, filter);
            showLoadingProgressDialog(getString(R.string.wait));
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dismissLoadingProgressDialog();
                    LEDSettingActivity.this.finish();
                }
            }, 30 * 1000);
            getLEDStatus();
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
            if (MokoConstants.ACTION_MQTT_PUBLISH.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_STATE, 0);
                if (state == MokoConstants.MQTT_STATE_SUCCESS) {
                    if (mPublishType > 0) {
                        dismissLoadingProgressDialog();
                        ToastUtils.showToast(LEDSettingActivity.this, "Succeed");
                        mHandler.removeMessages(0);
                    }
                }
            }
            if (MokoConstants.ACTION_MQTT_RECEIVE.equals(action)) {
                final String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                byte[] receive = intent.getByteArrayExtra(MokoConstants.EXTRA_MQTT_RECEIVE_MESSAGE);
                int header = receive[0] & 0xFF;
                if (header == 0x1b)// LED
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    if (mMokoDevice.uniqueId.equals(new String(id))) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                        byte[] dataLength = Arrays.copyOfRange(receive, 2 + length, 4 + length);
                        if (MokoUtils.toInt(dataLength) == 4) {
                            int allLength = receive.length;
                            bleConnectedEnable = receive[allLength - 1];
                            bleBroadcastEnable = receive[allLength - 2];
                            serverConnectedEnable = receive[allLength - 3];
                            serverConnectingEnable = receive[allLength - 4];
                            if (bleConnectedEnable == 1) {
                                cbBleConnected.setChecked(true);
                            }
                            if (bleBroadcastEnable == 1) {
                                cbBleBroadcast.setChecked(true);
                            }
                            if (serverConnectingEnable == 1) {
                                cbServerConnecting.setChecked(true);
                            }
                            if (serverConnectedEnable == 1) {
                                cbServerConnected.setChecked(true);
                            }
                        }
                    }
                }
            }
            if (AppConstants.ACTION_MODIFY_NAME.equals(action)) {
                MokoDevice device = DBTools.getInstance(LEDSettingActivity.this).selectDevice(mMokoDevice.uniqueId);
                mMokoDevice.nickName = device.nickName;
            }
            if (AppConstants.ACTION_DEVICE_STATE.equals(action)) {
                String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                if (topic.equals(mMokoDevice.topicPublish)) {
                    boolean isOnline = intent.getBooleanExtra(MokoConstants.EXTRA_MQTT_RECEIVE_STATE, false);
                    mMokoDevice.isOnline = isOnline;
                    if (!isOnline) {
                        finish();
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(serviceConnection);
    }

    public void back(View view) {
        finish();
    }

    @OnClick({R.id.tv_confirm})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_confirm:
                if (!mokoService.isConnected()) {
                    ToastUtils.showToast(this, R.string.network_error);
                    return;
                }
                if (!mMokoDevice.isOnline) {
                    ToastUtils.showToast(this, R.string.device_offline);
                    return;
                }
                bleBroadcastEnable = cbBleBroadcast.isChecked() ? 1 : 0;
                bleConnectedEnable = cbBleConnected.isChecked() ? 1 : 0;
                serverConnectingEnable = cbServerConnecting.isChecked() ? 1 : 0;
                serverConnectedEnable = cbServerConnected.isChecked() ? 1 : 0;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismissLoadingProgressDialog();
                        LEDSettingActivity.this.finish();
                    }
                }, 30 * 1000);
                showLoadingProgressDialog(getString(R.string.wait));
                setLEDStatus();
                break;
        }
    }

    private void setLEDStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = 1;
        byte[] message = MQTTMessageAssembler.assembleWriteLEDStatus(mMokoDevice.uniqueId,
                bleBroadcastEnable, bleConnectedEnable, serverConnectingEnable, serverConnectedEnable);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void getLEDStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadLEDStatus(mMokoDevice.uniqueId);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public MessageHandler mHandler;

    public class MessageHandler extends BaseMessageHandler<LEDSettingActivity> {

        public MessageHandler(LEDSettingActivity activity) {
            super(activity);
        }

        @Override
        protected void handleMessage(LEDSettingActivity activity, Message msg) {
        }
    }
}
