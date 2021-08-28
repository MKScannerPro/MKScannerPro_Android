## 1.Import and use SDK
### 1.1	Import module project mokosupport
### 1.2	Configure settings.gradle file and call mokosupport project:

	include ':app',':mokosupport'

### 1.3	Edit the build.gradle file of the main project:

	dependencies {
	    implementation fileTree(include: '*.jar', dir: 'libs')
	    implementation project(path: ':mokosupport')
	    ...
	}

### 1.4	Import SDK during project initialization:

	public class BaseApplication extends Application {
	    @Override
	    public void onCreate() {
	        super.onCreate();
	        // initialization
	        MokoSupport.getInstance().init(getApplicationContext());
	        MQTTSupport.getInstance().init(getApplicationContext());
	    }
	}


## 2.Function Introduction

- The methods provided in SDK include: Scanning bluetooth device, Communicate with bluetooth device, MQTT connection service, disconnection, subscription topic, unsubscribe topic, post topic, log record, etc.
- Scanning bluetooth device can be called by `MokoBleScanner`;
- Communicate with bluetooth device is called by `MokoSupport.getInstance()`;
- MQTT communication can be called by `MQTTSupport.getInstance()`;
- Three SSL connections are supported by `MQTTSupport.getInstance()`;

### 2.1 MokoSupport

Before connecting the device, it is necessary to press the device for a long time to make the device enter the scan state. After scanning the device, fill in the MQTT information and start the connection after entering the WIFI information

#### 2.1.1 Scanning Device

Start scanning, call the method`startScanDevice `

	mokoBleScanner.startScanDevice(MokoScanDeviceCallback mokoScanDeviceCallback)
	
Callback

	public interface MokoScanDeviceCallback {
	    void onStartScan();
	
	    void onScanDevice(DeviceInfo device);
	
	    void onStopScan();
	}

#### 2.1.2 Connectting Device

1、Get the connection status by registering the `EventBus.getDefault()` and subscribe `public void onConnectStatusEvent(ConnectStatusEvent event)`:

Connection status：

- Connection successful：`MokoConstants.ACTION_DISCOVER_SUCCESS`
- Connection failed：`MokoConstants.ACTION_DISCONNECTED`

2、Get the communication response by registering the broadcast :

- Order timeout：`MokoConstants.ACTION_ORDER_TIMEOUT`
- Order all Finish：`MokoConstants.ACTION_ORDER_FINISH`
- Order response：`MokoConstants.ACTION_ORDER_RESULT`

Get a response：

	OrderTaskResponse response = event.getResponse();

#### 2.1.3 Setting MQTT to Device

Use this method `MokoSupport.getInstance().sendOrder(OrderTask... orderTasks)` to send the device MQTT configuration information.

1、Set Passowrd(When conncet the device with bluetooth,you should set password first):

    public static OrderTask setPassword(String password) {
        SetPasswordTask task = new SetPasswordTask();
        task.setData(password);
        return task;
    }

1、Set host

    public static OrderTask setMqttHost(String host) {
        ParamsTask task = new ParamsTask();
        task.setMqttHost(host);
        return task;
    }

2、Set port
	
    public static OrderTask setMqttPort(@IntRange(from = 0, to = 65535) int port) {
        ParamsTask task = new ParamsTask();
        task.setMqttPort(port);
        return task;
    }
	
3、Set session

    public static OrderTask setMqttCleanSession(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setMqttCleanSession(enable);
        return task;
    }
	
4、Set deviceId

    public static OrderTask setMqttDeivceId(String deviceId) {
        ParamsTask task = new ParamsTask();
        task.setMqttDeviceId(deviceId);
        return task;
    }
	
5、Set clientId

    public static OrderTask setMqttClientId(String clientId) {
        ParamsTask task = new ParamsTask();
        task.setMqttClientId(clientId);
        return task;
    }
	
6、Set username:

    public static OrderTask setMqttUserName(String username) {
        ParamsTask task = new ParamsTask();
        task.setLongChar(ParamsKeyEnum.KEY_MQTT_USERNAME, username);
        return task;
    }
	
7、Set password

    public static OrderTask setMqttPassword(String password) {
        ParamsTask task = new ParamsTask();
        task.setLongChar(ParamsKeyEnum.KEY_MQTT_PASSWORD, password);
        return task;
    }
	
8、Set keepAlive

    public static OrderTask setMqttKeepAlive(@IntRange(from = 10, to = 120) int keepAlive) {
        ParamsTask task = new ParamsTask();
        task.setMqttKeepAlive(keepAlive);
        return task;
    }

9、Set qos

    public static OrderTask setMqttQos(@IntRange(from = 0, to = 2) int qos) {
        ParamsTask task = new ParamsTask();
        task.setMqttQos(qos);
        return task;
    }

10、Set connectMode

    public static OrderTask setMqttConnectMode(@IntRange(from = 0, to = 3) int mode) {
        ParamsTask task = new ParamsTask();
        task.setMqttConnectMode(mode);
        return task;
    }

11、Set CA cert

    public static OrderTask setCA(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsKeyEnum.KEY_MQTT_CA, file);
        return task;
    }

12、Set client cert

    public static OrderTask setClientCert(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsKeyEnum.KEY_MQTT_CLIENT_CERT, file);
        return task;
    }
	
13、Set client private key

    public static OrderTask setClientKey(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsKeyEnum.KEY_MQTT_CLIENT_KEY, file);
        return task;
    }
	
14、Set publish topic

    public static OrderTask setMqttPublishTopic(String topic) {
        ParamsTask task = new ParamsTask();
        task.setMqttPublishTopic(topic);
        return task;
    }
	
15、Set Subscribe topic

    public static OrderTask setMqttSubscribeTopic(String topic) {
        ParamsTask task = new ParamsTask();
        task.setMqttSubscribeTopic(topic);
        return task;
    }
	
16、Set WIFI SSID

    public static OrderTask setWifiSSID(String SSID) {
        ParamsTask task = new ParamsTask();
        task.setWifiSSID(SSID);
        return task;
    }
	
17、Set WIFI password

    public static OrderTask setWifiPassword(String password) {
        ParamsTask task = new ParamsTask();
        task.setWifiPassword(password);
        return task;
    }
	
18、Exit Config mode(When this command is set, the device disconnects from bluetooth and begins to connect to MQTT)

    public static OrderTask exitConfigMode() {
        ParamsTask task = new ParamsTask();
        task.exitConfigMode();
        return task;
    }
    
19、Set NTP Url

    public static OrderTask setNTPUrl(String url) {
        ParamsTask task = new ParamsTask();
        task.setNTPUrl(url);
        return task;
    }
    
20、Set NTP Timezone

    public static OrderTask setNTPTimeZone(@IntRange(from = -12, to = 12) int timeZone) {
        ParamsTask task = new ParamsTask();
        task.setNTPTimeZone(timeZone);
        return task;
    }
	
	
### 2.2	MQTTSupport

#### 2.2.1 Connect to the MQTT server

1、Create `MqttAndroidClient`

	public void connectMqtt(String mqttAppConfigStr)
	
2、Connect to the server

	public void connectMqtt(MqttConnectOptions options)
	
 Get the creation status according to `MqttCallbackExtended` and receive the return data form server

	@Override
    public void connectComplete(boolean reconnect, String serverURI) {
        ...
    }
    @Override
    public void connectionLost(Throwable cause) {
        ...
    }
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        ...
    }
    
3、Get the connection status by registering the EventBus:

Connection status：

- Connection success：`MQTTConnectionCompleteEvent`
- Connection failed：`MQTTConnectionFailureEvent`
- Disconnect：`MQTTConnectionLostEvent`

4、Receive the return data from server by registering the EventBus

Return data：

- Return data Topic and Message：`MQTTMessageArrivedEvent`

5、Get the Publish、Subscribe、UnSubscribe status by registering the EventBus

- `MQTTPublishSuccessEvent`
- `MQTTPublishFailureEvent`
- `MQTTSubscribeSuccessEvent`
- `MQTTSubscribeFailureEvent`
- `MQTTUnSubscribeSuccessEvent`
- `MQTTUnSubscribeFailureEvent`


When connected to the server successfully,the device can publish and subscribe different topics by the server,default topic format：

	device side：{device_name}/{device_id}/device_to_app
	app side：{device_name}/{device_id}/app_to_device

The return data is a JSON String,refer to the protocol documentation(communication between wifi and app),eg：

	device publish connection to network status
	
	MK107-2B52/testDeviceId/device_to_app:
	{
    "msg_id":3003,
    "device_info":{
        "device_id":"testDeviceId",
        "mac":"a4cf12782b52"
    },
    "data":{
        "net_state":"online"
    }
	}


#### 2.2.2 Subscribe topic

	MQTTSupport.getInstance().subscribe(String topic, int qos)
	
#### 2.2.3 Publish information

	MQTTSupport.getInstance().publish(String topic, String message, int msgId, int qos)

#### 2.2.4 Unsubscribe topic

	MQTTSupport.getInstance().unSubscribe(String topic)
	
#### 2.2.5 Determine whether the MQTT is connected

	MQTTSupport.getInstance().isConnected()
	
#### 2.2.6 Disconnection

	disconnectMqtt.getInstance().disconnectMqtt()
	
#### 2.2.7 TCP

	mqttConfig.connectMode = 0;
	...
	MokoSupport.getInstance().connectMqtt(connOpts);
	
#### 2.2.8 SSL(CA certificate file)
	
	mqttConfig.connectMode = 2;
	...
	connOpts.setSocketFactory(getSingleSocketFactory(mqttConfig.caPath));
	...
	MokoSupport.getInstance().connectMqtt(connOpts);
	
#### 2.2.9 SSL(Self signed certificates)
	
	mqttConfig.connectMode = 3;
	...
	connOpts.setSocketFactory(getSocketFactory(mqttConfig.caPath, mqttConfig.clientKeyPath, mqttConfig.clientCertPath));
	...
	MokoSupport.getInstance().connectMqtt(connOpts);
	
#### 2.2.10 SSL(CA signed server certificate)

	mqttConfig.connectMode = 1;
	...
	connOpts.setSocketFactory(getAllTMSocketFactory());

## 3.Save Log to SD Card

- SDK integrates the Log saved to the SD card function, is called [https://github.com/elvishew/xLog](https://github.com/elvishew/xLog "XLog")
- initialization method in `initXLog`
- The folder name and file name saved on the SD card can be modified.

		public class BaseApplication extends Application {
			private static final String TAG = "MKScannerPro";
    		private static final String LOG_FILE = "MKScannerPro.txt";
    		private static final String LOG_FOLDER = "MKScannerPro";
			...
		}

- Storage strategy: only store the data of the day and the data of the day before , the file is suffixed with.bak
- call method：
	- LogModule.v("log info");
	- LogModule.d("log info");
	- LogModule.i("log info");
	- LogModule.w("log info");
	- LogModule.e("log info");


