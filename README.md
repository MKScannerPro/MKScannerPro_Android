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
	        Toasty.Config.getInstance().apply();
	    }
	}


## 2.Function Introduction

- The methods provided in SDK include: Scanning bluetooth device, Communicate with bluetooth device, MQTT connection service, disconnection, subscription topic, unsubscribe topic, post topic, log record, etc.
- Scanning bluetooth device can be called by `MokoSupport.getInstance()`;
- Communicate with bluetooth device is called by `MokoBlueService`;
- MQTT communication can be called by `MokoSupport.getInstance()`;
- Three SSL connections are supported by `MokoService`;

### 2.1 MokoBlueService

Before connecting the device, it is necessary to press the device for a long time to make the device enter the scan state. After scanning the device, fill in the MQTT information and start the connection after entering the WIFI information

#### 2.1.1 Scanning Device

Start scanning, call the method`startScanDevice `

	MokoSupport.getInstance().startScanDevice(MokoScanDeviceCallback mokoScanDeviceCallback)
	
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
- Connection failed：`MokoConstants.ACTION_CONN_STATUS_DISCONNECTED`

2、Get the communication response by registering the broadcast :

- Order timeout：`MokoConstants.ACTION_ORDER_TIMEOUT`
- Order all Finish：`MokoConstants.ACTION_ORDER_FINISH`
- Order response：`MokoConstants.ACTION_ORDER_RESULT`

Get a response：

	OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);

#### 2.1.3 Setting MQTT to Device

Use this method `MokoSupport.getInstance().sendOrder(OrderTask... orderTasks)` to send the device MQTT configuration information

1、Set host:

	ZWriteHostSumTask(MokoOrderTaskCallback callback, String host)
	ZWriteHostTask(MokoOrderTaskCallback callback, String host)

2、Set port:
	
	ZWritePortTask(MokoOrderTaskCallback callback, int port)
	
3、Set session:

	ZWriteSessionTask(MokoOrderTaskCallback callback, int session)
	
4、Set deviceId:

	ZWriteDeviceIdSumTask(MokoOrderTaskCallback callback, String deviceId)
	ZWriteDeviceIdTask(MokoOrderTaskCallback callback, String deviceId)
	
5、Set clientId:

	ZWriteClientIdSumTask(MokoOrderTaskCallback callback, String clientId)
	ZWriteClientIdTask(MokoOrderTaskCallback callback, String clientId)
	
6、Set username:

	ZWriteUsernameSumTask(MokoOrderTaskCallback callback, String username)
	ZWriteUsernameTask(MokoOrderTaskCallback callback, String username)
	
7、Set password

	ZWritePasswordSumTask(MokoOrderTaskCallback callback, String password)
	ZWritePasswordTask(MokoOrderTaskCallback callback, String password)
	
8、Set keepAlive

	ZWriteKeepAliveTask(MokoOrderTaskCallback callback, int keepAlive)

9、Set qos

	ZWriteQosTask(MokoOrderTaskCallback callback, int qos)

10、Set connectMode

	ZWriteConnectModeTask(MokoOrderTaskCallback callback, int connectMode)

11、Set CA cert

	ZWriteCASumTask(MokoOrderTaskCallback callback, int dataLength)
	ZWriteCATask(MokoOrderTaskCallback callback, byte[] fileBytes)

12、Set client cert

	ZWriteClientCertSumTask(MokoOrderTaskCallback callback, int dataLength)
	ZWriteClientCertTask(MokoOrderTaskCallback callback, byte[] fileBytes)
	
13、Set client private key

	ZWriteClientPrivateSumTask(MokoOrderTaskCallback callback, int dataLength)
	ZWriteClientPrivateTask(MokoOrderTaskCallback callback, byte[] fileBytes)
	
14、Set publish topic

	ZWritePublishSumTask(MokoOrderTaskCallback callback, String publishTopic)
	ZWritePublishask(MokoOrderTaskCallback callback, String publishTopic)
	
15、Set Subscribe topic

	ZWriteSubscribeSumTask(MokoOrderTaskCallback callback, String publishTopic)
	ZWriteSubscribeTask(MokoOrderTaskCallback callback, String publishTopic)
	
16、Set WIFI SSID

	ZWriteStaNameSumTask(MokoOrderTaskCallback callback, String satName)
	ZWriteStaNameTask(MokoOrderTaskCallback callback, String satName)
	
17、Set WIFI password

	ZWriteStaPasswordSumTask(MokoOrderTaskCallback callback, String staPassword)
	ZWriteStaPasswordask(MokoOrderTaskCallback callback, String staPassword)
	
18、Start connect(When this command is set, the device disconnects from bluetooth and begins to connect to MQTT)

	ZWriteStartConnectTask(MokoOrderTaskCallback callback)
	
	
### 2.2	MokoSupport

#### 2.2.1 Connect to the MQTT server

1、Create `MqttAndroidClient`

	public void creatClient(String host, String port, String clientId, boolean tlsConnection)
	
2、Connect to the server

	public void connectMqtt(MqttConnectOptions options)
	
 Get the creation status according to `MqttCallbackHandler` and receive the return data form server

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
    
3、Get the connection status by registering the broadcast:

Broadcast ACTION：`MokoConstants.ACTION_MQTT_CONNECTION`

Connection status：

- Connection success：`MokoConstants.MQTT_CONN_STATUS_SUCCESS`
- Connection failed：`MokoConstants.MQTT_CONN_STATUS_FAILED`
- Disconnect：`MokoConstants.MQTT_CONN_STATUS_LOST`

4、Receive the return data from server by registering the broadcast

Broadcast ACTION：`MokoConstants.ACTION_MQTT_RECEIVE`

Return data：

- Return data Topic：`MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC`
- Return data Message：`MokoConstants.EXTRA_MQTT_RECEIVE_MESSAGE`

When connected to the server successfully,the device can publish and subscribe different topics by the server,default topic format：

	device side：{device_name}/{device_id}/device_to_app
	app side：{device_name}/{device_id}/app_to_device

The return data is array of bytes,refer to the protocol documentation(communication between wifi and app),eg：

	device publish connection to network status
	
	Key:0x24
	device publish：24 02 31 32 00 01 01
	device id length:02   
	device id:31 32 （string：“12”）
	data length：00 01  
	data（networkstatus）: 01(online)/00(offline）

5、Combine data using `MQTTMessageAssembler` and publish to device,refer to the protocol documentation(communication between wifi and app),eg:

	byte[] message = MQTTMessageAssembler.assembleWriteScanSwitch(String id, boolean scanSwitch)
	MokoSupport.getInstance().publish(String topic, byte[] message, int qos)


#### 2.2.2 Action monitor

MQTT communication contains four kinds of Actions. To execute each Action, you need to set `ActionListener` to monitor the state of the Action:

	public enum Action {
	        /**
	         * Connect Action
	         **/
	        CONNECT,
	        /**
	         * Subscribe Action
	         **/
	        SUBSCRIBE,
	        /**
	         * Publish Action
	         **/
	        PUBLISH,
	        /**
	         * UnSubscribe Action
	         **/
	        UNSUBSCRIBE
	    }
	    
	    
Get the Action status by registering the broadcast:

1、CONNECT
	
Broadcast ACTION：`MokoConstants.ACTION_MQTT_CONNECTION`

- Connection failed：`MokoConstants.MQTT_CONN_STATUS_FAILED`

2、SUBSCRIBE

Broadcast ACTION：`MokoConstants.ACTION_MQTT_SUBSCRIBE`

- Subscribe Topic：`MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC`
- Subscribe status：`MokoConstants.EXTRA_MQTT_STATE`

3、PUBLISH

Broadcast ACTION：`MokoConstants.ACTION_MQTT_PUBLISH`

- Publish status：`MokoConstants.EXTRA_MQTT_STATE`

4、UNSUBSCRIBE

Broadcast ACTION：`MokoConstants.ACTION_MQTT_UNSUBSCRIBE`

- Unsubscribe status：`MokoConstants.EXTRA_MQTT_STATE`

#### 2.2.3 Subscribe topic

	MokoSupport.getInstance().subscribe(String topic, int qos)
	
#### 2.2.4 Publish information

	MokoSupport.getInstance().publish(String topic, MqttMessage message)

#### 2.2.5 Unsubscribe topic

	MokoSupport.getInstance().unSubscribe(String topic)
	
#### 2.2.6 Determine whether the MQTT is connected

	MokoSupport.getInstance().isConnected()
	
#### 2.2.7 Disconnection

	MokoSupport.getInstance().disconnectMqtt()
	
	
### 2.3	MokoService

#### 2.3.1 TCP

	mqttConfig.connectMode = 0;
	...
	MokoSupport.getInstance().connectMqtt(connOpts);
	
#### 2.3.2 SSL(One-way authentication)
	
	mqttConfig.connectMode = 1;
	...
	connOpts.setSocketFactory(getSingleSocketFactory(mqttConfig.caPath));
	...
	MokoSupport.getInstance().connectMqtt(connOpts);
	
#### 2.3.3 SSL(Two-way authentication)
	
	mqttConfig.connectMode = 3;
	...
	connOpts.setSocketFactory(getSocketFactory(mqttConfig.caPath, mqttConfig.clientKeyPath, mqttConfig.clientCertPath));
	...
	MokoSupport.getInstance().connectMqtt(connOpts);
	
#### 2.3.4 SSL(All Trust)

	mqttConfig.connectMode > 0 && mqttConfig.caPath = null;
	...
	connOpts.setSocketFactory(getAllTMSocketFactory());

## 3.Save Log to SD Card

- SDK integrates the Log saved to the SD card function, is called [https://github.com/elvishew/xLog](https://github.com/elvishew/xLog "XLog")
- initialization method in `MokoSupport.getInstance().init(getApplicationContext())`
- The folder name and file name saved on the SD card can be modified.

		public class LogModule {
			private static final String TAG = "mokoScanner";// file name 
		    private static final String LOG_FOLDER = "mokoScanner";// folder name
			...
		}

- Storage strategy: only store the data of the day and the data of the day before , the file is suffixed with.bak
- call method：
	- LogModule.v("log info");
	- LogModule.d("log info");
	- LogModule.i("log info");
	- LogModule.w("log info");
	- LogModule.e("log info");


