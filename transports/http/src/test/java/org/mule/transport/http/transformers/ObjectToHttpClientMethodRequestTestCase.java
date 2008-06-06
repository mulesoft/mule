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

    // @Override
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
        Object response = transformer.transform(message);
        
        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;
        
        assertEquals("fruits=apple%20orange&body=test", httpMethod.getQueryString());
    }
}


