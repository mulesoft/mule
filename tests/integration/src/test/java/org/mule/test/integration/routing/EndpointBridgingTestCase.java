/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class EndpointBridgingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/bridge-mule-flow.xml";
    }

    @Test
    public void testSynchronousBridging() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://bridge.inbound", "test", null);
        assertNotNull(result);
        assertEquals("Received: test", result.getPayloadAsString());
    }
}
