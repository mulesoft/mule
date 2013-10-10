/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.lookup;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testRouterSuccess() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://router", "GetID", null);
        assertNotNull(reply);
        assertTrue(reply.getPayloadAsString().contains("<ErrorStatus>Success</ErrorStatus>"));
    }

    @Test
    public void testRouterFailure() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://routerBad", "GetID", null);
        assertNotNull(reply);
        assertFalse(reply.getPayloadAsString().contains("<ErrorStatus>Success</ErrorStatus>"));
    }
}


