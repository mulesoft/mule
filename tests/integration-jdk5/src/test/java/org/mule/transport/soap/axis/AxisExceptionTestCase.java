/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.services.TestComponent;
import org.mule.tck.testmodels.services.TestComponentException;

public class AxisExceptionTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "axis-using-cxf-config.xml";
    }

    public void testSuccessCall() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("axis:http://localhost:63381/services/AxisService?method=receive",
            new DefaultMuleMessage("test"));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof String);
        assertEquals("Received: test", reply.getPayloadAsString());
    }

    public void testExceptionCall() throws Exception
    {
        try
        {
            MuleClient client = new MuleClient();
            client.send("axis:http://localhost:63381/services/AxisService?method=throwsException",
                new DefaultMuleMessage("test"));

            fail("should have thrown exception");
        }
        catch (DispatchException dispatchExc)
        {
            Throwable t = dispatchExc.getCause().getCause();

            assertNotNull(t);
            assertEquals(TestComponentException.class.getName() + ": "
                         + TestComponentException.MESSAGE_PREFIX + TestComponent.EXCEPTION_MESSAGE,
                t.getMessage());
        }
    }

    public void testExceptionBasedRoutingForAxis() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("vm://localhost.test", new DefaultMuleMessage("test"));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof String);
        assertEquals("Received: test", reply.getPayloadAsString());
    }
}
