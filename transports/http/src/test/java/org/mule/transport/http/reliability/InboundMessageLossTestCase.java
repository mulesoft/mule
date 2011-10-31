/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.reliability;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.security.Credentials;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.api.transport.ReplyToHandler;
import org.mule.exception.AbstractMessagingExceptionStrategy;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.management.stats.ProcessingTime;
import org.mule.message.DefaultExceptionPayload;
import org.mule.routing.filters.WildcardFilter;
import org.mule.tck.DynamicPortTestCase;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.activation.DataHandler;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verify that no inbound messages are lost when exceptions occur.  
 * The message must either make it all the way to the SEDA queue (in the case of 
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 * 
 * In the case of the HTTP transport, there is no way to restore the source message
 * so an exception is simply returned to the client.
 */
public class InboundMessageLossTestCase extends DynamicPortTestCase
{
    protected HttpClient httpClient = new HttpClient();
    
    @Override
    protected String getConfigResources()
    {
        return "reliability/inbound-message-loss.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        // Set SystemExceptionStrategy to redeliver messages (this can only be configured programatically for now)
        ((DefaultSystemExceptionStrategy) muleContext.getExceptionListener()).setRollbackTxFilter(new WildcardFilter("*"));
    }

    public void testNoException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/noException");
        int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_OK, status);
        assertEquals("Here you go", request.getResponseBodyAsString());
    }
    
    public void testTransformerException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/transformerException");
        int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("Failure"));
    }

    public void testHandledTransformerException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/handledTransformerException");
        int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_OK, status);
        assertTrue(request.getResponseBodyAsString().contains("Success"));
    }

    public void testNotHandledTransformerException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/notHandledTransformerException");
        int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("Bad news"));
    }

    public void testRouterException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/routerException");
        int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("Failure"));
    }
    
    public void testComponentException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/componentException");
        int status = httpClient.executeMethod(request);
        // Component exception occurs after the SEDA queue for an asynchronous request, but since 
        // this request is synchronous, the failure propagates back to the client.
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("exception"));
    }    

    protected HttpMethodBase createRequest(String uri)
    {
        return new GetMethod(uri);
    }
    
    protected String getBaseUri()
    {
        return "http://localhost:" + getPorts().get(0);
    }
    
    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

    /**
     * Custom Exception Handler that handles an exception
     */
    public static class Handler extends AbstractMessagingExceptionStrategy
    {
        public Handler(MuleContext muleContext)
        {
            super(muleContext);
        }

        @Override
        public MuleEvent handleException(Exception ex, MuleEvent event, RollbackSourceCallback rollbackMethod)
        {
            doHandleException(ex, event, rollbackMethod);
            return new DefaultMuleEvent(new DefaultMuleMessage("Success!", muleContext), event);
        }
    }

    /**
     * Custom Exception Handler that creates a different exception
     */
    public static class BadHandler extends AbstractMessagingExceptionStrategy
    {
        public BadHandler(MuleContext muleContext)
        {
            super(muleContext);
        }

        @Override
        public MuleEvent handleException(Exception ex, MuleEvent event, RollbackSourceCallback rollbackMethod)
        {
            doHandleException(ex, event, rollbackMethod);
            DefaultMuleMessage message = new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
            message.setExceptionPayload(
                new DefaultExceptionPayload(new MessagingException(event, new RuntimeException("Bad news!"))));
            return new DefaultMuleEvent(message, event);
        }
    }
}
