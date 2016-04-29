/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration.transactions.local;

import org.mule.runtime.transport.jms.integration.AbstractJmsSingleTransactionSingleServiceTestCase;

import org.junit.Test;

/**
 * Test all combinations of (inbound) JOIN_IF_POSSIBLE. They should all pass, except
 * for ALWAYS_JOIN on the outbound endpoint, since no transaction should be created by JOIN_IF_POSSIBLE.
 */
public class JmsSingleTransactionSingleServiceJoinIfPossibleConfigurationTestCase extends
    AbstractJmsSingleTransactionSingleServiceTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "integration/transactions/local/jms-single-tx-single-service-join-if-possible.xml";
    }

    @Override
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
