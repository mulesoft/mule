/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.functional.functional.FlowAssert;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests to validate that MuleClient can be used from MessageProcessor and JavaComponent in order to dispatch an event to
 * a sub-flow, without losing the Flow variables.
 */
public class MuleClientDispatchWithoutLosingVariablesTestCase extends FunctionalTestCase
{
    @ClassRule
    public static DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/client/client-flow-session-vars-when-dispatch-flow.xml";
    }

    private void doSendMessageToHttp(String flowName) throws Exception
    {
        MuleMessage result = flowRunner(flowName).withPayload("TEST1").run().getMessage();
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
        doSendMessageToHttp("flowVarsFlowUsingProcessor");
    }

    @Test
    public void testSessionVarsAfterDispatchFromMessageProcessor() throws Exception
    {
        doSendMessageToHttp("sessionVarsFlowUsingProcessor");
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
        doSendMessageToHttp("flowVarsFlowUsingJavaComponent");
    }

    @Test
    public void testSessionVarsAfterDispatchFromJavaComponent() throws Exception
    {
        doSendMessageToHttp("sessionVarsFlowUsingJavaComponent");
    }

    @Test
    public void testSessionVarsFlowUsingJavaComponentRequestResponse() throws Exception
    {
        doSendMessageToHttp("sessionVarsFlowUsingJavaComponentRequestResponse");
    }

    public static class MessageProcessorDispatchFlowUsingNewMuleClient implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            event.getMuleContext().getClient().dispatch(getUrl("innertest"), new DefaultMuleMessage("payload"));
            return event;

        }
    }

    public static class JavaComponentDispatchFlowUsingNewMuleClient implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            eventContext.getMuleContext().getClient().dispatch(getUrl("innertest"), new DefaultMuleMessage("payload"));
            return eventContext.getMessage();
        }
    }

    public static class JavaComponentSendFlowUsingNewMuleClient implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            eventContext.sendEvent(new DefaultMuleMessage("payload"), getUrl("innerrequestresponsetest"));
            return eventContext.getMessage();
        }
    }

    private static String getUrl(String path)
    {
        return String.format("http://localhost:%s/%s", port.getValue(), path);
    }

}
