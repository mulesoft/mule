/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mule.processor.AbstractRedeliveryPolicy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JmsNoRedeliveryTestCase extends AbstractJmsRedeliveryTestCase
{

    public JmsNoRedeliveryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected int getMaxRedelivery()
    {
        return JmsConnector.REDELIVERY_FAIL_ON_FIRST;
    }

    @Override
    protected int getMaxRedeliveryAttempts()
    {
        return AbstractRedeliveryPolicy.REDELIVERY_FAIL_ON_FIRST;
    }

    @Test
    public void testNoRedelivery() throws Exception
    {
        client.dispatch(JMS_INPUT_QUEUE, TEST_MESSAGE, null);

        assertTrue(messageRedeliveryExceptionFired.await(timeout, TimeUnit.MILLISECONDS));
        assertEquals("MessageRedeliveredException never fired.", 0, messageRedeliveryExceptionFired.getCount());
        assertEquals("Wrong number of delivery attempts", 1, callback.getCallbackCount());

        assertMessageInDlq();
    }

    @Test
    public void testRedeliveryWithRollbackExceptionStrategy() throws Exception
    {
        client.dispatch(JMS_INPUT_QUEUE2, TEST_MESSAGE, null);

        assertTrue(messageRedeliveryExceptionFired.await(timeout, TimeUnit.MILLISECONDS));
        assertEquals("MessageRedeliveredException never fired.", 0, messageRedeliveryExceptionFired.getCount());
        assertEquals("Wrong number of delivery attempts", 1, callback.getCallbackCount());

        assertMessageInDlqRollbackEs();
    }



}
