package com.moko.support.service;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.google.gson.Gson;
import com.moko.support.MokoConstants;
import com.moko.support.entity.DeviceResponse;
import com.moko.support.log.LogModule;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class SocketThread extends Thread {
    private Handler mInHandle;
    private Handler mOutHandle;
    public Socket client = null;
    private String ip = "192.168.4.1";
    private int port = 8266;
    private int timeout = 10000;

    private DataOutputStream out;
    private DataInputStream in;
    public boolean isRun = true;

    public SocketThread(Handler inHandler, Handler outHandler, Context context) {
        mInHandle = inHandler;
        mOutHandle = outHandler;
    }


    @Override
    public void run() {
        LogModule.i("线程socket开始运行");
        if (!conn()) {
            return;
        }
        LogModule.i("1.run开始");
        String responseJson;
        while (isRun) {
            try {
                if (client != null) {
                    LogModule.i("2.等待设备发送数据");
                    byte[] b = new byte[1024];
                    in.read(b);
                    int len = 0;
                    for (int i = 0; i < b.length; i++) {
                        if (b[i] == 0) {
                            break;
                        }
                        len++;
                    }
                    if (len == 0) {
                        return;
                    }
                    responseJson = new String(b, 0, len);
//                    line = Utils.bytes2HexString(b, b.length);
                    LogModule.i("3.getdata" + responseJson + " len=" + responseJson.length());
                    DeviceResponse response = new Gson().fromJson(responseJson, DeviceResponse.class);
                    LogModule.i("4.start set Message");
                    Message msg = mInHandle.obtainMessage();
                    msg.obj = response;
                    mInHandle.sendMessage(msg);// 结果返回给UI处理
                    LogModule.i("5.send to handler");
                    if (response.result.header == MokoConstants.HEADER_SET_WIFI_INFO) {
                        LogModule.i("6.断开连接");
                        isRun = false;
                        close();
                    }
                } else {
                    LogModule.i("没有可用连接");
                    socketState(MokoConstants.CONN_STATUS_FAILED);
                }
            } catch (SocketTimeoutException e) {
                LogModule.i("获取数据超时");
                e.printStackTrace();
                socketState(MokoConstants.CONN_STATUS_TIMEOUT);
                close();
                conn();
            } catch (Exception e) {
                LogModule.i("数据接收错误" + e.getMessage());
                e.printStackTrace();
                close();
            }
        }
    }

    /**
     * 连接socket服务器
     */
    public boolean conn() {
        try {
            LogModule.i("获取到ip端口:" + ip + ":" + port);
            LogModule.i("连接中……");
            socketState(MokoConstants.CONN_STATUS_CONNECTING);
            client = new Socket(ip, port);
//            client.setSoTimeout(timeout);// 设置阻塞时间
            LogModule.i("连接成功");
            in = new DataInputStream(client.getInputStream());
            out = new DataOutputStream(client.getOutputStream());
            LogModule.i("输入输出流获取成功");
            socketState(MokoConstants.CONN_STATUS_SUCCESS);
            return true;
        } catch (UnknownHostException e) {
            LogModule.i("连接错误UnknownHostException 重新获取");
            e.printStackTrace();
            socketState(MokoConstants.CONN_STATUS_FAILED);
        } catch (IOException e) {
            LogModule.i("连接服务器io错误");
            e.printStackTrace();
            socketState(MokoConstants.CONN_STATUS_FAILED);
        } catch (Exception e) {
            LogModule.i("连接服务器错误Exception" + e.getMessage());
            e.printStackTrace();
            socketState(MokoConstants.CONN_STATUS_FAILED);
        }
        return false;
    }

    private void socketState(int code) {
        Message msg = mOutHandle.obtainMessage();
        msg.what = code;
        mOutHandle.sendMessage(msg);// 结果返回给UI处理
    }

    /**
     * 发送数据
     *
     * @param mess
     */
    public void send(String mess) {
        try {
            if (client != null) {
                LogModule.i("发送" + mess + "至"
                        + client.getInetAddress().getHostAddress() + ":"
                        + String.valueOf(client.getPort()));
                out.write(mess.getBytes());
            } else {
                LogModule.i("连接不存在，重新连接");
                conn();
            }
        } catch (Exception e) {
            LogModule.i("send error");
            e.printStackTrace();
        } finally {
            LogModule.i("发送完毕");
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        try {
            isRun = false;
            if (client != null) {
                LogModule.i("close in");
                in.close();
                LogModule.i("close out");
                out.close();
                LogModule.i("close client");
                client.close();
            }
        } catch (Exception e) {
            LogModule.i("close err");
            e.printStackTrace();
        }

    }
}
