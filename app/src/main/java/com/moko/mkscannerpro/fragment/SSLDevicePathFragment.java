package com.moko.mkscannerpro.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.dialog.BottomDialog;
import com.moko.mkscannerpro.utils.ToastUtils;

import java.util.ArrayList;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class SSLDevicePathFragment extends Fragment {

    private static final String TAG = SSLDevicePathFragment.class.getSimpleName();
    @BindView(R.id.cb_ssl)
    CheckBox cbSsl;
    @BindView(R.id.tv_certification)
    TextView tvCertification;
    @BindView(R.id.ll_ca)
    LinearLayout llCa;
    @BindView(R.id.ll_client_key)
    LinearLayout llClientKey;
    @BindView(R.id.ll_client_cert)
    LinearLayout llClientCert;
    @BindView(R.id.cl_certificate)
    ConstraintLayout clCertificate;
    @BindView(R.id.et_mqtt_host)
    EditText etMqttHost;
    @BindView(R.id.et_mqtt_port)
    EditText etMqttPort;
    @BindView(R.id.cl_cert_server)
    ConstraintLayout clCertServer;
    @BindView(R.id.et_ca_path)
    EditText etCaPath;
    @BindView(R.id.et_client_key_path)
    EditText etClientKeyPath;
    @BindView(R.id.et_client_cert_path)
    EditText etClientCertPath;


    private BaseActivity activity;

    private int connectMode = 0;

    private ArrayList<String> values;
    private int selected;

    public SSLDevicePathFragment() {
    }

    public static SSLDevicePathFragment newInstance() {
        SSLDevicePathFragment fragment = new SSLDevicePathFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_ssl_device_path, container, false);
        ButterKnife.bind(this, view);
        activity = (BaseActivity) getActivity();
        clCertificate.setVisibility(connectMode > 0 ? View.VISIBLE : View.GONE);
        cbSsl.setChecked(connectMode > 0);
        cbSsl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    connectMode = 0;
                } else {
                    if (selected == 0) {
                        connectMode = 1;
                    } else if (selected == 1) {
                        connectMode = 2;
                    } else if (selected == 2) {
                        connectMode = 3;
                    }
                }
                clCertificate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        values = new ArrayList<>();
        values.add("CA signed server certificate");
        values.add("CA certificate file");
        values.add("Self signed certificates");
        if (connectMode > 0) {
            selected = connectMode - 1;
            tvCertification.setText(values.get(selected));
        }
        if (selected == 0) {
            llCa.setVisibility(View.GONE);
            llClientKey.setVisibility(View.GONE);
            llClientCert.setVisibility(View.GONE);
            clCertServer.setVisibility(View.GONE);
        } else if (selected == 1) {
            llCa.setVisibility(View.VISIBLE);
            llClientKey.setVisibility(View.GONE);
            llClientCert.setVisibility(View.GONE);
            clCertServer.setVisibility(View.VISIBLE);
        } else if (selected == 2) {
            llCa.setVisibility(View.VISIBLE);
            llClientKey.setVisibility(View.VISIBLE);
            llClientCert.setVisibility(View.VISIBLE);
            clCertServer.setVisibility(View.VISIBLE);
        }
        return view;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
    }


    public void selectCertificate() {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(values, selected);
        dialog.setListener(value -> {
            selected = value;
            tvCertification.setText(values.get(selected));
            if (selected == 0) {
                connectMode = 1;
                llCa.setVisibility(View.GONE);
                llClientKey.setVisibility(View.GONE);
                llClientCert.setVisibility(View.GONE);
                clCertServer.setVisibility(View.GONE);
            } else if (selected == 1) {
                connectMode = 2;
                llCa.setVisibility(View.VISIBLE);
                llClientKey.setVisibility(View.GONE);
                llClientCert.setVisibility(View.GONE);
                clCertServer.setVisibility(View.VISIBLE);
            } else if (selected == 2) {
                connectMode = 3;
                llCa.setVisibility(View.VISIBLE);
                llClientKey.setVisibility(View.VISIBLE);
                llClientCert.setVisibility(View.VISIBLE);
                clCertServer.setVisibility(View.VISIBLE);
            }
        });
        dialog.show(activity.getSupportFragmentManager());
    }


    public boolean isValid() {
        final String host = etMqttHost.getText().toString();
        final String port = etMqttPort.getText().toString();
        final String caFile = etCaPath.getText().toString();
        final String clientKeyFile = etClientKeyPath.getText().toString();
        final String clientCertFile = etClientCertPath.getText().toString();
        if (connectMode > 1) {
            if (TextUtils.isEmpty(host)) {
                ToastUtils.showToast(activity, "Host error");
                return false;
            }
            if (TextUtils.isEmpty(port)) {
                ToastUtils.showToast(activity, "Port error");
                return false;
            }
            int portInt = Integer.parseInt(port);
            if (portInt > 65535) {
                ToastUtils.showToast(activity, "Port error");
                return false;
            }
        }
        if (connectMode == 2) {
            if (TextUtils.isEmpty(caFile)) {
                ToastUtils.showToast(activity, getString(R.string.mqtt_verify_ca));
                return false;
            }
        } else if (connectMode == 3) {
            if (TextUtils.isEmpty(caFile)) {
                ToastUtils.showToast(activity, getString(R.string.mqtt_verify_ca));
                return false;
            }
            if (TextUtils.isEmpty(clientKeyFile)) {
                ToastUtils.showToast(activity, getString(R.string.mqtt_verify_client_key));
                return false;
            }
            if (TextUtils.isEmpty(clientCertFile)) {
                ToastUtils.showToast(activity, getString(R.string.mqtt_verify_client_cert));
                return false;
            }
        }
        return true;
    }

    public int getConnectMode() {
        return connectMode;
    }

    public String getSSLHost() {
        final String host = etMqttHost.getText().toString();
        return host;
    }

    public int getSSLPort() {
        final String port = etMqttPort.getText().toString();
        int portInt = Integer.parseInt(port);
        return portInt;
    }

    public String getCAPath() {
        final String caFile = etCaPath.getText().toString();
        return caFile;
    }

    public String getClientCerPath() {
        final String clientCertFile = etClientCertPath.getText().toString();
        return clientCertFile;
    }

    public String getClientKeyPath() {
        final String clientKeyFile = etClientKeyPath.getText().toString();
        return clientKeyFile;
    }
}
