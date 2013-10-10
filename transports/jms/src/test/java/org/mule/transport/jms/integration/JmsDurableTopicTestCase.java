/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;

import org.junit.Test;

public class JmsDurableTopicTestCase extends AbstractJmsFunctionalTestCase
{

    private String clientId;

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-durable-topic.xml";
    }

    @Test
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
