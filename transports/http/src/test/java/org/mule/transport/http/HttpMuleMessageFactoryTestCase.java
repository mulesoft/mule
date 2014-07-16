/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.URI;
import org.junit.Test;

public class HttpMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    private static final Header[] HEADERS = new Header[] { new Header("foo-header", "foo-value") };
    private static final String REQUEST_LINE = "GET /services/Echo HTTP/1.1";
    private static final String MULTIPART_BOUNDARY = "------------------------------2eab2c5d5c7e";
    private static final String MULTIPART_MESSAGE = MULTIPART_BOUNDARY + "\n" + "Content-Disposition: form-data; name=\"payload\"\n" + TEST_MESSAGE
                                                    + "\n" + MULTIPART_BOUNDARY + "--";
    private static final String URI = "http://localhost/services/Echo";
    private static final String REQUEST = "/services/Echo?name=John&lastname=Galt";

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new HttpMuleMessageFactory();
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        RequestLine requestLine = RequestLine.parseLine(REQUEST_LINE);
        HttpRequest request = new HttpRequest(requestLine, HEADERS, null, encoding);
        return request;
    }
    
    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is not a valid transport message for HttpMuleMessageFactory";
    }

    @Override
    public void testValidPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding, muleContext);
        assertNotNull(message);
        assertEquals("/services/Echo", message.getPayload());
        // note that on this level it's only message factory, and it adds messages from http request to the inbound scope
        assertEquals(HttpConstants.METHOD_GET, message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
        assertEquals("foo-value", message.getInboundProperty("foo-header"));
    }
    
    @Test(expected=MessageTypeNotSupportedException.class)
    public void testInvalidPayloadOnHttpMuleMessageFactory() throws Exception
    {
        HttpMuleMessageFactory factory = new HttpMuleMessageFactory();
        factory.extractPayload(getUnsupportedTransportMessage(), encoding);
    }
    
    @Test
    public void testHttpRequestPostPayload() throws Exception
    {
        HttpMuleMessageFactory factory = (HttpMuleMessageFactory) createMuleMessageFactory();
        factory.setExchangePattern(MessageExchangePattern.ONE_WAY);

        HttpRequest request = createPostHttpRequest();
        MuleMessage message = factory.create(request, encoding, muleContext);
        assertNotNull(message);
        assertEquals(byte[].class, message.getPayload().getClass());
        byte[] payload = (byte[]) message.getPayload();
        assertTrue(Arrays.equals(TEST_MESSAGE.getBytes(), payload));
    }

    private HttpRequest createPostHttpRequest() throws Exception
    {
        String line = REQUEST_LINE.replace(HttpConstants.METHOD_GET, HttpConstants.METHOD_POST);
        RequestLine requestLine = RequestLine.parseLine(line);
        InputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        return new HttpRequest(requestLine, HEADERS, stream, encoding);
    }
    
    @Test
    public void testHttpRequestMultiPartPayload() throws Exception
    {
        HttpMuleMessageFactory factory = (HttpMuleMessageFactory) createMuleMessageFactory();
        factory.setExchangePattern(MessageExchangePattern.ONE_WAY);

        HttpRequest request = createMultiPartHttpRequest();
        MuleMessage message = factory.create(request, encoding, muleContext);
        assertNotNull(message);
        assertEquals(byte[].class, message.getPayload().getClass());
        byte[] payload = (byte[]) message.getPayload();
        assertTrue(Arrays.equals(MULTIPART_MESSAGE.getBytes(), payload));
    }

    private HttpRequest createMultiPartHttpRequest() throws Exception
    {
        String line = REQUEST_LINE.replace(HttpConstants.METHOD_GET, HttpConstants.METHOD_POST);
        RequestLine requestLine = RequestLine.parseLine(line);
        InputStream stream = new ByteArrayInputStream(MULTIPART_MESSAGE.getBytes());
        Header[] headers = new Header[]{new Header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY.substring(2))};
        return new HttpRequest(requestLine, headers, stream, encoding);
    }

    @Test
    public void testHttpMethodGet() throws Exception
    {
        InputStream body = new ByteArrayInputStream("/services/Echo".getBytes());
        HttpMethod method = createMockHttpMethod(HttpConstants.METHOD_GET, body, URI, HEADERS);
        
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(method, encoding, muleContext);
        assertNotNull(message);
        assertEquals("/services/Echo", message.getPayloadAsString());
        assertEquals(HttpConstants.METHOD_GET, message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
        assertEquals(HttpVersion.HTTP_1_1.toString(), message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY));
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }
    
    @Test
    public void testHttpMethodPost() throws Exception
    {
        InputStream body = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        HttpMethod method = createMockHttpMethod(HttpConstants.METHOD_POST, body, "http://localhost/services/Echo", HEADERS);

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(method, encoding, muleContext);
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayloadAsString());
        assertEquals(HttpConstants.METHOD_POST, message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
        assertEquals(HttpVersion.HTTP_1_1.toString(), message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY));
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    @Test
    public void testQueryParamProperties() throws Exception
    {
        InputStream body = new ByteArrayInputStream(REQUEST.getBytes());
        HttpMethod method = createMockHttpMethod(HttpConstants.METHOD_GET, body, "http://localhost" + REQUEST, HEADERS);

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(method, encoding, muleContext);
        Map<String, Object> queryParams = (Map<String, Object>) message.getInboundProperty(HttpConnector.HTTP_QUERY_PARAMS);
        assertNotNull(queryParams);
        assertEquals("John", queryParams.get("name"));
        assertEquals("John", message.getInboundProperty("name"));
        assertEquals("Galt", queryParams.get("lastname"));
        assertEquals("Galt", message.getInboundProperty("lastname"));

        assertEquals("name=John&lastname=Galt", message.getInboundProperty(HttpConnector.HTTP_QUERY_STRING));
    }

    @Test
    public void testHeaderProperties() throws Exception
    {
        InputStream body = new ByteArrayInputStream(REQUEST.getBytes());
        Header[] headers = new Header[3];
        headers[0] = new Header("foo-header", "foo-value");
        headers[1] = new Header("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        headers[2] = new Header("Host", "localhost");

        HttpMethod method = createMockHttpMethod(HttpConstants.METHOD_GET, body, URI, headers);

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(method, encoding, muleContext);
        Map<String, Object> httpHeaders = message.getInboundProperty(HttpConnector.HTTP_HEADERS);
        assertNotNull(headers);
        assertEquals("foo-value", httpHeaders.get("foo-header"));
        assertEquals("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)", httpHeaders.get("User-Agent"));
        assertEquals("localhost", httpHeaders.get("Host"));
        assertEquals("false", httpHeaders.get("Keep-Alive"));
        assertEquals("false", httpHeaders.get("Connection"));
        assertEquals("", message.getInboundProperty(HttpConnector.HTTP_QUERY_STRING));
    }

    private HttpMethod createMockHttpMethod(String method, InputStream body, String uri, Header[] headers) throws Exception
    {
        HttpMethod httpMethod = mock(HttpMethod.class);
        when(httpMethod.getName()).thenReturn(method);
        when(httpMethod.getStatusLine()).thenReturn(new StatusLine("HTTP/1.1 200 OK"));
        when(httpMethod.getStatusCode()).thenReturn(HttpConstants.SC_OK);
        when(httpMethod.getURI()).thenReturn(new URI(uri, false));
        when(httpMethod.getResponseHeaders()).thenReturn(headers);
        when(httpMethod.getResponseBodyAsStream()).thenReturn(body);
        
        return httpMethod;
    }

    @Test
    public void testMultipleHeaderWithSameName() throws Exception
    {
        HttpMuleMessageFactory messageFactory = new HttpMuleMessageFactory();

        Header[] headers = new Header[4];
        headers[0] = new Header("k2", "priority");
        headers[1] = new Header("k1", "top");
        headers[2] = new Header("k2", "always");
        headers[3] = new Header("k2", "true");

        Map<String, Object> parsedHeaders = messageFactory.convertHeadersToMap(headers, "http://localhost/");

        assertEquals(2, parsedHeaders.size());
        assertEquals("top", parsedHeaders.get("k1"));
        assertEquals("priority,always,true", parsedHeaders.get("k2"));
    }

    @Test
    public void testProcessQueryParams() throws UnsupportedEncodingException
    {
        HttpMuleMessageFactory messageFactory = new HttpMuleMessageFactory();
        
        String queryParams = "key1=value1&key2=value2&key1=value4&key3=value3&key1=value5";
        Map<String, Object> processedParams = messageFactory.processQueryParams("http://localhost:8080/resources?" + queryParams, "UTF-8");

        Object value1 = processedParams.get("key1");
        assertNotNull(value1);
        assertTrue(value1 instanceof List);
        assertTrue(((List)value1).contains("value1"));
        assertTrue(((List)value1).contains("value4"));
        assertTrue(((List)value1).contains("value5"));
        
        Object value2 = processedParams.get("key2");
        assertNotNull(value2);
        assertEquals("value2", value2);

        Object value3 = processedParams.get("key3");
        assertNotNull(value3);
        assertEquals("value3", value3);

    }
    
    @Test
    public void testProcessEscapedQueryParams() throws UnsupportedEncodingException
    {
        HttpMuleMessageFactory messageFactory = new HttpMuleMessageFactory();
        
        String queryParams = "key1=value%201&key2=value2&key%203=value3&key%203=value4";
        Map<String, Object> processedParams = messageFactory.processQueryParams("http://localhost:8080/resources?" + queryParams, "UTF-8");
        
        Object value1 = processedParams.get("key1");
        assertNotNull(value1);
        assertEquals("value 1", value1);
        
        Object value2 = processedParams.get("key2");
        assertNotNull(value2);
        assertEquals("value2", value2);
        
        Object value3 = processedParams.get("key 3");
        assertNotNull(value3);
        assertTrue(value3 instanceof List);
        assertTrue(((List)value3).contains("value3"));
        assertTrue(((List)value3).contains("value4"));

    }

    @Test
    public void testProcessWsdlQueryParam() throws UnsupportedEncodingException
    {
        HttpMuleMessageFactory messageFactory = new HttpMuleMessageFactory();

        Map<String, Object> processedParams = messageFactory.processQueryParams("http://localhost:8080/resources?wsdl", "UTF-8");
        assertTrue(processedParams.containsKey("wsdl"));
        assertNull(processedParams.get("wsdl"));
    }
}
