// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.augeg4.util;

import it.bz.idm.bdp.augeg4.fun.retrieve.AugeCallback;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class AugeMqttClient {

    private static final Logger LOG = LoggerFactory.getLogger(AugeMqttClient.class.getName());

    public static MqttClient build(AugeMqttConfiguration mqttConfiguration) throws MqttException {
        MqttClient client = buildMqttClient(mqttConfiguration);
        LOG.debug("connect...:");
        MqttConnectOptions genericConnOpts = buildConnOpts(mqttConfiguration);
        genericConnOpts.setConnectionTimeout(5000);
        genericConnOpts.setMaxInflight(6000);
		client.connect(genericConnOpts);
        return client;
    }

    public static MqttClient build(AugeMqttConfiguration mqttConfiguration, AugeCallback callback) throws MqttException  {
        MqttClient client = buildMqttClient(mqttConfiguration);
        client.setCallback(callback);
        LOG.debug("connect...:");
        MqttConnectOptions genericConnOpts = buildConnOpts(mqttConfiguration);
        genericConnOpts.setAutomaticReconnect(true);
        client.connect(genericConnOpts);
        return client;
    }

    private static MqttClient buildMqttClient(AugeMqttConfiguration mqttConfiguration) throws MqttException {
        MemoryPersistence persistence = new MemoryPersistence();
        String serverPort = mqttConfiguration.getServerPort();
        String serverURL = mqttConfiguration.getServerURI() + ":" + serverPort;
        String clientID = mqttConfiguration.getClientID();
        String topic = mqttConfiguration.getTopic();
        LOG.debug("MQTT client with:" + serverURL + " " + clientID + " " + topic);
        return new MqttClient(serverURL, clientID, persistence);
    }

    private static MqttConnectOptions buildConnOpts(AugeMqttConfiguration mqttConfiguration) {
        String userName = mqttConfiguration.getUserName();
        String userPass = mqttConfiguration.getUserPass();
        LOG.debug("Connection with:" + userName + " (password omitted)");
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(false);
        connOpts.setPassword(userPass.toCharArray());
        connOpts.setUserName(userName);
        return connOpts;
    }
}
