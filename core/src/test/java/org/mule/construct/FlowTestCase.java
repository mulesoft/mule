/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class FlowTestCase extends AbstractFlowConstuctTestCase
{

    private Flow flow;
    private SensingNullMessageProcessor sensingMessageProcessor;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        sensingMessageProcessor = getSensingNullMessageProcessor();

        flow = new Flow("test-flow", muleContext);
        flow.setMessageSource(directInboundMessageSource);

        List<MessageProcessor> processors = new ArrayList<MessageProcessor>();
        processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("f")));
        processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("e")));
        processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("d")));
        processors.add(new StringAppendTransformer("a"));
        processors.add(new StringAppendTransformer("b"));
        processors.add(new StringAppendTransformer("c"));
        processors.add(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setOutboundProperty("thread", Thread.currentThread());
                return event;
            }
        });
        processors.add(sensingMessageProcessor);
        flow.setMessageProcessors(processors);
    }

    @Override
    protected AbstractFlowConstruct getFlowConstruct() throws Exception
    {
        return flow;
    }

    @Test
    public void testProcessOneWayEndpoint() throws Exception
    {
        flow.initialise();
        flow.start();
        MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello",
            MessageExchangePattern.ONE_WAY, muleContext));
        Thread.sleep(50);

        assertNull(response);

        assertEquals("helloabc", sensingMessageProcessor.event.getMessageAsString());
        assertNotSame(Thread.currentThread(), sensingMessageProcessor.event.getMessage().getOutboundProperty(
            "thread"));
    }
    
    @Test
    public void testProcessRequestResponseEndpoint() throws Exception
    {
        flow.initialise();
        flow.start();
        MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello",
            MessageExchangePattern.REQUEST_RESPONSE, muleContext));

        assertEquals("helloabcdef", response.getMessageAsString());
        assertEquals(Thread.currentThread(), response.getMessage().getOutboundProperty("thread"));

        // Sensed (out) event also is appended with 'def' because it's the same event
        // instance
        assertEquals("helloabcdef", sensingMessageProcessor.event.getMessageAsString());
        assertEquals(Thread.currentThread(), sensingMessageProcessor.event.getMessage().getOutboundProperty(
            "thread"));

    }

    @Test
    public void testProcessStopped() throws Exception
    {
        flow.initialise();

        try
        {
            directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello", muleContext));
            fail("exception expected");
        }
        catch (Exception e)
        {
        }
    }
}
