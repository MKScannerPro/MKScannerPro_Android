package com.moko.mkscannerpro.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.adapter.MQTTFragmentAdapter;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.databinding.ActivityMqttAppBinding;
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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

public class SetAppMQTTActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {
    private final String FILTER_ASCII = "[ -~]*";
    private ActivityMqttAppBinding mBind;

    private GeneralFragment generalFragment;
    private UserFragment userFragment;
    private SSLFragment sslFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;

    private MQTTConfig mqttConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = ActivityMqttAppBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
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
        mBind.etMqttHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etMqttClientId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etMqttSubscribeTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        mBind.etMqttPublishTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        createFragment();
        initData();
        adapter = new MQTTFragmentAdapter(this);
        adapter.setFragmentList(fragments);
        mBind.vpMqtt.setAdapter(adapter);
        mBind.vpMqtt.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    mBind.rbGeneral.setChecked(true);
                } else if (position == 1) {
                    mBind.rbUser.setChecked(true);
                } else if (position == 2) {
                    mBind.rbSsl.setChecked(true);
                }
            }
        });
        mBind.vpMqtt.setOffscreenPageLimit(3);
        mBind.rgMqtt.setOnCheckedChangeListener(this);
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
        ToastUtils.showToast(SetAppMQTTActivity.this, getString(R.string.success));
        dismissLoadingProgressDialog();
        Intent intent = new Intent();
        intent.putExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_APP, mqttConfigStr);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionFailureEvent(MQTTConnectionFailureEvent event) {
        ToastUtils.showToast(SetAppMQTTActivity.this, getString(R.string.mqtt_connect_failed));
        dismissLoadingProgressDialog();
        finish();
    }

    private void initData() {
        mBind.etMqttHost.setText(mqttConfig.host);
        mBind.etMqttPort.setText(mqttConfig.port);
        mBind.etMqttClientId.setText(mqttConfig.clientId);
        mBind.etMqttSubscribeTopic.setText(mqttConfig.topicSubscribe);
        mBind.etMqttPublishTopic.setText(mqttConfig.topicPublish);
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
                mBind.vpMqtt.setCurrentItem(0);
                break;
            case R.id.rb_user:
                mBind.vpMqtt.setCurrentItem(1);
                break;
            case R.id.rb_ssl:
                mBind.vpMqtt.setCurrentItem(2);
                break;
        }
    }

    public void onSave(View view) {
        String host = mBind.etMqttHost.getText().toString().replaceAll(" ", "");
        String port = mBind.etMqttPort.getText().toString();
        String clientId = mBind.etMqttClientId.getText().toString().replaceAll(" ", "");

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
        mqttConfig.topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString().replaceAll(" ", "");
        mqttConfig.topicPublish = mBind.etMqttPublishTopic.getText().toString().replaceAll(" ", "");
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
        mBind.etMqttHost.postDelayed(() -> {
            try {
                MQTTSupport.getInstance().connectMqtt(mqttConfigStr);
            } catch (FileNotFoundException e) {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "The SSL certificates path is invalid, please select a valid file path and save it.");
                // 读取stacktrace信息
                final Writer result = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(result);
                e.printStackTrace(printWriter);
                StringBuffer errorReport = new StringBuffer();
                errorReport.append(result.toString());
                XLog.e(errorReport.toString());
            }
        }, 2000);
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
