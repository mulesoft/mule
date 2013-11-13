/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.util.WebServiceOnlineCheck;
import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-6926: flaky test (Relies on an external server which can go offline after isDisabledInThisEnvironment is executed)")
public class MessagePropertiesPropagationTestCase extends FunctionalTestCase
{
    @Override
    protected boolean isFailOnTimeout()
    {
        // Do not fail test case upon timeout because this probably just means
        // that the 3rd-party web service is off-line.
        return false;
    }

    /**
     * If a simple call to the web service indicates that it is not responding properly,
     * we disable the test case so as to not report a test failure which has nothing to do
     * with Mule.
     */
    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return !WebServiceOnlineCheck.isWebServiceOnline();
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/meps/message-properties-propagation.xml";
    }

    /**
     * As per EE-1613, the Correlation-related properties _should_ be propagated to the response message by default.
     */
    @Test
    public void testPropagatedPropertiesWithHttpTransport() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("Content-Type", "application/x-www-form-urlencoded");
        props.put(MuleProperties.MULE_CORRELATION_ID_PROPERTY, "TestID");
        props.put(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, "TestGroupSize");
        props.put(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, "TestSequence");

        MuleMessage response = client.send("vm://httpService1", "symbol=IBM", props);
        assertNotNull(response);
        checkPayLoad(response.getPayloadAsString());
        assertEquals("TestID", response.getOutboundProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY));
        assertEquals("TestGroupSize", response.getOutboundProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY));
        assertEquals("TestSequence", response.getOutboundProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY));
    }

    /**
     * As per EE-1613, the Correlation-related properties _should_ be propagated to the response message by default.
     */
    @Test
    public void testPropagatedPropertiesWithCxfTransport() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_CORRELATION_ID_PROPERTY, "TestID");
        props.put(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, "TestGroupSize");
        props.put(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, "TestSequence");

        MuleMessage response = client.send("vm://cxfService1", "IBM", props);
        assertNotNull(response);
        checkPayLoad(response.getPayloadAsString());
        assertEquals("TestID", response.getOutboundProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY));
        assertEquals("TestGroupSize", response.getOutboundProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY));
        assertEquals("TestSequence", response.getOutboundProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY));
    }

    /**
     * As per EE-1611/MULE-4302, custom properties and non-Correlation-related properties _should not_ be propagated
     * to the response message by default.
     */
    @Test
    public void testNotPropagatedPropertiesWithHttpTransport() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("Content-Type", "application/x-www-form-urlencoded");
        props.put("some", "thing");
        props.put("other", "stuff");
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/bizarre;charset=utf-16");

        MuleMessage response = client.send("vm://httpService1", "symbol=IBM", props);
        assertNotNull(response);
        assertNull(response.getInboundProperty("some"));
        assertNull(response.getInboundProperty("other"));
        assertEquals("text/plain; charset=utf-8", response.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("utf-8", response.getEncoding());
    }

    /**
     * As per EE-1611/MULE-4302, custom properties and non-Correlation-related properties _should not_ be propagated
     * to the response message by default.
     */
    @Test
    public void testNotPropagatedPropertiesWithCxfTransport() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("some", "thing");
        props.put("other", "stuff");
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/bizarre;charset=utf-16");

        MuleMessage response = client.send("vm://cxfService1", "IBM", props);
        assertNotNull(response);
        assertNull(response.getInboundProperty("some"));
        assertNull(response.getInboundProperty("other"));
        assertEquals("text/xml; charset=utf-8", response.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("utf-8", response.getEncoding());
    }

    /**
     * Force the properties to be propagated to the response message.
     */
    @Test
    public void testForcePropagatedPropertiesWithHttpTransport() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("Content-Type", "application/x-www-form-urlencoded");
        props.put("some", "thing");
        props.put("other", "stuff");

        MuleMessage response = client.send("vm://httpService2", "symbol=IBM", props);
        assertNotNull(response);
        checkPayLoad(response.getPayloadAsString());
        assertEquals("thing", response.getInboundProperty("some"));
        assertEquals("stuff", response.getInboundProperty("other"));
    }

    /**
     * Force the properties to be propagated to the response message.
     * MULE-4986
     */
    public void xtestForcePropagatedPropertiesWithCxfTransport() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("some", "thing");
        props.put("other", "stuff");

        MuleMessage response = client.send("vm://cxfService2", "symbol=IBM", props);
        assertNotNull(response);
        checkPayLoad(response.getPayloadAsString());
        assertEquals("thing", response.getOutboundProperty("some"));
        assertEquals("stuff", response.getOutboundProperty("other"));
    }

    private void checkPayLoad(String payload)
    {
        assertNotNull("payload is null", payload);
        assertTrue("payload does not contain 'PreviousClose': " + payload, payload.contains("PreviousClose"));
    }
}
