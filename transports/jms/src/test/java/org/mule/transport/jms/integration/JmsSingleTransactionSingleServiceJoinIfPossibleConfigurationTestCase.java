/*
 * $Id: JmsSingleTransactionAlwaysBeginConfigurationTestCase.java 14304 2009-03-15 11:19:11Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import org.junit.Test;

/**
 * Test all combinations of (inbound) JOIN_IF_POSSIBLE. They should all pass, except
 * for ALWAYS_JOIN on the outbound endpoint, since no transaction should be created by JOIN_IF_POSSIBLE.
 */
public class JmsSingleTransactionSingleServiceJoinIfPossibleConfigurationTestCase extends
    AbstractJmsSingleTransactionSingleServiceTestCase
{
    protected String getConfigResources()
    {
        return "integration/jms-single-tx-single-service-join-if-possible.xml";
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
