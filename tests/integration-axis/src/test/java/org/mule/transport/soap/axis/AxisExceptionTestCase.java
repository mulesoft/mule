/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class AxisExceptionTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
    {
        return "axis-using-cxf-config.xml";
    }

    @Test
    public void testSuccessCall() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("axis:http://localhost:" + dynamicPort1.getNumber() + "/services/AxisService?method=receive",
            getTestMuleMessage());

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof String);
        assertEquals("Received: test", reply.getPayloadAsString());
    }

    // TODO This test causes an infinite loop in the method org.apache.axis.encoding.SerializationContext.serialize()
    @Test
    public void testExceptionCall() throws Exception
    {
        MuleClient client = muleContext.getClient();
        try
        {
            client.send("axis:http://localhost:" + dynamicPort1.getNumber() + "/services/AxisService?method=throwsException", getTestMuleMessage());
            fail("should have thrown exception");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testExceptionBasedRoutingForAxis() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("vm://localhost.test", getTestMuleMessage());

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof String);
        assertEquals("Received: test", reply.getPayloadAsString());
    }
}
