/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor.SYNCHRONOUS_NONBLOCKING_EVENT_ERROR_MESSAGE;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;

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
        final MuleEvent muleEvent = flowRunner("flow1").withPayload("0").run();
        assertThat(getPayloadAsString(muleEvent.getMessage()), is("012xyzabc312xyzabc3"));
    }

    @Test
    public void dynamicFlowRef() throws Exception
    {
        assertEquals("0A", flowRunner("flow2").withPayload("0")
                                              .withFlowVariable("letter", "A")
                                              .run()
                                              .getMessageAsString());
        assertEquals("0B", flowRunner("flow2").withPayload("0")
                                              .withFlowVariable("letter", "B")
                                              .run()
                                              .getMessageAsString());
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
        flowRunner("flow2").withPayload("0")
                           .withFlowVariable("letter", "J")
                           .run();

        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.size(), is(1));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(0), is("/flow2/processors/0/sub-flow-J/subprocessors/0"));
    }

    @Test
    public void dynamicFlowRefProcessorPathSameSubflowFromSingleFlow() throws Exception
    {
        flowRunner("flow3").withPayload("0")
                           .withFlowVariable("letter", "J")
                           .run();

        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.size(), is(2));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(0), is("/flow3/processors/0/sub-flow-J/subprocessors/0"));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(1), is("/flow3/processors/1/sub-flow-J/subprocessors/0"));
    }

    @Test
    public void dynamicFlowRefProcessorPathSameSubflowFromDifferentFlow() throws Exception
    {
        flowRunner("flow2").withPayload("0")
                           .withFlowVariable("letter", "J")
                           .run();

        flowRunner("flow3").withPayload("0")
                           .withFlowVariable("letter", "J")
                           .run();

        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.size(), is(3));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(0), is("/flow2/processors/0/sub-flow-J/subprocessors/0"));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(1), is("/flow3/processors/0/sub-flow-J/subprocessors/0"));
        assertThat(ProcessorPathAssertingProcessor.traversedProcessorPaths.get(2), is("/flow3/processors/1/sub-flow-J/subprocessors/0"));
    }

    @Test
    public void dynamicFlowRefWithChoice() throws Exception
    {
        assertEquals("0A", flowRunner("flow2").withPayload("0")
                                              .withFlowVariable("letter", "C")
                                              .run()
                                              .getMessageAsString());
    }

    @Test
    public void dynamicFlowRefWithScatterGather() throws Exception
    {
        List<MuleMessage> messageList = (List<MuleMessage>) flowRunner("flow2").withPayload("0")
                                                                               .withFlowVariable("letter", "SG")
                                                                               .run()
                                                                               .getMessage()
                                                                               .getPayload();

        List payloads = messageList.stream().map(MuleMessage::getPayload).collect(toList());
        assertEquals("0A", payloads.get(0));
        assertEquals("0B", payloads.get(1));
    }

    @Test(expected=MessagingException.class)
    public void flowRefNotFound() throws Exception
    {
        assertEquals("0C", flowRunner("flow2").withPayload("0")
                                              .withFlowVariable("letter", "Z")
                                              .run()
                                              .getMessageAsString());
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
    public void nonBlockingFlowRefToAsyncFlow() throws Exception
    {
        Response response = Request.Post(String.format("http://localhost:%s/%s", port.getNumber(), "nonBlockingFlowRefToAsyncFlow"))
                .connectTimeout(RECEIVE_TIMEOUT).bodyString(TEST_MESSAGE, ContentType.TEXT_PLAIN).execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(500));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), containsString(SYNCHRONOUS_NONBLOCKING_EVENT_ERROR_MESSAGE));
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
