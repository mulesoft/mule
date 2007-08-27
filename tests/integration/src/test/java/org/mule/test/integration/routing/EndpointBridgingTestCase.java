/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class EndpointBridgingTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/bridge-mule.xml";
    }

    public void testSynchronousBridging() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("vm://bridge.inbound", "test", null);
        assertNotNull(result);
        assertEquals("Received: test", result.getPayloadAsString());
    }
}
