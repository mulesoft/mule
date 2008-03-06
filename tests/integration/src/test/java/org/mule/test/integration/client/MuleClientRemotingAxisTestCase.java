/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.client;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.module.xml.transformers.XStreamWireFormat;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.services.Person;

public class MuleClientRemotingAxisTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/client/axis-client-test-mule-config.xml";
    }

    public void testRequestResponse() throws Throwable
    {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("remoteEndpoint");
        try
        {
            MuleMessage result = dispatcher.sendRemote(
                "axis:http://localhost:38104/mule/services/mycomponent2?method=echo", "test", null);
            assertNotNull(result);
            assertEquals("test", result.getPayloadAsString());
        }
        finally
        {
            client.dispose();
        }
    }

    public void testRequestResponseComplex() throws Exception
    {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("remoteEndpoint");
        dispatcher.setWireFormat(new XStreamWireFormat());

        try
        {
            MuleMessage result = dispatcher.sendRemote(
                "axis:http://localhost:38104/mule/services/mycomponent3?method=getPerson", "Fred", null);
            assertNotNull(result);
            logger.debug(result.getPayload());
            assertTrue(result.getPayload() instanceof Person);
            assertEquals("Fred", ((Person)result.getPayload()).getFirstName());
            assertEquals("Flintstone", ((Person)result.getPayload()).getLastName());
        }
        finally
        {
            client.dispose();
        }
    }

    public void testRequestResponseComplex2() throws Exception
    {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("remoteEndpoint");
        dispatcher.setWireFormat(new XStreamWireFormat());

        try
        {
            String[] args = new String[]{"Betty", "Rubble"};
            MuleMessage result = dispatcher.sendRemote(
                "axis:http://localhost:38104/mule/services/mycomponent3?method=addPerson", args, null);
            assertNotNull(result);
            assertTrue(result.getPayload() instanceof Person);
            assertEquals("Betty", ((Person)result.getPayload()).getFirstName());
            assertEquals("Rubble", ((Person)result.getPayload()).getLastName());

            // do a receive
            result = client.send("axis:http://localhost:38104/mule/services/mycomponent3?method=getPerson",
                "Betty", null);
            assertNotNull(result);
            assertTrue(result.getPayload() instanceof Person);
            assertEquals("Betty", ((Person)result.getPayload()).getFirstName());
            assertEquals("Rubble", ((Person)result.getPayload()).getLastName());
        }
        finally
        {
            client.dispose();
        }
    }
}
