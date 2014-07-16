/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.DynamicPipelineException;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DynamicFlowTestCase extends FunctionalTestCase
{

    private MuleClient client;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/dynamic-flow.xml";
    }

    @Before
    public void before()
    {
        client = muleContext.getClient();
    }

    @Test
    public void addPreMessageProccesor() throws Exception
    {
        MuleMessage result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(static)", result.getPayloadAsString());

        Flow flow = getFlow("dynamicFlow");
        String pipelineId = flow.dynamicPipeline(null).injectBefore(new StringAppendTransformer("(pre)")).resetAndUpdate();
        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)", result.getPayloadAsString());

        flow.dynamicPipeline(pipelineId).injectBefore(new StringAppendTransformer("(pre1)"), new StringAppendTransformer("(pre2)")).resetAndUpdate();
        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre1)(pre2)(static)", result.getPayloadAsString());
    }

    @Test
    public void addPrePostMessageProccesor() throws Exception
    {
        Flow flow = getFlow("dynamicFlow");
        String pipelineId = flow.dynamicPipeline(null).injectBefore(new StringAppendTransformer("(pre)"))
                .injectAfter(new StringAppendTransformer("(post)"))
                .resetAndUpdate();
        MuleMessage result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)(post)", result.getPayloadAsString());

        flow.dynamicPipeline(pipelineId).injectBefore(new StringAppendTransformer("(pre)"))
                .injectAfter(new StringAppendTransformer("(post1)"), new StringAppendTransformer("(post2)"))
                .resetAndUpdate();
        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)(post1)(post2)", result.getPayloadAsString());
    }

    @Test
    public void dynamicComponent() throws Exception
    {
        //invocation #1
        MuleMessage result = client.send("vm://dynamicComponent", "source->", null);
        assertEquals("source->(static)", result.getPayloadAsString());

        //invocation #2
        result = client.send("vm://dynamicComponent", "source->", null);
        assertEquals("source->chain update #1(static)", result.getPayloadAsString());

        //invocation #3
        result = client.send("vm://dynamicComponent", "source->", null);
        assertEquals("source->chain update #2(static)", result.getPayloadAsString());
    }

    @Test
    public void exceptionOnInjectedMessageProcessor() throws Exception
    {
        List<MessageProcessor> preList = new ArrayList<MessageProcessor>();
        List<MessageProcessor> postList = new ArrayList<MessageProcessor>();

        Flow flow = getFlow("exceptionFlow");
        preList.add(new StringAppendTransformer("(pre)"));
        preList.add(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                throw new RuntimeException("force exception!");
            }
        });
        postList.add(new StringAppendTransformer("(post)"));
        flow.dynamicPipeline(null).injectBefore(preList).injectAfter(postList).resetAndUpdate();
        MuleMessage result = client.send("vm://exception", "source->", null);
        assertEquals("source->(pre)(handled)", result.getPayloadAsString());
    }

    @Test
    public void applyLifecycle() throws Exception
    {
        StringBuilder expected = new StringBuilder();

        Flow flow = getFlow("dynamicFlow");
        LifecycleMessageProcessor lifecycleMessageProcessor = new LifecycleMessageProcessor();
        String pipelineId = flow.dynamicPipeline(null).injectBefore(lifecycleMessageProcessor).resetAndUpdate();
        MuleMessage result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)", result.getPayloadAsString());
        assertEquals(expected.append("ISP").toString(), lifecycleMessageProcessor.getSteps());

        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)", result.getPayloadAsString());
        assertEquals(expected.append("P").toString(), lifecycleMessageProcessor.getSteps());

        flow.dynamicPipeline(pipelineId).reset();
        assertEquals(expected.append("TD").toString(), lifecycleMessageProcessor.getSteps());

        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(static)", result.getPayloadAsString());
        assertEquals(expected.toString(), lifecycleMessageProcessor.getSteps());
    }

    @Test
    public void applyAwareInterfaces() throws Exception
    {
        Flow flow = getFlow("dynamicFlow");
        UberAwareMessageProcessor awareMessageProcessor = new UberAwareMessageProcessor();
        flow.dynamicPipeline(null).injectBefore(awareMessageProcessor).resetAndUpdate();
        MuleMessage result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)", result.getPayloadAsString());
        assertNotNull(awareMessageProcessor.getFlowConstruct());
        assertNotNull(awareMessageProcessor.getMuleContext());
    }

    @Test (expected = DynamicPipelineException.class)
    public void invalidInitialPipelineId() throws Exception
    {
        getFlow("dynamicFlow").dynamicPipeline("invalid-id").resetAndUpdate();
    }

    @Test (expected = DynamicPipelineException.class)
    public void invalidNullPipelineId() throws Exception
    {
        getFlow("dynamicFlow").dynamicPipeline(null).resetAndUpdate();
        getFlow("dynamicFlow").dynamicPipeline(null).reset();
    }

    @Test (expected = DynamicPipelineException.class)
    public void invalidPipelineId() throws Exception
    {
        String id = getFlow("dynamicFlow").dynamicPipeline(null).resetAndUpdate();
        getFlow("dynamicFlow").dynamicPipeline(id + "x").reset();
    }

    private static Flow getFlow(String flowName) throws MuleException
    {
        return (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
    }

    public static class Component implements Callable
    {
        private String pipelineId;
        private int count;

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            Flow flow = (Flow) eventContext.getMuleContext().getRegistry().lookupFlowConstruct("dynamicComponentFlow");
            pipelineId = flow.dynamicPipeline(pipelineId).injectBefore(new StringAppendTransformer("chain update #" + ++count)).resetAndUpdate();
            return eventContext.getMessage();
        }

    }

    private static class LifecycleMessageProcessor implements MessageProcessor, Lifecycle
    {

        private StringBuffer steps = new StringBuffer();

        @Override
        public void dispose()
        {
            steps.append("D");
        }

        @Override
        public void initialise() throws InitialisationException
        {
            steps.append("I");
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            steps.append("P");
            event.getMessage().setPayload(event.getMessage().getPayload() + "(pre)");
            return event;
        }

        @Override
        public void start() throws MuleException
        {
            steps.append("S");

        }

        @Override
        public void stop() throws MuleException
        {
            steps.append("T");
        }

        public String getSteps()
        {
            return steps.toString();
        }
    }

    private static class UberAwareMessageProcessor implements MessageProcessor, MuleContextAware, FlowConstructAware
    {

        private FlowConstruct flowConstruct;
        private MuleContext muleContext;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            event.getMessage().setPayload(event.getMessage().getPayload() + "(pre)");
            return event;
        }

        @Override
        public void setFlowConstruct(FlowConstruct flowConstruct)
        {

            this.flowConstruct = flowConstruct;
        }

        @Override
        public void setMuleContext(MuleContext muleContext)
        {
            this.muleContext = muleContext;
        }

        public FlowConstruct getFlowConstruct()
        {
            return flowConstruct;
        }

        public MuleContext getMuleContext()
        {
            return muleContext;
        }
    }

}
