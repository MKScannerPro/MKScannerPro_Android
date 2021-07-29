package com.moko.mkscannerpro.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.entity.ScanDevice;

public class ScanDeviceAdapter extends BaseQuickAdapter<ScanDevice, BaseViewHolder> {
    public ScanDeviceAdapter() {
        super(R.layout.item_scan_device);
    }

    @Override
    protected void convert(BaseViewHolder helper, ScanDevice item) {
        helper.setText(R.id.tv_device_name, mContext.getString(R.string.scan_device_name, item.name));
        helper.setText(R.id.tv_device_rssi, mContext.getString(R.string.scan_device_rssi, item.rssi));
        helper.setText(R.id.tv_device_mac, mContext.getString(R.string.scan_device_mac, item.mac));
        helper.setText(R.id.tv_device_raw_data, mContext.getString(R.string.scan_device_raw_data, item.rawData));
    }
}
