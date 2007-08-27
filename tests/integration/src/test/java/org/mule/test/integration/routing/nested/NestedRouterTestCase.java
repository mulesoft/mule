/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.nested;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

public class NestedRouterTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/nested/nestedrouter-test.xml";
    }

    public void testNestedRouter() throws UMOException
    {
        MuleClient client = new MuleClient();
        String message = "Mule";
        client.dispatch("vm://invoker.in", message, null);
        UMOMessage reply = client.receive("vm://invoker.out", 10000);
        assertNotNull(reply);
        assertEquals("Received: Hello " + message + " " + 0xC0DE, reply.getPayload());
    }
}
