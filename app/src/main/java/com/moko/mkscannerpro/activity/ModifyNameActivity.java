package com.moko.mkscannerpro.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;
import com.moko.mkscannerpro.db.DBTools;
import com.moko.mkscannerpro.entity.MokoDevice;
import com.moko.mkscannerpro.utils.ToastUtils;
import com.moko.support.event.MQTTConnectionCompleteEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;


public class ModifyNameActivity extends BaseActivity {
    private final String FILTER_ASCII = "[ -~]*";
    public static String TAG = ModifyNameActivity.class.getSimpleName();

    @BindView(R.id.et_nick_name)
    EditText etNickName;
    private MokoDevice device;
    private InputFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_device_name);
        ButterKnife.bind(this);
        device = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etNickName.setText(device.nickName);
        etNickName.setSelection(etNickName.getText().toString().length());
        etNickName.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(20)});
        etNickName.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager inputManager = (InputMethodManager) etNickName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(etNickName, 0);
            }
        }, 300);
    }


    public void modifyDone(View view) {
        String nickName = etNickName.getText().toString();
        if (TextUtils.isEmpty(nickName)) {
            ToastUtils.showToast(this, R.string.modify_device_name_empty);
            return;
        }
        device.nickName = nickName;
        DBTools.getInstance(this).updateDevice(device);
        // 跳转首页，刷新数据
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_ID, device.deviceId);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
    }
}
