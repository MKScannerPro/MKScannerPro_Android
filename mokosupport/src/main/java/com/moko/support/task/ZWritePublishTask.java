package com.moko.support.task;

import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;

import java.util.Arrays;


public class ZWritePublishTask extends OrderTask {

    private byte[] orderData;

    private int packetCount;
    private int dataLength;
    private int packetIndex;
    private int dataOrigin;
    private byte[] dataBytes;
    private boolean isFailed;

    public ZWritePublishTask(MokoOrderTaskCallback callback, String publishTopic) {
        super(OrderType.CHARACTERISTIC, OrderEnum.WRITE_PUBLISH, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        dataBytes = publishTopic.getBytes();
        dataLength = dataBytes.length;
        packetIndex = 1;
        byte[] indexBytes = MokoUtils.toByteArray(packetIndex, 2);
        if (dataLength % 16 > 0) {
            packetCount = dataLength / 16 + 1;
        } else {
            packetCount = dataLength / 16;
        }
        delayTime = DEFAULT_DELAY_TIME + 100 * packetCount;
        if (packetCount > 1) {
            orderData = new byte[20];
            orderData[0] = (byte) order.getOrderHeader();
            orderData[1] = indexBytes[0];
            orderData[2] = indexBytes[1];
            orderData[3] = (byte) 16;
            for (int i = 0; i < 16; i++, dataOrigin++) {
                orderData[i + 4] = dataBytes[dataOrigin];
            }
        } else {
            orderData = new byte[dataLength + 4];
            orderData[0] = (byte) order.getOrderHeader();
            orderData[1] = indexBytes[0];
            orderData[2] = indexBytes[1];
            orderData[3] = (byte) dataLength;
            for (int i = 0; i < dataLength; i++) {
                orderData[i + 4] = dataBytes[i];
            }
        }
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }

    @Override
    public void assembleData(byte[] value) {
        byte[] index = Arrays.copyOfRange(value, 1, 3);
        if (!isFailed && packetCount > MokoUtils.toInt(index)) {
            packetIndex++;
            byte[] indexBytes = MokoUtils.toByteArray(packetIndex, 2);
            int length = dataLength - dataOrigin;
            if (length > 16) {
                orderData = new byte[20];
                orderData[0] = (byte) order.getOrderHeader();
                orderData[1] = indexBytes[0];
                orderData[2] = indexBytes[1];
                orderData[3] = (byte) 16;
                for (int i = 0; i < 16; i++, dataOrigin++) {
                    orderData[i + 4] = dataBytes[dataOrigin];
                }
            } else {
                orderData = new byte[4 + length];
                orderData[0] = (byte) order.getOrderHeader();
                orderData[1] = indexBytes[0];
                orderData[2] = indexBytes[1];
                orderData[3] = (byte) length;
                for (int i = 0; i < length; i++, dataOrigin++) {
                    orderData[i + 4] = dataBytes[dataOrigin];
                }
            }
            MokoSupport.getInstance().sendCustomOrder(this);
        }
    }

    @Override
    public void parseValue(byte[] value) {
        if (value.length != 4
                || order.getOrderHeader() != (value[0] & 0xFF)
                || 0x02 != (value[1] & 0xFF)
                || 0xAA != (value[2] & 0xFF)) {
            isFailed = true;
            return;
        }
        response.responseValue = value;
        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
    }
}
