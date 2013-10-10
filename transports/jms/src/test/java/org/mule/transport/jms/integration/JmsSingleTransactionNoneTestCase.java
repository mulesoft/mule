/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.junit.Test;

/**
 * Send and recieve JmsMessage without any tx
 */
public class JmsSingleTransactionNoneTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-single-tx-NONE.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        purge(getInboundQueueName());
        purge(getJmsConfig().getMiddleDestinationName());
        purge(getOutboundQueueName());
    }

    @Test
    public void testNoneTx() throws Exception
    {
        send(scenarioNoTx);
        receive(scenarioNoTx);
        receive(scenarioNotReceive);
    }

}
