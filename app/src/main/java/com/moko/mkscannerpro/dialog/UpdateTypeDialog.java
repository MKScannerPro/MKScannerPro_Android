package com.moko.mkscannerpro.dialog;

import android.text.TextUtils;
import android.view.View;


import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.view.WheelView;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class UpdateTypeDialog extends MokoBaseDialog {

    @BindView(R.id.wv_update_type)
    WheelView wvUpdateType;
    private int selected;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_update_type;
    }

    @Override
    public void bindView(View v) {
        ButterKnife.bind(this, v);

        wvUpdateType.setData(createData());
        wvUpdateType.setDefault(selected);
    }

    private ArrayList<String> createData() {
        String[] updateTypes = getResources().getStringArray(R.array.update_type);
        ArrayList<String> data = new ArrayList<>();
        Collections.addAll(data, updateTypes);
        return data;
    }


    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    @OnClick({R.id.tv_cancel, R.id.tv_confirm})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_cancel:
                dismiss();
                break;
            case R.id.tv_confirm:
                dismiss();
                if (TextUtils.isEmpty(wvUpdateType.getSelectedText())) {
                    return;
                }
                if (wvUpdateType.getSelected() < 0) {
                    return;
                }
                if (listener != null) {
                    listener.onDataSelected(wvUpdateType.getSelected());
                }
                break;
        }
    }

    private OnDataSelectedListener listener;

    public void setListener(OnDataSelectedListener listener) {
        this.listener = listener;
    }

    public interface OnDataSelectedListener {
        void onDataSelected(int selected);
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }
}
