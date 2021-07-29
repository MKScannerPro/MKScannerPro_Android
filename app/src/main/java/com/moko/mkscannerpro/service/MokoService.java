package com.moko.mkscannerpro.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.moko.mkscannerpro.AppConstants;
import com.moko.mkscannerpro.BuildConfig;
import com.moko.mkscannerpro.R;
import com.moko.mkscannerpro.entity.MQTTConfig;
import com.moko.mkscannerpro.utils.SPUtiles;
import com.moko.support.callback.ActionListener;
import com.moko.support.handler.MqttCallbackHandler;
import com.moko.support.log.LogModule;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


/**
 * @Date 2017/12/7 0007
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.mkscannerpro.service.MokoService
 */
public class MokoService extends Service {

    private boolean onCreate;
    
    private IBinder mBinder = new LocalBinder();
    
    private MqttAndroidClient mqttAndroidClient;

    public class LocalBinder extends Binder {
        public MokoService getService() {
            return MokoService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogModule.i("启动后台服务");
        onCreate = true;
        connectMqtt();
    }

    public void connectMqtt() {
        String mqttAppConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        if (!TextUtils.isEmpty(mqttAppConfigStr)) {
            MQTTConfig mqttConfig = new Gson().fromJson(mqttAppConfigStr, MQTTConfig.class);
            if (!mqttConfig.isError(null)) {
                String uri;
                if (mqttConfig.connectMode > 0) {
                    uri = "ssl://" + mqttConfig.host + ":" + mqttConfig.port;
                } else {
                    uri = "tcp://" + mqttConfig.host + ":" + mqttConfig.port;
                }
                mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), uri, mqttConfig.clientId);
                mqttAndroidClient.setCallback(new MqttCallbackHandler(this));
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setAutomaticReconnect(true);
                connOpts.setCleanSession(mqttConfig.cleanSession);
                connOpts.setKeepAliveInterval(mqttConfig.keepAlive);
                if (!TextUtils.isEmpty(mqttConfig.username)) {
                    connOpts.setUserName(mqttConfig.username);
                }
                if (!TextUtils.isEmpty(mqttConfig.password)) {
                    connOpts.setPassword(mqttConfig.password.toCharArray());
                }
                if (mqttConfig.connectMode > 0) {
                    if (TextUtils.isEmpty(mqttConfig.caPath)) {
                        // 单向不验证
                        try {
                            connOpts.setSocketFactory(getAllTMSocketFactory());
                            connOpts.setSSLHostnameVerifier(new HostnameVerifier() {
                                @Override
                                public boolean verify(String hostname, SSLSession session) {
                                    return true;
                                }
                            });
                        } catch (Exception e) {
                            // 读取stacktrace信息
                            final Writer result = new StringWriter();
                            final PrintWriter printWriter = new PrintWriter(result);
                            e.printStackTrace(printWriter);
                            StringBuffer errorReport = new StringBuffer();
                            errorReport.append(result.toString());
                            LogModule.e(errorReport.toString());
                        }
                    } else {
                        switch (mqttConfig.connectMode) {
                            case 1:
                                // 单向验证
                                try {
                                    connOpts.setSocketFactory(getSingleSocketFactory(mqttConfig.caPath));
                                } catch (Exception e) {
                                    // 读取stacktrace信息
                                    final Writer result = new StringWriter();
                                    final PrintWriter printWriter = new PrintWriter(result);
                                    e.printStackTrace(printWriter);
                                    StringBuffer errorReport = new StringBuffer();
                                    errorReport.append(result.toString());
                                    LogModule.e(errorReport.toString());
                                }
                                break;
                            case 3:
                                // 双向验证
                                try {
                                    connOpts.setSocketFactory(getSocketFactory(mqttConfig.caPath, mqttConfig.clientKeyPath, mqttConfig.clientCertPath));
                                } catch (Exception e) {
                                    // 读取stacktrace信息
                                    final Writer result = new StringWriter();
                                    final PrintWriter printWriter = new PrintWriter(result);
                                    e.printStackTrace(printWriter);
                                    StringBuffer errorReport = new StringBuffer();
                                    errorReport.append(result.toString());
                                    LogModule.e(errorReport.toString());
                                }
                                break;
                        }
                    }
                }
                try {
                    connectMqtt(connOpts);
                } catch (MqttException e) {
                    // 读取stacktrace信息
                    final Writer result = new StringWriter();
                    final PrintWriter printWriter = new PrintWriter(result);
                    e.printStackTrace(printWriter);
                    StringBuffer errorReport = new StringBuffer();
                    errorReport.append(result.toString());
                    LogModule.e(errorReport.toString());
                }
                return;
            }
        }
    }

    @Override
    public void onDestroy() {
        LogModule.i("关闭后台服务");
        super.onDestroy();
        disconnectMqtt();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (onCreate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onCreate = false;
            final String CHANNEL_ID = BuildConfig.APPLICATION_ID;
            final String CHANNEL_NAME = getString(R.string.app_name);
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
            Notification notification = new Notification.Builder(this, CHANNEL_ID).build();
            startForeground(1, notification);
        }
        return super.onStartCommand(intent, flags, startId);
    }


    static class AllTM implements TrustManager, X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(X509Certificate[] certs) {
            LogModule.i("isServerTrusted");
            for (java.security.cert.X509Certificate certificate : certs) {
                LogModule.w("Accepting:" + certificate);
            }
            return true;
        }

        public boolean isClientTrusted(X509Certificate[] certs) {
            LogModule.i("isClientTrusted");
            for (java.security.cert.X509Certificate certificate : certs) {
                LogModule.w("Accepting:" + certificate);
            }
            return true;
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            LogModule.i("Server authtype=" + authType);
            for (java.security.cert.X509Certificate certificate : certs) {
                LogModule.w("Accepting:" + certificate);
            }
            return;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            LogModule.i("Client authtype=" + authType);
            for (java.security.cert.X509Certificate certificate : certs) {
                LogModule.w("Accepting:" + certificate);
            }
            return;
        }
    }


    /**
     * 单向不验证
     *
     * @Date 2019/8/5
     * @Author wenzheng.liu
     * @Description
     */
    private SocketFactory getAllTMSocketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new AllTM();
        trustAllCerts[0] = tm;
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return sc.getSocketFactory();
    }

    /**
     * 单向验证
     *
     * @return
     * @throws Exception
     */

    private SSLSocketFactory getSingleSocketFactory(String caFile) throws Exception {
        // 中间证书地址
        Security.addProvider(new BouncyCastleProvider());

        X509Certificate caCert = null;

        FileInputStream fis = new FileInputStream(caFile);

        BufferedInputStream bis = new BufferedInputStream(fis);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        while (bis.available() > 0) {

            caCert = (X509Certificate) cf.generateCertificate(bis);

        }

        KeyStore caKs =
                KeyStore.getInstance(KeyStore.getDefaultType());

        caKs.load(null, null);

        caKs.setCertificateEntry("ca-certificate", caCert);

        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance("X509");

        tmf.init(caKs);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

        sslContext.init(null, tmf.getTrustManagers(), null);

        return sslContext.getSocketFactory();

    }

    /**
     * 双向验证
     *
     * @return
     * @throws Exception
     */
    private SSLSocketFactory getSocketFactory(String caFile, String clientKeyFile, String clientCertFile) throws Exception {

        FileInputStream ca = new FileInputStream(caFile);
        FileInputStream clientCert = new FileInputStream(clientCertFile);
        FileInputStream clientKey = new FileInputStream(clientKeyFile);
        Security.addProvider(new BouncyCastleProvider());
        // load CA certificate
        X509Certificate caCert = null;

        BufferedInputStream bis = new BufferedInputStream(ca);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        while (bis.available() > 0) {
            caCert = (X509Certificate) cf.generateCertificate(bis);
        }

        // load client certificate
        bis = new BufferedInputStream(clientCert);
        X509Certificate cert = null;
        while (bis.available() > 0) {
            cert = (X509Certificate) cf.generateCertificate(bis);
        }

        // load client private key
        PEMParser pemParser = new PEMParser(new InputStreamReader(clientKey));
        Object object = pemParser.readObject();
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                .build("".toCharArray());
        JcaPEMKeyConverter converter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            //适配Android P及以后版本，否则报错
            converter = new JcaPEMKeyConverter();
        } else {
            converter = new JcaPEMKeyConverter().setProvider("BC");
        }

        KeyPair key;
        if (object instanceof PEMEncryptedKeyPair) {
            LogModule.e("Encrypted key - we will use provided password");
            key = converter.getKeyPair(((PEMEncryptedKeyPair) object)
                    .decryptKeyPair(decProv));
        } else {
            LogModule.e("Unencrypted key - no password needed");
            key = converter.getKeyPair((PEMKeyPair) object);
        }
        pemParser.close();

        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate
        // us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", cert);
        ks.setKeyEntry("private-key", key.getPrivate(), "".toCharArray(),
                new java.security.cert.Certificate[]{cert});
        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory
                        .getDefaultAlgorithm());
        kmf.init(ks, "".toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }

    private void connectMqtt(MqttConnectOptions options) throws MqttException {
        if (mqttAndroidClient != null && !mqttAndroidClient.isConnected()) {
            mqttAndroidClient.connect(options, null, new ActionListener(this, ActionListener.Action.CONNECT));
        }
    }


    public void disconnectMqtt() {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                mqttAndroidClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            mqttAndroidClient = null;
        }
    }

    public void subscribe(String topic, int qos) throws MqttException {
        if (mqttAndroidClient != null&& mqttAndroidClient.isConnected()) {
            mqttAndroidClient.subscribe(topic, qos, null, new ActionListener(this, ActionListener.Action.SUBSCRIBE));
        }
    }

    public void unSubscribe(String topic) throws MqttException {
        if (mqttAndroidClient != null&& mqttAndroidClient.isConnected()) {
            mqttAndroidClient.unsubscribe(topic, null, new ActionListener(this, ActionListener.Action.UNSUBSCRIBE));
        }
    }

    public void publish(String topic, byte[] message, int qos) throws MqttException {
        if (mqttAndroidClient != null) {
            mqttAndroidClient.publish(topic, message, qos, false, null, new ActionListener(this, ActionListener.Action.PUBLISH));
        }
    }

    public boolean isConnected() {
        if (mqttAndroidClient != null) {
            return mqttAndroidClient.isConnected();
        }
        return false;
    }
}
