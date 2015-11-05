/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.AggregationContext;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.transport.DispatchException;
import org.mule.construct.Flow;
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

import org.hamcrest.Matchers;
import org.junit.Test;

public class ScatterGatherRouterTestCase extends FunctionalTestCase
{

    private static final String EXCEPTION_MESSAGE_TITLE_PREFIX = "Exception(s) were found for route(s): " + LINE_SEPARATOR;
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
            assertThat(ep, is(notNullValue()));
            assertThat(e, sameInstance(ep.getException()));

            Map<Integer, Throwable> exceptions = e.getExceptions();
            assertThat(1, is(exceptions.size()));
            assertThat(exceptions.get(2), instanceOf(ResponseTimeoutException.class));
        }
    }

    @Test
    public void routeWithException() throws Exception
    {
        assertRouteException("routeWithException",
                             EXCEPTION_MESSAGE_TITLE_PREFIX + "\t1: org.mule.tck.exceptions.FunctionalTestException: Functional Test Service Exception. Component that caused exception is:",
                             "}.");
    }

    @Test
    public void routeWithExceptionWithMessage() throws Exception
    {
        assertRouteException("routeWithExceptionWithMessage",
                             EXCEPTION_MESSAGE_TITLE_PREFIX + "\t1: org.mule.tck.exceptions.FunctionalTestException: I'm a message. Component that caused exception is:",
                             "}.");
    }

    @Test
    public void routeWithNonMuleException() throws Exception
    {
        assertRouteException("routeWithNonMuleException",
                             EXCEPTION_MESSAGE_TITLE_PREFIX + "\t1: java.lang.NullPointerException: nonMule. Component that caused exception is:",
                             "}.");
    }

    @Test
    public void routeWithMelException() throws Exception
    {
        assertRouteException("routeWithMelException",
                             EXCEPTION_MESSAGE_TITLE_PREFIX + "\t1: Execution of the expression \"invalidExpr\" failed. (org.mule.api.expression.ExpressionRuntimeException).",
                             ").");
    }

    @Test
    public void routeWithExceptionInSequentialProcessing() throws Exception
    {
        assertRouteException("routeWithExceptionInSequentialProcessing",
                             EXCEPTION_MESSAGE_TITLE_PREFIX + "\t1: org.mule.tck.exceptions.FunctionalTestException: Functional Test Service Exception. Component that caused exception is:",
                             "}.");
    }

    private void assertRouteException(String flow, String exceptionMessageStart, String exceptionMessageEnd) throws Exception
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
            assertThat(ep, is(notNullValue()));
            assertThat(e, sameInstance(ep.getException()));
            
            assertThat(e.getMessage(), startsWith(exceptionMessageStart));
            assertThat(e.getMessage(), endsWith(exceptionMessageEnd));

            Map<Integer, Throwable> exceptions = e.getExceptions();
            assertThat(1, is(exceptions.size()));
            assertThat(exceptions.get(1), instanceOf(DispatchException.class));
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
    public void failedEventInAggregationStrategy() throws Exception
    {
        runFlow("failedEventInAggregationStrategy", getTestEvent(""));
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
            assertThat(e.getCause(), instanceOf(UnsupportedOperationException.class));
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

    @Test
    public void setsVariablesAfterRouting() throws Exception
    {
        runFlow("setsVariablesAfterRouting");
        FlowAssert.verify("setsVariablesAfterRouting");
    }

    @Test
    public void returnsCorrectDataType() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage(TEST_PAYLOAD, muleContext);
        message.setOutboundProperty("Content-Type", "application/json");

        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.REQUEST_RESPONSE, mock(Flow.class));
        MuleMessage response = runFlow("dataType", event).getMessage();

        assertThat(response, is(Matchers.instanceOf(DefaultMessageCollection.class)));
        assertThat(((DefaultMessageCollection) response).size(), is(3));
        assertThat(((DefaultMessageCollection) response).getMessage(0).getDataType().getMimeType(), is("text/plain"));
        assertThat(((DefaultMessageCollection) response).getMessage(1).getDataType().getMimeType(), is("*/*"));
        assertThat(((DefaultMessageCollection) response).getMessage(2).getDataType().getMimeType(), is("*/*"));
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

    public static class FlatteningTestAggregationStrategy implements AggregationStrategy {

        @Override
        public MuleEvent aggregate(AggregationContext context) throws MuleException
        {
            MuleEvent event = context.getOriginalEvent();
            event.getMessage().setPayload(context.getEvents());


            return event;
        }
    }
}
