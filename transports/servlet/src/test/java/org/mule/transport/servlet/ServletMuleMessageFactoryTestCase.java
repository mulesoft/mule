/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.util.UUID;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpSession;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
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

    @Test
    public void testPostPayload() throws Exception
    {
        Object payload = buildPostRequest();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertTrue(message.getPayload() instanceof InputStream);
    }

    @Test
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

    @Test
    public void testContentEncodingWithCharsetLast() throws Exception
    {
        String contentType = "text/plain;charset=UTF-21";
        Object payload = buildGetRequestWithContentType(contentType);
        MuleMessage message = factory.create(payload, encoding);
        assertEquals("UTF-21", message.getEncoding());
        assertInboundScopedProperty(contentType, message, CONTENT_TYPE_PROPERTY_KEY);
    }

    @Test
    public void testContentEncodingWithCharsetFirst() throws Exception
    {
        String contentType = "charset=UTF-21;text/plain";
        Object payload = buildGetRequestWithContentType(contentType);
        MuleMessage message = factory.create(payload, encoding);
        assertEquals("UTF-21", message.getEncoding());
        assertInboundScopedProperty(contentType, message, CONTENT_TYPE_PROPERTY_KEY);
    }

    @Test
    public void testMessageIdFromHttpSession() throws Exception
    {
        String sessionId = UUID.getUUID();
        Object payload = buildGetRequestWithSession(sessionId);
        MuleMessage message = factory.create(payload, encoding);
        assertEquals(sessionId, message.<Object>getInboundProperty(ServletConnector.SESSION_ID_PROPERTY_KEY));
    }

    /**
     * Test for MULE-5101
     */
    @Test
    public void testUniqueMessageId() throws Exception
    {
        String sessionId = UUID.getUUID();
        Object payload = buildGetRequestWithSession(sessionId);
        Object payload2 = buildGetRequestWithSession(sessionId);
        MuleMessage message = factory.create(payload, encoding);
        MuleMessage message2 = factory.create(payload2, encoding);
        assertEquals(sessionId, message.<Object>getInboundProperty(ServletConnector.SESSION_ID_PROPERTY_KEY));
        assertEquals(sessionId, message2.<Object>getInboundProperty(ServletConnector.SESSION_ID_PROPERTY_KEY));

        assertFalse(message.getUniqueId().equals(message2.getUniqueId()));
    }

    @Test
    public void testCharacterEncodingFromHttpRequest() throws Exception
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.characterEncoding = "UTF-21";
        Object payload = builder.buildRequest();

        MuleMessage message = factory.create(payload, encoding);
        assertInboundScopedProperty(builder.characterEncoding, message, CHARACTER_ENCODING_PROPERTY_KEY);
    }

    @Test
    public void testRequestPropertiesAreConvertedToMessageProperties() throws Exception
    {
        Object payload = buildGetRequestWithParameterValue("foo-param", "foo-value");
        MuleMessage message = factory.create(payload, encoding);
        assertInboundScopedProperty("foo-value", message, "foo-param");
    }

    @Test
    public void testRequestAttributesAreConvertedToMessageProperties() throws Exception
    {
        Object payload = buildGetRequestWithAttributeValue("foo-attribute", "foo-value");
        MuleMessage message = factory.create(payload, encoding);
        assertInboundScopedProperty("foo-value", message, "foo-attribute");
    }

    @Test
    public void testRequestHeadersAreConvertedToMessageProperties() throws Exception
    {
        Object payload = buildGetRequestWithHeaders();
        MuleMessage message = factory.create(payload, encoding);
        assertInboundScopedProperty("foo-value", message, "foo-header");
        assertInboundScopedProperty("MULE_HEADER_VALUE", message, "MULE_HEADER");
        assertInboundScopedProperty("localhost:8080", message, HttpConstants.HEADER_HOST);

        Object[] expected = new Object[] { "value-one", "value-two" };
        assertTrue(Arrays.equals(expected, (Object[]) message.getInboundProperty("multi-value")));
    }

    private void assertInboundScopedProperty(Object expected, MuleMessage message, String key)
    {
        Object value = message.getInboundProperty(key);
        assertEquals(expected, value);
    }

    private void assertRequestParameterProperty(String expected, MuleMessage message, String key)
    {
        String propertyKey = ServletConnector.PARAMETER_PROPERTY_PREFIX + key;
        // message factory puts props in the inbound scope
        Object value = message.getInboundProperty(propertyKey);
        assertEquals(expected, value);
    }

    private Map<String, Object> retrieveMapProperty(MuleMessage message, String key)
    {
        return message.getInboundProperty(key);
    }

    private Object buildGetRequestWithContentType(String contentType) throws Exception
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.contentType = contentType;
        return builder.buildRequest();
    }

    private Object buildGetRequestWithSession(String sessionId) throws Exception
    {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(sessionId);

        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.session = session;
        return builder.buildRequest();
    }

    private Object buildGetRequestWithParameterValue(String key, String value) throws Exception
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.parameters = new HashMap<String, String[]>();
        builder.parameters.put(key, new String[] { value });
        return builder.buildRequest();
    }

    private Object buildGetRequestWithAttributeValue(String key, String value) throws Exception
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.attributes.put(key, value);
        return builder.buildRequest();
    }

    private Object buildGetRequestWithHeaders() throws Exception
    {
        MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder();
        builder.headers.put("foo-header", "foo-value");
        builder.headers.put("X-MULE_HEADER", "MULE_HEADER_VALUE");
        builder.headers.put(HttpConstants.HEADER_HOST, "localhost");

        Vector<String> multiValue = new Vector<String>();
        multiValue.add("value-one");
        multiValue.add("value-two");
        builder.headers.put("multi-value", multiValue.elements());

        return builder.buildRequest();
    }

    private Object buildPostRequest() throws Exception
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
}
