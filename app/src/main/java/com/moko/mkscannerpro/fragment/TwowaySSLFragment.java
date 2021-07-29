package com.moko.mkscannerpro.fragment;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.utils.FileUtils;
import com.moko.mkscannerpro.utils.ToastUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TwowaySSLFragment extends Fragment {

    private static final String TAG = TwowaySSLFragment.class.getSimpleName();
    @BindView(R.id.tv_ca_file)
    TextView tvCaFile;
    @BindView(R.id.tv_client_key_file)
    TextView tvClientKeyFile;
    @BindView(R.id.tv_client_cert_file)
    TextView tvClientCertFile;
    @BindView(R.id.iv_delete)
    ImageView ivDelete;


    private BaseActivity activity;


    public TwowaySSLFragment() {
    }

    public static TwowaySSLFragment newInstance() {
        TwowaySSLFragment fragment = new TwowaySSLFragment();
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
        View view = inflater.inflate(R.layout.fragment_ssl_twoway, container, false);
        ButterKnife.bind(this, view);
        activity = (BaseActivity) getActivity();
        tvCaFile.setText(caFile);
        if (!TextUtils.isEmpty(caFile)) {
            ivDelete.setVisibility(View.VISIBLE);
        }
        tvClientKeyFile.setText(clientKeyFile);
        tvClientCertFile.setText(clientCertFile);
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

    @OnClick({R.id.iv_select_ca_file, R.id.iv_select_client_key_file, R.id.iv_select_client_cert_file, R.id.iv_delete})
    public void onViewClicked(View v) {
        switch (v.getId()) {
            case R.id.iv_select_ca_file:
                // 选择文件
                Intent caIntent = new Intent(Intent.ACTION_GET_CONTENT);
                caIntent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                caIntent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(caIntent, "select file first!"), AppConstants.REQUEST_CODE_CA_FILE);
                } catch (ActivityNotFoundException ex) {
                    ToastUtils.showToast(activity, "install file manager app");
                }
                break;
            case R.id.iv_select_client_key_file:
                // 选择文件
                Intent keyIntent = new Intent(Intent.ACTION_GET_CONTENT);
                keyIntent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                keyIntent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(keyIntent, "select file first!"), AppConstants.REQUEST_CODE_CLIENT_KEY_FILE);
                } catch (ActivityNotFoundException ex) {
                    ToastUtils.showToast(activity, "install file manager app");
                }
                break;
            case R.id.iv_select_client_cert_file:
                // 选择文件
                Intent certIntent = new Intent(Intent.ACTION_GET_CONTENT);
                certIntent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                certIntent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(certIntent, "select file first!"), AppConstants.REQUEST_CODE_CLIENT_CEAR_FILE);
                } catch (ActivityNotFoundException ex) {
                    ToastUtils.showToast(activity, "install file manager app");
                }
                break;
            case R.id.iv_delete:
                tvCaFile.setText("");
                ivDelete.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case AppConstants.REQUEST_CODE_CA_FILE:
                if (resultCode == activity.RESULT_OK) {
                    //得到uri，后面就是将uri转化成file的过程。
                    Uri uri = data.getData();
                    String firmwareFilePath = FileUtils.getPath(activity, uri);
                    //
                    final File firmwareFile = new File(firmwareFilePath);
                    if (firmwareFile.exists()) {
                        tvCaFile.setText(firmwareFilePath);
                        ivDelete.setVisibility(View.VISIBLE);
                    } else {
                        ToastUtils.showToast(activity, "file is not exists!");
                    }
                }
                break;
            case AppConstants.REQUEST_CODE_CLIENT_KEY_FILE:
                if (resultCode == activity.RESULT_OK) {
                    //得到uri，后面就是将uri转化成file的过程。
                    Uri uri = data.getData();
                    String firmwareFilePath = FileUtils.getPath(activity, uri);
                    //
                    final File firmwareFile = new File(firmwareFilePath);
                    if (firmwareFile.exists()) {
                        tvClientKeyFile.setText(firmwareFilePath);
                    } else {
                        ToastUtils.showToast(activity, "file is not exists!");
                    }
                }
                break;
            case AppConstants.REQUEST_CODE_CLIENT_CEAR_FILE:
                if (resultCode == activity.RESULT_OK) {
                    //得到uri，后面就是将uri转化成file的过程。
                    Uri uri = data.getData();
                    String firmwareFilePath = FileUtils.getPath(activity, uri);
                    //
                    final File firmwareFile = new File(firmwareFilePath);
                    if (firmwareFile.exists()) {
                        tvClientCertFile.setText(firmwareFilePath);
                    } else {
                        ToastUtils.showToast(activity, "file is not exists!");
                    }
                }
                break;
        }
    }

    private String caFile;
    private String clientKeyFile;
    private String clientCertFile;

    public void setCAFilePath(MQTTConfig mqttConfig) {
        caFile = mqttConfig.caPath;
    }

    public void setClientKeyPath(MQTTConfig mqttConfig) {
        clientKeyFile = mqttConfig.clientKeyPath;
    }

    public void setClientCertPath(MQTTConfig mqttConfig) {
        clientCertFile = mqttConfig.clientCertPath;
    }

    public String getCAFilePath() {
        return tvCaFile.getText().toString();
    }

    public String getClientKeyPath() {
        return tvClientKeyFile.getText().toString();
    }

    public String getClientCertPath() {
        return tvClientCertFile.getText().toString();
    }


}
