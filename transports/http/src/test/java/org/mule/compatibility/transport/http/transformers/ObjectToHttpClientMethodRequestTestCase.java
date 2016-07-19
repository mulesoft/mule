/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.transformers;

import static java.nio.charset.StandardCharsets.UTF_8;
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
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
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
    
    private MuleMessage setupRequestContext(final String url, final String method) throws Exception
    {
        HttpRequest request = new HttpRequest(new RequestLine(method, url, HTTP_1_1), null, UTF_8);
        
        endpoint = getEndpointFactory().getInboundEndpoint(url);
        
        MuleEvent event = getTestEvent(request, endpoint);
        MuleMessage message = MuleMessage.builder(event.getMessage())
                .addOutboundProperty(HTTP_METHOD_PROPERTY, method)
                .addOutboundProperty(MULE_ENDPOINT_PROPERTY, url)
                .build();
        RequestContext.setEvent(event);

        return message;
    }

    private MuleMessage setupRequestContextForCollection(final String url, final String method,
                                                                   List<MuleMessage> messages) throws Exception
    {
        HttpRequest request = new HttpRequest(new RequestLine(method, url, HTTP_1_1), null, UTF_8);
        
        endpoint = getEndpointFactory().getInboundEndpoint(url);
        
        MuleEvent event = getTestEvent(request, endpoint);
        MuleMessage message = MuleMessage.builder()
                .payload(messages)
                .addOutboundProperty(HTTP_METHOD_PROPERTY, method)
                .addOutboundProperty(MULE_ENDPOINT_PROPERTY, url)
                .build();
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
        // transforming NullPayload will make sure that no body=xxx query is added
        MuleMessage message = MuleMessage.builder(setupRequestContext("http://localhost:8080/services", METHOD_GET))
                .payload(NullPayload.getInstance())
                .build();

        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);

        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;

        assertEquals(null, httpMethod.getQueryString());
    }

    @Test
    public void testUrlWithQuery() throws Exception
    {
        // transforming NullPayload will make sure that no body=xxx query is added
        MuleMessage message = MuleMessage.builder(setupRequestContext("http://localhost:8080/services?method=echo", METHOD_GET))
                .payload(NullPayload.getInstance())
                .build();

        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);

        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;

        assertEquals("method=echo", httpMethod.getQueryString());
    }

    @Test
    public void testUrlWithUnescapedQuery() throws Exception
    {
        // transforming NullPayload will make sure that no body=xxx query is added
        MuleMessage message = MuleMessage.builder(setupRequestContext("http://mycompany.com/test?fruits=apple%20orange", METHOD_GET))
                .payload(NullPayload.getInstance())
                .build();

        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);

        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;

        assertEquals("fruits=apple%20orange", httpMethod.getQueryString());
    }

    @Test
    public void testAppendedUrl() throws Exception
    {
        // transforming NullPayload will make sure that no body=xxx query is added
        MuleMessage message = MuleMessage.builder(setupRequestContext("http://mycompany.com/test?fruits=apple%20orange", METHOD_GET))
                .payload("test")
                .addOutboundProperty(HTTP_GET_BODY_PARAM_PROPERTY, "body")
                .build();

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

        MuleMessage message = MuleMessage.builder(setupRequestContext("http://mycompany.com/", METHOD_GET))
                .payload(encodedPayload)
                .addOutboundProperty(HTTP_ENCODE_PARAMVALUE, false)
                .addOutboundProperty(HTTP_GET_BODY_PARAM_PROPERTY, "body")
                .build();

        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object result = transformer.transform(message);

        assertTrue(result instanceof GetMethod);

        String expected = "body=" + encodedPayload;
        assertEquals(expected, ((GetMethod) result).getQueryString());
    }

    @Test
    public void testPostMethod() throws Exception
    {

        final String contentType = "text/plain";
        MuleMessage message = MuleMessage.builder(setupRequestContext("http://localhost:8080/services", METHOD_POST))
                .payload("I'm a payload")
                .mediaType(MediaType.parse(contentType))
                .build();

        final ObjectToHttpClientMethodRequest transformer = createTransformer();
        final Object response = transformer.transform(message);

        assertTrue(response instanceof PostMethod);
        final HttpMethod httpMethod = (HttpMethod) response;
        assertEquals(null, httpMethod.getQueryString());

        assertEquals(contentType, httpMethod.getRequestHeader(HEADER_CONTENT_TYPE).getValue());
    }

    public void testPutMethod() throws Exception
    {
    	final String contentType = "text/plain";
        MuleMessage message = MuleMessage.builder(setupRequestContext("http://localhost:8080/services", METHOD_PUT))
                .payload("I'm a payload")
                .addOutboundProperty(HEADER_CONTENT_TYPE, contentType)
                .build();

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
        final String contentType = "text/plain";
        String payload = "I'm a payload";
        MuleMessage message = MuleMessage.builder(setupRequestContext("http://localhost:8080/services", METHOD_POST))
                .payload(payload)
                .mediaType(MediaType.parse(contentType))
                .addOutboundProperty(HTTP_VERSION_PROPERTY, HTTP10)
                .build();

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
        final String contentType = "text/plain";
        String payload = "I'm a payload";

        final MuleMessage messageOne = MuleMessage.builder(setupRequestContext("http://localhost:8080/services", METHOD_POST))
                .payload(payload)
                .build();

        final MuleMessage message = MuleMessage.builder(setupRequestContextForCollection("http://localhost:8080/services",
                                                                                         METHOD_POST, singletonList(messageOne)))
                .mediaType(MediaType.parse(contentType))
                .addOutboundProperty(HTTP_VERSION_PROPERTY, HTTP10)
                .build();

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
