package com.moko.mkscannerpro.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkscannerpro.R;

public class ScanDeviceAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    public ScanDeviceAdapter() {
        super(R.layout.item_scan_device);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.tv_scan_device_info, item);
    }
}
