/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import org.junit.Test;

/**
 * Testing durable topic with external subscriber
 */
public class JmsDurableTopicSingleTxTestCase extends JmsDurableTopicTestCase
{
    @Override
    protected String getConfigFile()
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
