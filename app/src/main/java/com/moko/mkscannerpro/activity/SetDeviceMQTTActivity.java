package com.moko.mkscannerpro.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.adapter.MQTTFragmentAdapter;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.dialog.BottomDialog;
import com.moko.mkscannerpro.dialog.CustomDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.fragment.GeneralDeviceFragment;
import com.moko.mkscannerpro.fragment.SSLDeviceFragment;
import com.moko.mkscannerpro.fragment.UserDeviceFragment;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.MokoSupport;
import com.moko.support.OrderTaskAssembler;
import com.moko.support.entity.MsgNotify;
import com.moko.support.entity.NetworkingStatus;
import com.moko.support.entity.OrderCHAR;
import com.moko.support.entity.ParamsKeyEnum;
import com.moko.support.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import butterknife.BindView;
import butterknife.ButterKnife;

public class SetDeviceMQTTActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {
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
    @BindView(R.id.et_device_id)
    EditText etDeviceId;
    @BindView(R.id.et_ntp_url)
    EditText etNtpUrl;
    @BindView(R.id.tv_time_zone)
    TextView tvTimeZone;
    private GeneralDeviceFragment generalFragment;
    private UserDeviceFragment userFragment;
    private SSLDeviceFragment sslFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;

    private MQTTConfig mqttAppConfig;
    private MQTTConfig mqttDeviceConfig;

    private ArrayList<String> mTimeZones;
    private int mSelectedTimeZone;
    private String mWifiSSID;
    private String mWifiPassword;
    private String mSelectedDeviceName;
    private String mSelectedDeviceMac;
    private boolean savedParamsError;
    private CustomDialog mqttConnDialog;
    private DonutProgress donutProgress;
    private boolean isSettingSuccess;
    private boolean isDeviceConnectSuccess;
    private Handler mHandler;
    private InputFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_device);
        ButterKnife.bind(this);
        String MQTTConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        mqttAppConfig = new Gson().fromJson(MQTTConfigStr, MQTTConfig.class);
        mSelectedDeviceName = getIntent().getStringExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_NAME);
        mSelectedDeviceMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_MAC);
        if (TextUtils.isEmpty(MQTTConfigStr)) {
            mqttDeviceConfig = new MQTTConfig();
        } else {
            Gson gson = new Gson();
            mqttDeviceConfig = gson.fromJson(MQTTConfigStr, MQTTConfig.class);
            mqttDeviceConfig.connectMode = 0;
            mqttDeviceConfig.qos = 1;
            mqttDeviceConfig.keepAlive = 60;
            mqttDeviceConfig.clientId = "";
            mqttDeviceConfig.username = "";
            mqttDeviceConfig.password = "";
            mqttDeviceConfig.caPath = "";
            mqttDeviceConfig.clientKeyPath = "";
            mqttDeviceConfig.clientCertPath = "";
            mqttDeviceConfig.topicPublish = "";
            mqttDeviceConfig.topicSubscribe = "";
        }
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
        etDeviceId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        etNtpUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
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
        mTimeZones = new ArrayList<>();
        for (int i = 0; i <= 24; i++) {
            if (i < 12) {
                mTimeZones.add(String.format("UTC-%02d", 12 - i));
            } else if (i == 12) {
                mTimeZones.add("UTC+00");
            } else {
                mTimeZones.add(String.format("UTC+%02d", i - 12));
            }
        }
        mSelectedTimeZone = 12;
        tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            if (isSettingSuccess) {
                EventBus.getDefault().cancelEventDelivery(event);
                return;
            }
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                finish();
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissLoadingProgressDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_PARAMS:
                    if (value.length >= 4) {
                        int header = value[0] & 0xFF;// 0xED
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header != 0xED)
                            return;
                        ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                        if (configKeyEnum == null) {
                            return;
                        }
                        int length = value[3] & 0xFF;
                        if (flag == 0x01) {
                            // write
                            int result = value[4] & 0xFF;
                            switch (configKeyEnum) {
                                case KEY_MQTT_HOST:
                                case KEY_MQTT_PORT:
                                case KEY_MQTT_CLIENT_ID:
                                case KEY_MQTT_SUBSCRIBE_TOPIC:
                                case KEY_MQTT_PUBLISH_TOPIC:
                                case KEY_MQTT_CLEAN_SESSION:
                                case KEY_MQTT_QOS:
                                case KEY_MQTT_KEEP_ALIVE:
                                case KEY_WIFI_SSID:
                                case KEY_WIFI_PASSWORD:
                                case KEY_MQTT_DEVICE_ID:
                                case KEY_NTP_URL:
                                case KEY_NTP_TIME_ZONE:
                                case KEY_MQTT_CONNECT_MODE:
                                case KEY_MQTT_USERNAME:
                                case KEY_MQTT_PASSWORD:
                                case KEY_MQTT_CA:
                                case KEY_MQTT_CLIENT_KEY:
                                case KEY_MQTT_CLIENT_CERT:
                                    if (result != 1) {
                                        savedParamsError = true;
                                    }
                                    break;
                                case KEY_EXIT_CONFIG_MODE:
                                    if (result != 1) {
                                        savedParamsError = true;
                                    }
                                    if (savedParamsError) {
                                        ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
                                    } else {
                                        isSettingSuccess = true;
                                        showConnMqttDialog();
                                        subscribeTopic();
                                    }
                                    break;
                            }
                        }
                        if (flag == 0x00) {
                            // read
                            switch (configKeyEnum) {
                                case KEY_DEVICE_NAME:
                                    if (length > 0) {
                                        byte[] data = Arrays.copyOfRange(value, 4, 4 + length);
                                        String name = new String(data);
                                        mSelectedDeviceName = name;
                                    }
                                    break;
                                case KEY_DEVICE_MAC:
                                    if (length > 0) {
                                        byte[] data = Arrays.copyOfRange(value, 4, 4 + length);
                                        String mac = MokoUtils.bytesToHexString(data);
                                        mSelectedDeviceMac = mac.toUpperCase();
                                    }
                                    break;
                            }
                        }
                    }
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        final String topic = event.getTopic();
        final String message = event.getMessage();
        if (TextUtils.isEmpty(topic) || isDeviceConnectSuccess) {
            return;
        }
        if (TextUtils.isEmpty(message))
            return;
        Type type = new TypeToken<MsgNotify<NetworkingStatus>>() {
        }.getType();
        MsgNotify<NetworkingStatus> msgNotify = new Gson().fromJson(message, type);
        if (msgNotify.msg_id != MQTTConstants.NOTIFY_MSG_ID_NETWORKING_STATUS)
            return;
        final String deviceId = msgNotify.device_info.device_id;
        if (!mqttDeviceConfig.deviceId.equals(deviceId)) {
            return;
        }
        if (donutProgress == null)
            return;
        if (!isDeviceConnectSuccess) {
            isDeviceConnectSuccess = true;
            donutProgress.setProgress(100);
            donutProgress.setText(100 + "%");
            // 关闭进度条弹框，保存数据，跳转修改设备名称页面
            etMqttHost.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dismissConnMqttDialog();
                    MokoDevice mokoDevice = DBTools.getInstance(SetDeviceMQTTActivity.this).selectDeviceByMac(mSelectedDeviceMac);
                    String mqttConfigStr = new Gson().toJson(mqttDeviceConfig, MQTTConfig.class);
                    if (mokoDevice == null) {
                        mokoDevice = new MokoDevice();
                        mokoDevice.name = mSelectedDeviceName;
                        mokoDevice.nickName = mSelectedDeviceName;
                        mokoDevice.mac = mSelectedDeviceMac;
                        mokoDevice.mqttInfo = mqttConfigStr;
                        mokoDevice.topicSubscribe = mqttDeviceConfig.topicSubscribe;
                        mokoDevice.topicPublish = mqttDeviceConfig.topicPublish;
                        mokoDevice.deviceId = mqttDeviceConfig.deviceId;
                        DBTools.getInstance(SetDeviceMQTTActivity.this).insertDevice(mokoDevice);
                    } else {
                        mokoDevice.name = mSelectedDeviceName;
                        mokoDevice.mac = mSelectedDeviceMac;
                        mokoDevice.mqttInfo = mqttConfigStr;
                        mokoDevice.topicSubscribe = mqttDeviceConfig.topicSubscribe;
                        mokoDevice.topicPublish = mqttDeviceConfig.topicPublish;
                        mokoDevice.deviceId = mqttDeviceConfig.deviceId;
                        DBTools.getInstance(SetDeviceMQTTActivity.this).updateDevice(mokoDevice);
                    }
                    Intent modifyIntent = new Intent(SetDeviceMQTTActivity.this, ModifyNameActivity.class);
                    modifyIntent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
                    startActivity(modifyIntent);
                }
            }, 500);
        }
    }

    private void createFragment() {
        fragments = new ArrayList<>();
        generalFragment = GeneralDeviceFragment.newInstance();
        userFragment = UserDeviceFragment.newInstance();
        sslFragment = SSLDeviceFragment.newInstance();
        fragments.add(generalFragment);
        fragments.add(userFragment);
        fragments.add(sslFragment);
    }

    private void initData() {
        etMqttHost.setText(mqttDeviceConfig.host);
        etMqttPort.setText(mqttDeviceConfig.port);
        etMqttClientId.setText(mqttDeviceConfig.clientId);
        generalFragment.setCleanSession(mqttDeviceConfig.cleanSession);
        generalFragment.setQos(mqttDeviceConfig.qos);
        generalFragment.setKeepAlive(mqttDeviceConfig.keepAlive);
        userFragment.setUserName(mqttDeviceConfig.username);
        userFragment.setPassword(mqttDeviceConfig.password);
        sslFragment.setConnectMode(mqttDeviceConfig.connectMode);
        sslFragment.setCAPath(mqttDeviceConfig.caPath);
        sslFragment.setClientKeyPath(mqttDeviceConfig.clientKeyPath);
        sslFragment.setClientCertPath(mqttDeviceConfig.clientCertPath);
    }

    public void back(View view) {
        back();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        MokoSupport.getInstance().disConnectBle();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
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

    public void onSave(View view) {
        String host = etMqttHost.getText().toString().replaceAll(" ", "");
        String port = etMqttPort.getText().toString();
        String clientId = etMqttClientId.getText().toString().replaceAll(" ", "");
        String deviceId = etDeviceId.getText().toString().replaceAll(" ", "");
        String topicSubscribe = etMqttSubscribeTopic.getText().toString().replaceAll(" ", "");
        String topicPublish = etMqttPublishTopic.getText().toString().replaceAll(" ", "");
        String ntpUrl = etNtpUrl.getText().toString().replaceAll(" ", "");

        if (TextUtils.isEmpty(host)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_host));
            return;
        }
        if (TextUtils.isEmpty(port)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port_empty));
            return;
        }
        if (Integer.parseInt(port) > 65535) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port));
            return;
        }
        if (TextUtils.isEmpty(clientId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_client_id_empty));
            return;
        }
        if (TextUtils.isEmpty(topicSubscribe)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_subscribe));
            return;
        }
        if (TextUtils.isEmpty(topicPublish)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_publish));
            return;
        }
        if (TextUtils.isEmpty(deviceId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_device_id_empty));
            return;
        }
        if (!generalFragment.isValid() || !sslFragment.isValid())
            return;
        mqttDeviceConfig.host = host;
        mqttDeviceConfig.port = port;
        mqttDeviceConfig.clientId = clientId;
        mqttDeviceConfig.cleanSession = generalFragment.isCleanSession();
        mqttDeviceConfig.qos = generalFragment.getQos();
        mqttDeviceConfig.keepAlive = generalFragment.getKeepAlive();
        mqttDeviceConfig.keepAlive = generalFragment.getKeepAlive();
        mqttDeviceConfig.topicSubscribe = topicSubscribe;
        mqttDeviceConfig.topicPublish = topicPublish;
        mqttDeviceConfig.username = userFragment.getUsername();
        mqttDeviceConfig.password = userFragment.getPassword();
        mqttDeviceConfig.connectMode = sslFragment.getConnectMode();
        mqttDeviceConfig.caPath = sslFragment.getCaPath();
        mqttDeviceConfig.clientKeyPath = sslFragment.getClientKeyPath();
        mqttDeviceConfig.clientCertPath = sslFragment.getClientCertPath();
        mqttDeviceConfig.deviceId = deviceId;
        mqttDeviceConfig.ntpUrl = ntpUrl;
        mqttDeviceConfig.timeZone = mSelectedTimeZone - 12;

        if (!mqttDeviceConfig.topicPublish.isEmpty() && !mqttDeviceConfig.topicSubscribe.isEmpty()
                && mqttDeviceConfig.topicPublish.equals(mqttDeviceConfig.topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return;
        }
        if ("{device_name}/{device_id}/app_to_device".equals(mqttDeviceConfig.topicSubscribe)) {
            mqttDeviceConfig.topicSubscribe = String.format("%s/%s/app_to_device", mSelectedDeviceName, deviceId);
        }
        if ("{device_name}/{device_id}/device_to_app".equals(mqttDeviceConfig.topicPublish)) {
            mqttDeviceConfig.topicPublish = String.format("%s/%s/device_to_app", mSelectedDeviceName, deviceId);
        }
        showWifiInputDialog();
    }

    private void showWifiInputDialog() {
        View wifiInputView = LayoutInflater.from(this).inflate(R.layout.wifi_input_content, null);
        final EditText etSSID = wifiInputView.findViewById(R.id.et_ssid);
        final EditText etPassword = wifiInputView.findViewById(R.id.et_password);
        etSSID.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});

        CustomDialog dialog = new CustomDialog.Builder(this)
                .setContentView(wifiInputView)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mWifiSSID = etSSID.getText().toString();
                        // 获取WIFI后，连接成功后发给设备
                        if (TextUtils.isEmpty(mWifiSSID)) {
                            ToastUtils.showToast(SetDeviceMQTTActivity.this, getString(R.string.wifi_verify_empty));
                            return;
                        }
                        dialog.dismiss();
                        mWifiPassword = etPassword.getText().toString();
                        setMQTTDeviceConfig();
                    }
                })
                .create();
        dialog.show();
    }

    private void setMQTTDeviceConfig() {
        try {
            showLoadingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getDeviceMac());
            orderTasks.add(OrderTaskAssembler.getDeviceName());
            orderTasks.add(OrderTaskAssembler.setMqttHost(mqttDeviceConfig.host));
            orderTasks.add(OrderTaskAssembler.setMqttPort(Integer.parseInt(mqttDeviceConfig.port)));
            orderTasks.add(OrderTaskAssembler.setMqttClientId(mqttDeviceConfig.clientId));
            orderTasks.add(OrderTaskAssembler.setMqttCleanSession(mqttDeviceConfig.cleanSession ? 1 : 0));
            orderTasks.add(OrderTaskAssembler.setMqttQos(mqttDeviceConfig.qos));
            orderTasks.add(OrderTaskAssembler.setMqttKeepAlive(mqttDeviceConfig.keepAlive));
            orderTasks.add(OrderTaskAssembler.setWifiSSID(mWifiSSID));
            orderTasks.add(OrderTaskAssembler.setWifiPassword(mWifiPassword));
            orderTasks.add(OrderTaskAssembler.setMqttDeivceId(mqttDeviceConfig.deviceId));
            orderTasks.add(OrderTaskAssembler.setMqttPublishTopic(mqttDeviceConfig.topicPublish));
            orderTasks.add(OrderTaskAssembler.setMqttSubscribeTopic(mqttDeviceConfig.topicSubscribe));
            if (!TextUtils.isEmpty(mqttDeviceConfig.username)) {
                orderTasks.add(OrderTaskAssembler.setMqttUserName(mqttDeviceConfig.username));
            }
            if (!TextUtils.isEmpty(mqttDeviceConfig.password)) {
                orderTasks.add(OrderTaskAssembler.setMqttPassword(mqttDeviceConfig.password));
            }
            orderTasks.add(OrderTaskAssembler.setMqttConnectMode(mqttDeviceConfig.connectMode));
            if (mqttDeviceConfig.connectMode == 2) {
                File file = new File(mqttDeviceConfig.caPath);
                orderTasks.add(OrderTaskAssembler.setCA(file));
            } else if (mqttDeviceConfig.connectMode == 3) {
                File caFile = new File(mqttDeviceConfig.caPath);
                orderTasks.add(OrderTaskAssembler.setCA(caFile));
                File clientKeyFile = new File(mqttDeviceConfig.clientKeyPath);
                orderTasks.add(OrderTaskAssembler.setClientKey(clientKeyFile));
                File clientCertFile = new File(mqttDeviceConfig.clientCertPath);
                orderTasks.add(OrderTaskAssembler.setClientCert(clientCertFile));
            }
            if (!TextUtils.isEmpty(mqttDeviceConfig.ntpUrl)) {
                orderTasks.add(OrderTaskAssembler.setNTPUrl(mqttDeviceConfig.ntpUrl));
            }
            orderTasks.add(OrderTaskAssembler.setNTPTimezone(mqttDeviceConfig.timeZone));
            orderTasks.add(OrderTaskAssembler.exitConfigMode());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } catch (Exception e) {
            ToastUtils.showToast(this, "File is missing");
        }
    }

    public void selectCertificate(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertificate();
    }

    public void selectCAFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCAFile();
    }

    public void selectKeyFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectKeyFile();
    }

    public void selectCertFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertFile();
    }

    public void selectTimeZone(View view) {
        if (isWindowLocked())
            return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mTimeZones, mSelectedTimeZone);
        dialog.setListener(value -> {
            mSelectedTimeZone = value;
            tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
        });
        dialog.show(getSupportFragmentManager());
    }

    private int progress;

    private void showConnMqttDialog() {
        isDeviceConnectSuccess = false;
        View view = LayoutInflater.from(this).inflate(R.layout.mqtt_conn_content, null);
        donutProgress = view.findViewById(R.id.dp_progress);
        mqttConnDialog = new CustomDialog.Builder(this)
                .setContentView(view)
                .create();
        mqttConnDialog.setCancelable(false);
        mqttConnDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                progress = 0;
                while (progress <= 100 && !isDeviceConnectSuccess) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            donutProgress.setProgress(progress);
                            donutProgress.setText(progress + "%");
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    progress++;
                }
            }
        }).start();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isDeviceConnectSuccess) {
                    isDeviceConnectSuccess = true;
                    isSettingSuccess = false;
                    dismissConnMqttDialog();
                    ToastUtils.showToast(SetDeviceMQTTActivity.this, getString(R.string.mqtt_connecting_timeout));
                    finish();
                }
            }
        }, 90 * 1000);
    }

    private void dismissConnMqttDialog() {
        if (mqttConnDialog != null && !isFinishing() && mqttConnDialog.isShowing()) {
            isDeviceConnectSuccess = true;
            isSettingSuccess = false;
            mqttConnDialog.dismiss();
            mHandler.removeMessages(0);
        }
    }

    private void subscribeTopic() {
        // 订阅
        try {
            if (TextUtils.isEmpty(mqttAppConfig.topicSubscribe)) {
                MQTTSupport.getInstance().subscribe(mqttDeviceConfig.topicPublish, mqttAppConfig.qos);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

//    private void syncError() {
//        isDeviceConnectSuccess = true;
//        dismissLoadingProgressDialog();
//        ToastUtils.showToast(this, "Error");
//        MokoSupport.getInstance().disConnectBle();
//    }
}
