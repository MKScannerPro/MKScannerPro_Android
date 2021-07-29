package com.moko.support.callback;

import android.content.Context;
import android.content.Intent;

import com.moko.support.MokoConstants;
import com.moko.support.log.LogModule;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

public class ActionListener implements IMqttActionListener {

    private static final String TAG = "ActionListener";

    /**
     * Actions that can be performed Asynchronously <strong>and</strong> associated with a
     * {@link ActionListener} object
     */
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

    /**
     * The {@link Action} that is associated with this instance of
     * <code>ActionListener</code>
     **/
    private final Action action;
    /**
     * {@link Context} for performing various operations
     **/
    private final Context context;

    /**
     * Creates a generic action listener for actions performed form any activity
     *
     * @param context The application context
     * @param action  The action that is being performed
     */
    public ActionListener(Context context, Action action) {
        this.context = context;
        this.action = action;
    }

    /**
     * The action associated with this listener has been successful.
     *
     * @param asyncActionToken This argument is not used
     */
    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        switch (action) {
            case CONNECT:
                connect();
                break;
            case SUBSCRIBE:
                subscribe(asyncActionToken.getTopics()[0]);
                break;
            case PUBLISH:
                publish();
                break;
            case UNSUBSCRIBE:
                unsubscribe();
                break;
        }

    }

    /**
     * A publish action has been successfully completed, update connection
     * object associated with the client this action belongs to, then notify the
     * user of success
     */
    private void publish() {
        LogModule.i(TAG + ":publish Success");
        Intent intent = new Intent(MokoConstants.ACTION_MQTT_PUBLISH);
        intent.putExtra(MokoConstants.EXTRA_MQTT_STATE, MokoConstants.MQTT_STATE_SUCCESS);
        context.sendBroadcast(intent);
    }

    /**
     * A addNewSubscription action has been successfully completed, update the connection
     * object associated with the client this action belongs to and then notify
     * the user of success
     */
    private void subscribe(String topic) {
        LogModule.i(TAG + ":" + topic + ":subscribe Success");
        Intent intent = new Intent(MokoConstants.ACTION_MQTT_SUBSCRIBE);
        intent.putExtra(MokoConstants.EXTRA_MQTT_RECEIVE_TOPIC, topic);
        intent.putExtra(MokoConstants.EXTRA_MQTT_STATE, MokoConstants.MQTT_STATE_SUCCESS);
        context.sendBroadcast(intent);
    }

    private void unsubscribe() {
        LogModule.i(TAG + ":unsubscribe Success");
        Intent intent = new Intent(MokoConstants.ACTION_MQTT_UNSUBSCRIBE);
        intent.putExtra(MokoConstants.EXTRA_MQTT_STATE, MokoConstants.MQTT_STATE_SUCCESS);
        context.sendBroadcast(intent);
    }

    /**
     * A connection action has been successfully completed, update the
     * connection object associated with the client this action belongs to and
     * then notify the user of success.
     */
    private void connect() {
        LogModule.i(TAG + ":connect Success");
    }

    /**
     * The action associated with the object was a failure
     *
     * @param token     This argument is not used
     * @param exception The exception which indicates why the action failed
     */
    @Override
    public void onFailure(IMqttToken token, Throwable exception) {
        switch (action) {
            case CONNECT:
                connect(exception);
                break;
            case SUBSCRIBE:
                subscribe(exception);
                break;
            case PUBLISH:
                publish(exception);
                break;
            case UNSUBSCRIBE:
                unsubscribe(exception);
                break;
        }

    }

    /**
     * A publish action was unsuccessful, notify user and update client history
     *
     * @param exception This argument is not used
     */
    private void publish(Throwable exception) {
        LogModule.i(TAG + ":publish Failed");
        Intent intent = new Intent(MokoConstants.ACTION_MQTT_PUBLISH);
        intent.putExtra(MokoConstants.EXTRA_MQTT_STATE, MokoConstants.MQTT_STATE_FAILED);
        context.sendBroadcast(intent);
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    /**
     * A addNewSubscription action was unsuccessful, notify user and update client history
     *
     * @param exception This argument is not used
     */
    private void subscribe(Throwable exception) {
        LogModule.i(TAG + ":subscribe Failed");
        Intent intent = new Intent(MokoConstants.ACTION_MQTT_SUBSCRIBE);
        intent.putExtra(MokoConstants.EXTRA_MQTT_STATE, MokoConstants.MQTT_STATE_FAILED);
        context.sendBroadcast(intent);
        if (exception != null) {
            exception.printStackTrace();
        }
    }


    private void unsubscribe(Throwable exception) {
        LogModule.i(TAG + ":unsubscribe Failed");
        Intent intent = new Intent(MokoConstants.ACTION_MQTT_UNSUBSCRIBE);
        intent.putExtra(MokoConstants.EXTRA_MQTT_STATE, MokoConstants.MQTT_STATE_FAILED);
        context.sendBroadcast(intent);
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    /**
     * A connect action was unsuccessful, notify the user and update client history
     *
     * @param exception This argument is not used
     */
    private void connect(Throwable exception) {
        LogModule.i(TAG + ":connect Failed");
        Intent intent = new Intent(MokoConstants.ACTION_MQTT_CONNECTION);
        intent.putExtra(MokoConstants.EXTRA_MQTT_CONNECTION_STATE, MokoConstants.MQTT_CONN_STATUS_FAILED);
        context.sendBroadcast(intent);
        if (exception != null) {
            exception.printStackTrace();
        }
    }

}