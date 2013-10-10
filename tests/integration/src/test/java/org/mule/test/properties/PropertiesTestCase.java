/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.properties;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PropertiesTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/properties/properties-config.xml";
    }

    /**
     * Test that the VM transport correctly copies outbound to inbound properties both for requests amd responses
     */
    @Test
    public void testProperties() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map<String, FunctionalTestComponent> components = muleContext.getRegistry().lookupByType(FunctionalTestComponent.class);
        MuleMessage msg1 = createOutboundMessage();
        MuleMessage response = client.send("vm://in", msg1);
        assertEquals(response.getPayloadAsString(), "OK(success)");
        assertNull(response.getInboundProperty("outbound1"));
        assertNull(response.getInboundProperty("outbound2"));
        assertNull(response.getOutboundProperty("outbound1"));
        assertNull(response.getOutboundProperty("outbound2"));
        assertNotNull(response.<Object>getInvocationProperty("invocation1"));
        assertNotNull(response.<Object>getInvocationProperty("invocation2"));
        assertEquals("123", response.getInboundProperty("outbound3"));
        assertEquals("456", response.getInboundProperty("outbound4"));
        assertNull(response.<Object>getInvocationProperty("invocation3"));
        assertNull(response.<Object>getInvocationProperty("invocation4"));

        MuleMessage msg2 = createOutboundMessage();
        client.dispatch("vm://inQueue", msg2);
        Thread.sleep(1000);
        response = client.request("vm://outQueue", 0);
        assertEquals(response.getPayloadAsString(), "OK");
        assertEquals("yes", response.getInboundProperty("outbound1"));
        assertEquals("no", response.getInboundProperty("outbound2"));
        assertNull(response.getOutboundProperty("outbound1"));
        assertNull(response.getOutboundProperty("outbound2"));
        assertNull(response.<Object>getInvocationProperty("invocation1"));
        assertNull(response.<Object>getInvocationProperty("invocation2"));

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
         * Create a message with outbound and invocation properties.  These should have been moved to the inbound scope
         * by the time the message is received.  Invocation properties should have been removed
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
