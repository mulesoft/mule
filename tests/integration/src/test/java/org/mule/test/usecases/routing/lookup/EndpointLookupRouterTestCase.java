/*
 * $Id$
 * --------------------------------------------------------------------------------------
 *
 * (c) 2003-2008 MuleSource, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSource's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSource. If such an agreement is not in place, you may not use the software.
 */

package org.mule.test.usecases.routing.lookup;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

/**
 * The router looks up a list of endpoints from an XML file and passes them to the <recipient-list-exception-based-router>
 */
public class EndpointLookupRouterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/routing/lookup/router-config.xml, org/mule/test/usecases/routing/lookup/services.xml";
    }

    public void testRouterSuccess() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("vm://router", "GetID", null);
        assertNotNull(reply);
        assertTrue(reply.getPayloadAsString().contains("<ErrorStatus>Success</ErrorStatus>"));
    }

    public void testRouterFailure() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("vm://routerBad", "GetID", null);
        assertNotNull(reply);
        assertFalse(reply.getPayloadAsString().contains("<ErrorStatus>Success</ErrorStatus>"));
    }
}


