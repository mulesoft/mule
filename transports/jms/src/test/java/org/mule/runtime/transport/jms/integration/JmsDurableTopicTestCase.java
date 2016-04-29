/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;

import org.junit.Ignore;
import org.junit.Test;

public class JmsDurableTopicTestCase extends AbstractJmsFunctionalTestCase
{
    private String clientId;

    @Override
    protected String getConfigFile()
    {
        return "integration/jms-durable-topic.xml";
    }

    @Test
    @Ignore("MULE-6926: Flaky test")
    public void testProviderDurableSubscriber() throws Exception
    {
        setClientId("Client1");
        receive(scenarioNotReceive);
        setClientId("Client2");
        receive(scenarioNotReceive);

        setClientId("Sender");
        send(scenarioNonTx);

        setClientId("Client1");
        receive(scenarioNonTx);
        receive(scenarioNotReceive);
        setClientId("Client2");
        receive(scenarioNonTx);
        receive(scenarioNotReceive);
    }

    Scenario scenarioNonTx = new NonTransactedScenario()
    {
        @Override
        public String getOutputDestinationName()
        {
            return getJmsConfig().getBroadcastDestinationName();
        }
    };

    Scenario scenarioNotReceive = new ScenarioNotReceive()
    {
        @Override
        public String getOutputDestinationName()
        {
            return getJmsConfig().getBroadcastDestinationName();
        }
    };

    @Override
    public Message receive(Scenario scenario) throws Exception
    {
        Connection connection = null;
        try
        {
            connection = getConnection(true, false);
            connection.setClientID(getClientId());
            connection.start();
            Session session = null;
            try
            {
                session = connection.createSession(scenario.isTransacted(), scenario.getAcknowledge());
                Topic destination = session.createTopic(scenario.getOutputDestinationName());
                MessageConsumer consumer = null;
                try
                {
                    consumer = session.createDurableSubscriber(destination, getClientId());
                    return scenario.receive(session, consumer);
                }
                finally
                {
                    if (consumer != null)
                    {
                        consumer.close();
                    }
                }
            }
            finally
            {
                if (session != null)
                {
                    session.close();
                }
            }
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }
}
