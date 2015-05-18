/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.registry.RegistrationException;
import org.mule.processor.AbstractRequestResponseMessageProcessor;

/**
 * Can be used to sense request and response threads used during processing.
 */
public class SensingNullRequestResponseMessageProcessor extends AbstractRequestResponseMessageProcessor
{
    public Thread requestThread;
    public Thread responseThread;

    @Override
    protected MuleEvent processRequest(MuleEvent event) throws MuleException
    {
        requestThread = Thread.currentThread();
        return super.processRequest(event);
    }

    @Override
    protected MuleEvent processResponse(MuleEvent event) throws MuleException
    {
        responseThread = Thread.currentThread();
        return super.processRequest(event);
    }

    public static void assertSameThread(MuleContext muleContext, String beanName) throws Exception
    {
        SensingNullRequestResponseMessageProcessor instance = getInstance(muleContext, beanName);
        assertThat(instance.requestThread, is(sameInstance(instance.responseThread)));
    }

    public static void assertDifferentThreads(MuleContext muleContext, String beanName) throws Exception
    {
        SensingNullRequestResponseMessageProcessor instance = getInstance(muleContext, beanName);
        assertThat(instance.requestThread, not(sameInstance(instance.responseThread)));
    }

    private static SensingNullRequestResponseMessageProcessor getInstance(MuleContext muleContext, String beanName) throws RegistrationException
    {
        return muleContext.getRegistry().lookupObject(beanName);
    }

}
