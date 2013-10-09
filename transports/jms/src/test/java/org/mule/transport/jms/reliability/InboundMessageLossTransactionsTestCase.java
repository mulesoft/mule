/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.reliability;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;


/**
 * Verify that no inbound messages are lost when exceptions occur.
 * The message must either make it all the way to the SEDA queue (in the case of
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 *
 * In the case of JMS, this will cause the failed message to be redelivered if
 * JMSRedelivery is configured.
 */
public class InboundMessageLossTransactionsTestCase extends InboundMessageLossTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "reliability/activemq-config.xml, reliability/inbound-message-loss-transactions.xml";
    }

    public void testComponentException() throws Exception
    {
        putMessageOnQueue("componentException");

        // Although a component exception occurs after the SEDA queue, the use of transactions
        // bypasses the SEDA queue, so message should get redelivered.
        assertTrue("Message should have been redelivered",
            messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }
}
