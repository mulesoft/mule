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

import java.util.HashMap;
import java.util.Map;

import org.mule.extras.client.MuleClient;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.gs.space.GSSpaceFactory;
import org.mule.providers.gs.transformers.UMOMessageToJavaSpaceEntry;
import org.mule.providers.space.SpaceConnector;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

public class GenericSpaceConnectorFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    private static final String IN_URL = "space:java://localhost/mule-space_container/mule-space?schema=cache";
    private static final String OUT_URL = "space:java://localhost/mule-space2_container/mule-space2?schema=cache";

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
        return "com.gs.home VM parameter not set.";
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
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOConnector createConnector() throws Exception
    {
        SpaceConnector con = new SpaceConnector();
        con.setName("spaceConnector");
        // Here we need to configure Gigaspace support manually
        con.setSpaceFactory(new GSSpaceFactory());
        con.registerSupportedProtocol("java");
        con.registerSupportedProtocol("rmi");
        Map serviceOverrides = new HashMap();
        serviceOverrides.put("message.adapter", JiniMessageAdapter.class.getName());
        serviceOverrides.put("outbound.transformer", UMOMessageToJavaSpaceEntry.class.getName());
        con.setServiceOverrides(serviceOverrides);
        return con;
    }

}
