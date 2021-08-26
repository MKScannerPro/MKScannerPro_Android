package com.moko.mkscannerpro.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.dialog.BottomDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MQTTConstants;
import com.moko.support.MQTTSupport;
import com.moko.support.entity.MsgConfigResult;
import com.moko.support.entity.MsgDeviceInfo;
import com.moko.support.entity.MsgNotify;
import com.moko.support.entity.OTAParams;
import com.moko.support.entity.OTAResult;
import com.moko.support.event.DeviceOnlineEvent;
import com.moko.support.event.MQTTMessageArrivedEvent;
import com.moko.support.handler.MQTTMessageAssembler;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OTAActivity extends BaseActivity {
    private final String FILTER_ASCII = "[ -~]*";

    public static String TAG = OTAActivity.class.getSimpleName();
    @BindView(R.id.et_host_content)
    EditText etHostContent;
    @BindView(R.id.et_host_port)
    EditText etHostPort;
    @BindView(R.id.et_host_catalogue)
    EditText etHostCatalogue;
    @BindView(R.id.tv_update_type)
    TextView tvUpdateType;


    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private ArrayList<String> mValues;
    private int mSelected;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota);
        ButterKnife.bind(this);
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        InputFilter inputFilter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etHostContent.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), inputFilter});
        etHostCatalogue.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), inputFilter});
        mHandler = new Handler(Looper.getMainLooper());
        String mqttConfigAppStr = SPUtiles.getStringValue(OTAActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mValues = new ArrayList<>();
        mValues.add("Firmware");
        mValues.add("CA certificate");
        mValues.add("Client certificate");
        mValues.add("Private key");
        tvUpdateType.setText(mValues.get(mSelected));
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_OTA_RESULT) {
            Type type = new TypeToken<MsgNotify<OTAResult>>() {
            }.getType();
            MsgNotify<OTAResult> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
            dismissLoadingProgressDialog();
//            mHandler.removeMessages(0);
            if (result.data.ota_result == 1) {
                ToastUtils.showToast(this, R.string.update_success);
            } else {
                ToastUtils.showToast(this, R.string.update_failed);
            }
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_OTA) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDevice.deviceId.equals(result.device_info.device_id)) {
                return;
            }
//            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.result_code == 0) {
                ToastUtils.showToast(this, "Set up succeed");
            } else {
                ToastUtils.showToast(this, "Set up failed");
            }
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

    public void startUpdate(View view) {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        String hostStr = etHostContent.getText().toString();
        String portStr = etHostPort.getText().toString();
        String catalogueStr = etHostCatalogue.getText().toString();
        if (TextUtils.isEmpty(hostStr)) {
            ToastUtils.showToast(this, R.string.mqtt_verify_host);
            return;
        }
        if (!TextUtils.isEmpty(portStr) && Integer.parseInt(portStr) > 65535) {
            ToastUtils.showToast(this, R.string.mqtt_verify_port_empty);
            return;
        }
        if (TextUtils.isEmpty(catalogueStr)) {
            ToastUtils.showToast(this, R.string.mqtt_verify_catalogue);
            return;
        }
        XLog.i("升级固件");
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 50 * 1000);
        showLoadingProgressDialog();
        setOTA(hostStr, Integer.parseInt(portStr), catalogueStr);
    }

    public void updateType(View view) {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mValues, mSelected);
        dialog.setListener(value -> {
            mSelected = value;
            tvUpdateType.setText(mValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }

    private void setOTA(String host, int port, String catalogue) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.device_id = mMokoDevice.deviceId;
        deviceInfo.mac = mMokoDevice.mac;
        OTAParams params = new OTAParams();
        params.file_type = mSelected;
        params.domain_name = host;
        params.port = port;
        params.file_way = catalogue;
        String message = MQTTMessageAssembler.assembleWriteOTA(deviceInfo, params);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_OTA, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
