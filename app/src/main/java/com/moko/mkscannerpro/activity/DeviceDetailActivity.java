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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.adapter.ScanDeviceAdapter;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.entity.ScanDevice;
import com.moko.mkscannerpro.service.MokoService;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.handler.BaseMessageHandler;
import com.moko.support.handler.MQTTMessageAssembler;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceDetailActivity extends BaseActivity {
    @BindView(R.id.tv_device_name)
    TextView tvDeviceName;
    @BindView(R.id.iv_scan_switch)
    ImageView ivScanSwitch;
    @BindView(R.id.tv_scan_device_total)
    TextView tvScanDeviceTotal;
    @BindView(R.id.rv_devices)
    RecyclerView rvDevices;
    @BindView(R.id.et_scan_interval)
    EditText etScanInterval;
    @BindView(R.id.ll_scan_interval)
    LinearLayout llScanInterval;
    private boolean mScanSwitch;
    private int mScanInterval;
    private MokoDevice mMokoDevice;
    private ScanDeviceAdapter mAdapter;
    private ArrayList<ScanDevice> mScanDevices;
    private int mPublishType;
    private MQTTConfig appMqttConfig;
    private MokoService mokoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        tvDeviceName.setText(mMokoDevice.nickName);
        mScanDevices = new ArrayList<>();
        mAdapter = new ScanDeviceAdapter();
        mAdapter.openLoadAnimation();
        mAdapter.replaceData(mScanDevices);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(mAdapter);
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
                    DeviceDetailActivity.this.finish();
                }
            }, 30 * 1000);
            getScanSwitch();
            getScanInterval();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void changeView() {
        ivScanSwitch.setImageResource(mScanSwitch ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        tvScanDeviceTotal.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
        tvScanDeviceTotal.setText(getString(R.string.scan_device_total, mScanDevices.size()));
        llScanInterval.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
        rvDevices.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
    }

    @OnClick({R.id.rl_edit_filter, R.id.tv_save, R.id.iv_scan_switch})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_edit_filter:
                // 获取扫描过滤
                if (!mokoService.isConnected()) {
                    ToastUtils.showToast(DeviceDetailActivity.this, R.string.network_error);
                    return;
                }
                if (!mMokoDevice.isOnline) {
                    ToastUtils.showToast(DeviceDetailActivity.this, R.string.device_offline);
                    return;
                }
//                showLoadingProgressDialog(getString(R.string.wait));
//                getFilterRSSI();
                Intent i = new Intent(DeviceDetailActivity.this, ScanFilterActivity.class);
                i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
                startActivity(i);
                break;
            case R.id.iv_scan_switch:
                // 切换扫描开关
                if (!mokoService.isConnected()) {
                    ToastUtils.showToast(DeviceDetailActivity.this, R.string.network_error);
                    return;
                }
                if (!mMokoDevice.isOnline) {
                    ToastUtils.showToast(DeviceDetailActivity.this, R.string.device_offline);
                    return;
                }
                mScanSwitch = !mScanSwitch;
                ivScanSwitch.setImageResource(mScanSwitch ? R.drawable.checkbox_open : R.drawable.checkbox_close);
                tvScanDeviceTotal.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
                tvScanDeviceTotal.setText(getString(R.string.scan_device_total, 0));
                llScanInterval.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
                rvDevices.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
                etScanInterval.setEnabled(mScanSwitch);
                etScanInterval.setText(mScanInterval + "");
                mScanDevices.clear();
                mAdapter.replaceData(mScanDevices);
                setScanSwitch();
                break;
            case R.id.tv_save:
                // 设置扫描间隔
                if (!mokoService.isConnected()) {
                    ToastUtils.showToast(DeviceDetailActivity.this, R.string.network_error);
                    return;
                }
                if (!mMokoDevice.isOnline) {
                    ToastUtils.showToast(DeviceDetailActivity.this, R.string.device_offline);
                    return;
                }
                String interval = etScanInterval.getText().toString();
                if (TextUtils.isEmpty(interval)) {
                    ToastUtils.showToast(this, "Failed");
                    return;
                }
                mScanInterval = Integer.parseInt(interval);
                if (mScanInterval < 10 || mScanInterval > 65535) {
                    ToastUtils.showToast(this, "Failed");
                    return;
                }
                setScanInterval();
                break;
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MokoConstants.ACTION_MQTT_CONNECTION.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_CONNECTION_STATE, 0);
            }
            if (MokoConstants.ACTION_MQTT_PUBLISH.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_STATE, 0);
                if (mPublishType == 1) {
                    mPublishType = 0;
                    if (state == MokoConstants.MQTT_STATE_SUCCESS) {
                        ToastUtils.showToast(DeviceDetailActivity.this, "Succeed");
                    } else {
                        ToastUtils.showToast(DeviceDetailActivity.this, "Failed");
                        etScanInterval.setText("");
                    }
                }
            }
            if (MokoConstants.ACTION_MQTT_RECEIVE.equals(action)) {
                final String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                byte[] receive = intent.getByteArrayExtra(MokoConstants.EXTRA_MQTT_RECEIVE_MESSAGE);
                int header = receive[0] & 0xFF;
                if (header == 0x21)// 蓝牙广播数据
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    int deviceSize = receive[2 + length] & 0xff;
                    byte[] deviceBytes = Arrays.copyOfRange(receive, 3 + length, receive.length);
                    if (mMokoDevice.uniqueId.equals(new String(id)) && mScanSwitch) {
                        try {
                            for (int i = 0, l = deviceBytes.length; i < l; ) {
                                ScanDevice scanDevice = new ScanDevice();
                                int deviceLength = deviceBytes[i] & 0xff;
                                i++;
                                String mac = MokoUtils.bytesToHexString(Arrays.copyOfRange(deviceBytes, i, i + 6));
                                scanDevice.mac = mac;
                                i += 6;
                                int rssi = deviceBytes[i];
                                scanDevice.rssi = rssi;
                                i++;
                                int dataLength = deviceBytes[i] & 0xff;
                                i++;
                                String rawData = MokoUtils.bytesToHexString(Arrays.copyOfRange(deviceBytes, i, i + dataLength));
                                scanDevice.rawData = rawData;
                                i += dataLength;
                                int nameLength = deviceLength - 8 - dataLength;
                                if (nameLength > 0) {
                                    String name = new String(Arrays.copyOfRange(deviceBytes, i, i + nameLength));
                                    scanDevice.name = name;
                                } else {
                                    scanDevice.name = "";
                                }
                                i += nameLength;
                                mScanDevices.add(0, scanDevice);
                            }
                            tvScanDeviceTotal.setText(getString(R.string.scan_device_total, mScanDevices.size()));
                            mAdapter.replaceData(mScanDevices);
                        } catch (Exception e) {
                            // 读取stacktrace信息
                            final Writer result = new StringWriter();
                            final PrintWriter printWriter = new PrintWriter(result);
                            e.printStackTrace(printWriter);
                            StringBuffer errorReport = new StringBuffer();
                            errorReport.append(MokoUtils.bytesToHexString(receive));
                            errorReport.append(result.toString());
                            LogModule.e(errorReport.toString());
                        }
                    }
                }
                if (header == 0x17)// 开关状态
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    if (!mMokoDevice.uniqueId.equals(new String(id))) {
                        return;
                    }
                    mScanSwitch = (receive[receive.length - 1] & 0xFF) == 1;
                    changeView();
                }
                if (header == 0x18)// 扫描时长
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    if (!mMokoDevice.uniqueId.equals(new String(id))) {
                        return;
                    }
                    dismissLoadingProgressDialog();
                    byte[] dataLengthBytes = Arrays.copyOfRange(receive, 2 + length, 4 + length);
                    int dataLength = MokoUtils.toInt(dataLengthBytes);
                    mScanInterval = MokoUtils.toInt(Arrays.copyOfRange(receive, receive.length - dataLength, receive.length));
                    etScanInterval.setText(mScanInterval + "");
                    mHandler.removeMessages(0);
                }
            }
            if (AppConstants.ACTION_MODIFY_NAME.equals(action)) {
                MokoDevice device = DBTools.getInstance(DeviceDetailActivity.this).selectDevice(mMokoDevice.uniqueId);
                mMokoDevice.nickName = device.nickName;
                tvDeviceName.setText(mMokoDevice.nickName);
            }
            if (AppConstants.ACTION_DEVICE_STATE.equals(action)) {
                String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                if (topic.equals(mMokoDevice.topicPublish)) {
                    boolean isOnline = intent.getBooleanExtra(MokoConstants.EXTRA_MQTT_RECEIVE_STATE, false);
                    mMokoDevice.isOnline = isOnline;
                    if (!isOnline) {
                        ToastUtils.showToast(DeviceDetailActivity.this, "device is off-line");
                        finish();
                    }
                }
            }
        }
    };

    private void setScanSwitch() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = 0;
        byte[] message = MQTTMessageAssembler.assembleWriteScanSwitch(mMokoDevice.uniqueId, mScanSwitch);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setScanInterval() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = 1;
        byte[] message = MQTTMessageAssembler.assembleWriteScanInterval(mMokoDevice.uniqueId, mScanInterval);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getScanSwitch() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadScanSwitch(mMokoDevice.uniqueId);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getScanInterval() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadScanInterval(mMokoDevice.uniqueId);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(serviceConnection);
    }

    public void more(View view) {
        Intent intent = new Intent(this, MoreActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(intent);
    }

    public void back(View view) {
        finish();
    }

    public MessageHandler mHandler;

    public class MessageHandler extends BaseMessageHandler<DeviceDetailActivity> {

        public MessageHandler(DeviceDetailActivity activity) {
            super(activity);
        }

        @Override
        protected void handleMessage(DeviceDetailActivity activity, Message msg) {
        }
    }
}
