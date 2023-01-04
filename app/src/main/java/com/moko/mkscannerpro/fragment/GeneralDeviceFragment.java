package com.moko.mkscannerpro.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.databinding.FragmentGeneralDeviceBinding;
import com.moko.mkscannerpro.utils.ToastUtils;

import androidx.fragment.app.Fragment;

public class GeneralDeviceFragment extends Fragment {

    private static final String TAG = GeneralDeviceFragment.class.getSimpleName();
    private FragmentGeneralDeviceBinding mBind;
    private BaseActivity activity;

    private boolean cleanSession;
    private int qos;
    private int keepAlive;

    public GeneralDeviceFragment() {
    }

    public static GeneralDeviceFragment newInstance() {
        GeneralDeviceFragment fragment = new GeneralDeviceFragment();
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
        mBind = FragmentGeneralDeviceBinding.inflate(inflater, container, false);
        activity = (BaseActivity) getActivity();
        mBind.cbCleanSession.setChecked(cleanSession);
        if (qos == 0) {
            mBind.rbQos1.setChecked(true);
        } else if (qos == 1) {
            mBind.rbQos2.setChecked(true);
        } else if (qos == 2) {
            mBind.rbQos3.setChecked(true);
        }
        mBind.etKeepAlive.setText(String.valueOf(keepAlive));
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

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void setCleanSession() {
        mBind.cbCleanSession.setChecked(cleanSession);
    }

    public void setQos() {
        if (qos == 0) {
            mBind.rbQos1.setChecked(true);
        } else if (qos == 1) {
            mBind.rbQos2.setChecked(true);
        } else if (qos == 2) {
            mBind.rbQos3.setChecked(true);
        }
    }

    public void setKeepAlive() {
        mBind.etKeepAlive.setText(String.valueOf(keepAlive));
    }

    public boolean isValid() {
        final String keepAliveStr = mBind.etKeepAlive.getText().toString();
        if (TextUtils.isEmpty(keepAliveStr)) {
            ToastUtils.showToast(getActivity(), "Error");
            return false;
        }
        final int keepAlive = Integer.parseInt(keepAliveStr);
        if (keepAlive < 10 || keepAlive > 120) {
            ToastUtils.showToast(getActivity(), "Keep Alive range is 10-120");
            return false;
        }
        return true;
    }

    public boolean isCleanSession() {
        return mBind.cbCleanSession.isChecked();
    }

    public int getQos() {
        int qos = 0;
        if (mBind.rbQos2.isChecked()) {
            qos = 1;
        } else if (mBind.rbQos3.isChecked()) {
            qos = 2;
        }
        return qos;
    }

    public int getKeepAlive() {
        String keepAliveStr = mBind.etKeepAlive.getText().toString();
        int keepAlive = Integer.parseInt(keepAliveStr);
        return keepAlive;
    }
}
