/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FlowRefTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port = new DynamicPort("port");

    private static String FLOW1_SENSING_PROCESSOR_NAME = "NonBlockingFlow1SensingProcessor";
    private static String FLOW2_SENSING_PROCESSOR_NAME = "NonBlockingFlow2SensingProcessor";
    private static String TO_QUEUED_ASYNC_FLOW1_SENSING_PROCESSOR_NAME = "NonBlockingToQueuedAsyncFlow1SensingProcessor";
    private static String TO_QUEUED_ASYNC_FLOW2_SENSING_PROCESSOR_NAME = "NonBlockingToQueuedAsyncFlow2SensingProcessor";
    private static String TO_ASYNC_FLOW1_SENSING_PROCESSOR_NAME = "NonBlockingToAsyncFlow1SensingProcessor";
    private static String TO_ASYNC_FLOW2_SENSING_PROCESSOR_NAME = "NonBlockingToAsyncFlow2SensingProcessor";
    private static String TO_SYNC_FLOW1_SENSING_PROCESSOR_NAME = "NonBlockingToSyncFlow1SensingProcessor";
    private static String TO_SYNC_FLOW2_SENSING_PROCESSOR_NAME = "NonBlockingToSyncFlow2SensingProcessor";
    private static String ERROR_MESSAGE = "ERROR";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow-ref.xml";
    }

    @Before
    public void before()
    {
        ProcessorPathAssertingProcessor.traversedProcessorPaths.clear();
    }

    @Test
    public void twoFlowRefsToSubFlow() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage msg = client.send("vm://two.flow.ref.to.sub.flow", new DefaultMuleMessage("0",
            muleContext));

        assertEquals("012xyzabc312xyzabc3", msg.getPayloadAsString());

    }

    @Test
    public void dynamicFlowRef() throws Exception
    {
        MuleEvent eventA = getTestEvent("0");
        eventA.setFlowVariable("letter", "A");
        MuleEvent eventB = getTestEvent("0");
        eventB.setFlowVariable("letter", "B");

        assertEquals("0A", ((Flow) getFlowConstruct("flow2")).process(eventA).getMessageAsString());
        assertEquals("0B", ((Flow) getFlowConstruct("flow2")).process(eventB).getMessageAsString());
    }

    public static class ProcessorPathAssertingProcessor implements MessageProcessor
    {

        private static List<String> traversedProcessorPaths = new ArrayList<>();

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            traversedProcessorPaths.add(((Flow) muleContext.getRegistry().lookupFlowConstruct(event.getFlowConstruct().getName())).getProcessorPath(this));
            return event;
        }
    }

    @Test
    public void dynamicFlowRefProcessorPath() throws Exception
    {
        MuleEvent eventJ = getTestEvent("0");
        eventJ.setFlowVariable("letter", "J");

        ((Flow) getFlowConstruct("flow2")).process(eventJ);

        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.size(), is(1));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(0), is("/flow2/processors/0/sub-flow-J/subprocessors/0"));
    }

    @Test
    public void dynamicFlowRefProcessorPathSameSubflowFromSingleFlow() throws Exception
    {
        MuleEvent eventJ = getTestEvent("0");
        eventJ.setFlowVariable("letter", "J");

        ((Flow) getFlowConstruct("flow3")).process(eventJ);

        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.size(), is(2));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(0), is("/flow3/processors/0/sub-flow-J/subprocessors/0"));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(1), is("/flow3/processors/1/sub-flow-J/subprocessors/0"));
    }

    @Test
    public void dynamicFlowRefProcessorPathSameSubflowFromDifferentFlow() throws Exception
    {
        MuleEvent eventJ1 = getTestEvent("0");
        eventJ1.setFlowVariable("letter", "J");

        ((Flow) getFlowConstruct("flow2")).process(eventJ1);

        MuleEvent eventJ2 = getTestEvent("0");
        eventJ2.setFlowVariable("letter", "J");

        ((Flow) getFlowConstruct("flow3")).process(eventJ2);

        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.size(), is(3));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(0), is("/flow2/processors/0/sub-flow-J/subprocessors/0"));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(1), is("/flow3/processors/0/sub-flow-J/subprocessors/0"));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(2), is("/flow3/processors/1/sub-flow-J/subprocessors/0"));
    }

    @Test
    public void dynamicFlowRefWithChoice() throws Exception
    {
        MuleEvent eventC = getTestEvent("0");
        eventC.setFlowVariable("letter", "C");

        assertEquals("0A", ((Flow) getFlowConstruct("flow2")).process(eventC).getMessageAsString());
    }

    @Test
    public void dynamicFlowRefWithScatterGather() throws Exception
    {
        MuleEvent eventSG = getTestEvent("0");
        eventSG.setFlowVariable("letter", "SG");

        List payload = (List) ((Flow) getFlowConstruct("flow2")).process(eventSG).getMessage().getPayload();
        assertEquals("0A", payload.get(0));
        assertEquals("0B", payload.get(1));
    }

    @Test(expected=MessagingException.class)
    public void flowRefNotFound() throws Exception
    {
        MuleEvent eventC = getTestEvent("0");
        eventC.setFlowVariable("letter", "Z");

        assertEquals("0C", ((Flow) getFlowConstruct("flow2")).process(eventC).getMessageAsString());
    }

    @Test
    public void nonBlockingFlowRef() throws Exception
    {
        Response response = Request.Post(String.format("http://localhost:%s/%s", port.getNumber(), "nonBlockingFlowRefBasic"))
                .connectTimeout(RECEIVE_TIMEOUT).bodyString(TEST_MESSAGE, ContentType.TEXT_PLAIN).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_MESSAGE));

        SensingNullRequestResponseMessageProcessor flow1RequestResponseProcessor = muleContext.getRegistry()
                .lookupObject(FLOW1_SENSING_PROCESSOR_NAME);
        SensingNullRequestResponseMessageProcessor flow2RequestResponseProcessor = muleContext.getRegistry()
                .lookupObject(FLOW2_SENSING_PROCESSOR_NAME);
        assertThat(flow1RequestResponseProcessor.requestThread, not(equalTo(flow1RequestResponseProcessor.responseThread)));
        assertThat(flow2RequestResponseProcessor.requestThread, not(equalTo(flow2RequestResponseProcessor
                                                                                    .responseThread)));
    }

    @Test
    public void nonBlockingFlowRefToQueuedAsyncFlow() throws Exception
    {
        Response response = Request.Post(String.format("http://localhost:%s/%s", port.getNumber(), "nonBlockingFlowRefToQueuedAsyncFlow"))
                .connectTimeout(RECEIVE_TIMEOUT).bodyString(TEST_MESSAGE, ContentType.TEXT_PLAIN).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(500));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is("Unable to process a synchronous event asynchronously."));
    }

    @Test
    public void nonBlockingFlowRefToAsyncFlow() throws Exception
    {
        Response response = Request.Post(String.format("http://localhost:%s/%s", port.getNumber(), "nonBlockingFlowRefToAsyncFlow"))
                .connectTimeout(RECEIVE_TIMEOUT).bodyString(TEST_MESSAGE, ContentType.TEXT_PLAIN).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(500));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is("Unable to process a synchronous event asynchronously."));
    }

    @Test
    public void nonBlockingFlowRefToSyncFlow() throws Exception
    {
        Response response = Request.Post(String.format("http://localhost:%s/%s", port.getNumber(), "nonBlockingFlowRefToSyncFlow"))
                .connectTimeout(RECEIVE_TIMEOUT).bodyString(TEST_MESSAGE, ContentType.TEXT_PLAIN).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_MESSAGE));

        SensingNullRequestResponseMessageProcessor flow1RequestResponseProcessor = muleContext.getRegistry()
                .lookupObject(TO_SYNC_FLOW1_SENSING_PROCESSOR_NAME);
        SensingNullRequestResponseMessageProcessor flow2RequestResponseProcessor = muleContext.getRegistry()
                .lookupObject(TO_SYNC_FLOW2_SENSING_PROCESSOR_NAME);
        assertThat(flow1RequestResponseProcessor.requestThread, equalTo(flow1RequestResponseProcessor.responseThread));
        assertThat(flow2RequestResponseProcessor.requestThread, equalTo(flow2RequestResponseProcessor.responseThread));
    }


    @Test
    public void nonBlockingFlowRefErrorHandling() throws Exception
    {
        Response response = Request.Post(String.format("http://localhost:%s/%s", port.getNumber(), "nonBlockingFlowRefErrorHandling"))
                .connectTimeout(RECEIVE_TIMEOUT).bodyString(TEST_MESSAGE, ContentType.TEXT_PLAIN).execute();
        HttpResponse httpResponse = response.returnResponse();

        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(ERROR_MESSAGE));
    }

}
