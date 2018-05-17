/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.apache.cxf.endpoint.ClientImpl.THREAD_LOCAL_REQUEST_CONTEXT;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.component.simple.EchoService;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.cxf.builder.SimpleClientMessageProcessorBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

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
        // After CXF-7710, when the request context is cleared, the Thread is also removed from the context
        // so when you invoke getRequestContext, the requestContext is recreated with
        // the current context. That is why getRequestContext is not empty and THREAD_LOCAL_REQUEST_CONTEXT is present.
        assertThat(processor.getClient().getRequestContext().entrySet(), hasSize(1));
        assertThat((Boolean) processor.getClient().getRequestContext().get(THREAD_LOCAL_REQUEST_CONTEXT), is(true));
        assertThat(processor.getClient().getResponseContext().isEmpty(), is(true));
        Object payload = response.getMessage().getPayload();
        assertThat(payload, IsInstanceOf.instanceOf(String.class));
        assertThat((String) payload, is("hello"));
        assertThat(gotEvent, is(true));
    }

}
