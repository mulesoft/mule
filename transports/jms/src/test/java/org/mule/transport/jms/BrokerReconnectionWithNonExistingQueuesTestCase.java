/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.context.notification.ConnectionNotification.CONNECTION_CONNECTED;
import static org.mule.context.notification.ConnectionNotification.CONNECTION_FAILED;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageReceiver;
import org.mule.context.notification.ConnectionNotification;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.listener.ConnectionListener;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

public class BrokerReconnectionWithNonExistingQueuesTestCase extends FunctionalTestCase
{

    private final Logger LOGGER = getLogger(BrokerReconnectionWithNonExistingQueuesTestCase.class);

    private static final int CONSUMER_COUNT = 1;

    private JmsConnector connector;
    private Connection connection;
    @Rule
    public DynamicPort port = new DynamicPort("port");

    protected BrokerService broker;
    protected TransportConnector transportConnector;
    protected String url;
    private ConnectionListener jmsConnectionListener;
    private ActiveMQSession adminSession;
    private Queue testQueue;

    @Override
    protected String getConfigFile()
    {
        return "jms-connection-reconnection-with-broker-reconnection.xml";
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        url = "tcp://localhost:" + this.port.getValue();

        System.setProperty("java.security.auth.login.config", getClasspathResourceFilename("JaasBrokerAuthPluginConfigs/login.config"));
        broker = BrokerFactory.createBroker("xbean:" + getClasspathResourceFilename("JaasBrokerAuthPluginConfigs/jaas-authenticated-broker-configuration.xml"));

        transportConnector = broker.addConnector(this.url);

        startBrokerAndAdminSession();
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // Connection listener used to be notified of JMS connector failures
        jmsConnectionListener = new ConnectionListener(muleContext)
                .setExpectedAction(CONNECTION_FAILED)
                .setNumberOfExecutionsRequired(1);
    }

    @Test
    public void connectorReconnectionWithForeverRetryPolicy() throws Exception
    {
        // Start broker with no 'test' queue created, and since a permissions-granted-user is required to create the queue
        // connection will fail.

        // Expect connection failure notification to be fired, caused by not being able to create test queue
        jmsConnectionListener.waitUntilNotificationsAreReceived();

        // Disconnect broker.
        stopBroker();
        // Expect connection failure due to underlying transport termination
        jmsConnectionListener
                .setExpectedAction(1)
                .waitUntilNotificationsAreReceived();

        // Reconnect broker.
        startBrokerAndAdminSession();
        jmsConnectionListener.setExpectedAction(1).waitUntilNotificationsAreReceived();

        // Create queue, and test message receiver connectivity.
        createTestQueue();
        jmsConnectionListener
                .setExpectedAction(CONNECTION_CONNECTED)
                .setNumberOfExecutionsRequired(1)
                .waitUntilNotificationsAreReceived();

        connector = muleContext.getRegistry().lookupObject("activeMQConnector");
        assertConsumersConnected();

        assertReceiverReceivesMessage();
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        stopBroker();
    }

    protected void startBrokerAndAdminSession() throws Exception
    {
        broker.start(true);
        broker.waitUntilStarted();

        connection = new ActiveMQConnectionFactory(this.url).createQueueConnection("admin", "admin");
        connection.start();

        // Create admin session for further queue creation and connection testing
        adminSession = (ActiveMQSession) connection.createSession(false, AUTO_ACKNOWLEDGE);
        testQueue = adminSession.createQueue("test");
    }

    protected void stopBroker() throws Exception
    {
        connection.close();

        broker.stop();
        broker.waitUntilStopped();
    }

    private void createTestQueue() throws JMSException
    {
        LOGGER.error("Creating test queue");
        // By creating a receiver/producer with admin privileges listening/connecting to the 'test' queue,
        // it is created by the broker
        adminSession.createProducer(testQueue);
    }

    private void assertConsumersConnected()
    {
        assertThat(connector.getReceivers().size(), is(CONSUMER_COUNT));

        for (MessageReceiver messageReceiver : connector.getReceivers().values())
        {
            MultiConsumerJmsMessageReceiver receiver = (MultiConsumerJmsMessageReceiver) messageReceiver;
            for (MultiConsumerJmsMessageReceiver.SubReceiver consumer : receiver.consumers)
            {
                assertThat(consumer.connected, is(true));
            }
        }
    }

    private void assertReceiverReceivesMessage() throws Exception
    {
        this.runFlow("put", TEST_MESSAGE);

        MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message, notNullValue());
        assertThat((String) message.getPayload(), is(TEST_MESSAGE));
    }

    private String getClasspathResourceFilename(String s)
    {
        return getClass().getClassLoader().getResource(s).getFile();
    }
}

