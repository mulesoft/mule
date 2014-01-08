/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.transport.DispatchException;
import org.mule.routing.CompositeRoutingException;
import org.mule.routing.EventMergeStrategy;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ScatterGatherRouterTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "scatter-gather-test.xml";
    }

    @Test
    public void minimalConfiguration() throws Exception
    {
        this.testFlow("minimalConfig", getTestEvent(""));
    }

    @Test(expected = MessagingException.class)
    public void consumablePayload() throws Exception
    {
        this.testFlow("minimalConfig", getTestEvent(new ByteArrayInputStream("hello world".getBytes())));
    }

    @Test
    public void timeout() throws Exception
    {
        try
        {
            this.runFlow("timeout");
            fail("Was expecting a timeout");
        }
        catch (CompositeRoutingException e)
        {
            MuleEvent response = e.getEvent();
            ExceptionPayload ep = response.getMessage().getExceptionPayload();
            assertNotNull(ep);
            assertSame(e, ep.getException());

            Map<Integer, Exception> exceptions = e.getExceptions();
            assertEquals(1, exceptions.size());
            assertTrue(exceptions.get(3) instanceof ResponseTimeoutException);
        }
    }

    @Test
    public void routeWithException() throws Exception
    {
        try
        {
            this.runFlow("routeWithException");
            fail("Was expecting a failure");
        }
        catch (CompositeRoutingException e)
        {
            MuleEvent response = e.getEvent();
            ExceptionPayload ep = response.getMessage().getExceptionPayload();
            assertNotNull(ep);
            assertSame(e, ep.getException());

            Map<Integer, Exception> exceptions = e.getExceptions();
            assertEquals(1, exceptions.size());
            assertTrue(exceptions.get(2) instanceof DispatchException);
        }
    }

    @Test
    public void customMergeStrategyByName() throws Exception
    {
        this.testFlow("customMergeStrategyByName", getTestEvent(""));
    }

    @Test
    public void customMergeStrategyByRef() throws Exception
    {
        this.testFlow("customMergeStrategyByRef", getTestEvent(""));
    }
    
    @Test
    public void customThreadingProfile() throws Exception {
        Date start = new Date();
        this.runFlow("customThreadingProfile", "");
        Date end = new Date();
        
        long waitTime = end.getTime() - start.getTime();
        assertTrue(String.format("it only took %d ms",  waitTime), waitTime >= 5000);
        FlowAssert.verify("customThreadingProfile");
    }

    public static class TestMergeStrategy implements EventMergeStrategy
    {

        @Override
        public MuleEvent merge(MuleEvent originalEvent, List<MuleEvent> events) throws MuleException
        {
            StringBuilder builder = new StringBuilder();
            for (MuleEvent event : events)
            {
                if (builder.length() > 0)
                {
                    builder.append(' ');
                }

                builder.append(event.getMessage().getPayload());
            }

            originalEvent.getMessage().setPayload(builder.toString());
            return originalEvent;
        }
    }
}
