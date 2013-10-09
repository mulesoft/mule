/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.routing.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * The router looks up a list of endpoints from an XML file and passes them to the
 * <recipient-list-exception-based-router>
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
        MuleMessage message = muleContext.getClient().send("vm://routerBad", "GetID", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(CouldNotRouteOutboundMessageException.class, message.getExceptionPayload().getRootException().getClass());

    }
}
