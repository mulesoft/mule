/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.component.simple.EchoService;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.cxf.builder.SimpleClientMessageProcessorBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CxfOutboundMessageProcessorTestCase extends AbstractMuleContextTestCase
{
    String msg = 
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>" +
            "<ns1:echo xmlns:ns1=\"http://simple.component.api.mule.org/\">" +
                "<ns1:return>hello</ns1:return>" +
            "</ns1:echo>" +
        "</soap:Body></soap:Envelope>";

    boolean gotEvent = false;
    Object payload;
    
    @Test
    public void testOutbound() throws Exception
    {
        CxfConfiguration config = new CxfConfiguration();
        config.setMuleContext(muleContext);
        config.initialise();
        
        // Build a CXF MessageProcessor
        SimpleClientMessageProcessorBuilder builder = new SimpleClientMessageProcessorBuilder();
        builder.setConfiguration(config);
        builder.setServiceClass(EchoService.class);
        builder.setOperation("echo");
        builder.setMuleContext(muleContext);
        
        CxfOutboundMessageProcessor processor = builder.build();
        
        MessageProcessor messageProcessor = new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                payload = event.getMessage().getPayload();
                try
                {
                    System.out.println(event.getMessage().getPayloadAsString());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                
                event.getMessage().setPayload(msg);
                gotEvent = true;
                return event;
            }
        };
        processor.setListener(messageProcessor);
        
        MuleEvent event = getTestEvent("hello", getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE));
        MuleEvent response = processor.process(event);
        assertThat(processor.getClient().getRequestContext().isEmpty(), Is.is(true));
        assertThat(processor.getClient().getResponseContext().isEmpty(), Is.is(true));
        Object payload = response.getMessage().getPayload();
        assertThat(payload, IsInstanceOf.instanceOf(String.class));
        assertThat((String) payload,Is.is("hello"));
        assertThat(gotEvent,Is.is(true));
    }

}
