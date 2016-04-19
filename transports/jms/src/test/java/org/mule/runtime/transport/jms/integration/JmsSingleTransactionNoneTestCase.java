/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import org.junit.Test;

/**
 * Send and recieve JmsMessage without any tx
 */
public class JmsSingleTransactionNoneTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigFile()
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
