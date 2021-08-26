package com.moko.mkscannerpro.fragment;

import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.base.BaseActivity;

import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class UserFragment extends Fragment {
    private final String FILTER_ASCII = "[ -~]*";
    private static final String TAG = UserFragment.class.getSimpleName();
    @BindView(R.id.et_mqtt_username)
    EditText etMqttUsername;
    @BindView(R.id.et_mqtt_password)
    EditText etMqttPassword;


    private BaseActivity activity;
    private String username;
    private String password;

    public UserFragment() {
    }

    public static UserFragment newInstance() {
        UserFragment fragment = new UserFragment();
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
        View view = inflater.inflate(R.layout.fragment_user_app, container, false);
        ButterKnife.bind(this, view);
        activity = (BaseActivity) getActivity();
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etMqttUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), filter});
        etMqttPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), filter});
        etMqttUsername.setText(username);
        etMqttPassword.setText(password);
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

    public void setUserName(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        String username = etMqttUsername.getText().toString();
        return username;
    }

    public String getPassword() {
        String password = etMqttPassword.getText().toString();
        return password;
    }
}
