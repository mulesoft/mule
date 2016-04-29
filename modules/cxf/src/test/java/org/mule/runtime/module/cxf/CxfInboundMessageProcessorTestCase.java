/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.module.cxf.builder.WebServiceMessageProcessorBuilder;
import org.mule.runtime.module.cxf.testmodels.Echo;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class CxfInboundMessageProcessorTestCase extends AbstractMuleContextTestCase
{
    String msg = 
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>" +
            "<ns1:echo xmlns:ns1=\"http://testmodels.cxf.module.runtime.mule.org/\">" +
                "<text>echo</text>" +
            "</ns1:echo>" +
        "</soap:Body></soap:Envelope>";

    boolean gotEvent = false;
    Object payload;
    
    @Test
    public void testInbound() throws Exception
    {
        CxfInboundMessageProcessor processor = createCxfMessageProcessor();
        
        MessageProcessor messageProcessor = new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                payload = event.getMessage().getPayload();
                assertEquals("echo", payload);
                event.getMessage().setPayload("echo");
                gotEvent = true;
                return event;
            }
        };
        processor.setListener(messageProcessor);
        
        MuleEvent event = getTestEvent(msg, getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE));
        
        MuleEvent response = processor.process(event);
        
        Object payload = response.getMessage().getPayload();
        assertTrue(payload instanceof OutputHandler);
        
        ((OutputHandler) payload).write(response, System.out);
        assertTrue(gotEvent);
    }
    
    @Test
    public void testOneWay() throws Exception
    {
        CxfInboundMessageProcessor processor = createCxfMessageProcessor();
        
        MessageProcessor messageProcessor = new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                payload = event.getMessage().getPayload();
                assertEquals("echo", payload);
                event.getMessage().setPayload("echo");
                gotEvent = true;
                return null;
            }
        };
        processor.setListener(messageProcessor);
        
        MuleEvent event = getTestEvent(msg, getTestInboundEndpoint(MessageExchangePattern.ONE_WAY));
        
        MuleEvent response = processor.process(event);
        
        assertTrue(gotEvent);
        assertNull(response);
    }

    private CxfInboundMessageProcessor createCxfMessageProcessor() throws MuleException
    {
        CxfConfiguration config = new CxfConfiguration();
        config.setMuleContext(muleContext);
        config.initialise();
        
        // Build a CXF MessageProcessor
        WebServiceMessageProcessorBuilder builder = new WebServiceMessageProcessorBuilder();
        builder.setConfiguration(config);
        builder.setServiceClass(Echo.class);
        builder.setMuleContext(muleContext);
        
        CxfInboundMessageProcessor processor = builder.build();
        processor.start();
        return processor;
    }

}
