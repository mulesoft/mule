/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.util.Collection;

import javax.jms.Connection;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;

public class JmsReconnectForeverTestCase extends AbstractBrokerFunctionalTestCase
{

    private static final int CONSUMER_COUNT = 1;
    private static final int TIMEOUT_MILLIS = 5000;
    private static final String PAYLOAD = "HELLO";
    private static final int POLL_DELAY_MILLIS = 100;

    private JmsConnector connector;
    private Connection connection;

    @Override
    protected String getConfigFile()
    {
        return "jms-reconnect-forever-config.xml";
    }

    @Override
    protected void startBroker() throws Exception
    {
        super.startBroker();

        // this is needed because for some reason the broker will reject any connections
        // otherwise
        connection = new ActiveMQConnectionFactory(this.url).createQueueConnection();
    }

    @Override
    protected void stopBroker() throws Exception
    {
        connection.close();
        super.stopBroker();
    }

    @Test
    public void reconnectAllConsumers() throws Exception
    {
        connector = muleContext.getRegistry().lookupObject("activemqconnector");

        Collection<MessageReceiver> receivers = connector.getReceivers().values();
        assertTrue(receivers != null && receivers.size() == 2);
        Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                boolean allConsumersSet = true;
                for (MessageReceiver messageReceiver : connector.getReceivers().values())
                {
                    MultiConsumerJmsMessageReceiver receiver = (MultiConsumerJmsMessageReceiver) messageReceiver;
                    allConsumersSet = allConsumersSet && (CONSUMER_COUNT == receiver.consumers.size());
                }
                return allConsumersSet;
            }

            @Override
            public String describeFailure()
            {
                return "Not all consumers were created.";
            }
        });
        this.assertConsumersConnected();
        this.assertMessageRouted("1");
        this.assertMessageRouted("2");
    }

    private void assertConsumersConnected()
    {
        for (MessageReceiver messageReceiver : connector.getReceivers().values())
        {
            MultiConsumerJmsMessageReceiver receiver = (MultiConsumerJmsMessageReceiver) messageReceiver;
            for (MultiConsumerJmsMessageReceiver.SubReceiver consumer : receiver.consumers)
            {
                assertThat(consumer.connected, is(true));
            }
        }

    }

    private void assertMessageRouted(String flow) throws Exception
    {
        this.runFlow("put" + flow, PAYLOAD);
        MuleMessage message = muleContext.getClient().request("vm://out" + flow, TIMEOUT_MILLIS);
        assertThat(message, notNullValue());
        assertThat(message.getPayloadAsString(), is(PAYLOAD));
    }
}

