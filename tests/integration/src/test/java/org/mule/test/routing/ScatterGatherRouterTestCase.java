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

import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.connector.DispatchException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.AggregationContext;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.routing.RoutingException;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.routing.AggregationStrategy;
import org.mule.routing.CompositeRoutingException;
import org.mule.util.concurrent.Latch;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
        flowRunner("minimalConfig").withPayload("").run();
    }

    @Test(expected = MessagingException.class)
    public void consumablePayload() throws Exception
    {
        flowRunner("minimalConfig").withPayload(new ByteArrayInputStream("hello world".getBytes())).run();
    }

    @Test
    public void timeout() throws Exception
    {
        MessagingException e = flowRunner("timeout").runExpectingException();
        assertThat(e, instanceOf(CompositeRoutingException.class));

        MuleEvent response = e.getEvent();
        ExceptionPayload ep = response.getMessage().getExceptionPayload();
        assertThat(ep, is(notNullValue()));
        assertThat(e, sameInstance(ep.getException()));

        Map<Integer, Throwable> exceptions = ((CompositeRoutingException) e).getExceptions();
        assertThat(1, is(exceptions.size()));
        assertThat(exceptions.get(2), instanceOf(ResponseTimeoutException.class));
    }

    @Test
    public void routeWithException() throws Exception
    {
        assertRouteException("routeWithException",
                             EXCEPTION_MESSAGE_TITLE_PREFIX + "\t1: org.mule.functional.exceptions.FunctionalTestException: Functional Test Service Exception. Component that caused exception is:",
                             "}.");
    }

    @Test
    public void routeWithExceptionWithMessage() throws Exception
    {
        assertRouteException("routeWithExceptionWithMessage",
                             EXCEPTION_MESSAGE_TITLE_PREFIX + "\t1: org.mule.functional.exceptions.FunctionalTestException: I'm a message. Component that caused exception is:",
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
                             EXCEPTION_MESSAGE_TITLE_PREFIX + "\t1: org.mule.functional.exceptions.FunctionalTestException: Functional Test Service Exception. Component that caused exception is:",
                             "}.");
    }

    private void assertRouteException(String flow, String exceptionMessageStart, String exceptionMessageEnd) throws Exception
    {
        MessagingException e = flowRunner(flow).runExpectingException();
        assertThat(e, instanceOf(CompositeRoutingException.class));

        MuleEvent response = e.getEvent();
        ExceptionPayload ep = response.getMessage().getExceptionPayload();
        assertThat(ep, is(notNullValue()));
        assertThat(e, sameInstance(ep.getException()));

        assertThat(e.getMessage(), startsWith(exceptionMessageStart));
        assertThat(e.getMessage(), endsWith(exceptionMessageEnd));

        Map<Integer, Throwable> exceptions = ((CompositeRoutingException) e).getExceptions();
        assertThat(1, is(exceptions.size()));
        assertThat(exceptions.get(1), instanceOf(DispatchException.class));
    }

    @Test
    public void customMergeStrategyByName() throws Exception
    {
        flowRunner("customMergeStrategyByName").withPayload("").run();
    }

    @Test
    public void customMergeStrategyByRef() throws Exception
    {
        flowRunner("customMergeStrategyByRef").withPayload("").run();
    }

    @Test
    public void sequentialProcessing() throws Exception
    {
        flowRunner("sequentialProcessing").withPayload("").runAndVerify("customThreadingProfile");
        assertThat(capturedThreads, hasSize(1));
    }

    @Test
    public void requestResponseInboundEndpoint() throws Exception
    {
        flowRunner("requestResponseInboundEndpoint").withPayload("");
    }

    @Test
    public void oneWayInboundEndpoint() throws Exception
    {
        flowRunner("oneWayInboundEndpoint").withPayload("").asynchronously().run();
    }

    @Test
    public void routesWithForeachAndInboundEndpoint() throws Exception
    {
        final String[] payload = new String[] {"apple", "banana", "orange"};
        flowRunner("routesWithForeachAndInboundEndpoint").withPayload(Arrays.asList(payload)).run();
    }

    @Test
    public void exceptionStrategy() throws Exception
    {
        flowRunner("exceptionStrategy").withPayload("").run();
    }

    @Test
    public void failedEventInAggregationStrategy() throws Exception
    {
        flowRunner("failedEventInAggregationStrategy").withPayload("").run();
    }

    @Test
    public void failingMergeStrategy() throws Exception
    {
        MessagingException e = flowRunner("failingMergeStrategy").withPayload("").runExpectingException();
        assertThat(e.getCause(), instanceOf(UnsupportedOperationException.class));
    }

    @Test
    public void messageProperties() throws Exception
    {
        flowRunner("messageProperties").withPayload("").run();
    }

    @Test
    public void oneWayRouteWithSingleResponse() throws Exception
    {
        flowRunner("oneWayRouteWithSingleResponse").withPayload("").run();
    }

    @Test
    public void oneWayRouteWithMultipleResponses() throws Exception
    {
        flowRunner("oneWayRouteWithMultipleResponses").withPayload("").run();
    }

    @Test
    public void expressionFilterRoute() throws Exception
    {
        flowRunner("expressionFilterRoute").withPayload("").run();
    }

    @Test
    public void doesThreading() throws Exception
    {
        // MuleEvent event = getTestEvent("");
        // event.setFlowVariable("latch", new Latch());
        flowRunner("doesThreading").withPayload("").withFlowVariable("latch", new Latch()).run();

        assertThat(capturedThreads, hasSize(3));
    }

    @Test
    public void oneWayRoutesOnly() throws Exception
    {
        flowRunner("oneWayRoutesOnly").withPayload("").run();
    }

    @Test
    public void setsVariablesAfterRouting() throws Exception
    {
        flowRunner("setsVariablesAfterRouting").run();
    }

    @Test
    public void returnsCorrectDataType() throws Exception
    {
        MuleMessage response = flowRunner("dataType").withPayload(TEST_PAYLOAD)
                                                     .withOutboundProperty("Content-Type", "application/json")
                                                     .run()
                                                     .getMessage();

        assertThat(response.getPayload(), is(Matchers.instanceOf(List.class)));
        assertThat(((List<MuleMessage>) response.getPayload()).size(), is(3));
        assertThat(((List<MuleMessage>) response.getPayload()).get(0).getDataType().getMimeType(), is("text/plain"));
        assertThat(((List<MuleMessage>) response.getPayload()).get(1).getDataType().getMimeType(), is("*/*"));
        assertThat(((List<MuleMessage>) response.getPayload()).get(2).getDataType().getMimeType(), is("*/*"));
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
