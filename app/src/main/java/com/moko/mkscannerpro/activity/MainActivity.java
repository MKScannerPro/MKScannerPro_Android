package com.moko.mkscannerpro.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.adapter.DeviceAdapter;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.dialog.AlertMessageDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.mkscannerpro.utils.Utils;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.entity.MsgNotify;
import com.moko.support.event.DeviceDeletedEvent;
import com.moko.support.event.DeviceModifyNameEvent;
import com.moko.support.event.DeviceOnlineEvent;
import com.moko.support.event.MQTTConnectionCompleteEvent;
import com.moko.support.event.MQTTConnectionFailureEvent;
import com.moko.support.event.MQTTConnectionLostEvent;
import com.moko.support.event.MQTTMessageArrivedEvent;
import com.moko.support.event.MQTTUnSubscribeFailureEvent;
import com.moko.support.event.MQTTUnSubscribeSuccessEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity implements BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemLongClickListener {

    @BindView(R.id.rl_empty)
    RelativeLayout rlEmpty;
    @BindView(R.id.rv_device_list)
    RecyclerView rvDeviceList;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    private ArrayList<MokoDevice> devices;
    private DeviceAdapter adapter;
    public Handler mHandler;
    public String MQTTAppConfigStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        devices = DBTools.getInstance(this).selectAllDevice();
        adapter = new DeviceAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(devices);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        rvDeviceList.setAdapter(adapter);
        if (devices.isEmpty()) {
            rlEmpty.setVisibility(View.VISIBLE);
            rvDeviceList.setVisibility(View.GONE);
        } else {
            rvDeviceList.setVisibility(View.VISIBLE);
            rlEmpty.setVisibility(View.GONE);
        }
        mHandler = new Handler(Looper.getMainLooper());
        MQTTAppConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        if (!TextUtils.isEmpty(MQTTAppConfigStr)) {
            tvTitle.setText(getString(R.string.mqtt_connecting));
        }
        StringBuffer buffer = new StringBuffer();
        // 记录机型
        buffer.append("机型：");
        buffer.append(android.os.Build.MODEL);
        buffer.append("=====");
        // 记录版本号
        buffer.append("手机系统版本：");
        buffer.append(android.os.Build.VERSION.RELEASE);
        buffer.append("=====");
        // 记录APP版本
        buffer.append("APP版本：");
        buffer.append(Utils.getVersionInfo(this));
        XLog.d(buffer.toString());
        try {
            MQTTSupport.getInstance().connectMqtt(MQTTAppConfigStr);
        } catch (FileNotFoundException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ToastUtils.showToast(this, "Please select your SSL certificates again, otherwise the APP can't use normally.");
                startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
            }
            // 读取stacktrace信息
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            e.printStackTrace(printWriter);
            StringBuffer errorReport = new StringBuffer();
            errorReport.append(result.toString());
            XLog.e(errorReport.toString());
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
        tvTitle.setText(getString(R.string.app_name));
        // 订阅所有设备的Topic
        subscribeAllDevices();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionLostEvent(MQTTConnectionLostEvent event) {
        tvTitle.setText(getString(R.string.mqtt_connecting));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionFailureEvent(MQTTConnectionFailureEvent event) {
        tvTitle.setText(getString(R.string.mqtt_connect_failed));
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        runOnUiThread(() -> {
            // 更新所有设备的网络状态
            updateDeviceNetworkStatus(event);
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTUnSubscribeSuccessEvent(MQTTUnSubscribeSuccessEvent event) {
        dismissLoadingProgressDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTUnSubscribeFailureEvent(MQTTUnSubscribeFailureEvent event) {
        dismissLoadingProgressDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        if (!devices.isEmpty()) {
            for (MokoDevice device : devices) {
                if (device.deviceId.equals(event.getDeviceId())) {
                    device.nickName = DBTools.getInstance(this).selectDevice(device.deviceId).nickName;
                    break;
                }
            }
        }
        adapter.replaceData(devices);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDeletedEvent(DeviceDeletedEvent event) {
        // 删除了设备
        int id = event.getId();
        if (id > 0 && mHandler.hasMessages(id)) {
            mHandler.removeMessages(id);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        XLog.i("onNewIntent...");
        setIntent(intent);
        if (getIntent().getExtras() != null) {
            String from = getIntent().getStringExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY);
            String deviceId = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_ID);
            if (ModifyNameActivity.TAG.equals(from)
                    || DeviceSettingActivity.TAG.equals(from)) {
                devices.clear();
                devices.addAll(DBTools.getInstance(this).selectAllDevice());
                if (!TextUtils.isEmpty(deviceId)) {
                    for (final MokoDevice device : devices) {
                        if (deviceId.equals(device.deviceId)) {
                            device.isOnline = true;
                            if (mHandler.hasMessages(device.id)) {
                                mHandler.removeMessages(device.id);
                            }
                            Message message = Message.obtain(mHandler, () -> {
                                device.isOnline = false;
                                XLog.i(device.deviceId + "离线");
                                adapter.replaceData(devices);
                            });
                            message.what = device.id;
                            mHandler.sendMessageDelayed(message, 62 * 1000);
                            break;
                        }
                    }
                }
                adapter.replaceData(devices);
                if (!devices.isEmpty()) {
                    rvDeviceList.setVisibility(View.VISIBLE);
                    rlEmpty.setVisibility(View.GONE);
                } else {
                    rvDeviceList.setVisibility(View.GONE);
                    rlEmpty.setVisibility(View.VISIBLE);
                }
            }
            if (ModifyMQTTSettingsActivity.TAG.equals(from)) {
                if (!TextUtils.isEmpty(deviceId)) {
                    MokoDevice mokoDevice = DBTools.getInstance(this).selectDevice(deviceId);
                    for (final MokoDevice device : devices) {
                        if (deviceId.equals(device.deviceId)) {
                            if (!device.topicPublish.equals(mokoDevice.topicPublish)) {
                                // 取消订阅
                                try {
                                    MQTTSupport.getInstance().unSubscribe(device.topicPublish);
                                } catch (MqttException e) {
                                    e.printStackTrace();
                                }
                            }
                            device.mqttInfo = mokoDevice.mqttInfo;
                            device.topicPublish = mokoDevice.topicPublish;
                            device.topicSubscribe = mokoDevice.topicSubscribe;
                            break;
                        }
                    }
                }
                adapter.replaceData(devices);
            }
        }
    }

    public void setAppMQTTConfig(View view) {
        if (isWindowLocked())
            return;
        startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
    }

    public void mainAddDevices(View view) {
        if (isWindowLocked())
            return;
        if (TextUtils.isEmpty(MQTTAppConfigStr)) {
            startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
            return;
        }
        if (Utils.isNetworkAvailable(this)) {
            MQTTConfig MQTTAppConfig = new Gson().fromJson(MQTTAppConfigStr, MQTTConfig.class);
            if (TextUtils.isEmpty(MQTTAppConfig.host)) {
                startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
                return;
            }
            startActivity(new Intent(this, DeviceScannerActivity.class));
        } else {
            String ssid = Utils.getWifiSSID(this);
            ToastUtils.showToast(this, String.format("SSID:%s, the network cannot available,please check", ssid));
            XLog.i(String.format("SSID:%s, the network cannot available,please check", ssid));
        }
    }

    public void about(View view) {
        if (isWindowLocked())
            return;
        // 关于
        startActivity(new Intent(this, AboutActivity.class));
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDevice mokoDevice = (MokoDevice) adapter.getItem(position);
        if (mokoDevice == null)
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(MainActivity.this, DeviceDetailActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
        startActivity(i);
    }

    @Override
    public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDevice mokoDevice = (MokoDevice) adapter.getItem(position);
        if (mokoDevice == null)
            return true;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Remove Device");
        dialog.setMessage("Please confirm again whether to \n remove the device");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(MainActivity.this, R.string.network_error);
                return;
            }
            showLoadingProgressDialog();
            // 取消订阅
            try {
                MQTTSupport.getInstance().unSubscribe(mokoDevice.topicPublish);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            XLog.i(String.format("删除设备:%s", mokoDevice.nickName));
            DBTools.getInstance(MainActivity.this).deleteDevice(mokoDevice);
            EventBus.getDefault().post(new DeviceDeletedEvent(mokoDevice.id));
            devices.remove(mokoDevice);
            adapter.replaceData(devices);
            if (devices.isEmpty()) {
                rlEmpty.setVisibility(View.VISIBLE);
                rvDeviceList.setVisibility(View.GONE);
            }
        });
        dialog.show(getSupportFragmentManager());
        return true;
    }

    private void subscribeAllDevices() {
        MQTTConfig MQTTAppConfig = new Gson().fromJson(MQTTAppConfigStr, MQTTConfig.class);
        if (!TextUtils.isEmpty(MQTTAppConfig.topicSubscribe)) {
            try {
                MQTTSupport.getInstance().subscribe(MQTTAppConfig.topicSubscribe, MQTTAppConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            if (devices.isEmpty()) {
                return;
            }
            for (MokoDevice device : devices) {
                try {
                    if (TextUtils.isEmpty(MQTTAppConfig.topicSubscribe)) {
                        MQTTSupport.getInstance().subscribe(device.topicPublish, MQTTAppConfig.qos);
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateDeviceNetworkStatus(MQTTMessageArrivedEvent event) {
        if (devices.isEmpty()) {
            return;
        }
        final String topic = event.getTopic();
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message))
            return;
        int msg_id;
        try {
            JsonObject object = new Gson().fromJson(message, JsonObject.class);
            JsonElement element = object.get("msg_id");
            msg_id = element.getAsInt();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (msg_id != MQTTConstants.NOTIFY_MSG_ID_NETWORKING_STATUS
                && msg_id != MQTTConstants.NOTIFY_MSG_ID_BLE_SCAN_RESULT)
            return;
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_SCAN_RESULT && isDurationVoid())
            return;
        Type type = new TypeToken<MsgNotify<Object>>() {
        }.getType();
        MsgNotify<Object> msgNotify = new Gson().fromJson(message, type);
        final String deviceId = msgNotify.device_info.device_id;
        for (final MokoDevice device : devices) {
            if (device.deviceId.equals(deviceId)) {
                device.isOnline = true;
                if (mHandler.hasMessages(device.id)) {
                    mHandler.removeMessages(device.id);
                }
                Message offline = Message.obtain(mHandler, () -> {
                    device.isOnline = false;
                    XLog.i(device.deviceId + "离线");
                    adapter.replaceData(devices);
                    EventBus.getDefault().post(new DeviceOnlineEvent(deviceId, false));
                });
                offline.what = device.id;
                mHandler.sendMessageDelayed(offline, 62 * 1000);
                adapter.replaceData(devices);
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;
        if (requestCode == AppConstants.REQUEST_CODE_MQTT_CONFIG_APP) {
            MQTTAppConfigStr = data.getStringExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_APP);
            tvTitle.setText(getString(R.string.app_name));
            // 订阅所有设备的Topic
            subscribeAllDevices();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MQTTSupport.getInstance().disconnectMqtt();
        if (!devices.isEmpty()) {
            for (final MokoDevice device : devices) {
                if (mHandler.hasMessages(device.id)) {
                    mHandler.removeMessages(device.id);
                }
            }
        }
    }

    // 记录上次收到信息的时间,屏蔽无效事件
    protected long mLastMessageTime = 0;

    public boolean isDurationVoid() {
        long current = SystemClock.elapsedRealtime();
        if (current - mLastMessageTime > 500) {
            mLastMessageTime = current;
            return false;
        } else {
            return true;
        }
    }
}
