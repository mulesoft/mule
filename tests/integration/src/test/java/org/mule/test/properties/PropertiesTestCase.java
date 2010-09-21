/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.properties;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.HashMap;
import java.util.Map;

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
    public void testProperties() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map<String, FunctionalTestComponent> components = muleContext.getRegistry().lookupByType(FunctionalTestComponent.class);
        Map properties = new HashMap();
        properties.put("outbound1", "yes");
        properties.put("outbound2", "no");
        MuleMessage response = client.send("vm://in", "OK", properties);
        assertEquals(response.getPayloadAsString(), "OK(success)");
        assertNull(response.getInboundProperty("outbound1"));
        assertNull(response.getInboundProperty("outbound2"));
        assertNull(response.getOutboundProperty("outbound1"));
        assertNull(response.getOutboundProperty("outbound2"));
        assertEquals("123", response.getInboundProperty("outbound3"));
        assertEquals("456", response.getInboundProperty("outbound4"));

        client.dispatch("vm://inQueue", "OK", properties);
        Thread.sleep(1000);
        response = client.request("vm://outQueue", 0);
        assertEquals(response.getPayloadAsString(), "OK");
        assertEquals("yes", response.getInboundProperty("outbound1"));
        assertEquals("no", response.getInboundProperty("outbound2"));
        assertNull(response.getOutboundProperty("outbound1"));
        assertNull(response.getOutboundProperty("outbound2"));
    }

    public static class Component
    {
        /**
         * Create a message with outbound properties.  These should have been moved to the inbound scope
         * by the time the message is received
         */
        public MuleMessage process(Object payload)
        {
            MuleMessage msg = new DefaultMuleMessage(payload + "(success)", muleContext);
            msg.setOutboundProperty("outbound3", "123");
            msg.setOutboundProperty("outbound4", "456");
            return msg;
        }
    }
}
