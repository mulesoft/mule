/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.reliability;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;


/**
 * Verify that no inbound messages are lost when exceptions occur.
 * The message must either make it all the way to the SEDA queue (in the case of
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 * <p/>
 * In the case of JMS, this will cause the failed message to be redelivered if
 * JMSRedelivery is configured.
 */
public class InboundMessageLossTransactionsTestCase extends InboundMessageLossTestCase
{

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
                "reliability/activemq-config.xml",
                "reliability/inbound-message-loss-flow-transactions.xml"
        };
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    @Override
    public void testComponentException() throws Exception
    {
        putMessageOnQueue("componentException");

        // Although a component exception occurs after the SEDA queue, the use of transactions
        // bypasses the SEDA queue, so message should get redelivered.
        assertTrue("Message should have been redelivered",
                   messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }

    @Test
    @Override
    public void testTransformerException() throws Exception
    {
        putMessageOnQueue("transformerException");

        assertTrue("Message should have been redelivered",
                   messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }

    @Test
    @Override
    public void testRouterException() throws Exception
    {
        putMessageOnQueue("routerException");

        assertTrue("Message should have been redelivered",
                   messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }
}
