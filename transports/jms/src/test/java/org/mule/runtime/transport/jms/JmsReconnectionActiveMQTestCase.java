/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.listener.ConnectionListener;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.context.notification.ConnectionNotification;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * MULE-7534: JMS connector doesn't reconnect to ActiveMQ broker 5.6 when using blocking=true
 * When using ActiveMQ 5.6, the createConnection() method of the connection factory returns an invalid connection
 * object instead of failing when the broker is down, in newer versions the createConnection() method throws an
 * exception. To reproduce the problem a custom connection factory is used that returns invalid Connection objects
 * when needed.
 */
public class JmsReconnectionActiveMQTestCase extends AbstractBrokerFunctionalTestCase
{

    private static final long PROBER_TIMEOUT = 3000;

    private Prober prober;
    private JmsConnector jmsConnector;

    @Override
    protected String getConfigFile()
    {
        return "jms-reconnection-activemq-config.xml";
    }

    @Override
    @Before
    public void doSetUp()
    {
        prober = new PollingProber(PROBER_TIMEOUT, PollingProber.DEFAULT_POLLING_INTERVAL);
    }

    @Test
    public void reconnectsAfterRestartingActiveMQBroker() throws Exception
    {
        jmsConnector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector");

        assertMessageRouted();

        // Stop the broker, and make the connection factory return invalid connections.
        ConnectionListener connectionListener = new ConnectionListener(muleContext)
                .setExpectedAction(ConnectionNotification.CONNECTION_FAILED).setNumberOfExecutionsRequired(3);

        CustomConnectionFactory.returnInvalidConnections = true;
        stopBroker();

        connectionListener.waitUntilNotificationsAreReceived();
        assertTrue(jmsConnector.isStopped());


        // Restart the broker
        CustomConnectionFactory.returnInvalidConnections = false;
        startBroker();

        // Wait until jmsConnector is reconnected and started.
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return jmsConnector.isStarted();
            }

            @Override
            public String describeFailure()
            {
                return "JMS connector did not restart";
            }
        });

        // Check that reconnection worked
        assertMessageRouted();
    }

    private void assertMessageRouted() throws Exception
    {
        flowRunner("put").withPayload(TEST_MESSAGE).run();
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
