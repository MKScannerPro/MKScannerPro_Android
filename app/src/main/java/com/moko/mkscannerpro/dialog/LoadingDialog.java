package com.moko.mkscannerpro.dialog;

import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.view.ProgressDrawable;

import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;

public class LoadingDialog extends MokoBaseDialog {

    public static final String TAG = LoadingDialog.class.getSimpleName();
    @BindView(R.id.iv_loading)
    ImageView ivLoading;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_loading;
    }

    @Override
    public void bindView(View v) {
        ButterKnife.bind(this, v);
        ProgressDrawable progressDrawable = new ProgressDrawable();
        progressDrawable.setColor(ContextCompat.getColor(getContext(), R.color.black_333333));
        ivLoading.setImageDrawable(progressDrawable);
        progressDrawable.start();
    }


    @Override
    public int getDialogStyle() {
        return R.style.CenterDialog;
    }

    @Override
    public int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    @Override
    public boolean getCancelOutside() {
        return false;
    }

    @Override
    public boolean getCancellable() {
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((ProgressDrawable) ivLoading.getDrawable()).stop();
    }
}
