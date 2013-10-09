/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.junit.Test;

/**
 * Testing durable topic with external subscriber
 */
public class JmsDurableTopicSingleTxTestCase extends JmsDurableTopicTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-durable-topic-single-tx.xml";
    }

    /**
     * @throws Exception
     */
    @Override
    @Test
    public void testProviderDurableSubscriber() throws Exception
    {
        setClientId("Client1");
        receive(scenarioNotReceive);
        setClientId("Client2");
        receive(scenarioNotReceive);

        setClientId("Sender");
        send(scenarioCommit);

        setClientId("Client1");
        receive(scenarioCommit);
        receive(scenarioNotReceive);
        setClientId("Client2");
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);

    }

    Scenario scenarioCommit = new ScenarioCommit()
    {

        @Override
        public String getOutputDestinationName()
        {
            return getJmsConfig().getBroadcastDestinationName();
        }
    };

    Scenario scenarioRollback = new ScenarioRollback()
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
}
