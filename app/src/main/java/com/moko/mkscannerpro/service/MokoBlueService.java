package com.moko.mkscannerpro.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;

import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoConnStateCallback;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderTaskResponse;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.handler.BaseMessageHandler;
import com.moko.support.log.LogModule;
import com.moko.support.task.OrderTask;
import com.moko.support.task.ZWriteCASumTask;
import com.moko.support.task.ZWriteCATask;
import com.moko.support.task.ZWriteClientCertSumTask;
import com.moko.support.task.ZWriteClientCertTask;
import com.moko.support.task.ZWriteClientIdSumTask;
import com.moko.support.task.ZWriteClientIdTask;
import com.moko.support.task.ZWriteClientPrivateSumTask;
import com.moko.support.task.ZWriteClientPrivateTask;
import com.moko.support.task.ZWriteConnectModeTask;
import com.moko.support.task.ZWriteDeviceIdSumTask;
import com.moko.support.task.ZWriteDeviceIdTask;
import com.moko.support.task.ZWriteHostSumTask;
import com.moko.support.task.ZWriteHostTask;
import com.moko.support.task.ZWriteKeepAliveTask;
import com.moko.support.task.ZWritePasswordSumTask;
import com.moko.support.task.ZWritePasswordTask;
import com.moko.support.task.ZWritePortTask;
import com.moko.support.task.ZWritePublishSumTask;
import com.moko.support.task.ZWritePublishTask;
import com.moko.support.task.ZWriteQosTask;
import com.moko.support.task.ZWriteResetTask;
import com.moko.support.task.ZWriteSessionTask;
import com.moko.support.task.ZWriteStaNameSumTask;
import com.moko.support.task.ZWriteStaNameTask;
import com.moko.support.task.ZWriteStaPasswordSumTask;
import com.moko.support.task.ZWriteStaPasswordTask;
import com.moko.support.task.ZWriteStartConnectTask;
import com.moko.support.task.ZWriteSubscribeSumTask;
import com.moko.support.task.ZWriteSubscribeTask;
import com.moko.support.task.ZWriteUsernameSumTask;
import com.moko.support.task.ZWriteUsernameTask;

import org.greenrobot.eventbus.EventBus;


/**
 * @Date 2017/12/7 0007
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.mkscannerpro.service.MokoBlueService
 */
public class MokoBlueService extends Service implements MokoConnStateCallback, MokoOrderTaskCallback {

    @Override
    public void onConnectSuccess() {
        ConnectStatusEvent connectStatusEvent = new ConnectStatusEvent();
        connectStatusEvent.setAction(MokoConstants.ACTION_DISCOVER_SUCCESS);
        EventBus.getDefault().post(connectStatusEvent);
    }

    @Override
    public void onDisConnected() {
        ConnectStatusEvent connectStatusEvent = new ConnectStatusEvent();
        connectStatusEvent.setAction(MokoConstants.ACTION_CONN_STATUS_DISCONNECTED);
        EventBus.getDefault().post(connectStatusEvent);
    }

    @Override
    public void onOrderResult(OrderTaskResponse response) {
        Intent intent = new Intent(new Intent(MokoConstants.ACTION_ORDER_RESULT));
        intent.putExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK, response);
        sendOrderedBroadcast(intent, null);
    }

    @Override
    public void onOrderTimeout(OrderTaskResponse response) {
        Intent intent = new Intent(new Intent(MokoConstants.ACTION_ORDER_TIMEOUT));
        intent.putExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK, response);
        sendOrderedBroadcast(intent, null);
    }

    @Override
    public void onOrderFinish() {
        sendBroadcast(new Intent(MokoConstants.ACTION_ORDER_FINISH));
    }

    @Override
    public void onCreate() {
        LogModule.i("创建MokoBlueService...onCreate");
        mHandler = new ServiceHandler(this);
        super.onCreate();
    }

    public void connectBluetoothDevice(String address) {
        MokoSupport.getInstance().connDevice(this, address, this);
    }

    /**
     * @Date 2017/5/23
     * @Author wenzheng.liu
     * @Description 断开手环
     */
    public void disConnectBle() {
        MokoSupport.getInstance().disConnectBle();
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogModule.i("启动MokoBlueService...onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    private IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        LogModule.i("绑定MokoBlueService...onBind");
        return mBinder;
    }

    @Override
    public void onLowMemory() {
        LogModule.i("内存吃紧，销毁MokoBlueService...onLowMemory");
        disConnectBle();
        super.onLowMemory();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogModule.i("解绑MokoBlueService...onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        LogModule.i("销毁MokoBlueService...onDestroy");
        disConnectBle();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public MokoBlueService getService() {
            return MokoBlueService.this;
        }
    }

    public ServiceHandler mHandler;

    public class ServiceHandler extends BaseMessageHandler<MokoBlueService> {

        public ServiceHandler(MokoBlueService service) {
            super(service);
        }

        @Override
        protected void handleMessage(MokoBlueService service, Message msg) {
        }
    }

    public OrderTask setHostSum(String host) {
        OrderTask orderTask = new ZWriteHostSumTask(this, host);
        return orderTask;
    }

    public OrderTask setHost(String host) {
        OrderTask orderTask = new ZWriteHostTask(this, host);
        return orderTask;
    }

    public OrderTask setPort(int port) {
        OrderTask orderTask = new ZWritePortTask(this, port);
        return orderTask;
    }

    public OrderTask setSession(int session) {
        OrderTask orderTask = new ZWriteSessionTask(this, session);
        return orderTask;
    }

    public OrderTask setDeviceIdSum(String deviceId) {
        OrderTask orderTask = new ZWriteDeviceIdSumTask(this, deviceId);
        return orderTask;
    }

    public OrderTask setDeviceId(String deviceId) {
        OrderTask orderTask = new ZWriteDeviceIdTask(this, deviceId);
        return orderTask;
    }

    public OrderTask setClientIdSum(String clientId) {
        OrderTask orderTask = new ZWriteClientIdSumTask(this, clientId);
        return orderTask;
    }

    public OrderTask setClientId(String clientId) {
        OrderTask orderTask = new ZWriteClientIdTask(this, clientId);
        return orderTask;
    }

    public OrderTask setUsernameSum(String username) {
        OrderTask orderTask = new ZWriteUsernameSumTask(this, username);
        return orderTask;
    }

    public OrderTask setUsername(String username) {
        OrderTask orderTask = new ZWriteUsernameTask(this, username);
        return orderTask;
    }

    public OrderTask setPasswordSum(String password) {
        OrderTask orderTask = new ZWritePasswordSumTask(this, password);
        return orderTask;
    }

    public OrderTask setPassword(String password) {
        OrderTask orderTask = new ZWritePasswordTask(this, password);
        return orderTask;
    }

    public OrderTask setKeepAlive(int keepAlive) {
        OrderTask orderTask = new ZWriteKeepAliveTask(this, keepAlive);
        return orderTask;
    }

    public OrderTask setQos(int qos) {
        OrderTask orderTask = new ZWriteQosTask(this, qos);
        return orderTask;
    }

    public OrderTask setConnectMode(int connectMode) {
        OrderTask orderTask = new ZWriteConnectModeTask(this, connectMode);
        return orderTask;
    }

    public OrderTask setCASum(int dataLength) {
        OrderTask orderTask = new ZWriteCASumTask(this, dataLength);
        return orderTask;
    }

    public OrderTask setCA(byte[] fileBytes) {
        OrderTask orderTask = new ZWriteCATask(this, fileBytes);
        return orderTask;
    }

    public OrderTask setClientCertSum(int dataLength) {
        OrderTask orderTask = new ZWriteClientCertSumTask(this, dataLength);
        return orderTask;
    }

    public OrderTask setClientCert(byte[] fileBytes) {
        OrderTask orderTask = new ZWriteClientCertTask(this, fileBytes);
        return orderTask;
    }

    public OrderTask setClientPrivateSum(int dataLength) {
        OrderTask orderTask = new ZWriteClientPrivateSumTask(this, dataLength);
        return orderTask;
    }

    public OrderTask setClientPrivate(byte[] fileBytes) {
        OrderTask orderTask = new ZWriteClientPrivateTask(this, fileBytes);
        return orderTask;
    }

    public OrderTask setPublishSum(String publish) {
        OrderTask orderTask = new ZWritePublishSumTask(this, publish);
        return orderTask;
    }

    public OrderTask setPublish(String publish) {
        OrderTask orderTask = new ZWritePublishTask(this, publish);
        return orderTask;
    }

    public OrderTask setSubscribeSum(String subscribe) {
        OrderTask orderTask = new ZWriteSubscribeSumTask(this, subscribe);
        return orderTask;
    }

    public OrderTask setSubscribe(String subscribe) {
        OrderTask orderTask = new ZWriteSubscribeTask(this, subscribe);
        return orderTask;
    }

    public OrderTask setStaNameSum(String staName) {
        OrderTask orderTask = new ZWriteStaNameSumTask(this, staName);
        return orderTask;
    }

    public OrderTask setStaName(String staName) {
        OrderTask orderTask = new ZWriteStaNameTask(this, staName);
        return orderTask;
    }

    public OrderTask setStaPasswordSum(String staPassword) {
        OrderTask orderTask = new ZWriteStaPasswordSumTask(this, staPassword);
        return orderTask;
    }

    public OrderTask setStaPassword(String staPassword) {
        OrderTask orderTask = new ZWriteStaPasswordTask(this, staPassword);
        return orderTask;
    }

    public OrderTask setReset() {
        OrderTask orderTask = new ZWriteResetTask(this);
        return orderTask;
    }

    public OrderTask setStartConnect() {
        OrderTask orderTask = new ZWriteStartConnectTask(this);
        return orderTask;
    }
}
