package com.moko.mkscannerpro.utils;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import es.dmoral.toasty.Toasty;

/**
 * toast方法
 *
 * @author jianweiwang
 */
public class ToastUtils {

    public static void showToast(Context context, int tipID) {
        String tip = (String) context.getResources().getText(tipID);
        showToast(context, tip);
    }

    /**
     * toast n个字以上 LENGTH_LONG
     *
     * @param context
     * @param tip
     */
    public static void showToast(Context context, String tip) {
        Toast toast = Toasty.normal(context, tip);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

}
