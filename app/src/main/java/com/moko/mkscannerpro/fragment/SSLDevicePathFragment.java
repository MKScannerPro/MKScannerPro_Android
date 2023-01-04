package com.moko.mkscannerpro.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.databinding.FragmentSslDevicePathBinding;
import com.moko.mkscannerpro.dialog.BottomDialog;
import com.moko.mkscannerpro.utils.ToastUtils;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;

public class SSLDevicePathFragment extends Fragment {

    private static final String TAG = SSLDevicePathFragment.class.getSimpleName();
    private FragmentSslDevicePathBinding mBind;


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
        mBind = FragmentSslDevicePathBinding.inflate(inflater, container, false);
        activity = (BaseActivity) getActivity();
        mBind.clCertificate.setVisibility(connectMode > 0 ? View.VISIBLE : View.GONE);
        mBind.cbSsl.setChecked(connectMode > 0);
        mBind.cbSsl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
                mBind.clCertificate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        values = new ArrayList<>();
        values.add("CA signed server certificate");
        values.add("CA certificate file");
        values.add("Self signed certificates");
        if (connectMode > 0) {
            selected = connectMode - 1;
            mBind.tvCertification.setText(values.get(selected));
        }
        if (selected == 0) {
            mBind.llCa.setVisibility(View.GONE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
            mBind.clCertServer.setVisibility(View.GONE);
        } else if (selected == 1) {
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
            mBind.clCertServer.setVisibility(View.VISIBLE);
        } else if (selected == 2) {
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.VISIBLE);
            mBind.llClientCert.setVisibility(View.VISIBLE);
            mBind.clCertServer.setVisibility(View.VISIBLE);
        }
        return mBind.getRoot();
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
            mBind.tvCertification.setText(values.get(selected));
            if (selected == 0) {
                connectMode = 1;
                mBind.llCa.setVisibility(View.GONE);
                mBind.llClientKey.setVisibility(View.GONE);
                mBind.llClientCert.setVisibility(View.GONE);
                mBind.clCertServer.setVisibility(View.GONE);
            } else if (selected == 1) {
                connectMode = 2;
                mBind.llCa.setVisibility(View.VISIBLE);
                mBind.llClientKey.setVisibility(View.GONE);
                mBind.llClientCert.setVisibility(View.GONE);
                mBind.clCertServer.setVisibility(View.VISIBLE);
            } else if (selected == 2) {
                connectMode = 3;
                mBind.llCa.setVisibility(View.VISIBLE);
                mBind.llClientKey.setVisibility(View.VISIBLE);
                mBind.llClientCert.setVisibility(View.VISIBLE);
                mBind.clCertServer.setVisibility(View.VISIBLE);
            }
        });
        dialog.show(activity.getSupportFragmentManager());
    }


    public boolean isValid() {
        final String host = mBind.etMqttHost.getText().toString();
        final String port = mBind.etMqttPort.getText().toString();
        final String caFile = mBind.etCaPath.getText().toString();
        final String clientKeyFile = mBind.etClientKeyPath.getText().toString();
        final String clientCertFile = mBind.etClientCertPath.getText().toString();
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
        final String host = mBind.etMqttHost.getText().toString();
        return host;
    }

    public int getSSLPort() {
        final String port = mBind.etMqttPort.getText().toString();
        int portInt = Integer.parseInt(port);
        return portInt;
    }

    public String getCAPath() {
        final String caFile = mBind.etCaPath.getText().toString();
        return caFile;
    }

    public String getClientCerPath() {
        final String clientCertFile = mBind.etClientCertPath.getText().toString();
        return clientCertFile;
    }

    public String getClientKeyPath() {
        final String clientKeyFile = mBind.etClientKeyPath.getText().toString();
        return clientKeyFile;
    }
}
