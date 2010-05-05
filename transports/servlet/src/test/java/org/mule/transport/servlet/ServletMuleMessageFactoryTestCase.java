/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.util.UUID;

import com.mockobjects.dynamic.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpSession;

public class ServletMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{    
    private static final String CHARACTER_ENCODING_PROPERTY_KEY = ServletConnector.CHARACTER_ENCODING_PROPERTY_KEY;
    private static final String CONTENT_TYPE_PROPERTY_KEY = ServletConnector.CONTENT_TYPE_PROPERTY_KEY;
    private static final String PARAMETER_MAP_PROPERTY_KEY = ServletConnector.PARAMETER_MAP_PROPERTY_KEY;

    private static final String REQUEST_URI = MockHttpServletRequestBuilder.REQUEST_URI;
    
    private MuleMessageFactory factory;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        factory = createMuleMessageFactory();
    }

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new ServletMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        return new MockHttpServletRequestBuilder().buildRequest();
    }
    
    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is not a valid transport message for ServletMuleMessageFactory";
    }
    
    /**
     * Tests creating a MuleMessage from a GET request
     */
    @Override
    public void testValidPayload() throws Exception
    {
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertEquals(REQUEST_URI, message.getPayload());
    }
    
    public void testGetPayloadWithQueryParameter() throws Exception
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.queryString = "foo=bar";
        Object payload = builder.buildRequest();
        
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        String expected = REQUEST_URI + "?" + builder.queryString;
        assertEquals(expected, message.getPayload());
    }
    
    public void testPostPayload() throws Exception
    {
        Object payload = buildPostRequest();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertTrue(message.getPayload() instanceof InputStream);
    }
    
    public void testRequestParametersAreConvertedToMessageProperties() throws Exception
    {
        Object payload = buildPostRequest();
        MuleMessage message = factory.create(payload, encoding);
        assertRequestParameterProperty("foo-value", message, "foo");
        assertRequestParameterProperty("bar-value", message, "bar");
        
        Map<String, Object> parameters = retrieveMapProperty(message, PARAMETER_MAP_PROPERTY_KEY);
        assertNotNull(parameters);
        assertEquals("foo-value", parameters.get("foo"));
        assertEquals("bar-value", parameters.get("bar"));
    }
    
    public void testContentEncodingWithCharsetLast() throws Exception
    {
        String contentType = "text/plain;charset=UTF-21";
        Object payload = buildGetRequestWithContentType(contentType);
        MuleMessage message = factory.create(payload, encoding);
        assertEquals("UTF-21", message.getEncoding());
        assertInboundScopedProperty(contentType, message, CONTENT_TYPE_PROPERTY_KEY);
    }
    
    public void testContentEncodingWithCharsetFirst() throws Exception
    {
        String contentType = "charset=UTF-21;text/plain";
        Object payload = buildGetRequestWithContentType(contentType);
        MuleMessage message = factory.create(payload, encoding);
        assertEquals("UTF-21", message.getEncoding());
        assertInboundScopedProperty(contentType, message, CONTENT_TYPE_PROPERTY_KEY);
    }
    
    public void testMessageIdFromHttpSession() throws Exception
    {
        String sessionId = UUID.getUUID();
        Object payload = buildGetRequestWithSession(sessionId);
        MuleMessage message = factory.create(payload, encoding);
        assertEquals(sessionId, message.getUniqueId());
    }
    
    public void testCharacterEncodingFromHttpRequest() throws Exception
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.characterEncoding = "UTF-21";
        Object payload = builder.buildRequest();
        
        MuleMessage message = factory.create(payload, encoding);
        assertInboundScopedProperty(builder.characterEncoding, message, CHARACTER_ENCODING_PROPERTY_KEY);
    }
        
    public void testRequestPropertiesAreConvertedToMessageProperties() throws Exception
    {
        Object payload = buildGetRequestWithParameterValue("foo-param", "foo-value");
        MuleMessage message = factory.create(payload, encoding);
        assertInboundScopedProperty("foo-value", message, "foo-param");
    }
    
    public void testRequestAttributesAreConvertedToMessageProperties() throws Exception
    {
        Object payload = buildGetRequestWithAttributeValue("foo-attribute", "foo-value");
        MuleMessage message = factory.create(payload, encoding);
        assertInboundScopedProperty("foo-value", message, "foo-attribute");
    }

    public void testRequestHeadersAreConvertedToMessageProperties() throws Exception
    {
        Object payload = buildGetRequestWithHeaders();
        MuleMessage message = factory.create(payload, encoding);
        assertInboundScopedProperty("foo-value", message, "foo-header");
        assertInboundScopedProperty("MULE_HEADER_VALUE", message, "MULE_HEADER");
        assertInboundScopedProperty("localhost:8080", message, HttpConstants.HEADER_HOST);
    }
        
    private void assertInboundScopedProperty(Object expected, MuleMessage message, String key)
    {
        Object value = message.getProperty(key, PropertyScope.INBOUND);
        assertEquals(expected, value);
    }
    
    private void assertRequestParameterProperty(String expected, MuleMessage message, String key)
    {
        String propertyKey = ServletConnector.PARAMETER_PROPERTY_PREFIX + key;
        Object value = message.getProperty(propertyKey);
        assertEquals(expected, value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> retrieveMapProperty(MuleMessage message, String key)
    {
        return (Map<String, Object>) message.getProperty(key, PropertyScope.INBOUND);
    }

    private Object buildGetRequestWithContentType(String contentType)
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.contentType = contentType;
        return builder.buildRequest();
    }

    private Object buildGetRequestWithSession(String sessionId)
    {
        Mock mockSession = new Mock(HttpSession.class);
        mockSession.expectAndReturn("getId", sessionId);
        HttpSession session = (HttpSession) mockSession.proxy();
        
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.session = session;
        return builder.buildRequest();
    }
    
    private Object buildGetRequestWithParameterValue(String key, String value)
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.parameters = new HashMap<String, String[]>();
        builder.parameters.put(key, new String[] { value });
        return builder.buildRequest();
    }

    private Object buildGetRequestWithAttributeValue(String key, String value)
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.attributes.put(key, value);
        return builder.buildRequest();
    }

    private Object buildGetRequestWithHeaders()
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.headers.put("foo-header", "foo-value");
        builder.headers.put("X-MULE_HEADER", "MULE_HEADER_VALUE");
        builder.headers.put(HttpConstants.HEADER_HOST, "localhost");
        return builder.buildRequest();
    }

    private Object buildPostRequest()
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.method = HttpConstants.METHOD_POST;
        
        InputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        builder.inputStream = new MockServletInputStream(stream);
        
        builder.parameters = new HashMap<String, String[]>();
        builder.parameters.put("foo", new String[] { "foo-value" });
        builder.parameters.put("bar", new String[] { "bar-value" });

        return builder.buildRequest();
    }
    
    private static class MockServletInputStream extends ServletInputStream
    {
        private InputStream input;

        public MockServletInputStream(InputStream dataStream)
        {
            super();
            input = dataStream;
        }

        @Override
        public int read() throws IOException
        {
            return input.read();
        }
    }
}
