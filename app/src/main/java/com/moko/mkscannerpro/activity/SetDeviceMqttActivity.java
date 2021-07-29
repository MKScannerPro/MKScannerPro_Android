package com.moko.mkscannerpro.activity;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.dialog.CustomDialog;
import com.moko.mkscannerpro.dialog.KeepAliveDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.fragment.OnewaySSLFragment;
import com.moko.mkscannerpro.fragment.TwowaySSLFragment;
import com.moko.mkscannerpro.service.MokoBlueService;
import com.moko.mkscannerpro.service.MokoService;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderTaskResponse;
import com.moko.support.event.ConnectStatusEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @Date 2018/6/7
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.mkscannerpro.activity.SetDeviceMqttActivity
 */
public class SetDeviceMqttActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {


    @BindView(R.id.et_mqtt_host)
    EditText etMqttHost;
    @BindView(R.id.et_mqtt_port)
    EditText etMqttPort;
    @BindView(R.id.iv_clean_session)
    ImageView ivCleanSession;
    @BindView(R.id.et_mqtt_username)
    EditText etMqttUsername;
    @BindView(R.id.et_mqtt_password)
    EditText etMqttPassword;
    @BindView(R.id.tv_qos)
    TextView tvQos;
    @BindView(R.id.tv_keep_alive)
    TextView tvKeepAlive;
    @BindView(R.id.et_mqtt_client_id)
    EditText etMqttClientId;
    @BindView(R.id.et_mqtt_device_id)
    EditText etMqttDeviceId;
    @BindView(R.id.rb_conn_mode_tcp)
    RadioButton rbConnModeTcp;
    @BindView(R.id.rb_conn_mode_ssl_oneway)
    RadioButton rbConnModeSslOneway;
    @BindView(R.id.rb_conn_mode_ssl_twoway)
    RadioButton rbConnModeSslTwoway;
    @BindView(R.id.rg_conn_mode)
    RadioGroup rgConnMode;
    @BindView(R.id.frame_connect_mode)
    FrameLayout frameConnectMode;
    @BindView(R.id.rl_client_id)
    RelativeLayout rlClientId;
    @BindView(R.id.tv_connect_mode)
    TextView tvConnectMode;
    @BindView(R.id.et_topic_subscribe)
    EditText etTopicSubscribe;
    @BindView(R.id.et_topic_publish)
    EditText etTopicPublish;

    private FragmentManager fragmentManager;
    private OnewaySSLFragment onewaySSLFragment;
    private TwowaySSLFragment twowaySSLFragment;


    private String[] mQosArray = new String[]{"0", "1", "2"};


    private MQTTConfig mqttConfig;

    private String mSelectedDeviceMac;
    private String mSelectedDeviceName;
    private MokoBlueService mMokoService;
    private boolean mReceiverTag = false;


    private CustomDialog wifiAlertDialog;
    private CustomDialog mqttConnDialog;
    private DonutProgress donutProgress;
    private String mWifiSSID;
    private String mWifiPassword;
    private boolean isSettingSuccess;
    private boolean isDeviceConnectSuccess;
    private MQTTConfig mAppMqttConfig;
    private File mFile;
    private MokoService mokoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_device);
        ButterKnife.bind(this);
        String mqttConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        mSelectedDeviceMac = getIntent().getStringExtra(MokoConstants.EXTRA_KEY_SELECTED_DEVICE_MAC);
        mSelectedDeviceName = getIntent().getStringExtra(MokoConstants.EXTRA_KEY_SELECTED_DEVICE_NAME);
        mAppMqttConfig = new Gson().fromJson(mqttConfigStr, MQTTConfig.class);
        if (TextUtils.isEmpty(mqttConfigStr)) {
            mqttConfig = new MQTTConfig();
        } else {
            Gson gson = new Gson();
            mqttConfig = gson.fromJson(mqttConfigStr, MQTTConfig.class);
            mqttConfig.connectMode = 0;
            mqttConfig.qos = 1;
            mqttConfig.keepAlive = 60;
            mqttConfig.clientId = "";
            mqttConfig.username = "";
            mqttConfig.password = "";
            mqttConfig.caPath = "";
            mqttConfig.clientKeyPath = "";
            mqttConfig.clientCertPath = "";
            mqttConfig.topicPublish = "";
            mqttConfig.topicSubscribe = "";
        }
        fragmentManager = getFragmentManager();
        createFragment();
        initData();
        // 注册广播接收器
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(MokoConstants.ACTION_MQTT_CONNECTION);
//        registerReceiver(mReceiver, filter);
        bindService(new Intent(this, MokoBlueService.class), mServiceConnection, BIND_AUTO_CREATE);
        bindService(new Intent(this, MokoService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMokoService = ((MokoBlueService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_ORDER_RESULT);
            filter.addAction(MokoConstants.ACTION_ORDER_TIMEOUT);
            filter.addAction(MokoConstants.ACTION_ORDER_FINISH);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(MokoConstants.ACTION_MQTT_RECEIVE);
            filter.setPriority(100);
            registerReceiver(mReceiver, filter);
            mReceiverTag = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mokoService = ((MokoService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
                    syncError();
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum order = response.order;
                    switch (order) {

                    }
                }
                if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {

                }
                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum order = response.order;
//                    int responseType = response.responseType;
//                    byte[] value = response.responseValue;
                    switch (order) {
                        case WRITE_HOST_PACKAGE_SUM:
                            MokoSupport.getInstance().sendOrder(mMokoService.setHost(mqttConfig.host));
                            break;
                        case WRITE_HOST:
                            MokoSupport.getInstance().sendOrder(mMokoService.setPort(Integer.parseInt(mqttConfig.port)));
                            break;
                        case WRITE_PORT:
                            MokoSupport.getInstance().sendOrder(mMokoService.setSession(mqttConfig.cleanSession ? 1 : 0));
                            break;
                        case WRITE_SESSION:
                            MokoSupport.getInstance().sendOrder(mMokoService.setDeviceIdSum(mqttConfig.uniqueId));
                            break;
                        case WRITE_DEVICE_ID_PACKAGE_SUM:
                            MokoSupport.getInstance().sendOrder(mMokoService.setDeviceId(mqttConfig.uniqueId));
                            break;
                        case WRITE_DEVICE_ID:
                            MokoSupport.getInstance().sendOrder(mMokoService.setClientIdSum(mqttConfig.clientId));
                            break;
                        case WRITE_CLIENT_ID_PACKAGE_SUM:
                            MokoSupport.getInstance().sendOrder(mMokoService.setClientId(mqttConfig.clientId));
                            break;
                        case WRITE_CLIENT_ID:
                            if (!TextUtils.isEmpty(mqttConfig.username)) {
                                MokoSupport.getInstance().sendOrder(mMokoService.setUsernameSum(mqttConfig.username));
                            } else if (!TextUtils.isEmpty(mqttConfig.password)) {
                                MokoSupport.getInstance().sendOrder(mMokoService.setPasswordSum(mqttConfig.password));
                            } else {
                                MokoSupport.getInstance().sendOrder(mMokoService.setKeepAlive(mqttConfig.keepAlive));
                            }
                            break;
                        case WRITE_USERNAME_PACKAGE_SUM:
                            MokoSupport.getInstance().sendOrder(mMokoService.setUsername(mqttConfig.username));
                            break;
                        case WRITE_USERNAME:
                            if (!TextUtils.isEmpty(mqttConfig.password)) {
                                MokoSupport.getInstance().sendOrder(mMokoService.setPasswordSum(mqttConfig.password));
                            } else {
                                MokoSupport.getInstance().sendOrder(mMokoService.setKeepAlive(mqttConfig.keepAlive));
                            }
                            break;
                        case WRITE_PASSWORD_PACKAGE_SUM:
                            MokoSupport.getInstance().sendOrder(mMokoService.setPassword(mqttConfig.password));
                            break;
                        case WRITE_PASSWORD:
                            MokoSupport.getInstance().sendOrder(mMokoService.setKeepAlive(mqttConfig.keepAlive));
                            break;
                        case WRITE_KEEPALIVE:
                            MokoSupport.getInstance().sendOrder(mMokoService.setQos(mqttConfig.qos));
                            break;
                        case WRITE_QOS:
                            MokoSupport.getInstance().sendOrder(mMokoService.setConnectMode(mqttConfig.connectMode == 3 ? 2 : mqttConfig.connectMode));
                            break;
                        case WRITE_CONNECTMODE:
                            if (mqttConfig.connectMode == 0 || (mqttConfig.connectMode > 0 && TextUtils.isEmpty(mqttConfig.caPath))) {
                                MokoSupport.getInstance().sendOrder(mMokoService.setPublishSum(mqttConfig.topicPublish));
                            } else {
                                // ssl
                                mFile = new File(mqttConfig.caPath);
                                MokoSupport.getInstance().sendOrder(mMokoService.setCASum((int) mFile.length()));
                            }
                            break;
                        case WRITE_CA_PACKAGE_SUM:
                            if (mFile != null && mFile.exists()) {
                                try {
                                    FileInputStream inputSteam = new FileInputStream(mFile);
                                    byte[] buffer = new byte[(int) mFile.length()];
                                    inputSteam.read(buffer);
                                    MokoSupport.getInstance().sendOrder(mMokoService.setCA(buffer));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    syncError();
                                }
                            } else {
                                syncError();
                            }
                            break;
                        case WRITE_CA:
                            if (mqttConfig.connectMode > 1) {
                                // 双向验证
                                mFile = new File(mqttConfig.clientCertPath);
                                MokoSupport.getInstance().sendOrder(mMokoService.setClientCertSum((int) mFile.length()));
                            } else {
                                MokoSupport.getInstance().sendOrder(mMokoService.setPublishSum(mqttConfig.topicPublish));
                            }
                            break;
                        case WRITE_CLIENTCERT_PACKAGE_SUM:
                            if (mFile != null && mFile.exists()) {
                                try {
                                    FileInputStream inputSteam = new FileInputStream(mFile);
                                    byte[] buffer = new byte[(int) mFile.length()];
                                    inputSteam.read(buffer);
                                    MokoSupport.getInstance().sendOrder(mMokoService.setClientCert(buffer));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    syncError();
                                }
                            } else {
                                syncError();
                            }
                            break;
                        case WRITE_CLIENTCERT:
                            mFile = new File(mqttConfig.clientKeyPath);
                            MokoSupport.getInstance().sendOrder(mMokoService.setClientPrivateSum((int) mFile.length()));
                            break;
                        case WRITE_CLIENTPRIVATE_PACKAGE_SUM:
                            if (mFile != null && mFile.exists()) {
                                try {
                                    FileInputStream inputSteam = new FileInputStream(mFile);
                                    byte[] buffer = new byte[(int) mFile.length()];
                                    inputSteam.read(buffer);
                                    MokoSupport.getInstance().sendOrder(mMokoService.setClientPrivate(buffer));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    syncError();
                                }
                            } else {
                                syncError();
                            }
                            break;
                        case WRITE_CLIENTPRIVATE:
                            MokoSupport.getInstance().sendOrder(mMokoService.setPublishSum(mqttConfig.topicPublish));
                            break;
                        case WRITE_PUBLISH_PACKAGE_SUM:
                            MokoSupport.getInstance().sendOrder(mMokoService.setPublish(mqttConfig.topicPublish));
                            break;
                        case WRITE_PUBLISH:
                            MokoSupport.getInstance().sendOrder(mMokoService.setSubscribeSum(mqttConfig.topicSubscribe));
                            break;
                        case WRITE_SUBSCRIBE_PACKAGE_SUM:
                            MokoSupport.getInstance().sendOrder(mMokoService.setSubscribe(mqttConfig.topicSubscribe));
                            break;
                        case WRITE_SUBSCRIBE:
                            MokoSupport.getInstance().sendOrder(mMokoService.setStaNameSum(mWifiSSID));
                            break;
                        case WRITE_STA_NAME_PACKAGE_SUM:
                            MokoSupport.getInstance().sendOrder(mMokoService.setStaName(mWifiSSID));
                            break;
                        case WRITE_STA_NAME:
                            MokoSupport.getInstance().sendOrder(mMokoService.setStaPasswordSum(mWifiPassword));
                            break;
                        case WRITE_STA_PASSWORD_PACKAGE_SUM:
                            MokoSupport.getInstance().sendOrder(mMokoService.setStaPassword(mWifiPassword));
                            break;
                        case WRITE_STA_PASSWORD:
                            MokoSupport.getInstance().sendOrder(mMokoService.setStartConnect());
                            break;
                        case WRITE_START_CONNECT:
                            // 完成配置，断开蓝牙
                            mMokoService.disConnectBle();
                            // 弹出加载弹框
                            showConnMqttDialog();
                            // 订阅主题
                            subscribeTopic();
                            break;
                    }
                }
                if (action.equals(MokoConstants.ACTION_MQTT_RECEIVE)) {
                    String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                    byte[] receive = intent.getByteArrayExtra(MokoConstants.EXTRA_MQTT_RECEIVE_MESSAGE);
                    if (TextUtils.isEmpty(topic) || isDeviceConnectSuccess) {
                        return;
                    }
                    if ((receive[0] & 0xFF) != 0x24) {
                        return;
                    }
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    if (!mqttConfig.uniqueId.equals(new String(id))) {
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
                                MokoDevice mokoDevice = DBTools.getInstance(SetDeviceMqttActivity.this).selectDeviceByName(mSelectedDeviceName);
                                String mqttConfigStr = new Gson().toJson(mqttConfig, MQTTConfig.class);
                                if (mokoDevice == null) {
                                    mokoDevice = new MokoDevice();
                                    mokoDevice.name = mSelectedDeviceName;
                                    mokoDevice.nickName = mSelectedDeviceName;
                                    mokoDevice.mqttInfo = mqttConfigStr;
                                    mokoDevice.topicSubscribe = mqttConfig.topicSubscribe;
                                    mokoDevice.topicPublish = mqttConfig.topicPublish;
                                    mokoDevice.uniqueId = mqttConfig.uniqueId;
                                    DBTools.getInstance(SetDeviceMqttActivity.this).insertDevice(mokoDevice);
                                } else {
                                    mokoDevice.name = mSelectedDeviceName;
                                    mokoDevice.mqttInfo = mqttConfigStr;
                                    mokoDevice.topicSubscribe = mqttConfig.topicSubscribe;
                                    mokoDevice.topicPublish = mqttConfig.topicPublish;
                                    mokoDevice.uniqueId = mqttConfig.uniqueId;
                                    DBTools.getInstance(SetDeviceMqttActivity.this).updateDevice(mokoDevice);
                                }
                                Intent modifyIntent = new Intent(SetDeviceMqttActivity.this, ModifyNameActivity.class);
                                modifyIntent.putExtra("mokodevice", mokoDevice);
                                startActivity(modifyIntent);
                            }
                        }, 500);
                    }
                }
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            dismissConnMqttDialog();
                            break;
                    }
                }
            }
        }
    };

    private void subscribeTopic() {
        // 订阅
        try {
            if (TextUtils.isEmpty(mAppMqttConfig.topicSubscribe)) {
                mokoService.subscribe(mqttConfig.topicPublish, mAppMqttConfig.qos);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(action)) {
            // 设备断开，通知页面更新
//            dismissConnMqttDialog();
            dismissLoadingProgressDialog();
//            ToastUtils.showToast(SetDeviceMqttActivity.this, "Disconnected");
        }
        if (MokoConstants.ACTION_DISCOVER_SUCCESS.equals(action)) {
            // 设备连接成功，通知页面更新
            etMqttHost.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 发送MQTT信息
                    MokoSupport.getInstance().sendOrder(mMokoService.setHostSum(mqttConfig.host));
                }
            }, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverTag) {
            unregisterReceiver(mReceiver);
        }
        unbindService(mServiceConnection);
        unbindService(serviceConnection);
        EventBus.getDefault().unregister(this);
    }

    private void createFragment() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        onewaySSLFragment = OnewaySSLFragment.newInstance();
        fragmentTransaction.add(R.id.frame_connect_mode, onewaySSLFragment);
        twowaySSLFragment = TwowaySSLFragment.newInstance();
        fragmentTransaction.add(R.id.frame_connect_mode, twowaySSLFragment);
        fragmentTransaction.hide(onewaySSLFragment).hide(twowaySSLFragment).commit();
    }

//    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (MokoConstants.ACTION_MQTT_CONNECTION.equals(action)) {
//                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_CONNECTION_STATE, 0);
//                if (state == MokoConstants.MQTT_CONN_STATUS_SUCCESS) {
//                    ToastUtils.showToast(SetDeviceMqttActivity.this, getString(R.string.success));
//                    dismissLoadingProgressDialog();
//                    SetDeviceMqttActivity.this.finish();
//                }
//            }
//        }
//    };

    private void initData() {
        etMqttHost.setText(mqttConfig.host);
        etMqttPort.setText(mqttConfig.port);
        tvQos.setText(mQosArray[mqttConfig.qos]);
        ivCleanSession.setImageDrawable(ContextCompat.getDrawable(this, mqttConfig.cleanSession ? R.drawable.checkbox_open : R.drawable.checkbox_close));
        rgConnMode.setOnCheckedChangeListener(this);
        switch (mqttConfig.connectMode) {
            case 0:
                rbConnModeTcp.setChecked(true);
                break;
            case 1:
                rbConnModeSslOneway.setChecked(true);
                break;
            case 3:
                rbConnModeSslTwoway.setChecked(true);
                break;
        }
        tvKeepAlive.setText(mqttConfig.keepAlive + "");
        etMqttClientId.setText(mqttConfig.clientId);
        etMqttDeviceId.setText(mqttConfig.uniqueId);
        etMqttUsername.setText(mqttConfig.username);
        etMqttPassword.setText(mqttConfig.password);
        etTopicSubscribe.setText("{device_name}/{device_id}/app_to_device");
        etTopicPublish.setText("{device_name}/{device_id}/device_to_app");
    }

    public void back(View view) {
        finish();
    }

    public void clearSettings(View view) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Clear All Parameters")
                .setMessage("Please confirm whether to clear all parameters?")
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mqttConfig.reset();
                        initData();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }

    public void checkQos(View view) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(true)
                .setSingleChoiceItems(mQosArray, mqttConfig.qos, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mqttConfig.qos = which;
                        tvQos.setText(mQosArray[mqttConfig.qos]);
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }


    public void checkKeepAlive(View view) {
        KeepAliveDialog dialog = new KeepAliveDialog();
        dialog.setSelected(mqttConfig.keepAlive);
        dialog.setListener(new KeepAliveDialog.OnDataSelectedListener() {
            @Override
            public void onDataSelected(String data) {
                mqttConfig.keepAlive = Integer.parseInt(data);
                tvKeepAlive.setText(data);
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    public void saveSettings(View view) {
        mqttConfig.host = etMqttHost.getText().toString().replaceAll(" ", "");
        mqttConfig.port = etMqttPort.getText().toString();
        mqttConfig.clientId = etMqttClientId.getText().toString().replaceAll(" ", "");
        mqttConfig.uniqueId = etMqttDeviceId.getText().toString().replaceAll(" ", "");
        mqttConfig.username = etMqttUsername.getText().toString().replaceAll(" ", "");
        mqttConfig.password = etMqttPassword.getText().toString().replaceAll(" ", "");
        mqttConfig.topicSubscribe = etTopicSubscribe.getText().toString().replaceAll(" ", "");
        mqttConfig.topicPublish = etTopicPublish.getText().toString().replaceAll(" ", "");
        if (mqttConfig.isError(this)) {
            return;
        }
        String clientId = etMqttClientId.getText().toString();
        if (TextUtils.isEmpty(clientId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_client_id_empty));
            return;
        }
        String deviceId = etMqttDeviceId.getText().toString();
        if (TextUtils.isEmpty(deviceId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_device_id_empty));
            return;
        }
        if (rbConnModeSslOneway.isChecked()) {
            mqttConfig.caPath = onewaySSLFragment.getCAFilePath();
        }
        if (rbConnModeSslTwoway.isChecked()) {
            mqttConfig.caPath = twowaySSLFragment.getCAFilePath();
//            if (TextUtils.isEmpty(mqttConfig.caPath)) {
//                ToastUtils.showToast(this, getString(R.string.mqtt_verify_ca));
//                return;
//            }
            mqttConfig.clientKeyPath = twowaySSLFragment.getClientKeyPath();
            if (TextUtils.isEmpty(mqttConfig.clientKeyPath)) {
                ToastUtils.showToast(this, getString(R.string.mqtt_verify_client_key));
                return;
            }
            mqttConfig.clientCertPath = twowaySSLFragment.getClientCertPath();
            if (TextUtils.isEmpty(mqttConfig.clientCertPath)) {
                ToastUtils.showToast(this, getString(R.string.mqtt_verify_client_cert));
                return;
            }
        }
        String topicSubscribe = etTopicSubscribe.getText().toString();
        if (TextUtils.isEmpty(topicSubscribe)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_topic_subscribe));
            return;
        }
        mqttConfig.topicSubscribe = topicSubscribe;
        String topicPublish = etTopicPublish.getText().toString();
        if (TextUtils.isEmpty(topicPublish)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_topic_publish));
            return;
        }
        mqttConfig.topicPublish = topicPublish;
//        if ("iot_plug".equals(function)) {
//            Intent intent = new Intent(this, SetDeviceMqttActivity.class);
//            intent.putExtra("function", function);
//            intent.putExtra("mqttConfig", mqttConfig);
//            startActivity(intent);
//        } else if ("iot_wall_switch".equals(function)) {
//            Intent intent = new Intent(this, AddWallSwitchActivity.class);
//            intent.putExtra("function", function);
//            intent.putExtra("mqttConfig", mqttConfig);
//            startActivity(intent);
//        }


//        String mqttConfigStr = new Gson().toJson(mqttConfig, MQTTConfig.class);
//        SPUtiles.setStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, mqttConfigStr);
//        stopService(new Intent(this, MokoService.class));
//        showLoadingProgressDialog(getString(R.string.mqtt_connecting));
//        tvKeepAlive.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                startService(new Intent(SetDeviceMqttActivity.this, MokoService.class));
//            }
//        }, 2000);
        if ("{device_name}/{device_id}/app_to_device".equals(mqttConfig.topicSubscribe)) {
            mqttConfig.topicSubscribe = String.format("%s/%s/app_to_device", mSelectedDeviceName, deviceId);
        }
        if ("{device_name}/{device_id}/device_to_app".equals(mqttConfig.topicPublish)) {
            mqttConfig.topicPublish = String.format("%s/%s/device_to_app", mSelectedDeviceName, deviceId);
        }
        if (!mqttConfig.topicPublish.isEmpty() && !mqttConfig.topicSubscribe.isEmpty()
                && mqttConfig.topicPublish.equals(mqttConfig.topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return;
        }
        showWifiInputDialog();
    }

    public void cleanSession(View view) {
        mqttConfig.cleanSession = !mqttConfig.cleanSession;
        ivCleanSession.setImageDrawable(ContextCompat.getDrawable(this, mqttConfig.cleanSession ? R.drawable.checkbox_open : R.drawable.checkbox_close));
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        switch (checkedId) {
            case R.id.rb_conn_mode_tcp:
                mqttConfig.connectMode = 0;
                fragmentTransaction.hide(onewaySSLFragment).hide(twowaySSLFragment).commit();
                break;
            case R.id.rb_conn_mode_ssl_oneway:
                mqttConfig.connectMode = 1;
                fragmentTransaction.show(onewaySSLFragment).hide(twowaySSLFragment).commit();
                onewaySSLFragment.setCAFilePath(mqttConfig);
                break;
            case R.id.rb_conn_mode_ssl_twoway:
                mqttConfig.connectMode = 3;
                fragmentTransaction.hide(onewaySSLFragment).show(twowaySSLFragment).commit();
                twowaySSLFragment.setCAFilePath(mqttConfig);
                twowaySSLFragment.setClientKeyPath(mqttConfig);
                twowaySSLFragment.setClientCertPath(mqttConfig);
                break;
        }
    }


    private InputFilter filter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source.equals(" ") || source.toString().contentEquals("\n")) return "";
            else return null;
        }
    };

    private void showWifiInputDialog() {
        View wifiInputView = LayoutInflater.from(this).inflate(R.layout.wifi_input_content, null);
        final EditText etSSID = wifiInputView.findViewById(R.id.et_ssid);
        etSSID.setFilters(new InputFilter[]{filter});
        final EditText etPassword = wifiInputView.findViewById(R.id.et_password);
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
                            ToastUtils.showToast(SetDeviceMqttActivity.this, getString(R.string.wifi_verify_empty));
                            return;
                        }
                        dialog.dismiss();
                        mWifiPassword = etPassword.getText().toString();
                        // 弹出加载弹框
//                        showConnMqttDialog();
                        showLoadingProgressDialog(getString(R.string.wait));
                        mMokoService.connectBluetoothDevice(mSelectedDeviceMac);
                    }
                })
                .create();
        dialog.show();
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
        mMokoService.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isDeviceConnectSuccess) {
                    isDeviceConnectSuccess = true;
                    dismissConnMqttDialog();
                    ToastUtils.showToast(SetDeviceMqttActivity.this, getString(R.string.mqtt_connecting_timeout));
                    mMokoService.disConnectBle();
                }
            }
        }, 90 * 1000);
    }

    private void dismissConnMqttDialog() {
        if (mqttConnDialog != null && !isFinishing() && mqttConnDialog.isShowing()) {
            isDeviceConnectSuccess = true;
            mqttConnDialog.dismiss();
            mMokoService.mHandler.removeMessages(0);
        }
    }

    private void syncError() {
        isDeviceConnectSuccess = true;
        dismissLoadingProgressDialog();
        ToastUtils.showToast(this, "Error");
        mMokoService.disConnectBle();
    }
}
