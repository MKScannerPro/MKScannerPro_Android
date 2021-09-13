package com.moko.mkscannerpro.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.entity.ConfigInfo;
import com.moko.support.entity.MsgDeviceInfo;
import com.moko.support.entity.MsgReadResult;
import com.moko.support.event.DeviceOnlineEvent;
import com.moko.support.event.MQTTMessageArrivedEvent;
import com.moko.support.handler.MQTTMessageAssembler;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SettingForDeviceActivity extends BaseActivity {

    public static String TAG = SettingForDeviceActivity.class.getSimpleName();
    @BindView(R.id.tv_type)
    TextView tvType;
    @BindView(R.id.tv_host)
    TextView tvHost;
    @BindView(R.id.tv_port)
    TextView tvPort;
    @BindView(R.id.tv_client_id)
    TextView tvClientId;
    @BindView(R.id.tv_user_name)
    TextView tvUserName;
    @BindView(R.id.tv_password)
    TextView tvPassword;
    @BindView(R.id.tv_clean_session)
    TextView tvCleanSession;
    @BindView(R.id.tv_qos)
    TextView tvQos;
    @BindView(R.id.tv_keep_alive)
    TextView tvKeepAlive;
    @BindView(R.id.tv_device_id)
    TextView tvDeviceId;
    @BindView(R.id.tv_subscribe_topic)
    TextView tvSubscribeTopic;
    @BindView(R.id.tv_publish_topic)
    TextView tvPublishTopic;

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_for_device);
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
        getSettingForDevice();
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
        if (msg_id == MQTTConstants.READ_MSG_ID_CONFIG_INFO) {
            Type type = new TypeToken<MsgReadResult<ConfigInfo>>() {
            }.getType();
            MsgReadResult<ConfigInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            tvHost.setText(result.data.host);
            tvPort.setText(String.valueOf(result.data.port));
            tvCleanSession.setText(result.data.clean_session == 0 ? "NO" : "YES");
            tvUserName.setText(result.data.username);
            tvPassword.setText(result.data.password);
            tvQos.setText(String.valueOf(result.data.qos));
            tvKeepAlive.setText(String.valueOf(result.data.keep_alive));
            tvClientId.setText(result.data.client_id);
            tvDeviceId.setText(result.data.device_id);

            if (result.data.connect_type == 0) {
                tvType.setText(getString(R.string.mqtt_connct_mode_tcp));
            } else {
                tvType.setText("SSL");
            }
            tvSubscribeTopic.setText(result.data.subscribe_topic);
            tvPublishTopic.setText(result.data.publish_topic);
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


    private void getSettingForDevice() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadDeviceConfigInfo(deviceInfo);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_CONFIG_INFO, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
