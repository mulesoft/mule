/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.services.TestComponent;
import org.mule.tck.testmodels.services.TestComponentException;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.DispatchException;

public class AxisExceptionTestCase extends FunctionalTestCase
{

    public AxisExceptionTestCase()
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "axis-using-xfire-config.xml";
    }

    public void testSuccessCall() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("axis:http://localhost:33381/services/AxisService?method=receive",
            new MuleMessage("test"));

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
            client.send("axis:http://localhost:33381/services/AxisService?method=throwsException",
                new MuleMessage("test"));

            fail("should have thrown exception");
        }
        catch (DispatchException dispatchExc)
        {
            Throwable t = dispatchExc.getCause();

            assertNotNull(t);
            assertEquals(TestComponentException.class.getName() + ": "
                         + TestComponentException.MESSAGE_PREFIX + TestComponent.EXCEPTION_MESSAGE,
                t.getMessage());
        }
    }

    public void testExceptionBasedRoutingForAxis() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("vm://localhost.test", new MuleMessage("test"));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof String);
        assertEquals("Received: test", reply.getPayloadAsString());
    }
}
