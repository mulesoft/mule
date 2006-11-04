/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs;

import org.mule.extras.client.MuleClient;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

public class GSConnectorFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    private String IN_URL = "gs:java://localhost/mule-space_container/mule-space?schema=cache";
    private String OUT_URL = "gs:java://localhost/mule-space2_container/mule-space2?schema=cache";

    // private String URL =
    // "jini:java://localhost/?address=/./mule-space?schema=cache";
    // private String URL = "jini:java://localhost:10098/ross-laptop/JavaSpaces";

    protected String checkPreReqs()
    {
        if (System.getProperty("com.gs.home", null) != null)
        {
            System.setProperty("com.gs.security.enabled", "false");
            System.setProperty("java.security.policy", System.getProperty("com.gs.home")
                                                       + "/policy/policy.all");
            return null;
        }
        return "com.gs.home VM parameter not set";
    }

    protected void sendTestData(int iterations) throws Exception
    {
        MuleClient client = new MuleClient();
        client.send(IN_URL, "hello", null);
    }

    protected void receiveAndTestResults() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage message = client.receive(OUT_URL, 10000L);
        assertNotNull(message);
        assertEquals("hello Received", message.getPayloadAsString());
    }

    protected UMOEndpointURI getInDest()
    {
        try
        {
            return new MuleEndpointURI(IN_URL);
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
            return new MuleEndpointURI(OUT_URL);
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
        GSConnector con = new GSConnector();
        con.setName("spaceConnector");
        return con;
    }
}
