package com.moko.support.task;

import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;


public class ZWritePortTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 4;

    private byte[] orderData;

    public ZWritePortTask(MokoOrderTaskCallback callback, int port) {
        super(OrderType.CHARACTERISTIC, OrderEnum.WRITE_PORT, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        byte[] portBytes = MokoUtils.toByteArray(port, 2);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) order.getOrderHeader();
        orderData[1] = 0x02;
        orderData[2] = portBytes[0];
        orderData[3] = portBytes[1];
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
