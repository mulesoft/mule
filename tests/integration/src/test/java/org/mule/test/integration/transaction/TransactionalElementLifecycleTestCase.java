/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.junit.Assert.fail;

import org.mule.api.MuleEventContext;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TransactionalElementLifecycleTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transaction/transactional-lifecycle-config.xml";
    }


    @Test
    public void testInitializeIsCalledInInnerExceptionStrategy() throws Exception
    {
        final Latch endDlqFlowLatch = new Latch();
        LocalMuleClient client = muleContext.getClient();
        FunctionalTestComponent functionalTestComponent = getFunctionalTestComponent("dlq-out");
        functionalTestComponent.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                endDlqFlowLatch.release();
            }
        });
        client.send("vm://in","message",null,RECEIVE_TIMEOUT);
        if (!endDlqFlowLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message wasn't received by dlq flow");
        }
    }
}
