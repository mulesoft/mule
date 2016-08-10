/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestExceptionStrategy.ExceptionCallback;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractExceptionStrategyTestCase extends AbstractIntegrationTestCase
{

    public static final int LATCH_AWAIT_TIMEOUT = 3000;

    protected final AtomicInteger systemExceptionCounter = new AtomicInteger();
    protected final AtomicInteger serviceExceptionCounter = new AtomicInteger();
    protected Latch latch;
    protected MuleClient client;

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
            @Override
            public void onException(Throwable t)
            {
                systemExceptionCounter.incrementAndGet();
                latch.countDown();
            }
        });
        muleContext.setExceptionListener(systemExceptionListener);

        for (FlowConstruct flow : muleContext.getRegistry().lookupFlowConstructs())
        {
            ((TestExceptionStrategy) flow.getExceptionListener()).setExceptionCallback(new ExceptionCallback()
            {
                @Override
                public void onException(Throwable t)
                {
                    serviceExceptionCounter.incrementAndGet();
                    latch.countDown();
                }
            });
        }
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        latch = null;
    }
}


