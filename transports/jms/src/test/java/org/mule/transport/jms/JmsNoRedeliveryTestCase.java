/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
