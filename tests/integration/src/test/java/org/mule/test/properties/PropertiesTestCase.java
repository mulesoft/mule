/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class PropertiesTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/properties/properties-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/properties/properties-config-flow.xml"}});
    }

    public PropertiesTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    /**
     * Test that the VM transport correctly copies outbound to inbound properties both for requests amd
     * responses
     */
    @Test
    public void testProperties() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage msg1 = createOutboundMessage();
        MuleMessage response = client.send("vm://in", msg1);
        assertEquals(response.getPayloadAsString(), "OK(success)");
        assertNull(response.getInboundProperty("outbound1"));
        assertNull(response.getInboundProperty("outbound2"));
        assertNull(response.getOutboundProperty("outbound1"));
        assertNull(response.getOutboundProperty("outbound2"));
        assertEquals("ja", response.<Object> getInvocationProperty("invocation1"));
        assertEquals("nein", response.<Object> getInvocationProperty("invocation2"));
        assertEquals("123", response.getInboundProperty("outbound3"));
        assertEquals("456", response.getInboundProperty("outbound4"));
        assertNull(response.<Object> getInvocationProperty("invocation3"));
        assertNull(response.<Object> getInvocationProperty("invocation4"));

        MuleMessage msg2 = createOutboundMessage();
        client.dispatch("vm://inQueue", msg2);
        response = client.request("vm://outQueue", RECEIVE_TIMEOUT);
        assertEquals(response.getPayloadAsString(), "OK");
        assertEquals("yes", response.getInboundProperty("outbound1"));
        assertEquals("no", response.getInboundProperty("outbound2"));
        assertNull(response.getOutboundProperty("outbound1"));
        assertNull(response.getOutboundProperty("outbound2"));
        assertNull(response.<Object> getInvocationProperty("invocation1"));
        assertNull(response.<Object> getInvocationProperty("invocation2"));

    }

    private MuleMessage createOutboundMessage()
    {
        MuleMessage msg = new DefaultMuleMessage("OK", muleContext);
        msg.setOutboundProperty("outbound1", "yes");
        msg.setOutboundProperty("outbound2", "no");
        msg.setInvocationProperty("invocation1", "ja");
        msg.setInvocationProperty("invocation2", "nein");
        return msg;
    }

    public static class Component
    {
        /**
         * Create a message with outbound and invocation properties. These should have been moved to the
         * inbound scope by the time the message is received. Invocation properties should have been removed
         */
        public MuleMessage process(Object payload)
        {
            MuleMessage msg = new DefaultMuleMessage(payload + "(success)", muleContext);
            msg.setOutboundProperty("outbound3", "123");
            msg.setOutboundProperty("outbound4", "456");
            msg.setInvocationProperty("invocation3", "UVW");
            msg.setInvocationProperty("invocation4", "XYZ");
            return msg;
        }
    }
}
