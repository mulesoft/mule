/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.api.processor.MessageProcessor;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests to validate that MuleClient can be used from JavaComponent/MessageProcessor in order to dispatch an event to
 * a sub-flow and if the component/processor throws an exception afterwards the main-flow exception strategy handles
 * it.
 */
public class MuleClientDispatchExceptionHandlingTestCase extends FunctionalTestCase
{
    @ClassRule
    public static DynamicPort port = new DynamicPort("port");

    // These attributes need to be accessed from JavaComponent and MessageProcessor static classes therefore
    // they are declared as static
    private static Latch innerFlowLatch;
    private static Latch exceptionLatch;
    private static MuleEvent eventFromMainFlow;
    private static MuleMessage messageFromMainFlow;
    private static boolean eventPropagated;
    private static boolean isSameMessage;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/client/client-dispatch-catch-exception-flow.xml";
    }

    /**
     * Validates that a JavaComponent after doing a dispatch to a sub-flow using MuleClient
     * throws an exception and the catch-exception-strategy defined in main-flow is called.
     * It also validates that original event passed to JavaComponent is later propagated
     * to the JavaComponent defined in catch-exception-strategy block.
     *
     * @throws Exception
     */
    @Test
    public void testCatchExceptionThrowFromJavaComponentToJavaComponent() throws Exception
    {
        doSendMessageToEndpoint("catchExceptionJavaComponentToJavaComponent");
    }

    @Test
    public void tesCatchExceptionThrowFromJavaComponentToMessageProcessor() throws Exception
    {
        doSendMessageToEndpoint("catchExceptionJavaComponentToMessageProcessor");
    }

    @Test
    public void testCatchExceptionThrowFromMessageProcessorToJavaComponent() throws Exception
    {
        doSendMessageToEndpoint("catchExceptionMessageProcessorToJavaComponent");
    }

    @Test
    public void tesCatchExceptionThrowFromMessageProcessorToMessageProcessor() throws Exception
    {
        doSendMessageToEndpoint("catchExceptionMessageProcessorToMessageProcessor");
    }

    @Test
    public void testCatchExceptionJavaComponentToJavaComponentRequestResponseInnerFlow() throws Exception
    {
        doSendMessageToEndpoint("catchExceptionJavaComponentToJavaComponentRequestResponseInnerFlow");
    }

    private void doSendMessageToEndpoint(String endpoint) throws Exception
    {
        innerFlowLatch = new Latch();
        exceptionLatch = new Latch();
        eventPropagated = true;
        isSameMessage = true;

        LocalMuleClient client = muleContext.getClient();
        MuleMessage result = client.send(getUrl(endpoint), getTestMuleMessage("Original Message"));

        boolean innerFlowCalled = innerFlowLatch.await(3, TimeUnit.SECONDS);
        assertThat(innerFlowCalled, is(true));
        boolean exceptionHandled = exceptionLatch.await(3, TimeUnit.SECONDS);
        assertThat(exceptionHandled, is(true));

        assertThat(isSameMessage, is(true));
        assertThat(eventPropagated, is(true));

        assertThat(result, notNullValue(MuleMessage.class));
    }

    private static String getUrl(String endpoint)
    {
        return String.format("http://localhost:%s/%s", port.getValue(), endpoint);
    }

    // Just a simple JavaComponent used in catch-exception-strategy block
    // in order to check that RequestContext has the correct event and message references
    public static class AssertEventComponent implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            // Validates that if another Component access to the RequestContext.getEvent() the one returned
            // is the correct, in this case it should be the same that it was set before doing the
            // eventContext.dispatchEvent() on main-flow java component where the exception happened right after
            // that invocatioeventPropagated = RequestContext.getEvent().equals(eventFromMainFlow);
            // Checking if message is still the same on catch-exception-strategy
            isSameMessage = RequestContext.getEvent().getMessage().equals(messageFromMainFlow);
            return eventContext.getMessage();
        }
    }

    public static class AssertEventProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            // Validates that if another Component access to the RequestContext.getEvent() the one returned
            // is the correct, in this case it should be the same that it was set before doing the
            // eventContext.dispatchEvent() on main-flow java component where the exception happened right after
            // that invocatioeventPropagated = RequestContext.getEvent().equals(eventFromMainFlow);
            // Checking if message is still the same on catch-exception-strategy
            isSameMessage = RequestContext.getEvent().getMessage().equals(messageFromMainFlow);
            return event;
        }
    }

    public static class DispatchInnerFlowThrowExceptionJavaComponent implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            eventFromMainFlow = RequestContext.getEvent();
            messageFromMainFlow = eventFromMainFlow.getMessage();

            eventContext.getMuleContext().getClient().dispatch(getUrl("innertest"), new DefaultMuleMessage("payload", eventContext.getMuleContext()));

            throw new Exception("expected exception!");
        }
    }

    public static class SendInnerFlowThrowExceptionJavaComponent implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            eventFromMainFlow = RequestContext.getEvent();
            messageFromMainFlow = eventFromMainFlow.getMessage();

            eventContext.sendEvent(new DefaultMuleMessage("payload", eventContext.getMuleContext()),
                                   getUrl("innerrequestresponsetest"));

            throw new Exception("expected exception!");
        }
    }

    public static class DispatchInnerFlowThrowExceptionMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            eventFromMainFlow = RequestContext.getEvent();
            messageFromMainFlow = eventFromMainFlow.getMessage();

            event.getMuleContext().getClient().dispatch(getUrl("innertest"),
                                                        new DefaultMuleMessage("payload", event.getMuleContext()));

            throw new DefaultMuleException("expected exception!");
        }
    }

    public static class ExecutionCountDownProcessor implements MessageProcessor
    {
        @Override
        public synchronized MuleEvent process(MuleEvent event) throws MuleException
        {
            exceptionLatch.countDown();
            return event;
        }
    }

    public static class InnerFlowCountDownProcessor implements MessageProcessor
    {
        @Override
        public synchronized MuleEvent process(MuleEvent event) throws MuleException
        {
            innerFlowLatch.countDown();
            return event;
        }
    }
}
