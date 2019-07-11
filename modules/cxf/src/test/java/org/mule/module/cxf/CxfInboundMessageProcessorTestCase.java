/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.OutputHandler;
import org.mule.module.cxf.builder.WebServiceMessageProcessorBuilder;
import org.mule.module.cxf.support.ProxySchemaValidationInInterceptor;
import org.mule.module.cxf.support.StreamClosingInterceptor;
import org.mule.module.cxf.testmodels.Echo;
import org.mule.module.xml.stax.DelegateXMLStreamReader;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.junit.Test;

public class CxfInboundMessageProcessorTestCase extends AbstractMuleContextTestCase
{
    private String msg =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>" +
            "<ns1:echo xmlns:ns1=\"http://testmodels.cxf.module.mule.org/\">" +
            "<text>echo</text>" +
            "</ns1:echo>" +
            "</soap:Body></soap:Envelope>";

    private String msgIncorrectLiteral =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>" +
                                         "<ns1:echo xmlns:ns1=\"http://testmodels.cxf.module.mule.org/\">" +
                                         "<textIncorrectLiteral>echo</textIncorrectLiteral>" +
                                         "</ns1:echo>" +
                                         "</soap:Body></soap:Envelope>";

    private static final String ANOTHER_VALUE = "another value";
    private String responseMsg =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>" +
                                 "<ns2:echoResponse xmlns:ns2=\"http://testmodels.cxf.module.mule.org/\">" +
                                 "<text>another value</text>" +
                                 "</ns2:echoResponse>" +
                                 "</soap:Body></soap:Envelope>";

    private String responseMsgIncorrectLiteral =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>" +
                                                 "<soap:Fault><faultcode>soap:Client</faultcode><faultstring>" +
                                                 "Schema validation error on message from client: tag name \"textIncorrectLiteral\" is not allowed. " +
                                                 "Possible tag names are: &lt;text>.</faultstring></soap:Fault></soap:Body></soap:Envelope>";


    private boolean gotEvent = false;
    Object payload;
    
    @Test
    public void testInbound() throws Exception
    {
        CxfInboundMessageProcessor processor = createCxfMessageProcessor();
        
        MessageProcessor messageProcessor = new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                payload = event.getMessage().getPayload();
                assertEquals("echo", payload);
                event.getMessage().setPayload(ANOTHER_VALUE);
                gotEvent = true;
                return event;
            }
        };
        processor.setListener(messageProcessor);
        
        MuleEvent event = getTestEvent(msg, getTestInboundEndpoint(REQUEST_RESPONSE));
        
        MuleEvent response = processor.process(event);
        Object payload = response.getMessage().getPayload();
        
        assertThat(payload, instanceOf(OutputHandler.class));
        assertThat(response.getMessage().getPayloadAsString(), is(responseMsg));
        assertTrue(gotEvent);
    }
    
    @Test
    public void testOneWay() throws Exception
    {
        CxfInboundMessageProcessor processor = createCxfMessageProcessor();
        
        MessageProcessor messageProcessor = new MessageProcessor()
        {
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

    @Test
    public void inboundWithValidationEnabled() throws Exception
    {
        inboundWithValidation(msg, responseMsg);
    }

    @Test
    public void inboundWithValidationEnabledIncorrectLiteral() throws Exception
    {
        inboundWithValidation(msgIncorrectLiteral, responseMsgIncorrectLiteral);
    }

    protected void inboundWithValidation(String msg, String responseMsg) throws MuleException, Exception
    {
        CxfInboundMessageProcessor processor = createMessageProcessorWithValidationEnabled();

        MessageProcessor messageProcessor = new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                payload = event.getMessage().getPayload();
                assertThat("echo", equalTo(payload));
                event.getMessage().setPayload(ANOTHER_VALUE);
                return event;
            }
        };
        processor.setListener(messageProcessor);

        MuleEvent event = getTestEvent(msg, getTestInboundEndpoint(REQUEST_RESPONSE));

        MuleEvent response = processor.process(event);

        Object payload = response.getMessage().getPayload();
        assertThat(payload, instanceOf(OutputHandler.class));
        assertThat(response.getMessage().getPayloadAsString(), is(responseMsg));
    }


    private CxfInboundMessageProcessor createMessageProcessorWithValidationEnabled() throws MuleException
    {
        CxfConfiguration config = new CxfConfiguration();
        config.setMuleContext(muleContext);
        config.initialise();

        // Build a CXF MessageProcessor
        WebServiceMessageProcessorBuilder builder = new WebServiceMessageProcessorBuilder();
        builder.setConfiguration(config);
        builder.setServiceClass(Echo.class);
        builder.setMuleContext(muleContext);
        builder.getInInterceptors().add(new CustomInterceptorWithDelegateStreamReader());
        builder.setValidationEnabled(true);

        CxfInboundMessageProcessor processor = builder.build();
        Server server = processor.getServer();
        // adding a proxy schema validation interceptor
        server.getEndpoint().getInInterceptors().add(new ProxySchemaValidationInInterceptor(config.getCxfBus(), server.getEndpoint(),
                                                                    server.getEndpoint().getService().getServiceInfos().get(0)));
        processor.start();
        return processor;
    }


    private class CustomInterceptorWithDelegateStreamReader extends AbstractPhaseInterceptor<Message>
    {
        CustomInterceptorWithDelegateStreamReader()
        {
            super(Phase.POST_STREAM);
            getAfter().add(StreamClosingInterceptor.class.getName());
            getAfter().add(StaxInInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault
        {
            XMLStreamReader reader = message.getContent(XMLStreamReader.class);

            if (reader != null)
            {
                XMLStreamReader replacement = new DelegateXMLStreamReader(reader);
                message.setContent(XMLStreamReader.class, replacement);
            }
        }

    }
}
