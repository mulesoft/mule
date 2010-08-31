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

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public abstract class AbstractExceptionOnOutboundTransformerIsHandledOnceTestCase extends FunctionalTestCase
{
    private final AtomicInteger systemExceptionCounter = new AtomicInteger();
    private final AtomicInteger serviceExceptionCounter = new AtomicInteger();

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        systemExceptionCounter.set(0);
        serviceExceptionCounter.set(0);

        TestExceptionStrategy es = new TestExceptionStrategy();
        es.setTestProperty("true");
        muleContext.setExceptionListener(es);
    }

    public void testExceptionIsHandledOnceAndOnlyOnService() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        final Latch latch = new Latch();

        TestExceptionStrategy systemExceptionListener = (TestExceptionStrategy) muleContext.getExceptionListener();
        systemExceptionListener.setExceptionCallback(new ExceptionCallback()
        {
            public void onException(Throwable t)
            {
                systemExceptionCounter.incrementAndGet();
                latch.countDown();
            }
        });

        TestExceptionStrategy serviceExceptionListener = 
            (TestExceptionStrategy) muleContext.getRegistry().lookupService("SomeService").getExceptionListener();
        serviceExceptionListener.setExceptionCallback(new ExceptionCallback()
        {
            public void onException(Throwable t)
            {
                serviceExceptionCounter.incrementAndGet();
                latch.countDown();
            }
        });

        client.send("vm://in", "FAIL", null);
        latch.await(1000, TimeUnit.MILLISECONDS); // sleep one second in case another exception comes
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
    }

}


