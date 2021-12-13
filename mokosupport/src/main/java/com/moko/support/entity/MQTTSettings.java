package com.moko.support.entity;

public class MQTTSettings {

    /**
     * wifi_ssid : ysh1234
     * wifi_passwd : 12345678
     * connect_mode : 3
     * mqtt_host : 47.104.81.55
     * mqtt_port : 1883
     * mqtt_username :
     * mqtt_passwd :
     * clean_session : 0
     * keep_alive : 60
     * qos : 0
     * subscribe_topic : app_to_device
     * publish_topic : device_to_app
     * client_id : client_1234
     * ssl_host : 192.168.1.99
     * ssl_port : 80
     * ca_way : /update_fold/zaws-root-ca.pem
     * client_cer_way : /update_fold/zdevice-certificate.pem.crt
     * client_key_way : /update_fold/zdevice-private.pem.key
     */

    public String wifi_ssid;
    public String wifi_passwd;
    public int connect_mode;
    public String mqtt_host;
    public int mqtt_port;
    public String mqtt_username;
    public String mqtt_passwd;
    public int clean_session = 1;
    public int keep_alive = 60;
    public int qos = 0;
    public String subscribe_topic;
    public String publish_topic;
    public String client_id;
    public String ssl_host = "";
    public int ssl_port;
    public String ca_way = "";
    public String client_cer_way = "";
    public String client_key_way = "";
}
