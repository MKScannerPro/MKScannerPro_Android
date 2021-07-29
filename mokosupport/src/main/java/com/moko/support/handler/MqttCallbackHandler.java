/*******************************************************************************
 * Copyright (c) 1999, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package com.moko.support.handler;

import android.content.Context;
import android.content.Intent;

import com.moko.support.MokoConstants;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Handles call backs from the MQTT Client
 */
public class MqttCallbackHandler implements MqttCallbackExtended {

    /**
     * {@link Context} for the application used to format and import external strings
     **/
    private final Context context;

    private static final String TAG = "MqttCallbackHandler";

    /**
     * Creates an <code>MqttCallbackHandler</code> object
     *
     * @param context The application's context
     */
    public MqttCallbackHandler(Context context) {
        this.context = context;
    }


    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        LogModule.i(TAG + ":connectComplete:reconnect-->" + reconnect);
        Intent intent = new Intent(MokoConstants.ACTION_MQTT_CONNECTION);
        intent.putExtra(MokoConstants.EXTRA_MQTT_CONNECTION_STATE, MokoConstants.MQTT_CONN_STATUS_SUCCESS);
        context.sendBroadcast(intent);
    }

    /**
     * @see MqttCallback#connectionLost(Throwable)
     */
    @Override
    public void connectionLost(Throwable cause) {
        LogModule.i(TAG + ":connectionLost");
        Intent intent = new Intent(MokoConstants.ACTION_MQTT_CONNECTION);
        intent.putExtra(MokoConstants.EXTRA_MQTT_CONNECTION_STATE, MokoConstants.MQTT_CONN_STATUS_LOST);
        context.sendBroadcast(intent);
    }

    /**
     * @see MqttCallback#messageArrived(String, MqttMessage)
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        LogModule.i(topic + "--->" + MokoUtils.bytesToHexString(message.getPayload()));
        Intent intent = new Intent(MokoConstants.ACTION_MQTT_RECEIVE);
        intent.putExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC, topic);
        intent.putExtra(MokoConstants.EXTRA_MQTT_RECEIVE_MESSAGE, message.getPayload());
        context.sendBroadcast(intent);
    }

    /**
     * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Do nothing
    }
}
