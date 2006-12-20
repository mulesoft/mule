/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.space;

import org.mule.extras.client.MuleClient;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.space.VMSpaceFactory;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

public class SpaceConnectorFunctionalTestCase extends AbstractProviderFunctionalTestCase
{

    protected void sendTestData(int iterations) throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("space:/test", "hello", null);
    }

    protected void receiveAndTestResults() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage message = client.receive("space:/test.out", 10000L);
        assertNotNull(message);
        assertEquals("hello Received", message.getPayloadAsString());
    }

    protected UMOEndpointURI getInDest()
    {
        try
        {
            return new MuleEndpointURI("space:/test");
        }
        catch (MalformedEndpointException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        try
        {
            return new MuleEndpointURI("space:/test.out");
        }
        catch (MalformedEndpointException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOConnector createConnector() throws Exception
    {
        SpaceConnector con = new SpaceConnector();
        con.setName("spaceConnector");
        con.setSpaceFactory(new VMSpaceFactory());
        return con;
    }
}
