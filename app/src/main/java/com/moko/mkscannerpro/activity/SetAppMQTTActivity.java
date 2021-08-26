package com.moko.mkscannerpro.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.adapter.MQTTFragmentAdapter;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.dialog.AlertMessageDialog;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.fragment.GeneralFragment;
import com.moko.mkscannerpro.fragment.SSLFragment;
import com.moko.mkscannerpro.fragment.UserFragment;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.MQTTSupport;
import com.moko.support.event.MQTTConnectionCompleteEvent;
import com.moko.support.event.MQTTConnectionFailureEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import butterknife.BindView;
import butterknife.ButterKnife;

public class SetAppMQTTActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {
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
    private GeneralFragment generalFragment;
    private UserFragment userFragment;
    private SSLFragment sslFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;

    private MQTTConfig mqttConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_app);
        ButterKnife.bind(this);
        String MQTTConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        if (TextUtils.isEmpty(MQTTConfigStr)) {
            mqttConfig = new MQTTConfig();
        } else {
            Gson gson = new Gson();
            mqttConfig = gson.fromJson(MQTTConfigStr, MQTTConfig.class);
        }
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        etMqttHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etMqttClientId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etMqttSubscribeTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etMqttPublishTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
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
    }


    private void createFragment() {
        fragments = new ArrayList<>();
        generalFragment = GeneralFragment.newInstance();
        userFragment = UserFragment.newInstance();
        sslFragment = SSLFragment.newInstance();
        fragments.add(generalFragment);
        fragments.add(userFragment);
        fragments.add(sslFragment);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 10)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        String mqttConfigStr = new Gson().toJson(mqttConfig, MQTTConfig.class);
        SPUtiles.setStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, mqttConfigStr);
        etMqttHost.post(() -> {
            ToastUtils.showToast(SetAppMQTTActivity.this, getString(R.string.success));
            dismissLoadingProgressDialog();
            Intent intent = new Intent();
            intent.putExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_APP, mqttConfigStr);
            setResult(RESULT_OK, intent);
            finish();
        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionFailureEvent(MQTTConnectionFailureEvent event) {
        ToastUtils.showToast(SetAppMQTTActivity.this, getString(R.string.mqtt_connect_failed));
        dismissLoadingProgressDialog();
    }

    private void initData() {
        etMqttHost.setText(mqttConfig.host);
        etMqttPort.setText(mqttConfig.port);
        etMqttClientId.setText(mqttConfig.clientId);
        etMqttSubscribeTopic.setText(mqttConfig.topicSubscribe);
        etMqttPublishTopic.setText(mqttConfig.topicPublish);
        generalFragment.setCleanSession(mqttConfig.cleanSession);
        generalFragment.setQos(mqttConfig.qos);
        generalFragment.setKeepAlive(mqttConfig.keepAlive);
        userFragment.setUserName(mqttConfig.username);
        userFragment.setPassword(mqttConfig.password);
        sslFragment.setConnectMode(mqttConfig.connectMode);
        sslFragment.setCAPath(mqttConfig.caPath);
        sslFragment.setClientKeyPath(mqttConfig.clientKeyPath);
        sslFragment.setClientCertPath(mqttConfig.clientCertPath);
    }

    public void back(View view) {
        back();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage("Please confirm whether to save the modified parameters?");
        dialog.setConfirm("YES");
        dialog.setCancel("NO");
        dialog.setOnAlertConfirmListener(() -> {
            onSave(null);
        });
        dialog.setOnAlertCancelListener(() -> {
            finish();
        });
        dialog.show(getSupportFragmentManager());
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
        if (!generalFragment.isValid() || !sslFragment.isValid())
            return;
        mqttConfig.host = host;
        mqttConfig.port = port;
        mqttConfig.clientId = clientId;
        mqttConfig.cleanSession = generalFragment.isCleanSession();
        mqttConfig.qos = generalFragment.getQos();
        mqttConfig.keepAlive = generalFragment.getKeepAlive();
        mqttConfig.keepAlive = generalFragment.getKeepAlive();
        mqttConfig.topicSubscribe = etMqttSubscribeTopic.getText().toString().replaceAll(" ", "");
        mqttConfig.topicPublish = etMqttPublishTopic.getText().toString().replaceAll(" ", "");
        mqttConfig.username = userFragment.getUsername();
        mqttConfig.password = userFragment.getPassword();
        mqttConfig.connectMode = sslFragment.getConnectMode();
        mqttConfig.caPath = sslFragment.getCaPath();
        mqttConfig.clientKeyPath = sslFragment.getClientKeyPath();
        mqttConfig.clientCertPath = sslFragment.getClientCertPath();

        if (!mqttConfig.topicPublish.isEmpty() && !mqttConfig.topicSubscribe.isEmpty()
                && mqttConfig.topicPublish.equals(mqttConfig.topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return;
        }
        String mqttConfigStr = new Gson().toJson(mqttConfig, MQTTConfig.class);
        MQTTSupport.getInstance().disconnectMqtt();
        showLoadingProgressDialog();
        etMqttHost.postDelayed(() -> MQTTSupport.getInstance().connectMqtt(mqttConfigStr), 2000);
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
}
