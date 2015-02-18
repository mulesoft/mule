/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.model.resolvers.EntryPointNotFoundException;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.util.ExceptionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MethodEntryPointsTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/resolvers/method-entrypoints-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/resolvers/method-entrypoints-config-flow.xml"}
        });
    }

    public MethodEntryPointsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testTooManySatisfiableMethods() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://service", "hello", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(EntryPointNotFoundException.class, message.getExceptionPayload()
            .getRootException()
            .getClass());
        assertTrue(ExceptionUtils.getRootCauseMessage(message.getExceptionPayload().getException()).indexOf(
            "Found too many possible methods on object") > -1);
    }

    @Test
    public void testBadMethodName() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://service?method=foo", "hello", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(EntryPointNotFoundException.class, message.getExceptionPayload()
            .getRootException()
            .getClass());
    }

    @Test
    public void testValidCallToReverse() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage msg = getTestMuleMessage("hello");
        msg.setOutboundProperty("method", "reverseString");
        MuleMessage message = client.send("vm://service", msg);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToUpperCase() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage msg = getTestMuleMessage("hello");
        msg.setOutboundProperty("method", "upperCaseString");
        MuleMessage message = client.send("vm://service", msg);
        assertNotNull(message);
        assertEquals("HELLO", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToReverseMethodSetOnEndpoint() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://service2-reverseString", "hello", null);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToUpperCaseMethodSetOnEndpoint() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://service2-upperCaseString", "hello", null);
        assertNotNull(message);
        assertEquals(message.getPayloadAsString(), "HELLO");
    }

    @Test
    public void testValidCallToReverseMethodSetAsHeader() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "reverseString");
        MuleMessage message = client.send("vm://service", "hello", props);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToUpperCaseMethodSetAsHeader() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "upperCaseString");
        MuleMessage message = client.send("vm://service", "hello", props);
        assertNotNull(message);
        assertEquals("HELLO", message.getPayloadAsString());
    }
}
