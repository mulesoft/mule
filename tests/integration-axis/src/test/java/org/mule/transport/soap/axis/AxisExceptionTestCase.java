/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AxisExceptionTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");
    
    @Override
    protected String getConfigResources()
    {
        return "axis-using-cxf-config.xml";
    }

    @Test
    public void testSuccessCall() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("axis:http://localhost:" + dynamicPort1.getNumber() + "/services/AxisService?method=receive",
            new DefaultMuleMessage("test", muleContext));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof String);
        assertEquals("Received: test", reply.getPayloadAsString());
    }

    // TODO This test causes an infinite loop in the method org.apache.axis.encoding.SerializationContext.serialize()
    @Test
    public void testExceptionCall() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("axis:http://localhost:" + dynamicPort1.getNumber() + "/services/AxisService?method=throwsException", new DefaultMuleMessage("test", muleContext));
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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://localhost.test", new DefaultMuleMessage("test", muleContext));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof String);
        assertEquals("Received: test", reply.getPayloadAsString());
    }

}
