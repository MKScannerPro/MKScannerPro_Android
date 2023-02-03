package com.moko.mkscannerpro.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.moko.mkremotegw.activity.RemoteMainActivity;
import com.moko.mkscannergw.activity.ScannerMainActivity;
import com.moko.mkscannerpro.databinding.ActivityMainBinding;
import com.moko.mkscannerpro.utils.Utils;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    public Handler mHandler;

    @Override
    protected void onCreate() {
        mHandler = new Handler(Looper.getMainLooper());
        StringBuffer buffer = new StringBuffer();
        // 记录机型
        buffer.append("机型：");
        buffer.append(android.os.Build.MODEL);
        buffer.append("=====");
        // 记录版本号
        buffer.append("手机系统版本：");
        buffer.append(android.os.Build.VERSION.RELEASE);
        buffer.append("=====");
        // 记录APP版本
        buffer.append("APP版本：");
        buffer.append(Utils.getVersionInfo(this));
        XLog.d(buffer.toString());
    }

    @Override
    protected ActivityMainBinding getViewBinding() {
        return ActivityMainBinding.inflate(getLayoutInflater());
    }


    public void about(View view) {
        if (isWindowLocked()) return;
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void onScannerGW(View view) {
        if (isWindowLocked()) return;
        XLog.i("打开Scanner Gateway");
        startActivity(new Intent(this, ScannerMainActivity.class));
    }

    public void onRemoteGW(View view) {
        if (isWindowLocked()) return;
        XLog.i("打开Remote Gateway");
        startActivity(new Intent(this, RemoteMainActivity.class));
    }
}
