/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transaction;

import static org.junit.Assert.fail;
import org.junit.Test;
import org.mule.api.MuleEventContext;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

public class TransactionalElementLifecycleTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
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
