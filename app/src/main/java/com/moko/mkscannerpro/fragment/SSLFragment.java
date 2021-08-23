package com.moko.mkscannerpro.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.dialog.BottomDialog;
import com.moko.mkscannerpro.utils.FileUtils;
import com.moko.mkscannerpro.utils.ToastUtils;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class SSLFragment extends Fragment {
    public static final int REQUEST_CODE_SELECT_CA = 0x10;
    public static final int REQUEST_CODE_SELECT_CLIENT_KEY = 0x11;
    public static final int REQUEST_CODE_SELECT_CLIENT_CERT = 0x12;

    private static final String TAG = SSLFragment.class.getSimpleName();
    @BindView(R.id.cb_ssl)
    CheckBox cbSsl;
    @BindView(R.id.tv_certification)
    TextView tvCertification;
    @BindView(R.id.tv_ca_file)
    TextView tvCaFile;
    @BindView(R.id.ll_ca)
    LinearLayout llCa;
    @BindView(R.id.tv_client_key_file)
    TextView tvClientKeyFile;
    @BindView(R.id.ll_client_key)
    LinearLayout llClientKey;
    @BindView(R.id.tv_client_cert_file)
    TextView tvClientCertFile;
    @BindView(R.id.ll_client_cert)
    LinearLayout llClientCert;
    @BindView(R.id.cl_certificate)
    ConstraintLayout clCertificate;


    private BaseActivity activity;

    private int connectMode;

    private String caPath;
    private String clientKeyPath;
    private String clientCertPath;

    private ArrayList<String> values;
    private int selected;

    public SSLFragment() {
    }

    public static SSLFragment newInstance() {
        SSLFragment fragment = new SSLFragment();
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
        View view = inflater.inflate(R.layout.fragment_ssl_app, container, false);
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
            tvCaFile.setText(caPath);
            tvClientKeyFile.setText(clientKeyPath);
            tvClientCertFile.setText(clientCertPath);
            tvCertification.setText(values.get(selected));
        }
        if (selected == 0) {
            llCa.setVisibility(View.GONE);
            llClientKey.setVisibility(View.GONE);
            llClientCert.setVisibility(View.GONE);
        } else if (selected == 1) {
            llCa.setVisibility(View.VISIBLE);
            llClientKey.setVisibility(View.GONE);
            llClientCert.setVisibility(View.GONE);
        } else if (selected == 2) {
            llCa.setVisibility(View.VISIBLE);
            llClientKey.setVisibility(View.VISIBLE);
            llClientCert.setVisibility(View.VISIBLE);
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

    public void setConnectMode(int connectMode) {
        this.connectMode = connectMode;
    }

    public void setCAPath(String caPath) {
        this.caPath = caPath;
    }

    public void setClientKeyPath(String clientKeyPath) {
        this.clientKeyPath = clientKeyPath;
    }

    public void setClientCertPath(String clientCertPath) {
        this.clientCertPath = clientCertPath;
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
            } else if (selected == 1) {
                connectMode = 2;
                llCa.setVisibility(View.VISIBLE);
                llClientKey.setVisibility(View.GONE);
                llClientCert.setVisibility(View.GONE);
            } else if (selected == 2) {
                connectMode = 3;
                llCa.setVisibility(View.VISIBLE);
                llClientKey.setVisibility(View.VISIBLE);
                llClientCert.setVisibility(View.VISIBLE);
            }
        });
        dialog.show(activity.getSupportFragmentManager());
    }

    public void selectCAFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_SELECT_CA);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(activity, "install file manager app");
        }
    }

    public void selectKeyFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_SELECT_CLIENT_KEY);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(activity, "install file manager app");
        }
    }

    public void selectCertFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_SELECT_CLIENT_CERT);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(activity, "install file manager app");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != activity.RESULT_OK)
            return;
        //得到uri，后面就是将uri转化成file的过程。
        Uri uri = data.getData();
        String filePath = FileUtils.getPath(activity, uri);
        if (TextUtils.isEmpty(filePath)) {
            ToastUtils.showToast(activity, "file path error!");
            return;
        }
        final File file = new File(filePath);
        if (file.exists()) {
            if (requestCode == REQUEST_CODE_SELECT_CA) {
                caPath = filePath;
                tvCaFile.setText(filePath);
            }
            if (requestCode == REQUEST_CODE_SELECT_CLIENT_KEY) {
                clientKeyPath = filePath;
                tvClientKeyFile.setText(filePath);
            }
            if (requestCode == REQUEST_CODE_SELECT_CLIENT_CERT) {
                clientCertPath = filePath;
                tvClientCertFile.setText(filePath);
            }
        } else {
            ToastUtils.showToast(activity, "file is not exists!");
        }
    }

    public boolean isValid() {
        final String caFile = tvCaFile.getText().toString();
        final String clientKeyFile = tvClientKeyFile.getText().toString();
        final String clientCertFile = tvClientCertFile.getText().toString();
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

    public String getCaPath() {
        return caPath;
    }

    public String getClientKeyPath() {
        return clientKeyPath;
    }

    public String getClientCertPath() {
        return clientCertPath;
    }
}
