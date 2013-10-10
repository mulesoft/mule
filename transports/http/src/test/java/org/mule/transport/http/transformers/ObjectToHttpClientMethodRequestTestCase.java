/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpRequest;
import org.mule.transport.http.RequestLine;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectToHttpClientMethodRequestTestCase extends AbstractMuleContextTestCase
{
    private MuleMessage setupRequestContext(String url) throws Exception
    {
        HttpRequest request = new HttpRequest(new RequestLine("GET", url, HttpVersion.HTTP_1_1), null, "UTF-8");

        MuleEvent event = getTestEvent(request, muleContext.getEndpointFactory().getInboundEndpoint(url));
        MuleMessage message = event.getMessage();
        message.setOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        message.setOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, url);
        RequestContext.setEvent(event);

        return message;
    }

    private ObjectToHttpClientMethodRequest createTransformer() throws Exception
    {
        ObjectToHttpClientMethodRequest transformer = new ObjectToHttpClientMethodRequest();
        transformer.setMuleContext(muleContext);
        transformer.setEndpoint(RequestContext.getEvent().getEndpoint());
        transformer.initialise();
        return transformer;
    }

    @Override
    protected void doTearDown() throws Exception
    {
        RequestContext.setEvent(null);
    }

    @Test
    public void testUrlWithoutQuery() throws Exception
    {
        MuleMessage message = setupRequestContext("http://localhost:8080/services");
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());

        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);

        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;

        assertEquals(null, httpMethod.getQueryString());
    }

    @Test
    public void testUrlWithQuery() throws Exception
    {
        MuleMessage message = setupRequestContext("http://localhost:8080/services?method=echo");
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());

        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);

        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;

        assertEquals("method=echo", httpMethod.getQueryString());
    }

    @Test
    public void testUrlWithUnescapedQuery() throws Exception
    {
        MuleMessage message = setupRequestContext("http://mycompany.com/test?fruits=apple%20orange");
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());

        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);

        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;

        assertEquals("fruits=apple%20orange", httpMethod.getQueryString());
    }

    @Test
    public void testAppendedUrl() throws Exception
    {
        MuleMessage message = setupRequestContext("http://mycompany.com/test?fruits=apple%20orange");
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload("test");
        message.setOutboundProperty(HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY, "body");

        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);

        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;

        assertEquals("fruits=apple%20orange&body=test", httpMethod.getQueryString());
    }

    @Test
    public void testEncodingOfParamValueTriggeredByMessageProperty() throws Exception
    {
        // the payload is already encoded, switch off encoding it in the transformer
        String encodedPayload = "encoded%20payload";
        MuleMessage message = setupRequestContext("http://mycompany.com/");
        message.setOutboundProperty(HttpConnector.HTTP_ENCODE_PARAMVALUE, false);
        message.setOutboundProperty(HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY, "body");
        message.setPayload(encodedPayload);


        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object result = transformer.transform(message);
        
        assertTrue(result instanceof GetMethod);
        
        String expected = "body=" + encodedPayload;
        assertEquals(expected, ((GetMethod) result).getQueryString());
    }
}
