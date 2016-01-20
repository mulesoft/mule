/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * Tests to validate that MuleClient can be used from MessageProcessor and JavaComponent in order to dispatch an event to
 * a sub-flow, without losing the Flow variables.
 */
public class MuleClientDispatchWithoutLosingVariablesTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/client/client-flow-session-vars-when-dispatch-flow.xml";
    }

    private void doSendMessageToVMEndpoint(String flowName) throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://" + flowName, "TEST1", null);
        assertThat(result, notNullValue(MuleMessage.class));
        FlowAssert.verify(flowName);
    }

    /**
     * When doing a dispatch from a MessageProcessor the event was overwritten in ThreadLocal by
     * OptimizedRequestContext while processing it and before dispatching it to a different thread so
     * the original event that is the one that has to continue the execution of the main flow
     * was losing the Flow variables.
     *
     * @throws Exception
     */
    @Test
    public void testFlowVarsAfterDispatchFromMessageProcessor() throws Exception
    {
        doSendMessageToVMEndpoint("flowVarsFlowUsingProcessor");
    }

    @Test
    public void testSessionVarsAfterDispatchFromMessageProcessor() throws Exception
    {
        doSendMessageToVMEndpoint("sessionVarsFlowUsingProcessor");
    }

    /**
     * When doing a dispatch from a JavaComponent the event was overwritten in ThreadLocal by
     * OptimizedRequestContext while processing it and before dispatching it to a different thread so
     * the original event that is the one that has to continue the execution of the main flow
     * was losing the Flow variables.
     *
     * @throws Exception
     */
    @Test
    public void testFlowVarsAfterDispatchFromJavaComponent() throws Exception
    {
        doSendMessageToVMEndpoint("flowVarsFlowUsingJavaComponent");
    }

    @Test
    public void testSessionVarsAfterDispatchFromJavaComponent() throws Exception
    {
        doSendMessageToVMEndpoint("sessionVarsFlowUsingJavaComponent");
    }

    @Test
    public void testSessionVarsFlowUsingJavaComponentRequestResponse() throws Exception
    {
        doSendMessageToVMEndpoint("sessionVarsFlowUsingJavaComponentRequestResponse");
    }

    public static class MessageProcessorDispatchFlowUsingNewMuleClient implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            event.getMuleContext().getClient().dispatch("vm://vminnertest", new DefaultMuleMessage("payload", event.getMuleContext()));
            return event;

        }
    }

    public static class JavaComponentDispatchFlowUsingNewMuleClient implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            eventContext.dispatchEvent(new DefaultMuleMessage("payload", eventContext.getMuleContext()), "vm://vminnertest");
            return eventContext.getMessage();
        }
    }

    public static class JavaComponentSendFlowUsingNewMuleClient implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            eventContext.sendEvent(new DefaultMuleMessage("payload", eventContext.getMuleContext()), "vm://vminnerrequestresponsetest");
            return eventContext.getMessage();
        }
    }

}
