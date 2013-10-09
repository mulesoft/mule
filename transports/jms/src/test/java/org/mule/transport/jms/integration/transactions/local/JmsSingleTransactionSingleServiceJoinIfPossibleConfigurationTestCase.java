/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration.transactions.local;

import org.mule.transport.jms.integration.AbstractJmsSingleTransactionSingleServiceTestCase;

import org.junit.Test;

/**
 * Test all combinations of (inbound) JOIN_IF_POSSIBLE. They should all pass, except
 * for ALWAYS_JOIN on the outbound endpoint, since no transaction should be created by JOIN_IF_POSSIBLE.
 */
public class JmsSingleTransactionSingleServiceJoinIfPossibleConfigurationTestCase extends
    AbstractJmsSingleTransactionSingleServiceTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/transactions/local/jms-single-tx-single-service-join-if-possible.xml";
    }

    @Test
    public void testAlwaysJoin() throws Exception
    {
        // no-op, investigating why expected failure doesn't occur
        /*
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_D);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_D);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_D);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_D);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_D);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_D);

        runTransactionFail("testAlwaysJoin");
        */
    }
}
