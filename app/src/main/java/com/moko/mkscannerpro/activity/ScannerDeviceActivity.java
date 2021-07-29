package com.moko.mkscannerpro.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.adapter.DeviceInfoAdapter;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.service.MokoBlueService;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoScanDeviceCallback;
import com.moko.support.entity.DeviceInfo;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ScannerDeviceActivity extends BaseActivity implements MokoScanDeviceCallback, BaseQuickAdapter.OnItemClickListener {


    @BindView(R.id.iv_refresh)
    ImageView ivRefresh;
    @BindView(R.id.rv_devices)
    RecyclerView rvDevices;
    private Animation animation = null;
    private MokoBlueService mMokoService;
    private DeviceInfoAdapter mAdapter;
    private HashMap<String, DeviceInfo> mDeviceMap;
    private ArrayList<DeviceInfo> mDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        ButterKnife.bind(this);
        mDeviceMap = new HashMap<>();
        mDevices = new ArrayList<>();
        mAdapter = new DeviceInfoAdapter();
        mAdapter.openLoadAnimation();
        mAdapter.replaceData(mDevices);
        mAdapter.setOnItemClickListener(this);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(mAdapter);
        Intent intent = new Intent(this, MokoBlueService.class);
        startService(intent);
        bindService(new Intent(this, MokoBlueService.class), mServiceConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMokoService = ((MokoBlueService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver, filter);
            if (animation == null) {
                startScan();
            }
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
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            if (animation != null) {
                                mMokoService.mHandler.removeMessages(0);
                                findViewById(R.id.iv_refresh).clearAnimation();
                                animation = null;
                                updateDevices();
                            }
                            break;

                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConnection);
        EventBus.getDefault().unregister(this);
        stopService(new Intent(this, MokoBlueService.class));
    }


    @Override
    public void onStartScan() {
        mDeviceMap.clear();
    }

    @Override
    public void onScanDevice(DeviceInfo deviceInfo) {
        mDeviceMap.put(deviceInfo.mac, deviceInfo);
        updateDevices();
    }

    @Override
    public void onStopScan() {
        findViewById(R.id.iv_refresh).clearAnimation();
        animation = null;
        updateDevices();
    }

    private void updateDevices() {
        mDevices.clear();
        mDevices.addAll(mDeviceMap.values());
        // 排序
        if (!mDevices.isEmpty()) {
            Collections.sort(mDevices, new Comparator<DeviceInfo>() {
                @Override
                public int compare(DeviceInfo lhs, DeviceInfo rhs) {
                    if (lhs.rssi > rhs.rssi) {
                        return -1;
                    } else if (lhs.rssi < rhs.rssi) {
                        return 1;
                    }
                    return 0;
                }
            });
        }
        mAdapter.replaceData(mDevices);
    }

    @OnClick({R.id.iv_refresh})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_refresh:
                if (animation == null) {
                    startScan();
                } else {
                    mMokoService.mHandler.removeMessages(0);
                    MokoSupport.getInstance().stopScanDevice();
                }
                break;
        }
    }

    private void startScan() {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return;
        }
        animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        ivRefresh.startAnimation(animation);
        if (!isLocationPermissionOpen()) {
            return;
        }
        MokoSupport.getInstance().startScanDevice(this);
        mMokoService.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MokoSupport.getInstance().stopScanDevice();
            }
        }, 1000 * 10);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            back();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void back() {
        if (animation != null) {
            mMokoService.mHandler.removeMessages(0);
            MokoSupport.getInstance().stopScanDevice();
        }
        finish();
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return;
        }
        DeviceInfo deviceInfo = (DeviceInfo) adapter.getItem(position);
        if (deviceInfo != null) {
            if (animation != null) {
                mMokoService.mHandler.removeMessages(0);
                MokoSupport.getInstance().stopScanDevice();
            }
            // 跳转配置页面
            Intent intent = new Intent(this, SetDeviceMqttActivity.class);
            intent.putExtra(MokoConstants.EXTRA_KEY_SELECTED_DEVICE_MAC, deviceInfo.mac);
            intent.putExtra(MokoConstants.EXTRA_KEY_SELECTED_DEVICE_NAME, deviceInfo.name);
            startActivity(intent);
        }
    }

    public void back(View view) {
        back();
    }
}
