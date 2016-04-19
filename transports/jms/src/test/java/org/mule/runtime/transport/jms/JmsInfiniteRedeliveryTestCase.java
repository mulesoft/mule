/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class JmsInfiniteRedeliveryTestCase extends AbstractJmsRedeliveryTestCase
{

    public static final int DEFAULT_REDELIVERY = 6;

    @Override
    protected int getMaxRedelivery()
    {
        return JmsConnector.REDELIVERY_IGNORE;
    }

    @Override
    protected int getMaxRedeliveryAttempts()
    {
        return Integer.MAX_VALUE;
    }

    @Test
    public void testInfiniteRedelivery() throws Exception
    {
        client.dispatch(JMS_INPUT_QUEUE, TEST_MESSAGE, null);

        assertFalse(messageRedeliveryExceptionFired.await(timeout, TimeUnit.MILLISECONDS));
        assertTrue(callback.getCallbackCount() > DEFAULT_REDELIVERY + 1);
        assertNoMessageInDlq(JMS_DEAD_LETTER);
    }

    @Test
    public void testRedeliveryWithRollbackExceptionStrategy() throws Exception
    {
        client.dispatch(JMS_INPUT_QUEUE2, TEST_MESSAGE, null);

        assertFalse(messageRedeliveryExceptionFired.await(timeout, TimeUnit.MILLISECONDS));
        assertTrue(callback.getCallbackCount() > DEFAULT_REDELIVERY + 1);
        assertNoMessageInDlq(JMS_DEAD_LETTER);
    }
}
