package com.moko.mkscannerpro.activity;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.adapter.MQTTFragmentAdapter;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.fragment.GeneralDeviceFragment;
import com.moko.mkscannerpro.fragment.SSLDevicePathFragment;
import com.moko.mkscannerpro.fragment.UserDeviceFragment;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.entity.MQTTReconnect;
import com.moko.support.entity.MQTTSettings;
import com.moko.support.entity.MsgConfigResult;
import com.moko.support.entity.MsgDeviceInfo;
import com.moko.support.event.DeviceOnlineEvent;
import com.moko.support.event.MQTTMessageArrivedEvent;
import com.moko.support.handler.MQTTMessageAssembler;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ModifyMQTTSettingsActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {
    public static String TAG = ModifyMQTTSettingsActivity.class.getSimpleName();
    private final String FILTER_ASCII = "[ -~]*";
    @BindView(R.id.et_mqtt_host)
    EditText etMqttHost;
    @BindView(R.id.et_mqtt_port)
    EditText etMqttPort;
    @BindView(R.id.et_mqtt_client_id)
    EditText etMqttClientId;
    @BindView(R.id.et_mqtt_subscribe_topic)
    EditText etMqttSubscribeTopic;
    @BindView(R.id.et_mqtt_publish_topic)
    EditText etMqttPublishTopic;
    @BindView(R.id.rb_general)
    RadioButton rbGeneral;
    @BindView(R.id.rb_user)
    RadioButton rbUser;
    @BindView(R.id.rb_ssl)
    RadioButton rbSsl;
    @BindView(R.id.vp_mqtt)
    ViewPager2 vpMqtt;
    @BindView(R.id.rg_mqtt)
    RadioGroup rgMqtt;
    @BindView(R.id.et_ssid)
    EditText etSSID;
    @BindView(R.id.et_password)
    EditText etPassword;

    private GeneralDeviceFragment generalFragment;
    private UserDeviceFragment userFragment;
    private SSLDevicePathFragment sslFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private MQTTSettings mMQTTSettings;

    public Handler mHandler;

    private InputFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_device_modify);
        ButterKnife.bind(this);

        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMQTTSettings = new MQTTSettings();
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etMqttHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etMqttClientId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etMqttSubscribeTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etMqttPublishTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etSSID.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        createFragment();
        initData();
        adapter = new MQTTFragmentAdapter(this);
        adapter.setFragmentList(fragments);
        vpMqtt.setAdapter(adapter);
        vpMqtt.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    rbGeneral.setChecked(true);
                } else if (position == 1) {
                    rbUser.setChecked(true);
                } else if (position == 2) {
                    rbSsl.setChecked(true);
                }
            }
        });
        vpMqtt.setOffscreenPageLimit(3);
        rgMqtt.setOnCheckedChangeListener(this);
        mHandler = new Handler(Looper.getMainLooper());
    }

    private void createFragment() {
        fragments = new ArrayList<>();
        generalFragment = GeneralDeviceFragment.newInstance();
        userFragment = UserDeviceFragment.newInstance();
        sslFragment = SSLDevicePathFragment.newInstance();
        fragments.add(generalFragment);
        fragments.add(userFragment);
        fragments.add(sslFragment);
    }

    private void initData() {
        generalFragment.setCleanSession(mMQTTSettings.clean_session == 1);
        generalFragment.setQos(mMQTTSettings.qos);
        generalFragment.setKeepAlive(mMQTTSettings.keep_alive);
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
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_MQTT_SETTINGS) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
            if (result.result_code == 0) {
                setDeviceReconnect();
            } else {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                ToastUtils.showToast(this, "Set up failed");
            }
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_MQTT_RECONNECT) {
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
            MQTTConfig mqttConfig = new Gson().fromJson(mMokoDevice.mqttInfo, MQTTConfig.class);
            mqttConfig.topicPublish = mMQTTSettings.publish_topic;
            mqttConfig.topicSubscribe = mMQTTSettings.subscribe_topic;
            mMokoDevice.topicPublish = mMQTTSettings.publish_topic;
            mMokoDevice.topicSubscribe = mMQTTSettings.subscribe_topic;
            mMokoDevice.mqttInfo = new Gson().toJson(mqttConfig, MQTTConfig.class);
            DBTools.getInstance(this).updateDevice(mMokoDevice);
            // 跳转首页，刷新数据
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_ID, mMokoDevice.deviceId);
            startActivity(intent);
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


    public void onSelectCertificate(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertificate();
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

        final String host = etMqttHost.getText().toString().trim();
        final String port = etMqttPort.getText().toString().trim();
        final String clientId = etMqttClientId.getText().toString().trim();
        String topicSubscribe = etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = etMqttPublishTopic.getText().toString().trim();
        final String wifiSSID = etSSID.getText().toString().trim();
        final String wifiPassword = etPassword.getText().toString().trim();

        mMQTTSettings.mqtt_host = host;
        mMQTTSettings.mqtt_port = Integer.parseInt(port);
        mMQTTSettings.client_id = clientId;
        if ("{device_name}/{device_id}/app_to_device".equals(topicSubscribe)) {
            topicSubscribe = String.format("%s/%s/app_to_device", mMokoDevice.name, mMokoDevice.deviceId);
        }
        if ("{device_name}/{device_id}/device_to_app".equals(topicPublish)) {
            topicPublish = String.format("%s/%s/device_to_app", mMokoDevice.name, mMokoDevice.deviceId);
        }
        mMQTTSettings.subscribe_topic = topicSubscribe;
        mMQTTSettings.publish_topic = topicPublish;
        mMQTTSettings.wifi_ssid = wifiSSID;
        mMQTTSettings.wifi_passwd = wifiPassword;
        mMQTTSettings.clean_session = generalFragment.isCleanSession() ? 1 : 0;
        mMQTTSettings.qos = generalFragment.getQos();
        mMQTTSettings.keep_alive = generalFragment.getKeepAlive();
        mMQTTSettings.mqtt_username = userFragment.getUsername();
        mMQTTSettings.mqtt_passwd = userFragment.getPassword();
        mMQTTSettings.connect_mode = sslFragment.getConnectMode();
        if (mMQTTSettings.connect_mode > 1) {
            mMQTTSettings.ssl_host = sslFragment.getSSLHost();
            mMQTTSettings.ssl_port = sslFragment.getSSLPort();
        }
        if (mMQTTSettings.connect_mode == 2) {
            mMQTTSettings.ca_way = sslFragment.getCAPath();
        }
        if (mMQTTSettings.connect_mode == 3) {
            mMQTTSettings.ca_way = sslFragment.getCAPath();
            mMQTTSettings.client_cer_way = sslFragment.getClientCerPath();
            mMQTTSettings.client_key_way = sslFragment.getClientKeyPath();
        }

        String message = MQTTMessageAssembler.assembleWriteMQTTSettings(deviceInfo, mMQTTSettings);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_MQTT_SETTINGS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void setDeviceReconnect() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;

        MQTTReconnect reconnect = new MQTTReconnect();
        reconnect.reconnect = 1;
        String message = MQTTMessageAssembler.assembleWriteDeviceReconnect(deviceInfo, reconnect);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_MQTT_RECONNECT, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private boolean isValid() {
        String host = etMqttHost.getText().toString().trim();
        String port = etMqttPort.getText().toString().trim();
        String clientId = etMqttClientId.getText().toString().trim();
        String topicSubscribe = etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = etMqttPublishTopic.getText().toString().trim();
        String ssid = etSSID.getText().toString().trim();

        if (TextUtils.isEmpty(host)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_host));
            return false;
        }
        if (TextUtils.isEmpty(port)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port_empty));
            return false;
        }
        if (Integer.parseInt(port) > 65535) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port));
            return false;
        }
        if (TextUtils.isEmpty(clientId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_client_id_empty));
            return false;
        }
        if (TextUtils.isEmpty(topicSubscribe)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_subscribe));
            return false;
        }
        if (TextUtils.isEmpty(topicPublish)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_publish));
            return false;
        }
        if (topicPublish.equals(topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return false;
        }
        if (TextUtils.isEmpty(ssid)) {
            ToastUtils.showToast(this, "SSID error");
            return false;
        }
        if (!generalFragment.isValid() || !sslFragment.isValid())
            return false;
        return true;
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        switch (checkedId) {
            case R.id.rb_general:
                vpMqtt.setCurrentItem(0);
                break;
            case R.id.rb_user:
                vpMqtt.setCurrentItem(1);
                break;
            case R.id.rb_ssl:
                vpMqtt.setCurrentItem(2);
                break;
        }
    }
}
