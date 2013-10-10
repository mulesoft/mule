/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestExceptionStrategy.ExceptionCallback;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractExceptionStrategyTestCase extends FunctionalTestCase
{
    public static final int LATCH_AWAIT_TIMEOUT = 3000;
    
    protected final AtomicInteger systemExceptionCounter = new AtomicInteger();
    protected final AtomicInteger serviceExceptionCounter = new AtomicInteger();
    protected Latch latch;
    protected LocalMuleClient client;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        if (client == null)
        {
            client = muleContext.getClient();
        }
        latch = new Latch();
        systemExceptionCounter.set(0);
        serviceExceptionCounter.set(0);

        TestExceptionStrategy systemExceptionListener = new TestExceptionStrategy();
        systemExceptionListener.setExceptionCallback(new ExceptionCallback()
        {
            public void onException(Throwable t)
            {
                systemExceptionCounter.incrementAndGet();
                latch.countDown();
            }
        });
        muleContext.setExceptionListener(systemExceptionListener);

        TestExceptionStrategy serviceExceptionListener = 
            (TestExceptionStrategy) muleContext.getRegistry().lookupModel("TestModel").getExceptionListener();
        serviceExceptionListener.setExceptionCallback(new ExceptionCallback()
        {
            public void onException(Throwable t)
            {
                serviceExceptionCounter.incrementAndGet();
                latch.countDown();
            }
        });
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        latch = null;
    }
}


