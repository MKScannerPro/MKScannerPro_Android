package com.moko.support;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.support.entity.MQTTConfig;
import com.moko.support.event.MQTTConnectionCompleteEvent;
import com.moko.support.event.MQTTConnectionFailureEvent;
import com.moko.support.event.MQTTConnectionLostEvent;
import com.moko.support.event.MQTTMessageArrivedEvent;
import com.moko.support.event.MQTTPublishFailureEvent;
import com.moko.support.event.MQTTPublishSuccessEvent;
import com.moko.support.event.MQTTSubscribeFailureEvent;
import com.moko.support.event.MQTTSubscribeSuccessEvent;
import com.moko.support.event.MQTTUnSubscribeFailureEvent;
import com.moko.support.event.MQTTUnSubscribeSuccessEvent;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

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

public class MQTTSupport {
    private static final String TAG = "MQTTSupport";

    private static volatile MQTTSupport INSTANCE;

    private Context mContext;


    private MqttAndroidClient mqttAndroidClient;
    private IMqttActionListener listener;

    private MQTTSupport() {
        //no instance
    }

    public static MQTTSupport getInstance() {
        if (INSTANCE == null) {
            synchronized (MQTTSupport.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MQTTSupport();
                }
            }
        }
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        listener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                XLog.w(String.format("%s:%s", TAG, "connect success"));
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                XLog.w(String.format("%s:%s", TAG, "connect failure"));
                if (mqttAndroidClient != null) {
                    mqttAndroidClient.close();
                    try {
                        mqttAndroidClient.disconnect();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    mqttAndroidClient = null;
                }
                EventBus.getDefault().post(new MQTTConnectionFailureEvent());
            }
        };
    }

    public void connectMqtt(String mqttAppConfigStr) {
        if (TextUtils.isEmpty(mqttAppConfigStr))
            return;
        MQTTConfig mqttConfig = new Gson().fromJson(mqttAppConfigStr, MQTTConfig.class);
        if (!mqttConfig.isError()) {
            String uri;
            if (mqttConfig.connectMode > 0) {
                uri = "ssl://" + mqttConfig.host + ":" + mqttConfig.port;
            } else {
                uri = "tcp://" + mqttConfig.host + ":" + mqttConfig.port;
            }
            mqttAndroidClient = new MqttAndroidClient(mContext, uri, mqttConfig.clientId);
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (reconnect) {
                        XLog.w("Reconnected to : " + serverURI);
                    } else {
                        XLog.w("Connected to : " + serverURI);
                    }
                    EventBus.getDefault().post(new MQTTConnectionCompleteEvent());
                }

                @Override
                public void connectionLost(Throwable cause) {
                    EventBus.getDefault().post(new MQTTConnectionLostEvent());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
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

                switch (mqttConfig.connectMode) {
                    case 1:
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
                            XLog.e(errorReport.toString());
                        }
                        break;
                    case 2:
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
                            XLog.e(errorReport.toString());
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
                            XLog.e(errorReport.toString());
                        }
                        break;
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
                XLog.e(errorReport.toString());
            }
            return;
        }
    }

    static class AllTM implements TrustManager, X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(X509Certificate[] certs) {
            XLog.d("isServerTrusted");
            for (java.security.cert.X509Certificate certificate : certs) {
                XLog.w("Accepting:" + certificate);
            }
            return true;
        }

        public boolean isClientTrusted(X509Certificate[] certs) {
            XLog.d("isClientTrusted");
            for (java.security.cert.X509Certificate certificate : certs) {
                XLog.w("Accepting:" + certificate);
            }
            return true;
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            XLog.d("Server authtype=" + authType);
            for (java.security.cert.X509Certificate certificate : certs) {
                XLog.w("Accepting:" + certificate);
            }
            return;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            XLog.d("Client authtype=" + authType);
            for (java.security.cert.X509Certificate certificate : certs) {
                XLog.w("Accepting:" + certificate);
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
            XLog.e("Encrypted key - we will use provided password");
            key = converter.getKeyPair(((PEMEncryptedKeyPair) object)
                    .decryptKeyPair(decProv));
        } else {
            XLog.e("Unencrypted key - no password needed");
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
            mqttAndroidClient.connect(options, null, listener);
        }
    }

    public void disconnectMqtt() {
        if (!isConnected())
            return;
        mqttAndroidClient.close();
        try {
            mqttAndroidClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mqttAndroidClient = null;
    }

    public void subscribe(String topic, int qos) throws MqttException {
        if (!isConnected())
            return;
        mqttAndroidClient.subscribe(topic, qos, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "subscribe success"));
                EventBus.getDefault().post(new MQTTSubscribeSuccessEvent(topic));
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "subscribe failure"));
                EventBus.getDefault().post(new MQTTSubscribeFailureEvent(topic));
            }
        });
        mqttAndroidClient.subscribe(topic, qos, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String messageInfo = new String(message.getPayload());
                XLog.w(String.format("Message:%s:%s", topic, messageInfo));
                EventBus.getDefault().post(new MQTTMessageArrivedEvent(topic, new String(message.getPayload())));
            }
        });

    }

    public void unSubscribe(String topic) throws MqttException {
        if (!isConnected())
            return;
        mqttAndroidClient.unsubscribe(topic, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "unsubscribe success"));
                EventBus.getDefault().post(new MQTTUnSubscribeSuccessEvent(topic));
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "unsubscribe failure"));
                EventBus.getDefault().post(new MQTTUnSubscribeFailureEvent(topic));
            }
        });

    }

    public void publish(String topic, String message, int msgId, int qos) throws MqttException {
        if (!isConnected())
            return;
        MqttMessage messageInfo = new MqttMessage();
        messageInfo.setPayload(message.getBytes());
        messageInfo.setQos(qos);
        messageInfo.setRetained(false);
        mqttAndroidClient.publish(topic, messageInfo, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "publish success"));
                EventBus.getDefault().post(new MQTTPublishSuccessEvent(topic, msgId));
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "publish failure"));
                EventBus.getDefault().post(new MQTTPublishFailureEvent(topic, msgId));
            }
        });
    }

    public boolean isConnected() {
        if (mqttAndroidClient != null) {
            return mqttAndroidClient.isConnected();
        }
        return false;
    }
}
