/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.context.notification.ConnectionNotification;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.listener.ConnectionListener;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * MULE-7534: JMS connector doesn't reconnect to ActiveMQ broker 5.6 when using blocking=true
 * When using ActiveMQ 5.6, the createConnection() method of the connection factory returns an invalid connection
 * object instead of failing when the broker is down, in newer versions the createConnection() method throws an
 * exception. To reproduce the problem a custom connection factory is used that returns invalid Connection objects
 * when needed.
 */
public class JmsReconnectionActiveMQTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    private BrokerService broker;

    @Override
    protected String getConfigResources()
    {
        return "jms-reconnection-activemq-config.xml";
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        startBroker();
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        stopBroker();
    }

    private void startBroker() throws Exception
    {
        broker = new BrokerService();
        broker.setUseJmx(false);
        broker.setPersistent(false);
        broker.addConnector("tcp://localhost:" + this.port.getValue());
        broker.start(true);
        broker.waitUntilStarted();
    }

    private void stopBroker() throws Exception
    {
        broker.stop();
        broker.waitUntilStopped();
    }

    @Test
    public void reconnectsAfterRestartingActiveMQBroker() throws Exception
    {
        assertMessageRouted();

        // Stop the broker, and make the connection factory return invalid connections.
        ConnectionListener connectionListener = new ConnectionListener(muleContext)
                .setExpectedAction(ConnectionNotification.CONNECTION_FAILED).setNumberOfExecutionsRequired(3);

        CustomConnectionFactory.returnInvalidConnections = true;
        stopBroker();

        connectionListener.waitUntilNotificationsAreReceived();

        // Restart the broker
        ConnectionListener reconnectionListener = new ConnectionListener(muleContext)
                .setExpectedAction(ConnectionNotification.CONNECTION_CONNECTED).setNumberOfExecutionsRequired(1);

        CustomConnectionFactory.returnInvalidConnections = false;
        startBroker();

        reconnectionListener.waitUntilNotificationsAreReceived();

        // Check that reconnection worked
        assertMessageRouted();
    }

    private void assertMessageRouted() throws Exception
    {
        this.runFlow("put", TEST_MESSAGE);
        MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayload());
    }


    private static class CustomConnectionFactory extends ActiveMQConnectionFactory
    {

        public static boolean returnInvalidConnections = false;

        @Override
        public Connection createConnection() throws JMSException
        {
            if (returnInvalidConnections)
            {
                Connection invalidConnection = Mockito.mock(Connection.class, Mockito.RETURNS_DEEP_STUBS);
                Mockito.doThrow(new JMSException("Fail to start connection")).when(invalidConnection).start();

                return invalidConnection;
            }
            else
            {
                return super.createConnection();
            }
        }

    }

}
