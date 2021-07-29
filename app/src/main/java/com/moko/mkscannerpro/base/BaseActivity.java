package com.moko.mkscannerpro.base;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import android.view.Window;

import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.activity.GuideActivity;
import com.moko.support.log.LogModule;

public class BaseActivity extends FragmentActivity {
    private FinishBroadCastReceiver mBroadCastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBroadCastReceiver = new FinishBroadCastReceiver(this);
        if (savedInstanceState != null) {
            Intent intent = new Intent(this, GuideActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadCastReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LogModule.i("onConfigurationChanged...");
        finish();
    }

    protected void finishActivity(Class<? extends Activity> clazz) {
        final Intent intent = new Intent(AppConstants.ACTION_FINISH_ACTIVITY);
        intent.putExtra("className", clazz.getName());
        sendBroadcast(intent);
    }

    protected final void finishActivities(Class<? extends Activity>... clazzs) {
        for (Class<? extends Activity> clazz : clazzs) {
            finishActivity(clazz);
        }
    }

    class FinishBroadCastReceiver extends BaseBroadcastReceiver {

        public FinishBroadCastReceiver(Context context) {
            super(context);
        }

        @Override
        public IntentFilter getIntentFilter() {
            return new IntentFilter(AppConstants.ACTION_FINISH_ACTIVITY);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String className = intent.getStringExtra("className");
            if (className != null && className.equals(BaseActivity.this.getClass().getName())
                    && !BaseActivity.this.isFinishing()) {
                BaseActivity.this.finish();
            }
        }
    }

    // 记录上次页面控件点击时间,屏蔽无效点击事件
    protected long mLastOnClickTime = 0;

    public boolean isWindowLocked() {
        long current = SystemClock.elapsedRealtime();
        if (current - mLastOnClickTime > 500) {
            mLastOnClickTime = current;
            return false;
        } else {
            return true;
        }
    }

    private ProgressDialog mLoadingDialog;

    public void showLoadingProgressDialog(String loadingText) {
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mLoadingDialog.setCanceledOnTouchOutside(false);
        mLoadingDialog.setCancelable(true);
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mLoadingDialog.setMessage(loadingText);
        if (!isFinishing() && mLoadingDialog != null && !mLoadingDialog.isShowing()) {
            mLoadingDialog.show();
        }
    }

    public void dismissLoadingProgressDialog() {
        if (!isFinishing() && mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    public boolean isSMSPermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isConstactsPermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isPhoneStatePermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isCameraPermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isWriteStoragePermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationPermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
