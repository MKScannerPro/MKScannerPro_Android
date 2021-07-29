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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.adapter.DeviceAdapter;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.dialog.RemoveDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.service.MokoService;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.handler.BaseMessageHandler;
import com.moko.support.log.LogModule;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @Date 2018/6/7
 * @Author wenzheng.liu
 * @Description 设备列表
 * @ClassPath com.moko.mkscannerpro.activity.MainActivity
 */
public class MainActivity extends BaseActivity implements DeviceAdapter.AdapterClickListener {

    @BindView(R.id.rl_empty)
    RelativeLayout rlEmpty;
    @BindView(R.id.lv_device_list)
    ListView lvDeviceList;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    private ArrayList<MokoDevice> devices;
    private DeviceAdapter adapter;
    private MokoService mokoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        devices = DBTools.getInstance(this).selectAllDevice();
        adapter = new DeviceAdapter(this);
        adapter.setListener(this);
        adapter.setItems(devices);
        lvDeviceList.setAdapter(adapter);
        if (devices.isEmpty()) {
            rlEmpty.setVisibility(View.VISIBLE);
            lvDeviceList.setVisibility(View.GONE);
        } else {
            lvDeviceList.setVisibility(View.VISIBLE);
            rlEmpty.setVisibility(View.GONE);
        }
        mHandler = new OfflineHandler(this);


        startService(new Intent(this, MokoService.class));
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
            filter.addAction(AppConstants.ACTION_MODIFY_NAME);
            filter.addAction(AppConstants.ACTION_DELETE_DEVICE);
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
                String title = "";
                if (state == MokoConstants.MQTT_CONN_STATUS_LOST) {
                    title = getString(R.string.mqtt_connecting);
                } else if (state == MokoConstants.MQTT_CONN_STATUS_SUCCESS) {
                    title = getString(R.string.app_name);
                } else if (state == MokoConstants.MQTT_CONN_STATUS_FAILED) {
                    title = getString(R.string.mqtt_connect_failed);
                }
                tvTitle.setText(title);
                if (state == 1) {
//                    try {
//                        mokoService.subscribe("lwz_123", 1);
//                    } catch (MqttException e) {
//                        e.printStackTrace();
//                    }
                    String mqttConfigAppStr = SPUtiles.getStringValue(MainActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
                    MQTTConfig appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
                    // 订阅
                    try {
                        if (!TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
                            mokoService.subscribe(appMqttConfig.topicSubscribe, appMqttConfig.qos);
                        }
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    if (devices.isEmpty()) {
                        return;
                    }
                    for (MokoDevice device : devices) {
                        // 订阅
//                        for (String topic : device.getDeviceTopics()) {
                        try {
                            if (TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
                                mokoService.subscribe(device.topicPublish, appMqttConfig.qos);
                            }
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
//                        }
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
            if (MokoConstants.ACTION_MQTT_PUBLISH.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_STATE, 0);
            }
            if (MokoConstants.ACTION_MQTT_RECEIVE.equals(action)) {
                final String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                byte[] receive = intent.getByteArrayExtra(MokoConstants.EXTRA_MQTT_RECEIVE_MESSAGE);
                if (devices.isEmpty()) {
                    return;
                }
                int header = receive[0] & 0xFF;
                if (header == 0x24)// 设备联网状态
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    for (final MokoDevice device : devices) {
                        if (device.uniqueId.equals(new String(id))) {
                            device.isOnline = true;
                            Intent i = new Intent(AppConstants.ACTION_DEVICE_STATE);
                            i.putExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC, topic);
                            i.putExtra(MokoConstants.EXTRA_MQTT_RECEIVE_STATE, true);
                            MainActivity.this.sendBroadcast(i);
                            if (mHandler.hasMessages(device.id)) {
                                mHandler.removeMessages(device.id);
                            }
                            Message message = Message.obtain(mHandler, new Runnable() {
                                @Override
                                public void run() {
                                    device.isOnline = false;
                                    LogModule.i(device.uniqueId + "离线");
                                    adapter.notifyDataSetChanged();
                                    Intent i = new Intent(AppConstants.ACTION_DEVICE_STATE);
                                    i.putExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC, topic);
                                    MainActivity.this.sendBroadcast(i);
                                }
                            });
                            message.what = device.id;
                            mHandler.sendMessageDelayed(message, 62 * 1000);
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
            }
            if (AppConstants.ACTION_MODIFY_NAME.equals(action)) {
                devices.clear();
                devices.addAll(DBTools.getInstance(MainActivity.this).selectAllDevice());
                adapter.notifyDataSetChanged();
            }
            if (AppConstants.ACTION_DELETE_DEVICE.equals(action)) {
                int id = intent.getIntExtra(AppConstants.EXTRA_DELETE_DEVICE_ID, -1);
                if (id > 0 && mHandler.hasMessages(id)) {
                    mHandler.removeMessages(id);
                }
            }
        }
    };


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogModule.i("onNewIntent...");
        setIntent(intent);
        if (getIntent().getExtras() != null) {
            String from = getIntent().getStringExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY);
            String uniqueId = getIntent().getStringExtra(AppConstants.EXTRA_KEY_UNIQUE_ID);
            if (ModifyNameActivity.TAG.equals(from)
                    || MoreActivity.TAG.equals(from)) {
                devices.clear();
                devices.addAll(DBTools.getInstance(this).selectAllDevice());
                if (!TextUtils.isEmpty(uniqueId)) {
                    for (final MokoDevice device : devices) {
                        if (uniqueId.equals(device.uniqueId)) {
                            device.isOnline = true;
                            if (mHandler.hasMessages(device.id)) {
                                mHandler.removeMessages(device.id);
                            }
                            Message message = Message.obtain(mHandler, new Runnable() {
                                @Override
                                public void run() {
                                    device.isOnline = false;
                                    LogModule.i(device.uniqueId + "离线");
                                    adapter.notifyDataSetChanged();
                                }
                            });
                            message.what = device.id;
                            mHandler.sendMessageDelayed(message, 62 * 1000);
                            break;
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                if (!devices.isEmpty()) {
                    lvDeviceList.setVisibility(View.VISIBLE);
                    rlEmpty.setVisibility(View.GONE);
                } else {
                    lvDeviceList.setVisibility(View.GONE);
                    rlEmpty.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(serviceConnection);
        stopService(new Intent(this, MokoService.class));
    }

    public void setAppMqttConfig(View view) {
        startActivity(new Intent(this, SetAppMqttActivity.class));
    }

    public void mainAddDevices(View view) {
//        MqttMessage message = new MqttMessage();
//        message.setPayload("332211".getBytes());
//        message.setQos(1);
//        try {
//            mokoService.publish("lwz_321", message);
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }

        String mqttAppConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
//        String mqttDeviceConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG, "");
//        if (TextUtils.isEmpty(mqttDeviceConfigStr)) {
//            startActivity(new Intent(this, SetDeviceMqttActivity.class));
//            return;e
//        }
        if (TextUtils.isEmpty(mqttAppConfigStr)) {
            startActivity(new Intent(this, SetAppMqttActivity.class));
            return;
        }
        MQTTConfig mqttConfig = new Gson().fromJson(mqttAppConfigStr, MQTTConfig.class);
        if (TextUtils.isEmpty(mqttConfig.host)) {
            startActivity(new Intent(this, SetAppMqttActivity.class));
            return;
        }
        startActivity(new Intent(this, ScannerDeviceActivity.class));
    }

    @Override
    public void deviceDetailClick(MokoDevice device) {
        if (!mokoService.isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!device.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(MainActivity.this, DeviceDetailActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, device);
        startActivity(i);
    }

    @Override
    public void deviceLongClick(final MokoDevice device) {
        RemoveDialog dialog = new RemoveDialog(this);
        dialog.setListener(new RemoveDialog.RemoveListener() {
            @Override
            public void onConfirmClick(RemoveDialog dialog) {
                if (!mokoService.isConnected()) {
                    ToastUtils.showToast(MainActivity.this, R.string.network_error);
                    return;
                }
                showLoadingProgressDialog(getString(R.string.wait));
                // 取消订阅
                try {
                    mokoService.unSubscribe(device.topicPublish);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                LogModule.i("删除设备");
                DBTools.getInstance(MainActivity.this).deleteDevice(device);
                Intent i = new Intent(AppConstants.ACTION_DELETE_DEVICE);
                i.putExtra(AppConstants.EXTRA_DELETE_DEVICE_ID, device.id);
                MainActivity.this.sendBroadcast(i);
                devices.remove(device);
                adapter.notifyDataSetChanged();
                if (devices.isEmpty()) {
                    rlEmpty.setVisibility(View.VISIBLE);
                    lvDeviceList.setVisibility(View.GONE);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void about(View view) {
        // 关于
        startActivity(new Intent(this, AboutActivity.class));
    }

    public OfflineHandler mHandler;

    public class OfflineHandler extends BaseMessageHandler<MainActivity> {

        public OfflineHandler(MainActivity activity) {
            super(activity);
        }

        @Override
        protected void handleMessage(MainActivity activity, Message msg) {
        }
    }
}
