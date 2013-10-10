/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.message;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VmPropertyScopeTestCase extends AbstractPropertyScopeTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/message/vm-property-scope-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/message/vm-property-scope-flow.xml"}});
    }

    public VmPropertyScopeTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testRequestResponseChain() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setOutboundProperty("foo", "fooValue");

        MuleMessage result = client.send("inbound2", message);
        assertEquals("test bar", result.getPayload());
        assertEquals("fooValue", result.<Object> getInboundProperty("foo4"));
    }

    @Test
    public void testOneWay() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setOutboundProperty("foo", "fooValue");

        client.dispatch("vm://queueIn", message);
        MuleMessage result = client.request("vm://queueOut", 2000);
        assertEquals("test bar", result.getPayload());
        assertEquals("fooValue", result.<Object> getInboundProperty("foo2"));
    }

    @Test
    public void testRRToOneWay() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setOutboundProperty("foo", "rrfooValue");

        MuleMessage echo = client.send("vm://rrQueueIn", message);
        MuleMessage result = client.request("vm://rrQueueOut", 2000);
        assertEquals("test baz", result.getPayload());
        assertEquals("rrfooValue", result.<Object> getInboundProperty("foo2"));
    }

    @Test
    public void testSimpleQueueAccess() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setOutboundProperty("foo", "rrfooValue");

        client.dispatch("vm://notInConfig", message);
        MuleMessage result = client.request("vm://notInConfig", 2000);
        assertEquals("test", result.getPayload());
        assertEquals("rrfooValue", result.<Object> getInboundProperty("foo"));
    }
}
