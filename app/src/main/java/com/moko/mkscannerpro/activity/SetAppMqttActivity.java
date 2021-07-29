package com.moko.mkscannerpro.activity;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.dialog.KeepAliveDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.fragment.OnewaySSLFragment;
import com.moko.mkscannerpro.fragment.TwowaySSLFragment;
import com.moko.mkscannerpro.service.MokoService;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MokoConstants;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @Date 2018/6/7
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.mkscannerpro.activity.SetAppMqttActivity
 */
public class SetAppMqttActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {


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
    @BindView(R.id.et_topic_subscribe)
    EditText etTopicSubscribe;
    @BindView(R.id.et_topic_publish)
    EditText etTopicPublish;

    private FragmentManager fragmentManager;
    private OnewaySSLFragment onewaySSLFragment;
    private TwowaySSLFragment twowaySSLFragment;


    private String[] mQosArray = new String[]{"0", "1", "2"};


    private MQTTConfig mqttConfig;
    private MokoService mokoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_app);
        ButterKnife.bind(this);
        String mqttConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        if (TextUtils.isEmpty(mqttConfigStr)) {
            mqttConfig = new MQTTConfig();
        } else {
            Gson gson = new Gson();
            mqttConfig = gson.fromJson(mqttConfigStr, MQTTConfig.class);
        }
        fragmentManager = getFragmentManager();
        createFragment();
        initData();
        bindService(new Intent(this, MokoService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mokoService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_MQTT_CONNECTION);
            registerReceiver(mReceiver, filter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void createFragment() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        onewaySSLFragment = OnewaySSLFragment.newInstance();
        fragmentTransaction.add(R.id.frame_connect_mode, onewaySSLFragment);
        twowaySSLFragment = TwowaySSLFragment.newInstance();
        fragmentTransaction.add(R.id.frame_connect_mode, twowaySSLFragment);
        fragmentTransaction.hide(onewaySSLFragment).hide(twowaySSLFragment).commit();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MokoConstants.ACTION_MQTT_CONNECTION.equals(action)) {
                int state = intent.getIntExtra(MokoConstants.EXTRA_MQTT_CONNECTION_STATE, 0);
                if (state == MokoConstants.MQTT_CONN_STATUS_SUCCESS) {
                    ToastUtils.showToast(SetAppMqttActivity.this, getString(R.string.success));
                    dismissLoadingProgressDialog();
                    SetAppMqttActivity.this.finish();
                } else if (state == MokoConstants.MQTT_CONN_STATUS_FAILED) {
                    ToastUtils.showToast(SetAppMqttActivity.this, getString(R.string.mqtt_connect_failed));
                    dismissLoadingProgressDialog();
                }
            }
        }
    };

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
        etMqttUsername.setText(mqttConfig.username);
        etMqttPassword.setText(mqttConfig.password);
        etTopicSubscribe.setText(mqttConfig.topicSubscribe);
        etTopicPublish.setText(mqttConfig.topicPublish);
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
        if (!mqttConfig.topicPublish.isEmpty() && !mqttConfig.topicSubscribe.isEmpty()
                && mqttConfig.topicPublish.equals(mqttConfig.topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return;
        }

        String mqttConfigStr = new Gson().toJson(mqttConfig, MQTTConfig.class);
        SPUtiles.setStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, mqttConfigStr);
        mokoService.disconnectMqtt();
        showLoadingProgressDialog(getString(R.string.mqtt_connecting));
        tvKeepAlive.postDelayed(new Runnable() {
            @Override
            public void run() {
                mokoService.connectMqtt();
            }
        }, 2000);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(serviceConnection);
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

}
