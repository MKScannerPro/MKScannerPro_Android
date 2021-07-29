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
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.dialog.FilterDelDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.service.MokoService;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.entity.DataTypeEnum;
import com.moko.support.entity.FilterRawData;
import com.moko.support.handler.BaseMessageHandler;
import com.moko.support.handler.MQTTMessageAssembler;
import com.moko.support.utils.MokoUtils;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ScanFilterActivity extends BaseActivity {

    private final String FILTER_ASCII = "\\A\\p{ASCII}*\\z";

    @BindView(R.id.tv_device_name)
    TextView tvDeviceName;
    @BindView(R.id.sb_rssi)
    SeekBar sbRssi;
    @BindView(R.id.tv_rssi)
    TextView tvRssi;
    @BindView(R.id.et_filter_name)
    EditText etFilterName;
    @BindView(R.id.ll_rssi)
    LinearLayout llRssi;
    @BindView(R.id.iv_name_filter)
    ImageView ivNameFilter;
    @BindView(R.id.iv_mac_filter)
    ImageView ivMacFilter;
    @BindView(R.id.et_filter_mac)
    EditText etFilterMac;
    @BindView(R.id.iv_raw_data_filter)
    ImageView ivRawDataFilter;
    @BindView(R.id.iv_raw_data_del)
    ImageView ivRawDataDel;
    @BindView(R.id.iv_raw_data_add)
    ImageView ivRawDataAdd;
    @BindView(R.id.ll_raw_data_filter)
    LinearLayout llRawDataFilter;
    private MokoDevice mMokoDevice;
    private int mPublishType;
    private int mFilterRSSI;
    private String mFilterName;
    private String mFilterMac;
    private int mFilterRawDataSumLength;
    private ArrayList<FilterRawData> filterRawDatas;
    private boolean isFilterNameOpen;
    private boolean isFilterMacOpen;
    private boolean isFilterRawDataOpen;
    private MQTTConfig appMqttConfig;
    private MokoService mokoService;
    private boolean isGetData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        ButterKnife.bind(this);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);

        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        tvDeviceName.setText(mMokoDevice.nickName);

        sbRssi.setProgress(Math.abs(-100 - mFilterRSSI));
        sbRssi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mFilterRSSI = progress - 100;
                tvRssi.setText(getString(R.string.scan_filter_rssi, mFilterRSSI));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tvRssi.setText(getString(R.string.scan_filter_rssi, mFilterRSSI));
        InputFilter inputFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (!(source + "").matches(FILTER_ASCII)) {
                    return "";
                }

                return null;
            }
        };
        etFilterName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(29), inputFilter});
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
                    isGetData = false;
                    dismissLoadingProgressDialog();
                    ScanFilterActivity.this.finish();
                }
            }, 30 * 1000);
            isGetData = true;
            getFilterRSSI();
            getFilterName();
            getFilterMac();
            getFilterRawData();
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
                    switch (mPublishType) {
                        case 0:
                            setFilterName();
                            return;
                        case 1:
                            setFilterMac();
                            return;
                        case 2:
                            setFilterRawData();
                            return;
                    }
                    if (mPublishType == 3) {
                        dismissLoadingProgressDialog();
                        ToastUtils.showToast(ScanFilterActivity.this, "Succeed");
                        mHandler.removeMessages(1);
                        finish();
                    }
                }
            }
            if (MokoConstants.ACTION_MQTT_RECEIVE.equals(action)) {
                final String topic = intent.getStringExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC);
                byte[] receive = intent.getByteArrayExtra(MokoConstants.EXTRA_MQTT_RECEIVE_MESSAGE);
                int header = receive[0] & 0xFF;
                if (!isGetData)
                    return;
                if (header == 0x19)// 蓝牙过滤RSSI
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    if (mMokoDevice.uniqueId.equals(new String(id))) {
                        mFilterRSSI = receive[receive.length - 1];
                        sbRssi.setProgress(Math.abs(-100 - mFilterRSSI));
                        tvRssi.setText(getString(R.string.scan_filter_rssi, mFilterRSSI));
                    }
                }
                if (header == 0x20)// 过滤扫描名称
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    if (mMokoDevice.uniqueId.equals(new String(id))) {
                        byte[] dataLengthBytes = Arrays.copyOfRange(receive, 2 + length, 4 + length);
                        int dataLength = MokoUtils.toInt(dataLengthBytes);
                        if (dataLength == 0) {
                            ivNameFilter.setImageResource(R.drawable.ic_cb_close);
                            etFilterName.setVisibility(View.GONE);
                            return;
                        }
                        isFilterNameOpen = true;
                        ivNameFilter.setImageResource(R.drawable.ic_cb_open);
                        etFilterName.setVisibility(View.VISIBLE);
                        mFilterName = new String(Arrays.copyOfRange(receive, 4 + length, receive.length));
                        etFilterName.setText(mFilterName);
                    }
                }
                if (header == 0x1d)// 过滤扫描MAC
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    if (mMokoDevice.uniqueId.equals(new String(id))) {
                        byte[] dataLengthBytes = Arrays.copyOfRange(receive, 2 + length, 4 + length);
                        int dataLength = MokoUtils.toInt(dataLengthBytes);
                        if (dataLength == 0) {
                            ivMacFilter.setImageResource(R.drawable.ic_cb_close);
                            etFilterMac.setVisibility(View.GONE);
                            return;
                        }
                        isFilterMacOpen = true;
                        ivMacFilter.setImageResource(R.drawable.ic_cb_open);
                        etFilterMac.setVisibility(View.VISIBLE);

                        mFilterMac = MokoUtils.bytesToHexString(Arrays.copyOfRange(receive, 4 + length, receive.length));
                        etFilterMac.setText(mFilterMac);
                    }
                }
                if (header == 0x1c)// 过滤扫描原始数据
                {
                    int length = receive[1] & 0xFF;
                    byte[] id = Arrays.copyOfRange(receive, 2, 2 + length);
                    if (mMokoDevice.uniqueId.equals(new String(id))) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                        isGetData = false;
                        byte[] dataLengthBytes = Arrays.copyOfRange(receive, 2 + length, 4 + length);
                        int dataLength = MokoUtils.toInt(dataLengthBytes);
                        if (dataLength == 0) {
                            ivRawDataFilter.setImageResource(R.drawable.ic_cb_close);
                            llRawDataFilter.setVisibility(View.GONE);
                            ivRawDataAdd.setVisibility(View.GONE);
                            ivRawDataDel.setVisibility(View.GONE);
                            return;
                        }
                        isFilterRawDataOpen = true;
                        ivRawDataFilter.setImageResource(R.drawable.ic_cb_open);
                        llRawDataFilter.setVisibility(View.VISIBLE);
                        ivRawDataAdd.setVisibility(View.VISIBLE);
                        ivRawDataDel.setVisibility(View.VISIBLE);

                        byte[] dataBytes = Arrays.copyOfRange(receive, 4 + length, receive.length);

                        for (int i = 0, l = dataBytes.length; i < l; ) {
                            View v = LayoutInflater.from(ScanFilterActivity.this).inflate(R.layout.item_raw_data_filter, llRawDataFilter, false);
                            EditText etDataType = v.findViewById(R.id.et_data_type);
                            EditText etMin = v.findViewById(R.id.et_min);
                            EditText etMax = v.findViewById(R.id.et_max);
                            EditText etRawData = v.findViewById(R.id.et_raw_data);
                            int filterLength = dataBytes[i] & 0xFF;
                            i++;
                            String type = MokoUtils.byte2HexString(dataBytes[i]);
                            i++;
                            String min = String.valueOf((dataBytes[i] & 0xFF));
                            i++;
                            String max = String.valueOf((dataBytes[i] & 0xFF));
                            i++;
                            String data = MokoUtils.bytesToHexString(Arrays.copyOfRange(dataBytes, i, i + filterLength - 3));
                            i += filterLength - 3;
                            etDataType.setText(type);
                            etMin.setText(min);
                            etMax.setText(max);
                            etRawData.setText(data);
                            llRawDataFilter.addView(v);
                        }
                    }
                }
            }
            if (AppConstants.ACTION_MODIFY_NAME.equals(action)) {
                MokoDevice device = DBTools.getInstance(ScanFilterActivity.this).selectDevice(mMokoDevice.uniqueId);
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

    @OnClick({R.id.tv_done, R.id.iv_name_filter, R.id.iv_mac_filter,
            R.id.iv_raw_data_filter, R.id.iv_raw_data_add, R.id.iv_raw_data_del})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_done:
                if (!mokoService.isConnected()) {
                    ToastUtils.showToast(this, R.string.network_error);
                    return;
                }
                if (!mMokoDevice.isOnline) {
                    ToastUtils.showToast(this, R.string.device_offline);
                    return;
                }
                if (!isVaild()) {
                    ToastUtils.showToast(this, "Params error");
                    return;
                }
                Message message = Message.obtain(mHandler, new Runnable() {
                    @Override
                    public void run() {
                        dismissLoadingProgressDialog();
                    }
                });
                message.what = 1;
                mHandler.sendMessageDelayed(message, 30 * 1000);
                showLoadingProgressDialog(getString(R.string.wait));
                setFilterRSSI();
                break;
            case R.id.iv_name_filter:
                isFilterNameOpen = !isFilterNameOpen;
                ivNameFilter.setImageResource(isFilterNameOpen ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
                etFilterName.setVisibility(isFilterNameOpen ? View.VISIBLE : View.GONE);
                break;
            case R.id.iv_mac_filter:
                isFilterMacOpen = !isFilterMacOpen;
                ivMacFilter.setImageResource(isFilterMacOpen ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
                etFilterMac.setVisibility(isFilterMacOpen ? View.VISIBLE : View.GONE);
                break;
            case R.id.iv_raw_data_filter:
                isFilterRawDataOpen = !isFilterRawDataOpen;
                ivRawDataFilter.setImageResource(isFilterRawDataOpen ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
                llRawDataFilter.setVisibility(isFilterRawDataOpen ? View.VISIBLE : View.GONE);
                ivRawDataAdd.setVisibility(isFilterRawDataOpen ? View.VISIBLE : View.GONE);
                ivRawDataDel.setVisibility(isFilterRawDataOpen ? View.VISIBLE : View.GONE);
                break;
            case R.id.iv_raw_data_add:
                int count = llRawDataFilter.getChildCount();
                if (count > 4) {
                    ToastUtils.showToast(this, "You can set up to 5 filters!");
                    return;
                }
                View v = LayoutInflater.from(ScanFilterActivity.this).inflate(R.layout.item_raw_data_filter, llRawDataFilter, false);
                llRawDataFilter.addView(v);
                break;
            case R.id.iv_raw_data_del:
                final int c = llRawDataFilter.getChildCount();
                if (c == 0) {
                    ToastUtils.showToast(this, "There are currently no filters to delete");
                    return;
                }
                FilterDelDialog dialog = new FilterDelDialog(this);
                dialog.setListener(new FilterDelDialog.FilterDelListener() {
                    @Override
                    public void onConfirmClick(FilterDelDialog dialog) {
                        if (c > 0) {
                            llRawDataFilter.removeViewAt(c - 1);
                        }
                        dialog.dismiss();
                    }
                });
                dialog.show();
                break;
        }
    }

    private boolean isVaild() {
        if (isFilterNameOpen) {
            // 发送设置的过滤名字
            mFilterName = etFilterName.getText().toString();
            if (TextUtils.isEmpty(mFilterName))
                return false;
        } else {
            mFilterName = "";
        }
        if (isFilterMacOpen) {
            // 发送设置的过滤MAC
            mFilterMac = etFilterMac.getText().toString();
            if (TextUtils.isEmpty(mFilterMac))
                return false;
            if (mFilterMac.length() != 12)
                return false;
        } else {
            mFilterMac = "";
        }
        filterRawDatas = new ArrayList<>();
        mFilterRawDataSumLength = 0;
        if (isFilterRawDataOpen) {
            // 发送设置的过滤RawData
            int count = llRawDataFilter.getChildCount();
            if (count == 0)
                return false;

            for (int i = 0; i < count; i++) {
                FilterRawData rawData = new FilterRawData();
                View v = llRawDataFilter.getChildAt(i);
                EditText etDataType = v.findViewById(R.id.et_data_type);
                EditText etMin = v.findViewById(R.id.et_min);
                EditText etMax = v.findViewById(R.id.et_max);
                EditText etRawData = v.findViewById(R.id.et_raw_data);
                final String dataTypeStr = etDataType.getText().toString();
                final String minStr = etMin.getText().toString();
                final String maxStr = etMax.getText().toString();
                final String rawDataStr = etRawData.getText().toString();

                if (TextUtils.isEmpty(dataTypeStr))
                    return false;

                final int dataType = Integer.parseInt(dataTypeStr, 16);
                final DataTypeEnum dataTypeEnum = DataTypeEnum.fromDataType(dataType);
                if (dataTypeEnum == null)
                    return false;
                if (TextUtils.isEmpty(rawDataStr))
                    return false;
                int length = rawDataStr.length();
                if (length % 2 != 0)
                    return false;
                int min = 0;
                if (!TextUtils.isEmpty(minStr))
                    min = Integer.parseInt(minStr);
                int max = 0;
                if (!TextUtils.isEmpty(maxStr))
                    max = Integer.parseInt(maxStr);
                if (min == 0 && max != 0)
                    return false;
                if (min > 29)
                    return false;
                if (max > 29)
                    return false;
                if (max < min)
                    return false;
                if (min > 0) {
                    int interval = max - min;
                    if (length != ((interval + 1) * 2))
                        return false;
                }
                rawData.rawDataLength = 3 + length / 2;
                rawData.deviceType = dataType;
                rawData.min = min;
                rawData.max = max;
                rawData.rawData = rawDataStr;
                mFilterRawDataSumLength += rawData.rawDataLength + 1;
                filterRawDatas.add(rawData);
            }
        }
        return true;
    }

    private void setFilterRSSI() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = 0;
        byte[] message = MQTTMessageAssembler.assembleWriteFilterRSSI(mMokoDevice.uniqueId, mFilterRSSI);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void setFilterName() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = 1;
        byte[] message = MQTTMessageAssembler.assembleWriteFilterName(mMokoDevice.uniqueId, mFilterName);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setFilterMac() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = 2;
        byte[] message = MQTTMessageAssembler.assembleWriteFilterMAC(mMokoDevice.uniqueId, mFilterMac);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setFilterRawData() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = 3;
        byte[] message = MQTTMessageAssembler.assembleWriteFilterRawData(mMokoDevice.uniqueId, mFilterRawDataSumLength, filterRawDatas);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getFilterRSSI() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = -1;
        byte[] message = MQTTMessageAssembler.assembleReadFilterRSSI(mMokoDevice.uniqueId);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getFilterName() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = -1;
        byte[] message = MQTTMessageAssembler.assembleReadFilterName(mMokoDevice.uniqueId);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getFilterMac() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = -1;
        byte[] message = MQTTMessageAssembler.assembleReadFilterMac(mMokoDevice.uniqueId);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getFilterRawData() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        mPublishType = -1;
        byte[] message = MQTTMessageAssembler.assembleReadFilterRawData(mMokoDevice.uniqueId);
        try {
            mokoService.publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public MessageHandler mHandler;

    public class MessageHandler extends BaseMessageHandler<ScanFilterActivity> {

        public MessageHandler(ScanFilterActivity activity) {
            super(activity);
        }

        @Override
        protected void handleMessage(ScanFilterActivity activity, Message msg) {
        }
    }
}
