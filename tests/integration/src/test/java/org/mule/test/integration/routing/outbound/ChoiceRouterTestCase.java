/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.outbound;

import org.mule.api.MuleException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ChoiceRouterTestCase extends FunctionalTestCase
{

    private MuleClient muleClient;

    @Override
    protected void doSetUp() throws Exception
    {
        super.setDisposeManagerPerSuite(true);
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/choice-router-test.xml";
    }

    public void testNoRouteFound()
    {
        try
        {
            muleClient.send("vm://no-default-route.in", "invalid", null);
            fail("should have got a MuleException");
        }
        catch (final MuleException me)
        {
            assertTrue(me.getCause() instanceof RoutePathNotFoundException);
        }
    }

    public void testRouteFound() throws Exception
    {
        final String result = muleClient.send("vm://no-default-route.in", "valid", null).getPayloadAsString();
        assertEquals("valid:echoed", result);
    }

}
