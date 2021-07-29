package com.moko.mkscannerpro.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.lang.ref.SoftReference;

public abstract class BaseBroadcastReceiver extends BroadcastReceiver {

    public BaseBroadcastReceiver(Context context) {
        final SoftReference<Context> mReference = new SoftReference<>(context);
        mReference.get().registerReceiver(this, getIntentFilter());
    }

    public abstract IntentFilter getIntentFilter();

}