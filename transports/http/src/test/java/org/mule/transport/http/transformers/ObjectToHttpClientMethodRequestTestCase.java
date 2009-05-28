/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.transformers;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpMethod;

public class ObjectToHttpClientMethodRequestTestCase extends AbstractMuleTestCase
{
    
    private MuleMessage setupRequestContext(String url) throws Exception
    {
        MuleEvent event = getTestEvent("test");
        MuleMessage message = event.getMessage();
        message.setStringProperty(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        message.setStringProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, url);
        RequestContext.setEvent(event);
        
        return message;
    }

    @Override
    protected void doTearDown() throws Exception
    {
        RequestContext.setEvent(null);
    }

    public void testUrlWithoutQuery() throws Exception
    {
        MuleMessage message = setupRequestContext("http://localhost:8080/services");
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());

        ObjectToHttpClientMethodRequest transformer = new ObjectToHttpClientMethodRequest();
        transformer.setMuleContext(muleContext);
        transformer.initialise();
        Object response = transformer.transform(message);
        
        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;
        
        assertEquals(null, httpMethod.getQueryString());
    }
    
    public void testUrlWithQuery() throws Exception
    {
        MuleMessage message = setupRequestContext("http://localhost:8080/services?method=echo");
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());
        
        ObjectToHttpClientMethodRequest transformer = new ObjectToHttpClientMethodRequest();
        transformer.setMuleContext(muleContext);
        transformer.initialise();
        Object response = transformer.transform(message);
        
        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;
        
        assertEquals("method=echo", httpMethod.getQueryString());
    }

    public void testUrlWithUnescapedQuery() throws Exception
    {
        MuleMessage message = setupRequestContext("http://mycompany.com/test?fruits=apple%20orange");
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());
        
        ObjectToHttpClientMethodRequest transformer = new ObjectToHttpClientMethodRequest();
        transformer.setMuleContext(muleContext);
        transformer.initialise();
        Object response = transformer.transform(message);
        
        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;
        
        assertEquals("fruits=apple%20orange", httpMethod.getQueryString());
    }
    
    public void testAppendedUrl() throws Exception
    {
        MuleMessage message = setupRequestContext("http://mycompany.com/test?fruits=apple%20orange");
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload("test");
        
        ObjectToHttpClientMethodRequest transformer = new ObjectToHttpClientMethodRequest();
        transformer.setMuleContext(muleContext);
        transformer.initialise();
        Object response = transformer.transform(message);
        
        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;
        
        assertEquals("fruits=apple%20orange&body=test", httpMethod.getQueryString());
    }

    public void testAppendedUrlWithExpressions() throws Exception
    {
        MuleMessage message = setupRequestContext("http://mycompany.com/test?fruits=#[header:fruit1],#[header:fruit2]&correlationID=#[message:correlationId]");
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload(NullPayload.getInstance());
        message.setCorrelationId("1234");
        message.setProperty("fruit1", "apple");
        message.setProperty("fruit2", "orange");
        ObjectToHttpClientMethodRequest transformer = new ObjectToHttpClientMethodRequest();
        transformer.setMuleContext(muleContext);
        transformer.initialise();
        Object response = transformer.transform(message);

        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;

        assertEquals("fruits=apple,orange&correlationID=1234", httpMethod.getQueryString());
    }

    public void testAppendedUrlWithBadExpressions() throws Exception
    {
        MuleMessage message = setupRequestContext("http://mycompany.com/test?param=#[foo:bar]}");
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload(NullPayload.getInstance());
        ObjectToHttpClientMethodRequest transformer = new ObjectToHttpClientMethodRequest();
        transformer.setMuleContext(muleContext);
        transformer.initialise();
        Object response = null;
        try
        {
            response = transformer.transform(message);
            fail("unknown evaluator was used");
        }
        catch (TransformerException e)
        {
            //Expected
            assertTrue(e.getMessage().contains("Evaluator for \"foo\" is not registered with Mule"));
        }

        message = setupRequestContext("http://mycompany.com/test?param=#[header:bar]");
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload(NullPayload.getInstance());
        try
        {
            response = transformer.transform(message);
            fail("Header 'bar' not set on the message");
        }
        catch (TransformerException e)
        {
            //Expected
            assertTrue(e.getMessage().contains("Expression Evaluator \"header\" with expression \"bar\" returned null but a value was required"));
        }

    }
}


