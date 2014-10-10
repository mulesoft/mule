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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.AggregationContext;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.transport.DispatchException;
import org.mule.routing.AggregationStrategy;
import org.mule.routing.CompositeRoutingException;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ScatterGatherRouterTestCase extends FunctionalTestCase
{

    private static Set<Thread> capturedThreads;

    @Override
    protected String getConfigFile()
    {
        return "scatter-gather-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        capturedThreads = new HashSet<>();
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

            Map<Integer, Throwable> exceptions = e.getExceptions();
            assertEquals(1, exceptions.size());
            assertTrue(exceptions.get(2) instanceof ResponseTimeoutException);
        }
    }

    @Test
    public void routeWithException() throws Exception
    {
        assertRouteException("routeWithException");
    }

    @Test
    public void routeWithExceptionInSequentialProcessing() throws Exception
    {
        assertRouteException("routeWithExceptionInSequentialProcessing");
    }

    private void assertRouteException(String flow) throws Exception
    {
        try
        {
            this.runFlow(flow);
            fail("Was expecting a failure");
        }
        catch (CompositeRoutingException e)
        {
            MuleEvent response = e.getEvent();
            ExceptionPayload ep = response.getMessage().getExceptionPayload();
            assertNotNull(ep);
            assertSame(e, ep.getException());

            Map<Integer, Throwable> exceptions = e.getExceptions();
            assertEquals(1, exceptions.size());
            assertTrue(exceptions.get(1) instanceof DispatchException);
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
    public void sequentialProcessing() throws Exception
    {
        this.runFlow("sequentialProcessing", "");
        assertThat(capturedThreads, hasSize(1));
        FlowAssert.verify("customThreadingProfile");
    }

    @Test
    public void requestResponseInboundEndpoint() throws Exception
    {
        muleContext.getClient().send("vm://requestResponseInboundEndpoint", getTestEvent("").getMessage());
        FlowAssert.verify("requestResponseInboundEndpoint");
    }

    @Test
    public void oneWayInboundEndpoint() throws Exception
    {
        muleContext.getClient().send("vm://oneWayInboundEndpoint", getTestEvent("").getMessage());
        FlowAssert.verify("oneWayInboundEndpoint");
    }

    @Test
    public void routesWithForeachAndInboundEndpoint() throws Exception
    {
        final String[] payload = new String[] {"apple", "banana", "orange"};
        muleContext.getClient().send("vm://routesWithForeachAndInboundEndpoint", getTestEvent(Arrays.asList(payload)).getMessage());
        FlowAssert.verify("routesWithForeachAndInboundEndpoint");
    }

    @Test
    public void exceptionStrategy() throws Exception
    {
        this.testFlow("exceptionStrategy", getTestEvent(""));
    }

    @Test
    public void failingMergeStrategy() throws Exception
    {
        try
        {
            this.runFlow("failingMergeStrategy", getTestEvent(""));
            fail("Was expecting a exception");
        }
        catch (MessagingException e)
        {
            assertEquals(UnsupportedOperationException.class, e.getCause().getClass());
        }
    }

    @Test
    public void messageProperties() throws Exception
    {
        this.testFlow("messageProperties", getTestEvent(""));
    }

    @Test
    public void oneWayRouteWithSingleResponse() throws Exception
    {
        muleContext.getClient().send("vm://oneWayRouteWithSingleResponse", getTestEvent("").getMessage());
        FlowAssert.verify("oneWayRouteWithSingleResponse");
    }

    @Test
    public void oneWayRouteWithMultipleResponses() throws Exception
    {
        muleContext.getClient().send("vm://oneWayRouteWithMultipleResponses", getTestEvent("").getMessage());
        FlowAssert.verify("oneWayRouteWithMultipleResponses");
    }

    @Test
    public void expressionFilterRoute() throws Exception
    {
        muleContext.getClient().send("vm://expressionFilterRoute", getTestEvent("").getMessage());
        FlowAssert.verify("expressionFilterRoute");
    }

    @Test
    public void doesThreading() throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("latch", new Latch());
        testFlow("doesThreading", event);

        assertThat(capturedThreads, hasSize(3));
    }

    @Test
    public void oneWayRoutesOnly() throws Exception
    {
        muleContext.getClient().send("vm://oneWayRoutesOnly", getTestEvent("").getMessage());
        FlowAssert.verify("oneWayRoutesOnly");
    }

    public static class TestAggregationStrategy implements AggregationStrategy
    {

        @Override
        public MuleEvent aggregate(AggregationContext context) throws MuleException
        {
            StringBuilder builder = new StringBuilder();
            for (MuleEvent event : context.getEvents())
            {
                if (builder.length() > 0)
                {
                    builder.append(' ');
                }

                builder.append(event.getMessage().getPayload());
            }

            context.getOriginalEvent().getMessage().setPayload(builder.toString());
            return context.getOriginalEvent();
        }
    }

    public static class FailingAggregationStrategy implements AggregationStrategy
    {

        @Override
        public MuleEvent aggregate(AggregationContext context) throws MuleException
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class ThreadCaptor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            capturedThreads.add(Thread.currentThread());
            if (capturedThreads.size() > 1)
            {
                Latch latch = event.getFlowVariable("latch");
                if (latch != null)
                {
                    latch.release();
                }
            }

            return event;
        }
    }
}
