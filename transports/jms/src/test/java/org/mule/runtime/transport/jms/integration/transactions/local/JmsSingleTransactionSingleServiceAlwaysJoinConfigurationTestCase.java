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
 * Test all combinations of (inbound) ALWAYS_JOIN.  They should all fail.
 */
public class JmsSingleTransactionSingleServiceAlwaysJoinConfigurationTestCase extends
    AbstractJmsSingleTransactionSingleServiceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "integration/transactions/local/jms-single-tx-single-service-always-join.xml";
    }

    @Override
    @Test
    public void testNone() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_A);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_A);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_A);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_A);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_A);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_A);

        runTransactionFail("testNone");
    }

    @Override
    @Test
    public void testAlwaysBegin() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_B);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_B);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_B);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_B);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_B);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_B);

        runTransactionFail("testAlwaysBegin");
    }

    @Override
    @Test
    public void testBeginOrJoin() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_C);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_C);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_C);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_C);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_C);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_C);

        runTransactionFail("testBeginOrJoin");
    }

    @Override
    @Test
    public void testAlwaysJoin() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_D);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_D);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_D);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_D);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_D);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_D);

        runTransactionFail("testAlwaysJoin");
    }

    @Override
    @Test
    public void testJoinIfPossible() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_E);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_E);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_E);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_E);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_E);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_E);

        runTransactionFail("testJoinIfPossible");
    }
}
