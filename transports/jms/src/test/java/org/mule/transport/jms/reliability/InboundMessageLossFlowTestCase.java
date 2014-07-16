/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.reliability;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.TimeUnit;


/**
 * Verify that no inbound messages are lost when exceptions occur.
 * The message must either make it all the way to the SEDA queue (in the case of
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 *
 * In the case of JMS, this will cause the failed message to be redelivered if
 * JMSRedelivery is configured.
 */
public class InboundMessageLossFlowTestCase extends InboundMessageLossTestCase
{
    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
            "reliability/activemq-config.xml",
            "reliability/inbound-message-loss-flow.xml"
        };
    }

    @Override
    public void testTransformerException() throws Exception
    {
        putMessageOnQueue("transformerException");

        // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        // Note that this behavior is different from services because the exception occurs before
        // the SEDA queue for services.
        assertFalse("Message should not have been redelivered",
            messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }

    @Override
    public void testRouterException() throws Exception
    {
        putMessageOnQueue("routerException");

        // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        // Note that this behavior is different from services because the exception occurs before
        // the SEDA queue for services.
        assertFalse("Message should not have been redelivered",
            messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }
}
