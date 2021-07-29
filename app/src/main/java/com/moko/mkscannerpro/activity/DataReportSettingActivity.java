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
import android.widget.EditText;

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

public class DataReportSettingActivity extends BaseActivity {


    @BindView(R.id.et_report_interval)
    EditText etReportInterval;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private MokoService mokoService;

    private int mPublishType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_report_setting);
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
                    DataReportSettingActivity.this.finish();
                }
            }, 30 * 1000);
            getDataReportInterval();
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
                        ToastUtils.showToast(DataReportSettingActivity.this, "Succeed");
                        mHandler.removeMessages(0);
                    }
                }
            }
            if (MokoConstants.ACTION_MQTT_RECEIVE.equals(action)) {
                final String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                byte[] receive = intent.getByteArrayExtra(MokoConstants.EXTRA_MQTT_RECEIVE_MESSAGE);
                int header = receive[0] & 0xFF;
                if (header == 0x1e)// LED
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    if (mMokoDevice.uniqueId.equals(new String(id))) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                        byte[] dataLength = Arrays.copyOfRange(receive, 2 + length, 4 + length);
                        if (MokoUtils.toInt(dataLength) == 1) {
                            int allLength = receive.length;
                            String intervalStr = String.valueOf(receive[allLength - 1] & 0xFF);
                            etReportInterval.setText(intervalStr);
                            etReportInterval.setSelection(intervalStr.length());
                        }
                    }
                }
            }
            if (AppConstants.ACTION_MODIFY_NAME.equals(action)) {
                MokoDevice device = DBTools.getInstance(DataReportSettingActivity.this).selectDevice(mMokoDevice.uniqueId);
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
                String intervalStr = etReportInterval.getText().toString();
                if (!mokoService.isConnected()) {
                    ToastUtils.showToast(this, R.string.network_error);
                    return;
                }
                if (!mMokoDevice.isOnline) {
                    ToastUtils.showToast(this, R.string.device_offline);
                    return;
                }
                if (TextUtils.isEmpty(intervalStr)) {
                    ToastUtils.showToast(this, "Para Error");
                    return;
                }
                int interval = Integer.parseInt(intervalStr);
                if (interval < 0 || interval > 60) {
                    ToastUtils.showToast(this, "Para Error");
                    return;
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismissLoadingProgressDialog();
                        DataReportSettingActivity.this.finish();
                    }
                }, 30 * 1000);
                showLoadingProgressDialog(getString(R.string.wait));
                setInterval(interval);
                break;
        }
    }

    private void setInterval(int interval) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = 1;
        byte[] message = MQTTMessageAssembler.assembleWriteDataReportInterval(mMokoDevice.uniqueId, interval);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void getDataReportInterval() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadDataReportInterval(mMokoDevice.uniqueId);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public MessageHandler mHandler;

    public class MessageHandler extends BaseMessageHandler<DataReportSettingActivity> {

        public MessageHandler(DataReportSettingActivity activity) {
            super(activity);
        }

        @Override
        protected void handleMessage(DataReportSettingActivity activity, Message msg) {
        }
    }
}
