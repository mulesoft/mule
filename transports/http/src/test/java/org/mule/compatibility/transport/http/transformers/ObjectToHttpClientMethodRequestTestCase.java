/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.transformers;

import static java.util.Collections.singletonList;
import static org.apache.commons.httpclient.HttpVersion.HTTP_1_1;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_ENCODE_PARAMVALUE;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_METHOD_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_VERSION_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_CONTENT_TYPE;
import static org.mule.compatibility.transport.http.HttpConstants.HTTP10;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_GET;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_POST;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_PUT;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENDPOINT_PROPERTY;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.transport.http.HttpRequest;
import org.mule.compatibility.transport.http.RequestLine;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MutableMuleMessage;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

public class ObjectToHttpClientMethodRequestTestCase extends AbstractMuleContextEndpointTestCase
{
    
    private InboundEndpoint endpoint;
    
    private MutableMuleMessage setupRequestContext(final String url, final String method) throws Exception
    {
        HttpRequest request = new HttpRequest(new RequestLine(method, url, HTTP_1_1), null, "UTF-8");
        
        endpoint = getEndpointFactory().getInboundEndpoint(url);
        
        MuleEvent event = getTestEvent(request, endpoint);
        MutableMuleMessage message = (MutableMuleMessage) event.getMessage();
        message.setOutboundProperty(HTTP_METHOD_PROPERTY, method);
        message.setOutboundProperty(MULE_ENDPOINT_PROPERTY, url);
        RequestContext.setEvent(event);

        return message;
    }

    private MutableMuleMessage setupRequestContextForCollection(final String url, final String method,
                                                                   List<MuleMessage> messages) throws Exception
    {
        HttpRequest request = new HttpRequest(new RequestLine(method, url, HTTP_1_1), null, "UTF-8");
        
        endpoint = getEndpointFactory().getInboundEndpoint(url);
        
        MuleEvent event = getTestEvent(request, endpoint);
        MutableMuleMessage message = new DefaultMuleMessage(messages, muleContext);
        message.setOutboundProperty(HTTP_METHOD_PROPERTY, method);
        message.setOutboundProperty(MULE_ENDPOINT_PROPERTY, url);
        RequestContext.setEvent(event);
        
        return message;
    }    

    private ObjectToHttpClientMethodRequest createTransformer() throws Exception
    {
        ObjectToHttpClientMethodRequest transformer = new ObjectToHttpClientMethodRequest();
        transformer.setMuleContext(muleContext);
        transformer.setEndpoint(endpoint);
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
        MutableMuleMessage message = setupRequestContext("http://localhost:8080/services", METHOD_GET);
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
        MutableMuleMessage message = setupRequestContext("http://localhost:8080/services?method=echo", METHOD_GET);
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
        MutableMuleMessage message = setupRequestContext("http://mycompany.com/test?fruits=apple%20orange", METHOD_GET);
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
        MutableMuleMessage message = setupRequestContext("http://mycompany.com/test?fruits=apple%20orange", METHOD_GET);
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload("test");
        message.setOutboundProperty(HTTP_GET_BODY_PARAM_PROPERTY, "body");

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
        MutableMuleMessage message = setupRequestContext("http://mycompany.com/", "GET");
        message.setOutboundProperty(HTTP_ENCODE_PARAMVALUE, false);
        message.setOutboundProperty(HTTP_GET_BODY_PARAM_PROPERTY, "body");
        message.setPayload(encodedPayload);


        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object result = transformer.transform(message);

        assertTrue(result instanceof GetMethod);

        String expected = "body=" + encodedPayload;
        assertEquals(expected, ((GetMethod) result).getQueryString());
    }

    public void testPostMethod() throws Exception
    {
        final MutableMuleMessage message = setupRequestContext("http://localhost:8080/services", METHOD_POST);
    	final String contentType = "text/plain";

        message.setPayload("I'm a payload");
        message.setOutboundProperty(HEADER_CONTENT_TYPE, contentType);

        final ObjectToHttpClientMethodRequest transformer = createTransformer();
        final Object response = transformer.transform(message);

        assertTrue(response instanceof PostMethod);
        final HttpMethod httpMethod = (HttpMethod) response;
        assertEquals(null, httpMethod.getQueryString());

        assertEquals(contentType, httpMethod.getRequestHeader(HEADER_CONTENT_TYPE).getValue());
    }

    public void testPutMethod() throws Exception
    {
        final MutableMuleMessage message = setupRequestContext("http://localhost:8080/services", METHOD_PUT);
    	final String contentType = "text/plain";

        message.setPayload("I'm a payload");
        message.setOutboundProperty(HEADER_CONTENT_TYPE, contentType);

        final ObjectToHttpClientMethodRequest transformer = createTransformer();
        final Object response = transformer.transform(message);

        assertTrue(response instanceof PutMethod);
        final HttpMethod httpMethod = (HttpMethod) response;
        assertEquals(null, httpMethod.getQueryString());

        assertEquals(contentType, httpMethod.getRequestHeader(HEADER_CONTENT_TYPE).getValue());
    }
    
    @Test
    public void testPostMethodWithHttp10ForMuleMessage() throws Exception
    {
        final MutableMuleMessage message = setupRequestContext("http://localhost:8080/services", METHOD_POST);
        final String contentType = "text/plain";

        String payload = "I'm a payload";
        message.setPayload(payload);
        message.setOutboundProperty(HEADER_CONTENT_TYPE, contentType);
        message.setOutboundProperty(HTTP_VERSION_PROPERTY, HTTP10);

        final ObjectToHttpClientMethodRequest transformer = createTransformer();
        final Object response = transformer.transform(message);

        assertTrue(response instanceof PostMethod);
        final HttpMethod httpMethod = (HttpMethod) response;
        assertEquals(null, httpMethod.getQueryString());
        final byte[] byteArrayContent = ((ByteArrayRequestEntity)((PostMethod)httpMethod).getRequestEntity()).getContent();
        assertArrayEquals(payload.getBytes(), byteArrayContent);
    }

    @Test
    public void testPostMethodWithHttp10ForMuleMessageCollection() throws Exception
    {
        MutableMuleMessage messageOne = setupRequestContext("http://localhost:8080/services", METHOD_POST);
        final MutableMuleMessage message = setupRequestContextForCollection("http://localhost:8080/services", METHOD_POST, singletonList(messageOne));
        final String contentType = "text/plain";
        
        String payload = "I'm a payload";
        messageOne.setPayload(payload);
        message.setOutboundProperty(HEADER_CONTENT_TYPE, contentType);
        message.setOutboundProperty(HTTP_VERSION_PROPERTY, HTTP10);
        
        final ObjectToHttpClientMethodRequest transformer = createTransformer();
        final Object response = transformer.transform(message);
        
        assertTrue(response instanceof PostMethod);
        final HttpMethod httpMethod = (HttpMethod) response;
        assertEquals(null, httpMethod.getQueryString());
        final byte[] byteArrayContent = ((ByteArrayRequestEntity)((PostMethod)httpMethod).getRequestEntity()).getContent();
        final byte[] expectedByteArrayContent = SerializationUtils.serialize((Serializable) message.getPayload());
        assertArrayEquals(expectedByteArrayContent, byteArrayContent);
    }


}
