/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;

public class AxisExceptionTestCase extends DynamicPortTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "axis-using-cxf-config.xml";
    }

    public void testSuccessCall() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("axis:http://localhost:" + getPorts().get(0) + "/services/AxisService?method=receive",
            new DefaultMuleMessage("test", muleContext));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof String);
        assertEquals("Received: test", reply.getPayloadAsString());
    }

    // TODO This test causes an infinite loop in the method org.apache.axis.encoding.SerializationContext.serialize()
    public void testExceptionCall() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("axis:http://localhost:" + getPorts().get(0) + "/services/AxisService?method=throwsException", new DefaultMuleMessage("test", muleContext));
            fail("should have thrown exception");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testExceptionBasedRoutingForAxis() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://localhost.test", new DefaultMuleMessage("test", muleContext));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof String);
        assertEquals("Received: test", reply.getPayloadAsString());
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
    }
}
