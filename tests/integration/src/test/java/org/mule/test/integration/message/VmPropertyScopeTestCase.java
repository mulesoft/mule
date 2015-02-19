/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

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
        MuleMessage message = getTestMuleMessage();
        message.setOutboundProperty("foo", "fooValue");

        MuleMessage result = client.send("inbound2", message);
        assertEquals("test bar", result.getPayload());
        assertEquals("fooValue", result.<Object> getInboundProperty("foo4"));
    }

    @Test
    public void testOneWay() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage message = getTestMuleMessage();
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
        MuleMessage message = getTestMuleMessage();
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
        MuleMessage message = getTestMuleMessage();
        message.setOutboundProperty("foo", "rrfooValue");

        client.dispatch("vm://notInConfig", message);
        MuleMessage result = client.request("vm://notInConfig", 2000);
        assertEquals("test", result.getPayload());
        assertEquals("rrfooValue", result.<Object> getInboundProperty("foo"));
    }
}
