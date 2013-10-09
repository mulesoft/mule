/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.resolvers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.model.resolvers.EntryPointNotFoundException;
import org.mule.module.client.MuleClient;
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
        MuleMessage message = muleContext.getClient().send("vm://service", "hello", null);
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
        MuleMessage message = muleContext.getClient().send("vm://service?method=foo", "hello", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(EntryPointNotFoundException.class, message.getExceptionPayload()
            .getRootException()
            .getClass());
    }

    @Test
    public void testValidCallToReverse() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage msg = new DefaultMuleMessage("hello", muleContext);
        msg.setOutboundProperty("method", "reverseString");
        MuleMessage message = client.send("vm://service", msg);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToUpperCase() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage msg = new DefaultMuleMessage("hello", muleContext);
        msg.setOutboundProperty("method", "upperCaseString");
        MuleMessage message = client.send("vm://service", msg);
        assertNotNull(message);
        assertEquals("HELLO", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToReverseMethodSetOnEndpoint() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://service2-reverseString", "hello", null);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToUpperCaseMethodSetOnEndpoint() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://service2-upperCaseString", "hello", null);
        assertNotNull(message);
        assertEquals(message.getPayloadAsString(), "HELLO");
    }

    @Test
    public void testValidCallToReverseMethodSetAsHeader() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "reverseString");
        MuleMessage message = client.send("vm://service", "hello", props);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToUpperCaseMethodSetAsHeader() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "upperCaseString");
        MuleMessage message = client.send("vm://service", "hello", props);
        assertNotNull(message);
        assertEquals("HELLO", message.getPayloadAsString());
    }

}
