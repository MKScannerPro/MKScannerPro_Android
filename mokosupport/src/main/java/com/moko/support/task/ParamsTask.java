package com.moko.support.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderCHAR;
import com.moko.support.entity.ParamsKeyEnum;

import java.io.File;
import java.io.FileInputStream;

import androidx.annotation.IntRange;

public class ParamsTask extends OrderTask {
    public byte[] data;

    public ParamsTask() {
        super(OrderCHAR.CHAR_PARAMS, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
    }

    @Override
    public byte[] assemble() {
        return data;
    }

    public void setData(ParamsKeyEnum key) {
        switch (key) {
            case KEY_DEVICE_MAC:
            case KEY_DEVICE_NAME:
            case KEY_PRODUCT_MODEL:
            case KEY_MANUFACTURER:
            case KEY_HARDWARE_VERSION:
            case KEY_SOFTWARE_VERSION:
            case KEY_FIRMWARE_VERSION:
            case KEY_DEVICE_TYPE:
                createGetConfigData(key.getParamsKey());
                break;
        }
    }

    private void createGetConfigData(int configKey) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x00,
                (byte) configKey,
                (byte) 0x00
        };
    }

    public void exitConfigMode() {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_EXIT_CONFIG_MODE.getParamsKey(),
                (byte) 0x01,
                (byte) 0x01
        };
    }

    public void setWifiSSID(String SSID) {
        byte[] dataBytes = SSID.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_WIFI_SSID.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setWifiPassword(String password) {
        byte[] dataBytes = password.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_WIFI_PASSWORD.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttConnectMode(@IntRange(from = 0, to = 3) int mode) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_CONNECT_MODE.getParamsKey(),
                (byte) 0x01,
                (byte) mode
        };
    }

    public void setMqttHost(String host) {
        byte[] dataBytes = host.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_HOST.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttPort(@IntRange(from = 0, to = 65535) int port) {
        byte[] dataBytes = MokoUtils.toByteArray(port, 2);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_PORT.getParamsKey(),
                (byte) 0x02,
                dataBytes[0],
                dataBytes[1]
        };
    }

    public void setMqttCleanSession(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_CLEAN_SESSION.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
    }

    public void setMqttKeepAlive(@IntRange(from = 10, to = 120) int keepAlive) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_KEEP_ALIVE.getParamsKey(),
                (byte) 0x01,
                (byte) keepAlive
        };
    }

    public void setMqttQos(@IntRange(from = 0, to = 2) int qos) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_QOS.getParamsKey(),
                (byte) 0x01,
                (byte) qos
        };
    }

    public void setMqttClientId(String clientId) {
        byte[] dataBytes = clientId.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_CLIENT_ID.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttDeviceId(String deviceId) {
        byte[] dataBytes = deviceId.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_DEVICE_ID.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttSubscribeTopic(String topic) {
        byte[] dataBytes = topic.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_SUBSCRIBE_TOPIC.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttPublishTopic(String topic) {
        byte[] dataBytes = topic.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_PUBLISH_TOPIC.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setNTPUrl(String url) {
        byte[] dataBytes = url.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_NTP_URL.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setNTPTimeZone(@IntRange(from = -12, to = 12) int timeZone) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_NTP_TIME_ZONE.getParamsKey(),
                (byte) 0x01,
                (byte) timeZone
        };
    }

    public void setConnectionTimeout(@IntRange(from = 0, to = 1440) int timeout) {
        byte[] dataBytes = MokoUtils.toByteArray(timeout, 2);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_CONNECTION_TIMEOUT.getParamsKey(),
                (byte) 0x02,
                dataBytes[0],
                dataBytes[1]
        };
    }

    public void setFile(ParamsKeyEnum key, File file) throws Exception {
        FileInputStream inputSteam = new FileInputStream(file);
        dataBytes = new byte[(int) file.length()];
        inputSteam.read(dataBytes);
        dataLength = dataBytes.length;
        if (dataLength % DATA_LENGTH_MAX > 0) {
            packetCount = dataLength / DATA_LENGTH_MAX + 1;
        } else {
            packetCount = dataLength / DATA_LENGTH_MAX;
        }
        remainPack = packetCount - 1;
        delayTime = DEFAULT_DELAY_TIME + 100 * packetCount;
        if (packetCount > 1) {
            data = new byte[DATA_LENGTH_MAX + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) 0x01;
            data[4] = (byte) remainPack;
            data[5] = (byte) DATA_LENGTH_MAX;
            for (int i = 0; i < DATA_LENGTH_MAX; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        } else {
            data = new byte[dataLength + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) 0x01;
            data[4] = (byte) remainPack;
            data[5] = (byte) dataLength;
            for (int i = 0; i < dataLength; i++) {
                data[i + 6] = dataBytes[i];
            }
        }
    }

    public void setLongChar(ParamsKeyEnum key, String character) {
        dataBytes = character.getBytes();
        dataLength = dataBytes.length;
        if (dataLength % DATA_LENGTH_MAX > 0) {
            packetCount = dataLength / DATA_LENGTH_MAX + 1;
        } else {
            packetCount = dataLength / DATA_LENGTH_MAX;
        }
        remainPack = packetCount - 1;
        delayTime = DEFAULT_DELAY_TIME + 100 * packetCount;
        if (packetCount > 1) {
            data = new byte[DATA_LENGTH_MAX + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) 0x01;
            data[4] = (byte) remainPack;
            data[5] = (byte) DATA_LENGTH_MAX;
            for (int i = 0; i < DATA_LENGTH_MAX; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        } else {
            data = new byte[dataLength + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) 0x01;
            data[4] = (byte) remainPack;
            data[5] = (byte) dataLength;
            for (int i = 0; i < dataLength; i++) {
                data[i + 6] = dataBytes[i];
            }
        }
    }

    private int packetCount;
    private int remainPack;
    private int dataLength;
    private int dataOrigin;
    private byte[] dataBytes;
    private static final int DATA_LENGTH_MAX = 238;

    @Override
    public boolean parseValue(byte[] value) {
        final int header = value[0] & 0xFF;
        if (header == 0xED)
            return true;
        final int cmd = value[2] & 0xFF;
        final int result = value[4] & 0xFF;
        if (result == 1) {
            remainPack--;
            if (remainPack >= 0) {
                assembleRemainData(cmd);
                return false;
            }
            return true;
        }
        return false;
    }

    private void assembleRemainData(int cmd) {
        int length = dataLength - dataOrigin;
        if (length > DATA_LENGTH_MAX) {
            data = new byte[DATA_LENGTH_MAX + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) cmd;
            data[3] = (byte) 0x00;
            data[4] = (byte) remainPack;
            data[5] = (byte) DATA_LENGTH_MAX;
            for (int i = 0; i < DATA_LENGTH_MAX; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        } else {
            data = new byte[length + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) cmd;
            data[3] = (byte) 0x00;
            data[4] = (byte) remainPack;
            data[5] = (byte) length;
            for (int i = 0; i < length; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        }
        MokoSupport.getInstance().sendDirectOrder(this);
    }
}
