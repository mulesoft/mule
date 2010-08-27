/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions.duplicatehandling;

import org.mule.api.client.LocalMuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestExceptionStrategy.ExceptionCallback;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractExceptionOnOutboundTransformerIsHandledOnceTestCase extends FunctionalTestCase
{
    private final AtomicInteger connectorExceptionCounter = new AtomicInteger();
    private final AtomicInteger serviceExceptionCounter = new AtomicInteger();

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        connectorExceptionCounter.set(0);
        serviceExceptionCounter.set(0);

        TestExceptionStrategy es = new TestExceptionStrategy();
        es.setTestProperty("true");
        muleContext.setExceptionListener(es);
    }

    public void testExceptionIsHandledOnceAndOnlyOnConnector() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in", "FAIL", null);

        final Latch latch = new Latch();

        TestExceptionStrategy connectorExceptionListener = (TestExceptionStrategy) muleContext.getExceptionListener();
        connectorExceptionListener.setExceptionCallback(new ExceptionCallback()
        {
            public void onException(Throwable t)
            {
                connectorExceptionCounter.incrementAndGet();
                latch.countDown();
            }
        });

        TestExceptionStrategy serviceExceptionListener = (TestExceptionStrategy) muleContext.getRegistry()
            .lookupService("SomeService")
            .getExceptionListener();
        serviceExceptionListener.setExceptionCallback(new ExceptionCallback()
        {
            public void onException(Throwable t)
            {
                serviceExceptionCounter.incrementAndGet();
                latch.countDown();
            }
        });

        latch.await();
        Thread.sleep(1000); // sleep one second in case another exception comes
        assertEquals(0, serviceExceptionCounter.get());
        assertEquals(1, connectorExceptionCounter.get());
    }

}


