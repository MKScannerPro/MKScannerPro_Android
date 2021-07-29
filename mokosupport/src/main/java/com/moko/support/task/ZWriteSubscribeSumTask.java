package com.moko.support.task;

import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;


public class ZWriteSubscribeSumTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 5;

    private byte[] orderData;

    public ZWriteSubscribeSumTask(MokoOrderTaskCallback callback, String subscribeTopic) {
        super(OrderType.CHARACTERISTIC, OrderEnum.WRITE_SUBSCRIBE_PACKAGE_SUM, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        byte[] dataBytes = subscribeTopic.getBytes();
        int dataLength = dataBytes.length;
        int packetCount;
        if (dataLength % 16 > 0) {
            packetCount = dataLength / 16 + 1;
        } else {
            packetCount = dataLength / 16;
        }
        byte[] packetCountBytes = MokoUtils.toByteArray(packetCount, 2);
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) order.getOrderHeader();
        orderData[1] = packetCountBytes[0];
        orderData[2] = packetCountBytes[1];
        orderData[3] = dataLengthBytes[0];
        orderData[4] = dataLengthBytes[1];
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }

    @Override
    public void parseValue(byte[] value) {
        if (value.length != 4)
            return;
        if (order.getOrderHeader() != (value[0] & 0xFF))
            return;
        if (0x02 != (value[1] & 0xFF))
            return;
        if (0xAA != (value[2] & 0xFF))
            return;
        response.responseValue = value;
        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
    }
}
